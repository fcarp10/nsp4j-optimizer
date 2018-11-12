package results;

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

    private Parameters pm;
    private Scenario scenario;
    // Elementary
    private boolean[][] rSP;
    private boolean[][][] rSPD;
    private boolean[][][] pXSV;
    private boolean[][][][] pXSVD;
    private double[] kL;
    private double[] kX;
    private double[] uL;
    private double[] uX;
    // Additional
    private boolean[] pX;
    private boolean[][][] sSVP;
    private double[][] dSP;
    private double cost;

    public Output(Parameters pm, Scenario scenario, OptimizationModel optimizationModel) {
        this.pm = pm;
        this.scenario = scenario;
        this.cost = optimizationModel.getObjVal();
        try {
            rSP = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    if (optimizationModel.getVariables().rSP[s][p].get(GRB.DoubleAttr.X) == 1.0)
                        rSP[s][p] = true;
            rSPD = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (optimizationModel.getVariables().rSPD[s][p][d].get(GRB.DoubleAttr.X) == 1.0)
                            rSPD[s][p][d] = true;
            pXSV = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        if (optimizationModel.getVariables().pXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                            pXSV[x][s][v] = true;
            pXSVD = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            if (optimizationModel.getVariables().pXSVD[x][s][v][d].get(GRB.DoubleAttr.X) == 1.0)
                                pXSVD[x][s][v][d] = true;
            uL = new double[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                uL[l] = optimizationModel.getVariables().uL[l].get(GRB.DoubleAttr.X);
            uX = new double[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                uX[x] = optimizationModel.getVariables().uX[x].get(GRB.DoubleAttr.X);
            kL = new double[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                kL[l] = optimizationModel.getVariables().kL[l].get(GRB.DoubleAttr.X);
            kX = new double[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                kX[x] = optimizationModel.getVariables().kX[x].get(GRB.DoubleAttr.X);
            pX = new boolean[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                if (optimizationModel.getVariables().pX[x].get(GRB.DoubleAttr.X) == 1.0)
                    this.pX[x] = true;
            sSVP = new boolean[pm.getServices().size()][pm.getServiceLengthAux()][pm.getPaths().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < pm.getPaths().size(); p++)
                        if (optimizationModel.getVariables().sSVP[s][v][p].get(GRB.DoubleAttr.X) == 1.0)
                            sSVP[s][v][p] = true;
            dSP = new double[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    dSP[s][p] = optimizationModel.getVariables().dSP[s][p].get(GRB.DoubleAttr.X);
        } catch (Exception ignored) {
        }
    }

    public Output(Parameters pm, LearningModel learningModel) {
        this.pm = pm;
        this.rSP = learningModel.getSp();
        this.rSPD = learningModel.getSpd();
        this.pXSV = learningModel.getXsv();
        this.pXSVD = learningModel.getXsvd();
        this.uX = learningModel.getUx();
        this.uL = learningModel.getUl();
        this.sSVP = learningModel.getSvp();
    }

    public Results generateResults(double cost, Output initialOutput) {

        List<Double> uL = new ArrayList<>(linkUtilizationMap().values());
        List<Double> uX = new ArrayList<>(serverUtilizationMap().values());
        int migrationsNum = 0;
        int replicationsNum = 0;
        if (initialOutput != null) {
            migrationsNum = calculateNumberOfMigrations(initialOutput);
            replicationsNum = calculateNumberOfReplications();
        }
        Results r = new Results(pm, scenario);
        r.setrSP(rSP);
        r.setrSPD(rSPD);
        r.setpXSV(pXSV);
        r.setpXSVD(pXSVD);
        r.setuX(uX);
        r.setuL(uL);
        r.setsSVP(sSVP);
        r.setdSP(dSP);
        r.setLinkResults(uL);
        r.setServerResults(uX);
        r.setFunctionResults(numOfFunctionsPerServer());
        r.setServiceDelayResults(serviceDelayList());
        r.setLuGraph(uL);
        r.setXuGraph(uX);
        r.setSdGraph(serviceDelayList());
        r.setTotalTraffic(pm.getTotalTrafficAux());
        r.setTrafficOnLinks(Auxiliary.roundDouble(trafficOnLinks(), 2));
        r.setAvgPathLength(Auxiliary.roundDouble(avgPathLength(), 2));
        r.setCost(Auxiliary.roundDouble(cost, 4));
        r.setMigrationsNum(migrationsNum);
        r.setReplicationsNum(replicationsNum);
        return r;
    }

    private int calculateNumberOfMigrations(Output initialPlacement) {
        int numOfMigrations = 0;
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (initialPlacement.pXSV[x][s][v] && !pXSV[x][s][v])
                        numOfMigrations++;
        return numOfMigrations;
    }

    private int calculateNumberOfReplications() {
        int numOfReplicas = 0;
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                int numOfReplicasPerFunction = 0;
                for (int x = 0; x < pm.getServers().size(); x++)
                    if (pXSV[x][s][v])
                        numOfReplicasPerFunction++;
                numOfReplicas += numOfReplicasPerFunction - 1;
            }
        return numOfReplicas;
    }

    public Map<Edge, Double> linkUtilizationMap() {
        Map<Edge, Double> linkMapResults = new HashMap<>();
        for (int l = 0; l < pm.getLinks().size(); l++)
            linkMapResults.put(pm.getLinks().get(l), Math.round(uL[l] * 10000.0) / 10000.0);
        return linkMapResults;
    }

    public Map<Server, Double> serverUtilizationMap() {
        Map<Server, Double> serverMapResults = new HashMap<>();
        for (int x = 0; x < pm.getServers().size(); x++)
            serverMapResults.put(pm.getServers().get(x), Math.round(uX[x] * 10000.0) / 10000.0);
        return serverMapResults;
    }

    private List<Integer> numOfFunctionsPerServer() {
        List<Integer> numOfFunctionsPerServer = new ArrayList<>();
        for (int x = 0; x < pm.getServers().size(); x++) {
            int numOfFunctions = 0;
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (this.pX[x])
                        numOfFunctions++;
            numOfFunctionsPerServer.add(numOfFunctions);
        }
        return numOfFunctionsPerServer;
    }

    private List<Double> serviceDelayList() {
        List<Double> serviceDelayList = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (this.dSP[s][p] > 0)
                    serviceDelayList.add(this.dSP[s][p]);
        return serviceDelayList;
    }

    private double avgPathLength() {
        double avgPathLength = 0;
        int usedPaths = 0;
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (rSP[s][p]) {
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
            trafficOnLinks += uL[l] * (int) pm.getLinks().get(l).getAttribute("capacity");
        return trafficOnLinks;
    }

    public Parameters getPm() {
        return pm;
    }

    public boolean[][] getrSP() {
        return rSP;
    }

    public boolean[][][] getrSPD() {
        return rSPD;
    }

    public boolean[] getpX() {
        return pX;
    }

    public boolean[][][] getpXSV() {
        return pXSV;
    }

    public boolean[][][][] getpXSVD() {
        return pXSVD;
    }

    public double[] getkL() {
        return kL;
    }

    public double[] getkX() {
        return kX;
    }

    public double[] getuL() {
        return uL;
    }

    public double[] getuX() {
        return uX;
    }

    public boolean[][][] getsSVP() {
        return sSVP;
    }

    public double getCost() {
        return cost;
    }
}
