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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      setrSP();
      setrSPD();
      setpXSV();
      setpXSVD();
      setUX();
      setUL();
      setsSVP();
      setdSP();
      setLinkResults(uL);
      setServerResults(uX);
      setFunctionResults(numOfFunctionsPerServer());
      setServiceDelayResults(serviceDelayList());
      generateLuGraph(uL);
      generateXuGraph(uX);
      generateSdGraph(serviceDelayList());
      totalTraffic = pm.getTotalTraffic();
      trafficLinks = Aux.roundDouble(trafficOnLinks(), 2);
      avgPathLength = Aux.roundDouble(avgPathLength(), 2);
      this.objVal = Aux.roundDouble(objVal, 4);
   }

   private int calculateNumberOfMigrations(GRBModel initialModel) throws GRBException {
      GRBVar[][][] pXSV = (GRBVar[][][]) rawVariables.get("pXSV");
      int numOfMigrations = 0;
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               if (initialModel.getVarByName(Aux.pXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 1.0
                       && pXSV[x][s][v].get(GRB.DoubleAttr.X) == 0.0)
                  numOfMigrations++;
      return numOfMigrations;
   }

   private int calculateNumberOfReplications() throws GRBException {
      GRBVar[][][] pXSV = (GRBVar[][][]) rawVariables.get(Aux.pXSV);
      int numOfReplicas = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            int numOfReplicasPerFunction = 0;
            for (int x = 0; x < pm.getServers().size(); x++)
               if (pXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                  numOfReplicasPerFunction++;
            numOfReplicas += numOfReplicasPerFunction - 1;
         }
      return numOfReplicas;
   }

   public Map<Edge, Double> linkUtilizationMap() throws GRBException {
      GRBVar[] uL = (GRBVar[]) rawVariables.get(Aux.uL);
      Map<Edge, Double> linkMapResults = new HashMap<>();
      for (int l = 0; l < pm.getLinks().size(); l++)
         linkMapResults.put(pm.getLinks().get(l), Aux.roundDouble(uL[l].get(GRB.DoubleAttr.X), 2));
      return linkMapResults;
   }

   public Map<Server, Double> serverUtilizationMap() throws GRBException {
      GRBVar[] uX = (GRBVar[]) rawVariables.get(Aux.uX);
      Map<Server, Double> serverMapResults = new HashMap<>();
      for (int x = 0; x < pm.getServers().size(); x++)
         serverMapResults.put(pm.getServers().get(x), Aux.roundDouble(uX[x].get(GRB.DoubleAttr.X), 2));
      return serverMapResults;
   }

   private List<Integer> numOfFunctionsPerServer() throws GRBException {
      GRBVar[] pX = (GRBVar[]) rawVariables.get(Aux.pX);
      List<Integer> numOfFunctionsPerServer = new ArrayList<>();
      for (int x = 0; x < pm.getServers().size(); x++) {
         int numOfFunctions = 0;
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               if (pX[x].get(GRB.DoubleAttr.X) == 1.0)
                  numOfFunctions++;
         numOfFunctionsPerServer.add(numOfFunctions);
      }
      return numOfFunctionsPerServer;
   }

   private List<Double> serviceDelayList() throws GRBException {
      GRBVar[][] dSP = (GRBVar[][]) rawVariables.get(Aux.dSP);
      List<Double> serviceDelayList = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (dSP[s][p].get(GRB.DoubleAttr.X) > 0)
               serviceDelayList.add(Aux.roundDouble(dSP[s][p].get(GRB.DoubleAttr.X), 2));
      return serviceDelayList;
   }

   private double avgPathLength() throws GRBException {
      GRBVar[][] rSP = (GRBVar[][]) rawVariables.get(Aux.rSP);
      double avgPathLength = 0;
      int usedPaths = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (rSP[s][p].get(GRB.DoubleAttr.X) == 1.0) {
               avgPathLength += pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getEdgePath().size();
               usedPaths++;
            }
      if (usedPaths != 0)
         avgPathLength = avgPathLength / usedPaths;
      return avgPathLength;
   }

   private double trafficOnLinks() throws GRBException {
      GRBVar[] uL = (GRBVar[]) rawVariables.get(Aux.uL);
      double trafficOnLinks = 0;
      for (int l = 0; l < pm.getLinks().size(); l++)
         trafficOnLinks += uL[l].get(GRB.DoubleAttr.X) * (int) pm.getLinks().get(l).getAttribute("capacity");
      return trafficOnLinks;
   }

   private void setrSP() throws GRBException {
      GRBVar[][] rSP = (GRBVar[][]) rawVariables.get(Aux.rSP);
      List<String> strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (rSP[s][p].get(GRB.DoubleAttr.X) == 1.0)
               strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "): ["
                       + pm.getServices().get(s).getId() + "]"
                       + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath());
      stringVariables.put("rSP", strings);
   }

   private void setrSPD() throws GRBException {
      GRBVar[][][] rSPD = (GRBVar[][][]) rawVariables.get(Aux.rSPD);
      List<String> strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               if (rSPD[s][p][d].get(GRB.DoubleAttr.X) == 1.0)
                  strings.add("(" + (s + this.offset) + "," + (p + this.offset) + ","
                          + (d + this.offset) + "): ["
                          + pm.getServices().get(s).getId() + "]"
                          + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "["
                          + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
      stringVariables.put("rSPD", strings);
   }

   private void setpXSV() throws GRBException {
      GRBVar[][][] pXSV = (GRBVar[][][]) rawVariables.get(Aux.pXSV);
      List<String> strings = new ArrayList<>();
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               if (pXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                  strings.add("(" + (x + this.offset) + "," + (s + this.offset) + ","
                          + (v + this.offset) + "): ["
                          + pm.getServers().get(x).getId() + "]["
                          + pm.getServices().get(s).getId() + "]["
                          + pm.getServices().get(s).getFunctions().get(v).getType() + "]");
      stringVariables.put("pXSV", strings);
   }

   private void setpXSVD() throws GRBException {
      GRBVar[][][][] pXSVD = (GRBVar[][][][]) rawVariables.get(Aux.pXSVD);
      List<String> strings = new ArrayList<>();
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (pXSVD[x][s][v][d].get(GRB.DoubleAttr.X) == 1.0)
                     strings.add("(" + (x + this.offset) + "," + (s + this.offset)
                             + "," + (v + this.offset) + "," + (d + this.offset) + "): ["
                             + pm.getServers().get(x).getId() + "]["
                             + pm.getServices().get(s).getId() + "]["
                             + pm.getServices().get(s).getFunctions().get(v).getType() + "]["
                             + pm.getServices().get(s).getTrafficFlow().getDemands().get(d) + "]");
      stringVariables.put("pXSVD", strings);
   }

   private void setUX() throws GRBException {
      GRBVar[] uX = (GRBVar[]) rawVariables.get(Aux.uX);
      List<String> strings = new ArrayList<>();
      for (int x = 0; x < pm.getServers().size(); x++)
         strings.add("(" + (x + this.offset) + "): ["
                 + pm.getServers().get(x).getId() + "]["
                 + uX[x].get(GRB.DoubleAttr.X) + "]");
      stringVariables.put("uX", strings);
   }

   private void setUL() throws GRBException {
      GRBVar[] uL = (GRBVar[]) rawVariables.get(Aux.uL);
      List<String> strings = new ArrayList<>();
      for (int l = 0; l < pm.getLinks().size(); l++)
         strings.add("(" + (l + this.offset) + "): ["
                 + pm.getLinks().get(l).getId() + "]["
                 + uL[l].get(GRB.DoubleAttr.X) + "]");
      stringVariables.put("uL", strings);
   }

   private void setsSVP() throws GRBException {
      GRBVar[][][] sSVP = (GRBVar[][][]) rawVariables.get(Aux.sSVP);
      List<String> strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               if (sSVP[s][v][p].get(GRB.DoubleAttr.X) == 1.0)
                  strings.add("(" + (s + this.offset) + "," + (v + this.offset) + "," + (p + this.offset) + "): "
                          + pm.getPaths().get(p).getNodePath());
      stringVariables.put("sSVP", strings);
   }

   private void setdSP() throws GRBException {
      GRBVar[][] dSP = (GRBVar[][]) rawVariables.get(Aux.dSP);
      List<String> strings = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (dSP[s][p].get(GRB.DoubleAttr.X) > 0)
               strings.add("(" + (s + this.offset) + "," + (p + this.offset) + "): "
                       + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath()
                       + "[" + Aux.roundDouble(dSP[s][p].get(GRB.DoubleAttr.X), 2) + "]");
      stringVariables.put("dSP", strings);
   }

   private void setLinkResults(List<Double> uL) {
      luSummary[0] = Aux.avg(new ArrayList<>(uL));
      luSummary[1] = Aux.min(new ArrayList<>(uL));
      luSummary[2] = Aux.max(new ArrayList<>(uL));
      luSummary[3] = Aux.vrc(new ArrayList<>(uL), luSummary[0]);
   }

   private void setServerResults(List<Double> uX) {
      xuSummary[0] = Aux.avg(new ArrayList<>(uX));
      xuSummary[1] = Aux.min(new ArrayList<>(uX));
      xuSummary[2] = Aux.max(new ArrayList<>(uX));
      xuSummary[3] = Aux.vrc(new ArrayList<>(uX), xuSummary[0]);
   }

   private void setFunctionResults(List<Integer> uF) {
      fuSummary[0] = Aux.avg(new ArrayList<>(uF));
      fuSummary[1] = Aux.min(new ArrayList<>(uF));
      fuSummary[2] = Aux.max(new ArrayList<>(uF));
      fuSummary[3] = Aux.vrc(new ArrayList<>(uF), fuSummary[0]);
   }

   private void setServiceDelayResults(List<Double> sd) {
      sdSummary[0] = Aux.avg(new ArrayList<>(sd));
      sdSummary[1] = Aux.min(new ArrayList<>(sd));
      sdSummary[2] = Aux.max(new ArrayList<>(sd));
      sdSummary[3] = Aux.vrc(new ArrayList<>(sd), sdSummary[0]);
   }

   private void generateLuGraph(List<Double> uL) {
      for (int i = 0; i < 10; i++)
         luGraph.add(new GraphData("0." + i, 0));
      for (Double anUL : uL)
         for (int j = 0; j < 10; j++)
            if (anUL * 10 < j + 1 && anUL * 10 >= j) {
               luGraph.get(j).setValue(luGraph.get(j).getValue() + 1);
               break;
            }
   }

   private void generateXuGraph(List<Double> uX) {
      for (int i = 0; i < 10; i++)
         xuGraph.add(new GraphData("0." + i, 0));
      for (Double anUX : uX)
         for (int j = 0; j < 10; j++)
            if (anUX * 10 < j + 1 && anUX * 10 >= j) {
               xuGraph.get(j).setValue(xuGraph.get(j).getValue() + 1);
               break;
            }
   }

   private void generateSdGraph(List<Double> sd) {
      double min = Aux.min(new ArrayList<>(sd));
      double max = Aux.max(new ArrayList<>(sd));
      double step = Aux.roundDouble((max - min) / 10, 2);
      if (max != min) {
         for (int i = 0; i < 10; i++)
            sdGraph.add(new GraphData(String.valueOf(step * i), 0));
         for (Double anSd : sd)
            for (int j = 0; j < 10; j++)
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
