package results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import gui.elements.GraphData;
import gui.elements.Scenario;
import gurobi.GRB;
import learning.LearningModel;
import lp.OptimizationModel;
import manager.Parameters;
import manager.elements.Server;
import org.graphstream.graph.Edge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Output {

    // @JsonIgnore tag -> ignores specific variable for json result file
    // transient modifier -> ignores specific variable for posting results to web UI.
    // scenario
    @JsonIgnore
    private transient Parameters pm;
    private transient Scenario scenario;
    @JsonIgnore
    private transient int offset;

    // elementary variables
    @JsonIgnore
    private transient boolean[][] rspVar;
    @JsonIgnore
    private transient boolean[][][] rspdVar;
    @JsonIgnore
    private transient boolean[][][] pxsvVar;
    @JsonIgnore
    private transient boolean[][][][] pxsvdVar;
    @JsonIgnore
    private transient double[] lkVar;
    @JsonIgnore
    private transient double[] xkVar;
    @JsonIgnore
    private transient double[] luVar;
    @JsonIgnore
    private transient double[] xuVar;
    // elementary variables results
    private List<String> rSP;
    private List<String> rSPD;
    private List<String> pXSV;
    private List<String> pXSVD;
    private List<String> uX;
    private List<String> uL;

    // additional variables
    @JsonIgnore
    private transient boolean[] pxVar;
    @JsonIgnore
    private transient boolean[][][] ssvpVar;
    @JsonIgnore
    private transient double[][] dspVar;

    // additional variables results
    private List<String> sSVP;
    private List<String> dSP;

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

    public Output(Parameters pm, Scenario scenario) {
        this.pm = pm;
        this.scenario = scenario;
        this.offset = (int) pm.getAux("offset_results");
        luSummary = new double[4];
        xuSummary = new double[4];
        fuSummary = new double[4];
        sdSummary = new double[4];
        rSP = new ArrayList<>();
        rSPD = new ArrayList<>();
        pXSV = new ArrayList<>();
        pXSVD = new ArrayList<>();
        uX = new ArrayList<>();
        uL = new ArrayList<>();
        sSVP = new ArrayList<>();
        dSP = new ArrayList<>();
    }

    public void setOptimizationModelResults(OptimizationModel model) {
        try {
            rspVar = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    if (model.getVariables().rSP[s][p].get(GRB.DoubleAttr.X) == 1.0)
                        rspVar[s][p] = true;
            rspdVar = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (model.getVariables().rSPD[s][p][d].get(GRB.DoubleAttr.X) == 1.0)
                            rspdVar[s][p][d] = true;
            pxsvVar = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        if (model.getVariables().pXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                            pxsvVar[x][s][v] = true;
            pxsvdVar = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            if (model.getVariables().pXSVD[x][s][v][d].get(GRB.DoubleAttr.X) == 1.0)
                                pxsvdVar[x][s][v][d] = true;
            luVar = new double[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                luVar[l] = model.getVariables().uL[l].get(GRB.DoubleAttr.X);
            xuVar = new double[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                xuVar[x] = model.getVariables().uX[x].get(GRB.DoubleAttr.X);
            lkVar = new double[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                lkVar[l] = model.getVariables().kL[l].get(GRB.DoubleAttr.X);
            xkVar = new double[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                xkVar[x] = model.getVariables().kX[x].get(GRB.DoubleAttr.X);
            pxVar = new boolean[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                if (model.getVariables().pX[x].get(GRB.DoubleAttr.X) == 1.0)
                    this.pxVar[x] = true;
            ssvpVar = new boolean[pm.getServices().size()][pm.getServiceLengthAux()][pm.getPaths().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < pm.getPaths().size(); p++)
                        if (model.getVariables().sSVP[s][v][p].get(GRB.DoubleAttr.X) == 1.0)
                            ssvpVar[s][v][p] = true;
            dspVar = new double[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    dspVar[s][p] = model.getVariables().dSP[s][p].get(GRB.DoubleAttr.X);
        } catch (Exception ignored) {
        }
    }

    public void setLearningModelResults(LearningModel learningModel) {
        this.rspVar = learningModel.getSp();
        this.rspdVar = learningModel.getSpd();
        this.pxsvVar = learningModel.getXsv();
        this.pxsvdVar = learningModel.getXsvd();
        this.xuVar = learningModel.getUx();
        this.luVar = learningModel.getUl();
        this.ssvpVar = learningModel.getSvp();
    }

    public void prepareVariablesForJsonFile(double cost, Output initialOutput) {

        List<Double> uL = new ArrayList<>(linkUtilizationMap().values());
        List<Double> uX = new ArrayList<>(serverUtilizationMap().values());
        if (initialOutput != null) {
            this.migrationsNum = calculateNumberOfMigrations(initialOutput);
            this.replicationsNum = calculateNumberOfReplications();
        }
        setrSP(rspVar);
        setrSPD(rspdVar);
        setpXSV(pxsvVar);
        setpXSVD(pxsvdVar);
        setuX(uX);
        setuL(uL);
        setsSVP(ssvpVar);
        setdSP(dspVar);
        setLinkResults(uL);
        setServerResults(uX);
        setFunctionResults(numOfFunctionsPerServer());
        setServiceDelayResults(serviceDelayList());
        setLuGraph(uL);
        setXuGraph(uX);
        setSdGraph(serviceDelayList());
        totalTraffic = pm.getTotalTrafficAux();
        trafficLinks = Auxiliary.roundDouble(trafficOnLinks(), 2);
        avgPathLength = Auxiliary.roundDouble(avgPathLength(), 2);
        objVal = Auxiliary.roundDouble(cost, 4);
    }

    private int calculateNumberOfMigrations(Output initialPlacement) {
        int numOfMigrations = 0;
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (initialPlacement.pxsvVar[x][s][v] && !pxsvVar[x][s][v])
                        numOfMigrations++;
        return numOfMigrations;
    }

    private int calculateNumberOfReplications() {
        int numOfReplicas = 0;
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                int numOfReplicasPerFunction = 0;
                for (int x = 0; x < pm.getServers().size(); x++)
                    if (pxsvVar[x][s][v])
                        numOfReplicasPerFunction++;
                numOfReplicas += numOfReplicasPerFunction - 1;
            }
        return numOfReplicas;
    }

    public Map<Edge, Double> linkUtilizationMap() {
        Map<Edge, Double> linkMapResults = new HashMap<>();
        for (int l = 0; l < pm.getLinks().size(); l++)
            linkMapResults.put(pm.getLinks().get(l), Math.round(luVar[l] * 10000.0) / 10000.0);
        return linkMapResults;
    }

    public Map<Server, Double> serverUtilizationMap() {
        Map<Server, Double> serverMapResults = new HashMap<>();
        for (int x = 0; x < pm.getServers().size(); x++)
            serverMapResults.put(pm.getServers().get(x), Math.round(xuVar[x] * 10000.0) / 10000.0);
        return serverMapResults;
    }

    private List<Integer> numOfFunctionsPerServer() {
        List<Integer> numOfFunctionsPerServer = new ArrayList<>();
        for (int x = 0; x < pm.getServers().size(); x++) {
            int numOfFunctions = 0;
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (this.pxVar[x])
                        numOfFunctions++;
            numOfFunctionsPerServer.add(numOfFunctions);
        }
        return numOfFunctionsPerServer;
    }

    private List<Double> serviceDelayList() {
        List<Double> serviceDelayList = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (this.dspVar[s][p] > 0)
                    serviceDelayList.add(this.dspVar[s][p]);
        return serviceDelayList;
    }

    private double avgPathLength() {
        double avgPathLength = 0;
        int usedPaths = 0;
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (rspVar[s][p]) {
                    avgPathLength += pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getEdgePath().size();
                    usedPaths++;
                }
        if (usedPaths != 0)
            avgPathLength = avgPathLength / usedPaths;
        return avgPathLength;
    }

    private double trafficOnLinks() {
        double trafficOnLinks = 0;
        for (int l = 0; l < pm.getLinks().size(); l++)
            trafficOnLinks += luVar[l] * (int) pm.getLinks().get(l).getAttribute("capacity");
        return trafficOnLinks;
    }

    private void setrSP(boolean rSPinput[][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (rSPinput[s][p])
                    rSP.add("(" + (s + this.offset) + "," + (p + this.offset) + "): ["
                            + pm.getServices().get(s).getId() + "]"
                            + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
    }

    private void setrSPD(boolean rSPDinput[][][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    if (rSPDinput[s][p][d])
                        rSPD.add("(" + (s + this.offset) + "," + (p + this.offset) + "," + (d + this.offset) + "): ["
                                + pm.getServices().get(s).getId() + "]"
                                + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath() + "["
                                + pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) + "]");
    }

    private void setpXSV(boolean pXSVinput[][][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++)
                    if (pXSVinput[x][s][v])
                        pXSV.add("(" + (x + this.offset) + "," + (s + this.offset) + "," + (v + this.offset) + "): ["
                                + pm.getServers().get(x).getId() + "]["
                                + pm.getServices().get(s).getId() + "]["
                                + pm.getServices().get(s).getFunctions().get(v).getType() + "]");
    }

    private void setpXSVD(boolean pXSVDinput[][][][]) {
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (pXSVDinput[x][s][v][d])
                            pXSVD.add("(" + (x + this.offset) + "," + (s + this.offset) + "," + (v + this.offset) + "," + (d + this.offset) + "): ["
                                    + pm.getServers().get(x).getId() + "]["
                                    + pm.getServices().get(s).getId() + "]["
                                    + pm.getServices().get(s).getFunctions().get(v).getType() + "]["
                                    + pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) + "]");
    }

    private void setuX(List<Double> uXinput) {
        for (int x = 0; x < pm.getServers().size(); x++)
            uX.add("(" + (x + this.offset) + "): ["
                    + pm.getServers().get(x).getId() + "]["
                    + uXinput.get(x) + "]");
    }

    private void setuL(List<Double> uLinput) {
        for (int l = 0; l < pm.getLinks().size(); l++)
            uL.add("(" + (l + this.offset) + "): ["
                    + pm.getLinks().get(l).getId() + "]["
                    + uLinput.get(l) + "]");
    }

    private void setsSVP(boolean sSVPinput[][][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int p = 0; p < pm.getPaths().size(); p++)
                    if (sSVPinput[s][v][p])
                        sSVP.add("(" + (s + this.offset) + "," + (v + this.offset) + "," + (p + this.offset) + "): "
                                + pm.getPaths().get(p).getNodePath());
    }

    private void setdSP(double dSPinput[][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (dSPinput[s][p] > 0)
                    dSP.add("(" + (s + this.offset) + "," + (p + this.offset) + "): "
                            + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath()
                            + "[" + Auxiliary.roundDouble(dSPinput[s][p], 2) + "]");
    }

    private void setLinkResults(List<Double> uL) {
        luSummary[0] = Auxiliary.avg(new ArrayList<>(uL));
        luSummary[1] = Auxiliary.min(new ArrayList<>(uL));
        luSummary[2] = Auxiliary.max(new ArrayList<>(uL));
        luSummary[3] = Auxiliary.vrc(new ArrayList<>(uL), luSummary[0]);
    }

    private void setServerResults(List<Double> uX) {
        xuSummary[0] = Auxiliary.avg(new ArrayList<>(uX));
        xuSummary[1] = Auxiliary.min(new ArrayList<>(uX));
        xuSummary[2] = Auxiliary.max(new ArrayList<>(uX));
        xuSummary[3] = Auxiliary.vrc(new ArrayList<>(uX), xuSummary[0]);
    }

    private void setFunctionResults(List<Integer> uF) {
        fuSummary[0] = Auxiliary.avg(new ArrayList<>(uF));
        fuSummary[1] = Auxiliary.min(new ArrayList<>(uF));
        fuSummary[2] = Auxiliary.max(new ArrayList<>(uF));
        fuSummary[3] = Auxiliary.vrc(new ArrayList<>(uF), fuSummary[0]);
    }

    private void setServiceDelayResults(List<Double> sd) {
        sdSummary[0] = Auxiliary.avg(new ArrayList<>(sd));
        sdSummary[1] = Auxiliary.min(new ArrayList<>(sd));
        sdSummary[2] = Auxiliary.max(new ArrayList<>(sd));
        sdSummary[3] = Auxiliary.vrc(new ArrayList<>(sd), sdSummary[0]);
    }

    private void setLuGraph(List<Double> uL) {
        luGraph = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            luGraph.add(new GraphData("0." + i, 0));
        for (Double anUL : uL)
            for (int j = 0; j < 10; j++)
                if (anUL * 10 < j + 1 && anUL * 10 >= j) {
                    luGraph.get(j).setValue(luGraph.get(j).getValue() + 1);
                    break;
                }
    }

    private void setXuGraph(List<Double> uX) {
        xuGraph = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            xuGraph.add(new GraphData("0." + i, 0));
        for (Double anUX : uX)
            for (int j = 0; j < 10; j++)
                if (anUX * 10 < j + 1 && anUX * 10 >= j) {
                    xuGraph.get(j).setValue(xuGraph.get(j).getValue() + 1);
                    break;
                }
    }

    private void setSdGraph(List<Double> sd) {
        sdGraph = new ArrayList<>();
        double min = Auxiliary.min(new ArrayList<>(sd));
        double max = Auxiliary.max(new ArrayList<>(sd));
        double step = (max - min) / 10;
        if (max != min) {
            for (int i = 0; i < 10; i++)
                sdGraph.add(new GraphData(String.valueOf(step * i), 0));
            for (Double anSd : sd)
                for (int j = 0; j < 10; j++)
                    if (anSd < Double.valueOf(sdGraph.get(j).getYear()) + step && anSd >= Double.valueOf(sdGraph.get(j).getYear())) {
                        sdGraph.get(j).setValue(sdGraph.get(j).getValue() + 1);
                        break;
                    }
        } else sdGraph.add(new GraphData(String.valueOf(max), sd.size()));
    }

    public Parameters getPm() {
        return pm;
    }

    public boolean[][] getRspVar() {
        return rspVar;
    }

    public boolean[][][] getRspdVar() {
        return rspdVar;
    }

    public boolean[] getPxVar() {
        return pxVar;
    }

    public boolean[][][] getPxsvVar() {
        return pxsvVar;
    }

    public boolean[][][][] getPxsvdVar() {
        return pxsvdVar;
    }

    public double[] getLkVar() {
        return lkVar;
    }

    public double[] getXkVar() {
        return xkVar;
    }

    public double[] getLuVar() {
        return luVar;
    }

    public double[] getXuVar() {
        return xuVar;
    }

    public boolean[][][] getSsvpVar() {
        return ssvpVar;
    }

    public double getObjVal() {
        return objVal;
    }

    public Scenario getScenario() {
        return scenario;
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

    public List<String> getrSP() {
        return rSP;
    }

    public List<String> getrSPD() {
        return rSPD;
    }

    public List<String> getpXSV() {
        return pXSV;
    }

    public List<String> getpXSVD() {
        return pXSVD;
    }

    public List<String> getuX() {
        return uX;
    }

    public List<String> getuL() {
        return uL;
    }

    public List<String> getsSVP() {
        return sSVP;
    }

    public List<String> getdSP() {
        return dSP;
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
