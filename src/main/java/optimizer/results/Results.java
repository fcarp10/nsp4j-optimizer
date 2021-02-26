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

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class Results {
   private transient static final Logger log = LoggerFactory.getLogger(Results.class);
   // @JsonIgnore tag -> ignores specific variable for json result file
   // transient modifier -> ignores specific variable for posting optimizer.results
   // to web UI.
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
   @JsonProperty("sd")
   private transient List<Double> sd;
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
      this.rawVariables = new LinkedHashMap<>();
      this.variables = new LinkedHashMap<>();
      this.lu = new ArrayList<>();
      this.xu = new ArrayList<>();
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
      setSummaryResults(luSummary, lu);
      setSummaryResults(xuSummary, xu);
      luGraph(lu);
      xuGraph(xu);
      Auxiliary.printLog(log, INFO, "summary results completed");

      // general variables
      zSP(); // binary, routing per path
      zSPD(); // binary, routing per demand
      fX(); // binary, used servers
      fXSV(); // binary, placement per server
      fXSVD(); // binary, placement per demand
      uX(); // link utilization
      uL(); // server utilization
      Auxiliary.printLog(log, INFO, "general variables results completed");

      // model specific variables
      if (sc.getObjFunc().equals(DIMEN))
         xN(); // integer, num servers per node
      if (sc.getObjFunc().equals(OPEX_SERVERS) || sc.getObjFunc().equals(FUNCTIONS_CHARGES)
            || sc.getObjFunc().equals(QOS_PENALTIES) || sc.getObjFunc().equals(ALL_MONETARY_COSTS)) {
         oX(); // opex per server
         oSV(); // function charges
         qSDP(); // qos penalties
      }

      // sync traffic variables
      if (sc.getConstraints().get(SYNC_TRAFFIC)) {
         gSVXY(); // binary, aux synchronization traffic
         hSVP(); // binary, traffic synchronization
         synchronizationTraffic = Auxiliary.roundDouble(synchronizationTraffic(), 2);
         Auxiliary.printLog(log, INFO, "synchronization traffic results completed");
      }

      sd = serviceDelayList(initialPlacement);
      setSummaryResults(sdSummary, sd);
      sdGraph(sd);
      dSVXD();
      Auxiliary.printLog(log, INFO, "service delay results completed");
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
            printLog(log, ERROR, "counting migrations: " + e.getMessage());
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
         printLog(log, ERROR, "counting replications: " + e.getMessage());
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
         printLog(log, ERROR, "link utilization results: " + e.getMessage());
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
         printLog(log, ERROR, "server utilization results: " + e.getMessage());
      }
      return serverMapResults;
   }

   private List<Double> serviceDelayList(boolean[][][] initialPlacement) {
      List<Double> serviceDelayList = new ArrayList<>();
      boolean[][][] zSPDvar = (boolean[][][]) rawVariables.get(zSPD);
      boolean[][][][] fXSVDvar = (boolean[][][][]) rawVariables.get(fXSVD);
      boolean[][][] fXSVvar = (boolean[][][]) rawVariables.get(fXSV);
      double[] uXvar = (double[]) rawVariables.get(uX);
      List<String> strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service service = pm.getServices().get(s);
         for (int p = 0; p < service.getTrafficFlow().getPaths().size(); p++) {
            Path path = service.getTrafficFlow().getPaths().get(p);
            for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
               if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                  if (zSPDvar[s][p][d]) {
                     double serviceDelay = 0;

                     // add processing delay
                     for (int n = 0; n < path.getNodePath().size(); n++)
                        for (int x = 0; x < pm.getServers().size(); x++)
                           if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
                              for (int v = 0; v < service.getFunctions().size(); v++) {
                                 if (fXSVDvar[x][s][v][d]) {
                                    Function function = service.getFunctions().get(v);
                                    double ratio = (double) function.getAttribute(FUNCTION_LOAD_RATIO)
                                          * (double) function.getAttribute(FUNCTION_PROCESS_TRAFFIC_DELAY)
                                          / ((int) function.getAttribute(FUNCTION_MAX_DEM)
                                                * (int) function.getAttribute(FUNCTION_MAX_BW)
                                                * (double) function.getAttribute(FUNCTION_LOAD_RATIO));
                                    double processingDelay = 0;
                                    for (int d1 = 0; d1 < service.getTrafficFlow().getDemands().size(); d1++)
                                       if (service.getTrafficFlow().getAux().get(d1))
                                          if (fXSVDvar[x][s][v][d1])
                                             processingDelay += ratio * service.getTrafficFlow().getDemands().get(d1);
                                    processingDelay += (double) function.getAttribute(FUNCTION_MIN_PROCESS_DELAY);
                                    processingDelay += (double) function.getAttribute(FUNCTION_PROCESS_DELAY)
                                          * uXvar[x];
                                    serviceDelay += processingDelay;
                                 }
                              }

                     // add propagation delay
                     for (Edge link : path.getEdgePath())
                        serviceDelay += (double) link.getAttribute(LINK_DELAY) * 1000; // in ms

                     // add service downtime
                     if (initialPlacement != null)
                        for (int x = 0; x < pm.getServers().size(); x++)
                           for (int v = 0; v < service.getFunctions().size(); v++)
                              if (initialPlacement[x][s][v] && !fXSVvar[x][s][v])
                                 serviceDelay += (double) service.getAttribute(SERVICE_DOWNTIME); // in ms

                     // print total end to end delay
                     serviceDelay = Auxiliary.roundDouble(serviceDelay, 3);
                     strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "," + (d + this.offset) + "): ["
                           + pm.getServices().get(s).getId() + "]"
                           + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "["
                           + serviceDelay + "]");
                     serviceDelayList.add(serviceDelay);
                  }
         }
      }
      variables.put(dSPD, strings);
      return serviceDelayList;
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
         printLog(log, ERROR, "calculating average path length: " + e.getMessage());
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
         printLog(log, ERROR, "calculating traffic on links: " + e.getMessage());
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
                     double trafficScaled = traffic * (double) pm.getServices().get(s).getFunctions().get(v)
                           .getAttribute(FUNCTION_SYNC_LOAD_RATIO);
                     if (var[s][v][p])
                        synchronizationTraffic += trafficScaled;
                  }
               }
            }
         }
      } catch (Exception e) {
         printLog(log, ERROR, "calculating synchronization traffic: " + e.getMessage());
      }
      return synchronizationTraffic;
   }

   /*****************************************
    * GENERAL VARIABLES
    ************************************/
   private void zSP() {
      try {
         boolean[][] var = (boolean[][]) rawVariables.get(zSP);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               if (var[s][p])
                  strings.add(
                        "(" + (s + this.offset) + "," + (p + this.offset) + "): [" + pm.getServices().get(s).getId()
                              + "]" + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath());
         variables.put(zSP, strings);
      } catch (Exception e) {
         printLog(log, ERROR, zSP + " var results: " + e.getMessage());
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
                        strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "," + (d + this.offset) + "): ["
                              + pm.getServices().get(s).getId() + "]"
                              + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "["
                              + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
         variables.put(zSPD, strings);
      } catch (Exception e) {
         printLog(log, ERROR, zSPD + " var results: " + e.getMessage());
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
         printLog(log, ERROR, fX + " var results: " + e.getMessage());
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
                     strings.add("(" + (x + this.offset) + "," + (s + this.offset) + "," + (v + this.offset) + "): ["
                           + pm.getServers().get(x).getId() + "][" + pm.getServices().get(s).getId() + "]["
                           + pm.getServices().get(s).getFunctions().get(v).getType() + "]");
         variables.put(fXSV, strings);
      } catch (Exception e) {
         printLog(log, ERROR, fXSV + " var results: " + e.getMessage());
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
                           strings.add("(" + (x + this.offset) + "," + (s + this.offset) + "," + (v + this.offset) + ","
                                 + (d + this.offset) + "): [" + pm.getServers().get(x).getId() + "]["
                                 + pm.getServices().get(s).getId() + "]["
                                 + pm.getServices().get(s).getFunctions().get(v).getType() + "]["
                                 + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
         variables.put(fXSVD, strings);
      } catch (Exception e) {
         printLog(log, ERROR, fXSVD + " var results: " + e.getMessage());
      }
   }

   private void uX() {
      try {
         double[] var = (double[]) rawVariables.get(uX);
         List<String> strings = new ArrayList<>();
         for (int x = 0; x < pm.getServers().size(); x++)
            strings.add("(" + (x + this.offset) + "): [" + pm.getServers().get(x).getId() + "]["
                  + Auxiliary.roundDouble(var[x], 3) + "]");
         variables.put(uX, strings);
      } catch (Exception e) {
         printLog(log, ERROR, uX + " var results: " + e.getMessage());
      }
   }

   private void uL() {
      try {
         double[] var = (double[]) rawVariables.get(uL);
         List<String> strings = new ArrayList<>();
         for (int l = 0; l < pm.getLinks().size(); l++)
            strings.add("(" + (l + this.offset) + "): [" + pm.getLinks().get(l).getId() + "]["
                  + pm.getLinks().get(l).getAttribute(LINK_DISTANCE) + "]["
                  + pm.getLinks().get(l).getAttribute(LINK_DELAY) + "][" + Auxiliary.roundDouble(var[l], 3) + "]["
                  + Auxiliary.roundDouble(var[l], 3) * (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY) + "]");
         variables.put(uL, strings);
      } catch (Exception e) {
         printLog(log, ERROR, uL + " var results: " + e.getMessage());
      }
   }

   /**********************************************************************************************/

   /**********************************
    * MODEL SPECIFIC VARIABLES
    **********************************/
   private void xN() {
      try {
         double[] var = (double[]) rawVariables.get(xN);
         List<String> strings = new ArrayList<>();
         for (int n = 0; n < pm.getNodes().size(); n++)
            if (var[n] > 0)
               strings.add("(" + (n + this.offset) + "): [" + pm.getNodes().get(n).getId() + "][" + var[n] + "]");
         variables.put(xN, strings);
      } catch (Exception e) {
         printLog(log, WARNING, xN + " var results: " + e.getMessage());
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
         printLog(log, WARNING, oX + " var results: " + e.getMessage());
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
         printLog(log, WARNING, oSV + " var results: " + e.getMessage());
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
                        strings.add("(" + (s + this.offset) + "," + (d + this.offset) + "," + (p + this.offset) + "): ["
                              + var[s][d][p] + "]");
                        qsdp.add(var[s][d][p]);
                     }
                     if (varAux != null)
                        if (varAux[s][d][p] != 0)
                           stringsAux.add("(" + (s + this.offset) + "," + (d + this.offset) + "," + (p + this.offset)
                                 + "): [" + (varAux[s][d][p]) + "]["
                                 + ((varAux[s][d][p] / (maxServiceDelay) - 1) * qosPenalty) + "][" + maxServiceDelay
                                 + "]");
                  }
         }
         variables.put(qSDP, strings);
         variables.put(ySDP, stringsAux);
      } catch (Exception e) {
         printLog(log, WARNING, qSDP + " var results: " + e.getMessage());
      }
   }

   /*************************************
    * TRAFFIC SYNC VARIABLES
    **********************************/
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
                           strings.add("(" + (s + this.offset) + "," + (v + this.offset) + "," + (x + this.offset) + ","
                                 + (y + this.offset) + "): [" + pm.getServers().get(x).getId() + "]["
                                 + pm.getServers().get(y).getId() + "]");
         variables.put(gSVXY, strings);
      } catch (Exception e) {
         printLog(log, WARNING, gSVXY + " var results: " + e.getMessage());
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
         printLog(log, WARNING, hSVP + " var results: " + e.getMessage());
      }
   }

   /**********************************************************************************************/

   /***********************************
    * SERVICE DELAY VARIABLES
    **********************************/
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
         // printLog(log, WARNING, dSVXD + " var results: " + e.getMessage());
      }
   }

   /**********************************************************************************************/

   private void setSummaryResults(double[] array, List<Double> var) {
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
            sdGraph.add(new GraphData(String.valueOf(Auxiliary.roundDouble((step * i) + min, 4)), 0));
         for (Double anSd : sd)
            for (int j = 0; j < xPoints + 1; j++)
               if (anSd < Double.parseDouble(sdGraph.get(j).getYear()) + step
                     && anSd >= Double.parseDouble(sdGraph.get(j).getYear())) {
                  sdGraph.get(j).setValue(sdGraph.get(j).getValue() + 1);
                  break;
               }
      } else
         sdGraph.add(new GraphData(String.valueOf(max), sd.size()));
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

   public List<Double> getSd() {
      return sd;
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
