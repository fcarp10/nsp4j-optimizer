package output;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gui.elements.GraphData;
import gui.elements.Scenario;
import manager.Parameters;
import manager.elements.Server;
import org.graphstream.graph.Edge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static output.Definitions.*;

public class Results {

   // @JsonIgnore tag -> ignores specific variable for json result file
   // transient modifier -> ignores specific variable for posting results to web UI.
   // scenario
   @JsonIgnore
   private transient Parameters pm;
   @JsonIgnore
   private transient int offset;
   @JsonIgnore
   private transient Map<String, Object> rawVariables;
   // for printing
   private transient Scenario scenario;
   // summary
   private double[] luSummary;
   private double[] xuSummary;
   private double[] fuSummary;
   private double[] sdSummary;
   private double avgPathLength;
   private double totalTraffic;
   private double trafficLinks;
   private int migrationsNum;
   private int replicationsNum;
   private double objVal;
   // variables
   private Map<String, List<String>> stringVariables;
   // graphs
   private List<GraphData> luGraph;
   private List<GraphData> xuGraph;
   private List<GraphData> sdGraph;

   public Results() {
      stringVariables = new HashMap<>();
      luGraph = new ArrayList<>();
      xuGraph = new ArrayList<>();
      sdGraph = new ArrayList<>();
   }

   public Results(Parameters pm, Scenario scenario) {
      this.pm = pm;
      this.scenario = scenario;
      this.offset = (int) pm.getAux("offset_results");
      luSummary = new double[4];
      xuSummary = new double[4];
      fuSummary = new double[4];
      sdSummary = new double[4];
      luGraph = new ArrayList<>();
      xuGraph = new ArrayList<>();
      sdGraph = new ArrayList<>();
      rawVariables = new HashMap();
      stringVariables = new HashMap<>();
   }

   public void setVariable(String key, Object variable) {
      rawVariables.put(key, variable);
   }

   public void initializeResults(double objVal, boolean[][][] initialPlacement, boolean additionalVariables) {
      List<Double> uL = new ArrayList<>(linkUtilizationMap().values());
      List<Double> uX = new ArrayList<>(serverUtilizationMap().values());
      if (initialPlacement != null)
         this.migrationsNum = countNumOfMigrations(initialPlacement);
      this.replicationsNum = countNumOfReplications();
      convertVariables(additionalVariables);
      setSummaryResults(luSummary, uL);
      setSummaryResults(xuSummary, uX);
      setSummaryResults(fuSummary, numOfFunctionsPerServer());
      List<Double> serviceDelayList = serviceDelayList();
      setSummaryResults(sdSummary, serviceDelayList);
      generateLuGraph(uL);
      generateXuGraph(uX);
      generateSdGraph(serviceDelayList);
      totalTraffic = pm.getTotalTraffic();
      trafficLinks = Auxiliary.roundDouble(trafficOnLinks(), 2);
      avgPathLength = Auxiliary.roundDouble(avgPathLength(), 2);
      this.objVal = Auxiliary.roundDouble(objVal, 4);
   }

   private int countNumOfMigrations(boolean[][][] initialPlacement) {
      int totalMigrations = 0;
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(fXSV);
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (initialPlacement[x][s][v] && !var[x][s][v])
                     totalMigrations++;
      } catch (Exception ignored) {
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
      } catch (Exception ignored) {
      }
      return totalReplicas;
   }

   public Map<Edge, Double> linkUtilizationMap() {
      Map<Edge, Double> linkMapResults = new HashMap<>();
      try {
         double[] var = (double[]) rawVariables.get(uL);
         for (int l = 0; l < pm.getLinks().size(); l++)
            linkMapResults.put(pm.getLinks().get(l), Auxiliary.roundDouble(var[l], 2));
      } catch (Exception ignored) {
      }
      return linkMapResults;
   }

   public Map<Server, Double> serverUtilizationMap() {
      Map<Server, Double> serverMapResults = new HashMap<>();
      try {
         double[] var = (double[]) rawVariables.get(uX);
         for (int x = 0; x < pm.getServers().size(); x++)
            serverMapResults.put(pm.getServers().get(x), Auxiliary.roundDouble(var[x], 2));
      } catch (Exception ignored) {
      }
      return serverMapResults;
   }

   List<Integer> numOfFunctionsPerServer() {
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
      } catch (Exception ignored) {
      }
      return numOfFunctionsPerServer;
   }

   List<Double> serviceDelayList() {
      List<Double> serviceDelayList = new ArrayList<>();
      try {
         double[][][] var = (double[][][]) rawVariables.get(dSPD);
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  serviceDelayList.add(var[s][p][d]);
      } catch (Exception ignored) {
      }
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
      } catch (Exception ignored) {
      }
      return avgPathLength;
   }

   private double trafficOnLinks() {
      double trafficOnLinks = 0;
      try {
         double[] var = (double[]) rawVariables.get(uL);
         for (int l = 0; l < pm.getLinks().size(); l++)
            trafficOnLinks += var[l] * (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY);
      } catch (Exception ignored) {
      }
      return trafficOnLinks;
   }

   private void convertVariables(boolean additional) {
      convertzSP();
      convertzSPD();
      convertfXSV();
      convertfXSVD();
      convertuX();
      convertuL();
      if (additional) {
         convertfX();
         convertgSVXY();
         convertsSVP();
         convertdSP();
         convertqSVXP();
      }
   }

   private void convertzSP() {
      try {
         boolean[][] var = (boolean[][]) rawVariables.get(zSP);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               if (var[s][p])
                  strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "): ["
                          + pm.getServices().get(s).getId() + "]"
                          + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath());
         stringVariables.put(zSP, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertzSPD() {
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(zSPD);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (var[s][p][d])
                     strings.add("(" + (s + this.offset) + "," + (p + this.offset) + ","
                             + (d + this.offset) + "): ["
                             + pm.getServices().get(s).getId() + "]"
                             + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "["
                             + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
         stringVariables.put(zSPD, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertfXSV() {
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
         stringVariables.put(fXSV, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertfXSVD() {
      try {
         boolean[][][][] var = (boolean[][][][]) rawVariables.get(fXSVD);
         List<String> strings = new ArrayList<>();
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     if (var[x][s][v][d])
                        strings.add("(" + (x + this.offset) + "," + (s + this.offset)
                                + "," + (v + this.offset) + "," + (d + this.offset) + "): ["
                                + pm.getServers().get(x).getId() + "]["
                                + pm.getServices().get(s).getId() + "]["
                                + pm.getServices().get(s).getFunctions().get(v).getType() + "]["
                                + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
         stringVariables.put(fXSVD, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertuX() {
      try {
         double[] var = (double[]) rawVariables.get(uX);
         List<String> strings = new ArrayList<>();
         for (int x = 0; x < pm.getServers().size(); x++)
            strings.add("(" + (x + this.offset) + "): ["
                    + pm.getServers().get(x).getId() + "]["
                    + var[x] + "]");
         stringVariables.put(uX, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertuL() {
      try {
         double[] var = (double[]) rawVariables.get(uL);
         List<String> strings = new ArrayList<>();
         for (int l = 0; l < pm.getLinks().size(); l++)
            strings.add("(" + (l + this.offset) + "): ["
                    + pm.getLinks().get(l).getId() + "]["
                    + var[l] + "]");
         stringVariables.put(uL, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertfX() {
      try {
         boolean[] var = (boolean[]) rawVariables.get(fX);
         List<String> strings = new ArrayList<>();
         for (int x = 0; x < pm.getServers().size(); x++)
            if (var[x])
               strings.add("(" + (x + this.offset) + "): [" + pm.getServers().get(x).getId() + "]");
         stringVariables.put(fX, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertgSVXY() {
      try {
         boolean[][][][] var = (boolean[][][][]) rawVariables.get(gSVXY);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int x = 0; x < pm.getServers().size(); x++)
                  for (int y = 0; y < pm.getServers().size(); y++)
                     if (x != y)
                        if (var[s][v][x][y])
                           strings.add("(" + (s + this.offset) + "," + (v + this.offset)
                                   + "," + (x + this.offset) + "," + (y + this.offset) + "): ["
                                   + pm.getServers().get(x).getId() + "][" + pm.getServers().get(y).getId() + "]");
         stringVariables.put(gSVXY, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertsSVP() {
      try {
         boolean[][][] var = (boolean[][][]) rawVariables.get(sSVP);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int p = 0; p < pm.getPaths().size(); p++)
                  if (var[s][v][p])
                     strings.add("(" + (s + this.offset) + "," + (v + this.offset) + "," + (p + this.offset) + "): "
                             + pm.getPaths().get(p).getNodePath());
         stringVariables.put(sSVP, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertdSP() {
      try {
         double[][][] var = (double[][][]) rawVariables.get(dSPD);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "," + (d + this.offset) + "): "
                          + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath()
                          + "[" + Auxiliary.roundDouble(var[s][p][d], 2) + "]");
         stringVariables.put(dSPD, strings);
      } catch (Exception ignored) {
      }
   }

   private void convertqSVXP() {
      try {
         double[][][][] var = (double[][][][]) rawVariables.get(ySVXD);
         List<String> strings = new ArrayList<>();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int x = 0; x < pm.getServers().size(); x++)
                  for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                     if (var[s][v][x][p] > 0)
                        strings.add("(" + (s + this.offset) + "," + (v + this.offset)
                                + "," + (x + this.offset) + "," + (p + this.offset) + "): "
                                + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath()
                                + "[" + var[s][v][x][p] + "]");
         stringVariables.put(ySVXD, strings);
      } catch (Exception ignored) {
      }
   }

   private void setSummaryResults(double[] array, List var) {
      array[0] = Auxiliary.avg(new ArrayList<>(var));
      array[1] = Auxiliary.min(new ArrayList<>(var));
      array[2] = Auxiliary.max(new ArrayList<>(var));
      array[3] = Auxiliary.vrc(new ArrayList<>(var), array[0]);
   }

   private void generateLuGraph(List<Double> uL) {
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

   private void generateXuGraph(List<Double> uX) {
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

   private void generateSdGraph(List<Double> sd) {
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

   public Map<String, List<String>> getStringVariables() {
      return stringVariables;
   }

   public double[] getLuSummary() {
      return luSummary;
   }

   public double[] getXuSummary() {
      return xuSummary;
   }

   public double[] getFuSummary() {
      return fuSummary;
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

   public int getMigrationsNum() {
      return migrationsNum;
   }

   public int getReplicationsNum() {
      return replicationsNum;
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
}
