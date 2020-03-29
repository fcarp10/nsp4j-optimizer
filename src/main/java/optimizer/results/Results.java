package optimizer.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Server;
import manager.elements.Service;
import manager.elements.TrafficFlow;
import optimizer.gui.GraphData;
import optimizer.gui.Scenario;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static optimizer.Parameters.*;
import static optimizer.results.Auxiliary.printLog;

public class Results {
   private transient static final Logger log = LoggerFactory.getLogger(Results.class);
   // @JsonIgnore tag -> ignores specific variable for json result file
   // transient modifier -> ignores specific variable for posting optimizer.results to web UI.
   @JsonIgnore
   private transient Parameters pm;
   @JsonIgnore
   private transient int offset;
   @JsonIgnore
   private transient LinkedHashMap<String, Object> rawVariables;
   private transient Scenario sc;
   @JsonProperty("objective_value")
   private double objVal;
   @JsonProperty("computation_time")
   private double computationTime;
   @JsonProperty("avg_path_length")
   private double avgPathLength;
   @JsonProperty("total_traffic")
   private double totalTraffic;
   @JsonProperty("traffic_on_links")
   private double trafficLinks;
   @JsonProperty("synchronization_traffic")
   private double synchronizationTraffic;
   @JsonProperty("total_number_of_functions")
   private double totalNumberOfFunctions;
   @JsonProperty("migrations")
   private Integer[] migrations;
   @JsonProperty("replications")
   private Integer[] replications;
   @JsonProperty("lu_summary")
   private double[] luSummary;
   @JsonProperty("xu_summary")
   private double[] xuSummary;
   @JsonProperty("fp_summary")
   private double[] fpSummary;
   @JsonProperty("sd_summary")
   private double[] sdSummary;
   @JsonProperty("variables")
   private LinkedHashMap<String, List<String>> variables;
   @JsonProperty("xu")
   private transient List<Double> xu;
   @JsonProperty("lu")
   private transient List<Double> lu;
   @JsonProperty("fp")
   private transient List<Integer> fp;
   @JsonProperty("sd")
   private transient List<Double> sd;
   @JsonProperty("st")
   private transient List<Integer> st;
   @JsonProperty("ox")
   private transient List<Double> ox;
   @JsonProperty("osv")
   private transient List<Double> osv;
   @JsonProperty("qsdp")
   private transient List<Double> qsdp;
   @JsonProperty("fxsv")
   private transient List<Double> fxsv;
   @JsonIgnore
   private List<GraphData> luGraph;
   @JsonIgnore
   private List<GraphData> xuGraph;
   @JsonIgnore
   private List<GraphData> sdGraph;

   public Results() {
      variables = new LinkedHashMap<>();
      luGraph = new ArrayList<>();
      xuGraph = new ArrayList<>();
      sdGraph = new ArrayList<>();
   }

   public Results(Parameters pm, Scenario scenario) {
      this.pm = pm;
      this.sc = scenario;
      this.offset = (int) pm.getAux("offset_results");
      this.luSummary = new double[4];
      this.xuSummary = new double[4];
      this.fpSummary = new double[4];
      this.sdSummary = new double[4];
      this.luGraph = new ArrayList<>();
      this.xuGraph = new ArrayList<>();
      this.sdGraph = new ArrayList<>();
      this.rawVariables = new LinkedHashMap();
      this.variables = new LinkedHashMap<>();
      this.lu = new ArrayList<>();
      this.xu = new ArrayList<>();
      this.fp = new ArrayList<>();
      this.sd = new ArrayList<>();
      this.fxsv = new ArrayList<>();
   }

   public void setVariable(String key, Object variable) {
      rawVariables.put(key, variable);
   }

   public void initializeResults(double objVal, boolean[][][] initialPlacement) {
      // summary results
      migrations = countMigrations(initialPlacement);
      replications = countReplications();
      totalNumberOfFunctions = pm.getTotalNumFunctions();
      totalTraffic = calculateTotalTraffic();
      trafficLinks = Auxiliary.roundDouble(trafficOnLinks(), 2);
      avgPathLength = Auxiliary.roundDouble(avgPathLength(), 2);
      this.objVal = Auxiliary.roundDouble(objVal, 6);
      lu = new ArrayList<>(linkUtilizationMap().values());
      xu = new ArrayList<>(serverUtilizationMap().values());
      fp = numOfFunctionsPerServer();
      setSummaryResults(luSummary, lu);
      setSummaryResults(xuSummary, xu);
      setSummaryResults(fpSummary, fp);
      luGraph(lu);
      xuGraph(xu);

      // general variables
      zSP(); // binary, routing per path
      zSPD(); // binary, routing per demand
      fX(); // binary, used servers
      fXSV(); // binary, placement per server
      fXSVD(); // binary, placement per demand
      uX(); // link utilization
      uL(); // server utilization

      // model specific variables
      if (sc.getModel().equals(SERVER_DIMENSIONING))
         xN(); // integer, num servers per node
      if (sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
              || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {
         oX(); // opex per server
         oSV(); // function charges
         qSDP(); // qos penalties
      }

      // sync traffic variables
      if (sc.getConstraints().get(SYNC_TRAFFIC)) {
         gSVXY(); // binary, aux synchronization traffic
         hSVP(); // binary, traffic synchronization
         synchronizationTraffic = Auxiliary.roundDouble(synchronizationTraffic(), 2);
      }

      // service delay variables
      if (sc.getConstraints().get(MAX_SERV_DELAY) || sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
              || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {
         sd = serviceDelayList(initialPlacement);
         st = serviceTypes();
         setSummaryResults(sdSummary, sd);
         sdGraph(sd);
         dSVX(); // processing delay
         mS(); // migration delay
         dSVXD();
      }
   }

   private Integer[] countMigrations(boolean[][][] initialPlacement) {
      Map<Integer, Integer> migrationsMap = new HashMap<>();
      for (Function functionType : pm.getFunctionTypes())
         migrationsMap.put(functionType.getType(), 0);
      if (initialPlacement != null)
         try {
            boolean[][][] var = (boolean[][][]) rawVariables.get(fXSV);
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int s = 0; s < pm.getServices().size(); s++)
                  for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                     if (initialPlacement[x][s][v] && !var[x][s][v]) {
                        int functionType = pm.getServices().get(s).getFunctions().get(v).getType();
                        migrationsMap.put(functionType, migrationsMap.get(functionType) + 1);
                     }
         } catch (Exception e) {
            printLog(log, ERROR, e.getMessage());
         }
      List<Integer> values = new ArrayList<>(migrationsMap.values());
      return values.<Integer>toArray(new Integer[0]);
   }

   private Integer[] countReplications() {
      Map<Integer, Integer> replicationsMap = new HashMap<>();
      for (Function functionType : pm.getFunctionTypes())
         replicationsMap.put(functionType.getType(), 0);
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(fXSV);
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               int replicasTemp = -1;
               for (int x = 0; x < pm.getServers().size(); x++)
                  if (var[x][s][v])
                     replicasTemp++;
               int functionType = pm.getServices().get(s).getFunctions().get(v).getType();
               replicationsMap.put(functionType, replicationsMap.get(functionType) + replicasTemp);
            }
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      List<Integer> values = new ArrayList<>(replicationsMap.values());
      return values.<Integer>toArray(new Integer[0]);
   }

   private int calculateTotalTraffic() {
      int totalTraffic = 0;
      for (TrafficFlow trafficFlow : pm.getTrafficFlows())
         for (int d = 0; d < trafficFlow.getDemands().size(); d++)
            if (trafficFlow.getAux().get(d))
               totalTraffic += trafficFlow.getDemands().get(d);
      return totalTraffic;
   }

   public Map<Edge, Double> linkUtilizationMap() {
      Map<Edge, Double> linkMapResults = new HashMap<>();
      try {
         double[] var = (double[]) rawVariables.get(uL);
         for (int l = 0; l < pm.getLinks().size(); l++)
            if (pm.getLinks().get(l).getAttribute(LINK_CLOUD) == null)
               linkMapResults.put(pm.getLinks().get(l), Auxiliary.roundDouble(var[l], 4));
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      return linkMapResults;
   }

   public Map<Server, Double> serverUtilizationMap() {
      Map<Server, Double> serverMapResults = new HashMap<>();
      try {
         double[] var = (double[]) rawVariables.get(uX);
         for (int x = 0; x < pm.getServers().size(); x++)
            serverMapResults.put(pm.getServers().get(x), Auxiliary.roundDouble(var[x], 4));
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      return serverMapResults;
   }

   private List<Integer> numOfFunctionsPerServer() {
      List<Integer> numOfFunctionsPerServer = new ArrayList<>();
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(fXSV);
         for (int x = 0; x < pm.getServers().size(); x++) {
            int numOfFunctions = 0;
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (var[x][s][v]) {
                     numOfFunctions++;
                     fxsv.add(1.0);
                  } else fxsv.add(0.0);
            numOfFunctionsPerServer.add(numOfFunctions);
         }
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      return numOfFunctionsPerServer;
   }

   private List<Double> serviceDelayList(boolean[][][] initialPlacement) {
      List<Double> serviceDelayList = new ArrayList<>();
      boolean[][][] zSPDvar = (boolean[][][]) rawVariables.get(zSPD);
      double[][][] dSVXvar = (double[][][]) rawVariables.get(dSVX);
      boolean[][][][] fXSVDvar = (boolean[][][][]) rawVariables.get(fXSVD);
      boolean[][][] fXSVvar = (boolean[][][]) rawVariables.get(fXSV);
      List<String> strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service service = pm.getServices().get(s);
         for (int p = 0; p < service.getTrafficFlow().getPaths().size(); p++) {
            Path path = service.getTrafficFlow().getPaths().get(p);
            for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
               if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                  if (zSPDvar[s][p][d]) {

                     // add processing delay
                     double endToEndDelay = 0;
                     for (int n = 0; n < path.getNodePath().size(); n++)
                        for (int x = 0; x < pm.getServers().size(); x++)
                           if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
                              for (int v = 0; v < service.getFunctions().size(); v++)
                                 if (fXSVDvar[x][s][v][d])
                                    endToEndDelay += dSVXvar[s][v][x]; // in ms
                     if (endToEndDelay == 0) continue;

                     // add propagation delay
                     for (Edge link : path.getEdgePath())
                        endToEndDelay += (double) link.getAttribute(LINK_DELAY) * 1000; // in ms

                     // add migration delay
                     double maxMigrationDelay = 0;
                     for (int q = 0; q < service.getTrafficFlow().getPaths().size(); q++) {
                        Path pathMgr = service.getTrafficFlow().getPaths().get(q);
                        if (initialPlacement != null)
                           for (int n = 0; n < pathMgr.getNodePath().size(); n++)
                              for (int x = 0; x < pm.getServers().size(); x++)
                                 if (pm.getServers().get(x).getParent().equals(pathMgr.getNodePath().get(n)))
                                    for (int v = 0; v < service.getFunctions().size(); v++)
                                       if (initialPlacement[x][s][v] && !fXSVvar[x][s][v]) {
                                          double migrationDelay = (double) service.getFunctions().get(v).getAttribute(FUNCTION_MIGRATION_DELAY);
                                          if (migrationDelay > maxMigrationDelay)
                                             maxMigrationDelay = migrationDelay;
                                       }
                     }
                     endToEndDelay += maxMigrationDelay; // in ms

                     // print total end to end delay
                     endToEndDelay = Auxiliary.roundDouble(endToEndDelay, 3);
                     strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "," + (d + this.offset) + "): ["
                             + pm.getServices().get(s).getId() + "]"
                             + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath()
                             + "[" + endToEndDelay + "]");
                     serviceDelayList.add(endToEndDelay);
                  }
         }
      }
      variables.put(dSPD, strings);
      return serviceDelayList;
   }

   private List<Integer> serviceTypes() {
      List<Integer> serviceTypesList = new ArrayList<>();
      try {
         boolean[][][] var2 = (boolean[][][]) rawVariables.get(zSPD);
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                     if (var2[s][p][d])
                        serviceTypesList.add(pm.getServices().get(s).getId());
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      return serviceTypesList;
   }

   private double avgPathLength() {
      double avgPathLength = 0;
      try {
         boolean[][] var = (boolean[][]) rawVariables.get(zSP);
         int usedPaths = 0;
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               if (var[s][p]) {
                  avgPathLength += pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getEdgePath().size();
                  usedPaths++;
               }
         if (usedPaths != 0)
            avgPathLength = avgPathLength / usedPaths;
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      return avgPathLength;
   }

   private double trafficOnLinks() {
      double trafficOnLinks = 0;
      try {
         double[] var = (double[]) rawVariables.get(uL);
         for (int l = 0; l < pm.getLinks().size(); l++)
            trafficOnLinks += var[l] * (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      return trafficOnLinks;
   }

   private double synchronizationTraffic() {
      double synchronizationTraffic = 0;
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(hSVP);
         for (int l = 0; l < pm.getLinks().size(); l++) {
            for (int p = 0; p < pm.getPaths().size(); p++) {
               if (!pm.getPaths().get(p).contains(pm.getLinks().get(l)))
                  continue;
               for (int s = 0; s < pm.getServices().size(); s++) {
                  double traffic = 0;
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                        traffic += pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
                  for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                     double trafficScaled = traffic * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_SYNC_LOAD_RATIO);
                     if (var[s][v][p])
                        synchronizationTraffic += trafficScaled;
                  }
               }
            }
         }
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      return synchronizationTraffic;
   }


   /***************************************** GENERAL VARIABLES ************************************/
   private void zSP() {
      try {
         boolean[][] var = (boolean[][]) rawVariables.get(zSP);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               if (var[s][p])
                  strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "): ["
                          + pm.getServices().get(s).getId() + "]"
                          + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath());
         variables.put(zSP, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void zSPD() {
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(zSPD);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                     if (var[s][p][d])
                        strings.add("(" + (s + this.offset) + "," + (p + this.offset) + ","
                                + (d + this.offset) + "): ["
                                + pm.getServices().get(s).getId() + "]"
                                + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "["
                                + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
         variables.put(zSPD, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void fX() {
      try {
         boolean[] var = (boolean[]) rawVariables.get(fX);
         List<String> strings = new ArrayList<>();
         for (int x = 0; x < pm.getServers().size(); x++)
            if (var[x])
               strings.add("(" + (x + this.offset) + "): [" + pm.getServers().get(x).getId() + "]");
         variables.put(fX, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void fXSV() {
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(fXSV);
         List<String> strings = new ArrayList<>();
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (var[x][s][v])
                     strings.add("(" + (x + this.offset) + "," + (s + this.offset) + ","
                             + (v + this.offset) + "): ["
                             + pm.getServers().get(x).getId() + "]["
                             + pm.getServices().get(s).getId() + "]["
                             + pm.getServices().get(s).getFunctions().get(v).getType() + "]");
         variables.put(fXSV, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void fXSVD() {
      try {
         boolean[][][][] var = (boolean[][][][]) rawVariables.get(fXSVD);
         List<String> strings = new ArrayList<>();
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                        if (var[x][s][v][d])
                           strings.add("(" + (x + this.offset) + "," + (s + this.offset)
                                   + "," + (v + this.offset) + "," + (d + this.offset) + "): ["
                                   + pm.getServers().get(x).getId() + "]["
                                   + pm.getServices().get(s).getId() + "]["
                                   + pm.getServices().get(s).getFunctions().get(v).getType() + "]["
                                   + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
         variables.put(fXSVD, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void uX() {
      try {
         double[] var = (double[]) rawVariables.get(uX);
         List<String> strings = new ArrayList<>();
         for (int x = 0; x < pm.getServers().size(); x++)
            strings.add("(" + (x + this.offset) + "): ["
                    + pm.getServers().get(x).getId() + "]["
                    + Auxiliary.roundDouble(var[x], 3) + "]");
         variables.put(uX, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void uL() {
      try {
         double[] var = (double[]) rawVariables.get(uL);
         List<String> strings = new ArrayList<>();
         for (int l = 0; l < pm.getLinks().size(); l++)
            strings.add("(" + (l + this.offset) + "): ["
                    + pm.getLinks().get(l).getId() + "]["
                    + pm.getLinks().get(l).getAttribute(LINK_DISTANCE) + "]["
                    + pm.getLinks().get(l).getAttribute(LINK_DELAY) + "]["
                    + Auxiliary.roundDouble(var[l], 3) + "]["
                    + Auxiliary.roundDouble(var[l], 3)
                    * (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY) + "]");
         variables.put(uL, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   /**********************************************************************************************/

   /********************************** MODEL SPECIFIC VARIABLES **********************************/
   private void xN() {
      try {
         double[] var = (double[]) rawVariables.get(xN);
         List<String> strings = new ArrayList<>();
         for (int n = 0; n < pm.getNodes().size(); n++)
            if (var[n] > 0)
               strings.add("(" + (n + this.offset) + "): [" + pm.getNodes().get(n).getId() + "][" + var[n] + "]");
         variables.put(xN, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void oX() {
      try {
         ox = new ArrayList<>();
         double[] var = (double[]) rawVariables.get(oX);
         List<String> strings = new ArrayList<>();
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) == null) {
               strings.add("(" + (x + this.offset) + "): [" + pm.getServers().get(x).getId() + "][" + var[x] + "]");
               ox.add(var[x]);
            }
         variables.put(oX, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void oSV() {
      try {
         osv = new ArrayList<>();
         double[][] var = (double[][]) rawVariables.get(oSV);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               if (var[s][v] > 0) {
                  strings.add("(" + (s + this.offset) + "," + (v + this.offset) + "): [" + var[s][v] + "]");
                  osv.add(var[s][v]);
               }
         variables.put(oSV, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void qSDP() {
      try {
         qsdp = new ArrayList<>();
         double[][][] var = (double[][][]) rawVariables.get(qSDP);
         List<String> strings = new ArrayList<>();
         double[][][] varAux = (double[][][]) rawVariables.get(ySDP);
         List<String> stringsAux = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++) {
            Service service = pm.getServices().get(s);
            double profit = 0;
            double maxServiceDelay = 0;
            maxServiceDelay += service.getMaxPropagationDelay();
            for (int v = 0; v < service.getFunctions().size(); v++)
               maxServiceDelay += (double) service.getFunctions().get(v).getAttribute(FUNCTION_MAX_DELAY);

            for (int v = 0; v < service.getFunctions().size(); v++)
               profit += (double) service.getFunctions().get(v).getAttribute(FUNCTION_CHARGES);
            double qosPenalty = (double) pm.getAux().get(QOS_PENALTY_RATIO) * profit; // in $/h

            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                  for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
                     if (var[s][d][p] != 0) {
                        strings.add("(" + (s + this.offset) + "," + (d + this.offset) + "," + (p + this.offset)
                                + "): [" + var[s][d][p] + "]");
                        qsdp.add(var[s][d][p]);
                     }
                     if (varAux[s][d][p] != 0) {
                        stringsAux.add("(" + (s + this.offset) + "," + (d + this.offset) + "," + (p + this.offset)
                                + "): [" + (varAux[s][d][p]) + "][" + ((varAux[s][d][p] / (maxServiceDelay) - 1) * qosPenalty) + "][" + maxServiceDelay + "]");
                     }
                  }
         }
         variables.put(qSDP, strings);
         variables.put(ySDP, stringsAux);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }


   /************************************* TRAFFIC SYNC VARIABLES **********************************/
   private void gSVXY() {
      try {
         boolean[][][][] var = (boolean[][][][]) rawVariables.get(gSVXY);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int x = 0; x < pm.getServers().size(); x++)
                  for (int y = 0; y < pm.getServers().size(); y++)
                     if (!pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent()))
                        if (var[s][v][x][y])
                           strings.add("(" + (s + this.offset) + "," + (v + this.offset)
                                   + "," + (x + this.offset) + "," + (y + this.offset) + "): ["
                                   + pm.getServers().get(x).getId() + "][" + pm.getServers().get(y).getId() + "]");
         variables.put(gSVXY, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void hSVP() {
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(hSVP);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int p = 0; p < pm.getPaths().size(); p++)
                  if (var[s][v][p])
                     strings.add("(" + (s + this.offset) + "," + (v + this.offset) + "," + (p + this.offset) + "): "
                             + pm.getPaths().get(p).getNodePath());
         variables.put(hSVP, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }
   /**********************************************************************************************/

   /*********************************** SERVICE DELAY VARIABLES **********************************/
   private void dSVX() {
      try {
         double[][][] processDelayVar = (double[][][]) rawVariables.get(dSVX);
         boolean[][][][] placementVar = (boolean[][][][]) rawVariables.get(fXSVD);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int x = 0; x < pm.getServers().size(); x++) {
                  boolean isUsed = false;
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                        if (placementVar[x][s][v][d]) isUsed = true;
                  if (processDelayVar[s][v][x] > 0 && isUsed)
                     strings.add("(" + (s + this.offset) + "," + (v + this.offset) + "," + (x + this.offset) + "): ["
                             + pm.getServers().get(x).getId() + "]["
                             + Auxiliary.roundDouble(processDelayVar[s][v][x], 2) + "]");
               }
         variables.put(dSVX, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void mS() {
      try {
         double[] migrationDelayVar = (double[]) rawVariables.get(mS);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            if (migrationDelayVar[s] > 0)
               strings.add("(" + (s + this.offset) + "): " + "[" + migrationDelayVar[s] + "]");
         variables.put(mS, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   private void dSVXD() {
      try {
         double[][][][] var = (double[][][][]) rawVariables.get(dSVXD);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int x = 0; x < pm.getServers().size(); x++)
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     if (var[s][v][x][d] > 0)
                        strings.add("(" + (s + this.offset) + "," + (v + this.offset) + "," + (x + this.offset) + ","
                                + (d + this.offset) + "): " + "[" + var[s][v][x][d] + "]");
         variables.put(dSVXD, strings);
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
   }

   /**********************************************************************************************/

   private void setSummaryResults(double[] array, List var) {
      array[0] = Auxiliary.avg(new ArrayList<>(var));
      array[1] = Auxiliary.min(new ArrayList<>(var));
      array[2] = Auxiliary.max(new ArrayList<>(var));
      array[3] = Auxiliary.vrc(new ArrayList<>(var), array[0]);
   }

   private void luGraph(List<Double> uL) {
      int xPoints = 10;
      for (int i = 0; i < xPoints; i++)
         luGraph.add(new GraphData("0." + i, 0));
      for (Double anUL : uL)
         for (int j = 0; j < xPoints; j++)
            if (anUL * 10 < j + 1 && anUL * 10 >= j) {
               luGraph.get(j).setValue(luGraph.get(j).getValue() + 1);
               break;
            }
   }

   private void xuGraph(List<Double> uX) {
      int xPoints = 10;
      for (int i = 0; i < xPoints; i++)
         xuGraph.add(new GraphData("0." + i, 0));
      for (Double anUX : uX)
         for (int j = 0; j < xPoints; j++)
            if (anUX * 10 < j + 1 && anUX * 10 >= j) {
               xuGraph.get(j).setValue(xuGraph.get(j).getValue() + 1);
               break;
            }
   }

   private void sdGraph(List<Double> sd) {
      int xPoints = 10;
      double min = Auxiliary.min(new ArrayList<>(sd));
      double max = Auxiliary.max(new ArrayList<>(sd));
      double step = Auxiliary.roundDouble((max - min) / xPoints, 4);
      if (max != min) {
         for (int i = 0; i < xPoints + 1; i++)
            sdGraph.add(new GraphData(String.valueOf(
                    Auxiliary.roundDouble((step * i) + min, 4)), 0));
         for (Double anSd : sd)
            for (int j = 0; j < xPoints + 1; j++)
               if (anSd < Double.parseDouble(sdGraph.get(j).getYear()) + step
                       && anSd >= Double.parseDouble(sdGraph.get(j).getYear())) {
                  sdGraph.get(j).setValue(sdGraph.get(j).getValue() + 1);
                  break;
               }
      } else sdGraph.add(new GraphData(String.valueOf(max), sd.size()));
   }

   public Parameters getPm() {
      return pm;
   }

   public double getObjVal() {
      return objVal;
   }

   public double getComputationTime() {
      return computationTime;
   }

   public void setComputationTime(double computationTime) {
      this.computationTime = computationTime;
   }

   public Scenario getScenario() {
      return sc;
   }

   public Map<String, Object> getRawVariables() {
      return rawVariables;
   }

   public Map<String, List<String>> getVariables() {
      return variables;
   }

   public double[] getLuSummary() {
      return luSummary;
   }

   public double[] getXuSummary() {
      return xuSummary;
   }

   public double[] getFpSummary() {
      return fpSummary;
   }

   public double[] getSdSummary() {
      return sdSummary;
   }

   public double getAvgPathLength() {
      return avgPathLength;
   }

   public double getTotalTraffic() {
      return totalTraffic;
   }

   public double getTrafficLinks() {
      return trafficLinks;
   }

   public double getSynchronizationTraffic() {
      return synchronizationTraffic;
   }

   public Integer[] getMigrations() {
      return migrations;
   }

   public Integer[] getReplications() {
      return replications;
   }

   public double getTotalNumberOfFunctions() {
      return totalNumberOfFunctions;
   }

   public List<GraphData> getLuGraph() {
      return luGraph;
   }

   public List<GraphData> getXuGraph() {
      return xuGraph;
   }

   public List<GraphData> getSdGraph() {
      return sdGraph;
   }

   public List<Double> getLu() {
      return lu;
   }

   public List<Double> getXu() {
      return xu;
   }

   public List<Integer> getFp() {
      return fp;
   }

   public List<Double> getSd() {
      return sd;
   }

   public List<Integer> getSt() {
      return st;
   }

   public List<Double> getOx() {
      return ox;
   }

   public List<Double> getOsv() {
      return osv;
   }

   public List<Double> getQsdp() {
      return qsdp;
   }

   public List<Double> getFxsv() {
      return fxsv;
   }
}
