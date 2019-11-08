package optimizer.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import optimizer.gui.GraphData;
import optimizer.gui.Scenario;
import manager.Parameters;
import manager.elements.Server;
import manager.elements.Service;
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
   private transient Scenario scenario;
   @JsonProperty("objective_value")
   private double objVal;
   @JsonProperty("avg_path_length")
   private double avgPathLength;
   @JsonProperty("total_traffic")
   private double totalTraffic;
   @JsonProperty("traffic_on_links")
   private double trafficLinks;
   @JsonProperty("synchronization_traffic")
   private double synchronizationTraffic;
   @JsonProperty("migrations")
   private int migrations;
   @JsonProperty("replications")
   private int replications;
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
      this.scenario = scenario;
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
   }

   public void setVariable(String key, Object variable) {
      rawVariables.put(key, variable);
   }

   public void initializeResults(double objVal, boolean[][][] initialPlacement) {
      migrations = countNumOfMigrations(initialPlacement);
      replications = countNumOfReplications();
      totalTraffic = pm.getTotalTraffic();
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
      uX(); // link utilization
      uL(); // server utilization
      if (scenario.getModel().equals(SERVER_DIMENSIONING))
         xN(); // integer, num servers per node
      if (scenario.getObjFunc().equals(NUM_SERVERS_OBJ)
              || scenario.getObjFunc().equals(NUM_SERVERS_UTIL_COSTS_OBJ))
         fX(); // binary, used servers
      zSP(); // binary, routing per path
      zSPD(); // binary, routing per demand
      fXSV(); // binary, placement per server
      fXSVD(); // binary, placement per demand
      if (scenario.getConstraints().get(SYNC_TRAFFIC)) {
         gSVXY(); // binary, aux synchronization traffic
         hSVP(); // binary, traffic synchronization
         synchronizationTraffic = Auxiliary.roundDouble(synchronizationTraffic(), 2);
      }
      if (scenario.getConstraints().get(SERV_DELAY)) {
         sd = serviceDelayList(initialPlacement);
         st = serviceTypes();
         setSummaryResults(sdSummary, sd);
         sdGraph(sd);
         dSVX(); // processing delay
         mS(); // migration delay
      }
   }

   private int countNumOfMigrations(boolean[][][] initialPlacement) {
      int totalMigrations = 0;
      if (initialPlacement != null)
         try {
            boolean[][][] var = (boolean[][][]) rawVariables.get(fXSV);
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int s = 0; s < pm.getServices().size(); s++)
                  for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                     if (initialPlacement[x][s][v] && !var[x][s][v])
                        totalMigrations++;
         } catch (Exception e) {
            printLog(log, ERROR, e.getMessage());
         }
      return totalMigrations;
   }

   private int countNumOfReplications() {
      int totalReplicas = 0;
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(fXSV);
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               int replicasPerFunction = -1;
               for (int x = 0; x < pm.getServers().size(); x++)
                  if (var[x][s][v])
                     replicasPerFunction++;
               totalReplicas += replicasPerFunction;
            }
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      return totalReplicas;
   }

   public Map<Edge, Double> linkUtilizationMap() {
      Map<Edge, Double> linkMapResults = new HashMap<>();
      try {
         double[] var = (double[]) rawVariables.get(uL);
         for (int l = 0; l < pm.getLinks().size(); l++)
            if (pm.getLinks().get(l).getAttribute(LINK_CLOUD) == null)
               linkMapResults.put(pm.getLinks().get(l), Auxiliary.roundDouble(var[l], 2));
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
            serverMapResults.put(pm.getServers().get(x), Auxiliary.roundDouble(var[x], 2));
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
                  if (var[x][s][v])
                     numOfFunctions++;
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
                                    endToEndDelay += dSVXvar[s][v][x];
                     if (endToEndDelay == 0) continue;

                     // add propagation delay
                     List<Edge> links = service.getTrafficFlow().getPaths().get(p).getEdgePath();
                     for (Edge link : links) endToEndDelay += (double) link.getAttribute(LINK_DELAY);

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
                     endToEndDelay += maxMigrationDelay;

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
      double step = Auxiliary.roundDouble((max - min) / xPoints, 2);
      if (max != min) {
         for (int i = 0; i < xPoints + 1; i++)
            sdGraph.add(new GraphData(String.valueOf(
                    Auxiliary.roundDouble((step * i) + min, 2)), 0));
         for (Double anSd : sd)
            for (int j = 0; j < xPoints + 1; j++)
               if (anSd < Double.valueOf(sdGraph.get(j).getYear()) + step
                       && anSd >= Double.valueOf(sdGraph.get(j).getYear())) {
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

   public Scenario getScenario() {
      return scenario;
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

   public int getMigrations() {
      return migrations;
   }

   public int getReplications() {
      return replications;
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
}