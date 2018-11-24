package output;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gui.elements.GraphData;
import gui.elements.Scenario;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;
import manager.Parameters;
import manager.elements.Server;
import org.graphstream.graph.Edge;

import java.util.*;

import static output.Definitions.*;

public class Results {

   // @JsonIgnore tag -> ignores specific variable for json result file
   // transient modifier -> ignores specific variable for posting results to web UI.
   // scenario
   @JsonIgnore
   private transient Parameters pm;
   private transient Scenario scenario;
   @JsonIgnore
   private transient int offset;
   @JsonIgnore
   private transient Map<String, Object> rawVariables;
   private Map<String, List<String>> stringVariables;

   // summary results
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

   public void prepareVariablesForJsonFile(double objVal, GRBModel initialModel) throws GRBException {
      List<Double> uL = new ArrayList<>(linkUtilizationMap().values());
      List<Double> uX = new ArrayList<>(serverUtilizationMap().values());
      if (initialModel != null) {
         this.migrationsNum = calculateNumberOfMigrations(initialModel);
         this.replicationsNum = calculateNumberOfReplications();
      }
      prepareVariablesForPrinting();
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

   private int calculateNumberOfMigrations(GRBModel initialModel) throws GRBException {
      GRBVar[][][] pXSVvar = (GRBVar[][][]) rawVariables.get(pXSV);
      int numOfMigrations = 0;
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               if (initialModel.getVarByName(pXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 1.0
                       && pXSVvar[x][s][v].get(GRB.DoubleAttr.X) == 0.0)
                  numOfMigrations++;
      return numOfMigrations;
   }

   private int calculateNumberOfReplications() throws GRBException {
      GRBVar[][][] pXSVvar = (GRBVar[][][]) rawVariables.get(pXSV);
      int numOfReplicas = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            int numOfReplicasPerFunction = 0;
            for (int x = 0; x < pm.getServers().size(); x++)
               if (pXSVvar[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                  numOfReplicasPerFunction++;
            numOfReplicas += numOfReplicasPerFunction - 1;
         }
      return numOfReplicas;
   }

   public Map<Edge, Double> linkUtilizationMap() throws GRBException {
      GRBVar[] uLvar = (GRBVar[]) rawVariables.get(uL);
      Map<Edge, Double> linkMapResults = new HashMap<>();
      for (int l = 0; l < pm.getLinks().size(); l++)
         linkMapResults.put(pm.getLinks().get(l), Auxiliary.roundDouble(uLvar[l].get(GRB.DoubleAttr.X), 2));
      return linkMapResults;
   }

   public Map<Server, Double> serverUtilizationMap() throws GRBException {
      GRBVar[] uXvar = (GRBVar[]) rawVariables.get(uX);
      Map<Server, Double> serverMapResults = new HashMap<>();
      for (int x = 0; x < pm.getServers().size(); x++)
         serverMapResults.put(pm.getServers().get(x), Auxiliary.roundDouble(uXvar[x].get(GRB.DoubleAttr.X), 2));
      return serverMapResults;
   }

   private List<Integer> numOfFunctionsPerServer() throws GRBException {
      GRBVar[] pXvar = (GRBVar[]) rawVariables.get(pX);
      List<Integer> numOfFunctionsPerServer = new ArrayList<>();
      for (int x = 0; x < pm.getServers().size(); x++) {
         int numOfFunctions = 0;
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               if (pXvar[x].get(GRB.DoubleAttr.X) == 1.0)
                  numOfFunctions++;
         numOfFunctionsPerServer.add(numOfFunctions);
      }
      return numOfFunctionsPerServer;
   }

   private List<Double> serviceDelayList() throws GRBException {
      GRBVar[][] dSPvar = (GRBVar[][]) rawVariables.get(dSP);
      List<Double> serviceDelayList = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (dSPvar[s][p].get(GRB.DoubleAttr.X) > 0)
               serviceDelayList.add(Auxiliary.roundDouble(dSPvar[s][p].get(GRB.DoubleAttr.X), 2));
      return serviceDelayList;
   }

   private double avgPathLength() throws GRBException {
      GRBVar[][] rSPvar = (GRBVar[][]) rawVariables.get(rSP);
      double avgPathLength = 0;
      int usedPaths = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (rSPvar[s][p].get(GRB.DoubleAttr.X) == 1.0) {
               avgPathLength += pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getEdgePath().size();
               usedPaths++;
            }
      if (usedPaths != 0)
         avgPathLength = avgPathLength / usedPaths;
      return avgPathLength;
   }

   private double trafficOnLinks() throws GRBException {
      GRBVar[] uLvar = (GRBVar[]) rawVariables.get(uL);
      double trafficOnLinks = 0;
      for (int l = 0; l < pm.getLinks().size(); l++)
         trafficOnLinks += uLvar[l].get(GRB.DoubleAttr.X) * (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY);
      return trafficOnLinks;
   }

   private void prepareVariablesForPrinting() throws GRBException {
      // primary variables
      GRBVar[][] rSPvar = (GRBVar[][]) rawVariables.get(rSP);
      GRBVar[][][] rSPDvar = (GRBVar[][][]) rawVariables.get(rSPD);
      GRBVar[][][] pXSVvar = (GRBVar[][][]) rawVariables.get(pXSV);
      GRBVar[][][][] pXSVDvar = (GRBVar[][][][]) rawVariables.get(pXSVD);
      GRBVar[] uXvar = (GRBVar[]) rawVariables.get(uX);
      GRBVar[] uLvar = (GRBVar[]) rawVariables.get(uL);
      // secondary variables
      GRBVar[] pXvar = (GRBVar[]) rawVariables.get(pX);
      GRBVar[][][][] gSVXYvar = (GRBVar[][][][]) rawVariables.get(gSVXY);
      GRBVar[][][] sSVPvar = (GRBVar[][][]) rawVariables.get(sSVP);
      GRBVar[][] dSPvar = (GRBVar[][]) rawVariables.get(dSP);
      GRBVar[][][] dSPXvar = (GRBVar[][][]) rawVariables.get(dSPX);

      // prepare rSP
      List<String> strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (rSPvar[s][p].get(GRB.DoubleAttr.X) == 1.0)
               strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "): ["
                       + pm.getServices().get(s).getId() + "]"
                       + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath());
      stringVariables.put(rSP, strings);

      // prepare rSPD
      strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               if (rSPDvar[s][p][d].get(GRB.DoubleAttr.X) == 1.0)
                  strings.add("(" + (s + this.offset) + "," + (p + this.offset) + ","
                          + (d + this.offset) + "): ["
                          + pm.getServices().get(s).getId() + "]"
                          + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "["
                          + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
      stringVariables.put(rSPD, strings);

      // prepare pXSV
      strings = new ArrayList<>();
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               if (pXSVvar[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                  strings.add("(" + (x + this.offset) + "," + (s + this.offset) + ","
                          + (v + this.offset) + "): ["
                          + pm.getServers().get(x).getId() + "]["
                          + pm.getServices().get(s).getId() + "]["
                          + pm.getServices().get(s).getFunctions().get(v).getType() + "]");
      stringVariables.put(pXSV, strings);

      // prepare pXSVD
      strings = new ArrayList<>();
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (pXSVDvar[x][s][v][d].get(GRB.DoubleAttr.X) == 1.0)
                     strings.add("(" + (x + this.offset) + "," + (s + this.offset)
                             + "," + (v + this.offset) + "," + (d + this.offset) + "): ["
                             + pm.getServers().get(x).getId() + "]["
                             + pm.getServices().get(s).getId() + "]["
                             + pm.getServices().get(s).getFunctions().get(v).getType() + "]["
                             + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
      stringVariables.put(pXSVD, strings);

      // prepare uX
      strings = new ArrayList<>();
      for (int x = 0; x < pm.getServers().size(); x++)
         strings.add("(" + (x + this.offset) + "): ["
                 + pm.getServers().get(x).getId() + "]["
                 + uXvar[x].get(GRB.DoubleAttr.X) + "]");
      stringVariables.put(uX, strings);

      // prepare uL
      strings = new ArrayList<>();
      for (int l = 0; l < pm.getLinks().size(); l++)
         strings.add("(" + (l + this.offset) + "): ["
                 + pm.getLinks().get(l).getId() + "]["
                 + uLvar[l].get(GRB.DoubleAttr.X) + "]");
      stringVariables.put(uL, strings);

      // prepare pX
      strings = new ArrayList<>();
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pXvar[x].get(GRB.DoubleAttr.X) == 1.0)
            strings.add("(" + (x + this.offset) + "): [" + pm.getServers().get(x).getId() + "]");
      stringVariables.put(pX, strings);

      // prepare gSVXY
      strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int y = 0; y < pm.getServers().size(); y++)
                  if (x != y)
                     if (gSVXYvar[s][v][x][y].get(GRB.DoubleAttr.X) == 1.0)
                        strings.add("(" + (s + this.offset) + "," + (v + this.offset)
                                + "," + (x + this.offset) + "," + (y + this.offset) + "): ["
                                + pm.getServers().get(x).getId() + "][" + pm.getServers().get(y).getId() + "]");
      stringVariables.put(gSVXY, strings);

      // prepare sSVP
      strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int p = 0; p < pm.getPaths().size(); p++)
               if (sSVPvar[s][v][p].get(GRB.DoubleAttr.X) == 1.0)
                  strings.add("(" + (s + this.offset) + "," + (v + this.offset) + "," + (p + this.offset) + "): "
                          + pm.getPaths().get(p).getNodePath());
      stringVariables.put(sSVP, strings);

      // prepare dSP
      strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (dSPvar[s][p].get(GRB.DoubleAttr.X) > 0)
               strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "): "
                       + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath()
                       + "[" + Auxiliary.roundDouble(dSPvar[s][p].get(GRB.DoubleAttr.X), 2) + "]");
      stringVariables.put(dSP, strings);

      // prepare dSPX
      strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int x = 0; x < pm.getServers().size(); x++)
               if (dSPXvar[s][p][x].get(GRB.DoubleAttr.X) == 1.0)
                  strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "," + (x + this.offset) + "): "
                          + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath()
                          + "[" + pm.getServers().get(x).getId() + "]");
      stringVariables.put(dSPX, strings);
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
            sdGraph.add(new GraphData(String.valueOf((step * i) + min), 0));
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
