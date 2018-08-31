package lp;

import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBException;
import network.Server;
import org.graphstream.graph.Edge;
import results.Results;
import utils.Auxiliary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Output {

    private Parameters pm;
    private boolean[][] tSP;
    private boolean[][][] tSPD;
    private boolean[] fX;
    private boolean[][][] fXSV;
    private boolean[][][][] fXSVD;
    private double[] ukL;
    private double[] ukX;
    private double[] uL;
    private double[] uX;
    private double[][][] mPSV;
    private int numOfMigrations;
    private int numOfReplicas;

    public Output(OptimizationModel optimizationModel) throws GRBException {
        pm = optimizationModel.getParameters();
        tSP = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (optimizationModel.getVariables().tSP[s][p].get(GRB.DoubleAttr.X) == 1.0)
                    tSP[s][p] = true;
        tSPD = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()][pm.getDemandsPerTrafficFlowAux()];
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    if (optimizationModel.getVariables().tSPD[s][p][d].get(GRB.DoubleAttr.X) == 1.0)
                        tSPD[s][p][d] = true;
        fX = new boolean[pm.getServers().size()];
        for (int x = 0; x < pm.getServers().size(); x++)
            if (optimizationModel.getVariables().fX[x].get(GRB.DoubleAttr.X) == 1.0)
                fX[x] = true;
        fXSV = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()];
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (optimizationModel.getVariables().fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                        fXSV[x][s][v] = true;
        fXSVD = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()][pm.getDemandsPerTrafficFlowAux()];
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (optimizationModel.getVariables().fXSVD[x][s][v][d].get(GRB.DoubleAttr.X) == 1.0)
                            fXSVD[x][s][v][d] = true;
        uL = new double[pm.getLinks().size()];
        for (int l = 0; l < pm.getLinks().size(); l++)
            uL[l] = optimizationModel.getVariables().uL[l].get(GRB.DoubleAttr.X);
        uX = new double[pm.getServers().size()];
        for (int x = 0; x < pm.getServers().size(); x++)
            uX[x] = optimizationModel.getVariables().uX[x].get(GRB.DoubleAttr.X);
        ukL = new double[pm.getLinks().size()];
        for (int l = 0; l < pm.getLinks().size(); l++)
            ukL[l] = optimizationModel.getVariables().ukL[l].get(GRB.DoubleAttr.X);
        ukX = new double[pm.getServers().size()];
        for (int x = 0; x < pm.getServers().size(); x++)
            ukX[x] = optimizationModel.getVariables().ukX[x].get(GRB.DoubleAttr.X);
        mPSV = new double[pm.getPaths().size()][pm.getServices().size()][pm.getServiceLengthAux()];
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int p = 0; p < pm.getPaths().size(); p++)
                    mPSV[p][s][v] = optimizationModel.getVariables().mPSV[p][s][v].get(GRB.DoubleAttr.X);
    }

    public void calculateNumberOfMigrations(Output initialPlacement) {
        numOfMigrations = 0;
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (initialPlacement.fXSV[x][s][v] && !fXSV[x][s][v])
                        numOfMigrations++;
    }

    public void calculateNumberOfReplications() {
        numOfReplicas = 0;
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                int numOfReplicasPerFunction = 0;
                for (int x = 0; x < pm.getServers().size(); x++)
                    if (fXSV[x][s][v])
                        numOfReplicasPerFunction++;
                numOfReplicas += numOfReplicasPerFunction - 1;
            }
    }

    public double[][][] getUtilizationPerFunction() {
        double[][][] utilizationPerFunction = new double[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()];
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    double functionUtilization = 0;
                    for (int r = 0; r < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); r++)
                        if (fXSVD[x][s][v][r])
                            functionUtilization += (pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(r)
                                    * pm.getServices().get(s).getFunctions().get(v).getLoad());
                    utilizationPerFunction[x][s][v] = functionUtilization;
                }
        return utilizationPerFunction;
    }

    public Results generateResults(double cost) throws GRBException {

        List<Double> lu = new ArrayList<>(linksMap().values());
        List<Double> xu = new ArrayList<>(serversMap().values());
        List<Integer> numOfFunctionsPerServer = Auxiliary.listsSizes(new ArrayList<>(functionsMap().values()));

        return new Results(pm, lu, xu, numOfFunctionsPerServer, pm.getTotalTrafficAux()
                , Auxiliary.roundDouble(trafficOnLinks(), 2), Auxiliary.roundDouble(avgPathLength(), 2)
                , Auxiliary.roundDouble(cost, 4), numOfMigrations, numOfReplicas
                , fXSV, fXSVD, tSP, tSPD, mPSV);
    }

    public Map<Edge, Double> linksMap() {
        Map<Edge, Double> linkMapResults = new HashMap<>();
        for (int l = 0; l < pm.getLinks().size(); l++)
            linkMapResults.put(pm.getLinks().get(l), Math.round(uL[l] * 10000.0) / 10000.0);
        return linkMapResults;
    }

    public Map<Server, Double> serversMap() {
        Map<Server, Double> serverMapResults = new HashMap<>();
        for (int x = 0; x < pm.getServers().size(); x++)
            serverMapResults.put(pm.getServers().get(x), Math.round(uX[x] * 10000.0) / 10000.0);
        return serverMapResults;
    }

    private Map<Server, List<Integer>> functionsMap() {
        Map<Server, List<Integer>> functionsMap = new HashMap<>();
        for (int x = 0; x < pm.getServers().size(); x++) {
            List<Integer> functions = new ArrayList<>();
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (fXSV[x][s][v])
                        functions.add(pm.getServices().get(s).getFunctions().get(v).getType());
            functionsMap.put(pm.getServers().get(x), functions);
        }
        return functionsMap;
    }

    public Map<Server, String> functionsStringMap() {
        Map<Server, String> functionsStringMap = new HashMap<>();
        for (int x = 0; x < pm.getServers().size(); x++) {
            StringBuilder stringVnf = new StringBuilder();
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (fXSV[x][s][v])
                        stringVnf.append("s").append(String.valueOf(s)).append("v").append(String.valueOf(v)).append("\n");
            functionsStringMap.put(pm.getServers().get(x), stringVnf.toString());
        }
        return functionsStringMap;
    }

    private double avgPathLength() {
        double avgPathLength = 0;
        int usedPaths = 0;
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (tSP[s][p]) {
                    avgPathLength += pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getEdgePath().size();
                    usedPaths++;
                }
        avgPathLength = avgPathLength / usedPaths;
        return avgPathLength;
    }

    private double trafficOnLinks() {
        double trafficOnLinks = 0;
        for (int l = 0; l < pm.getLinks().size(); l++)
            trafficOnLinks += uL[l] * (int) pm.getLinks().get(l).getAttribute("capacity");
        return trafficOnLinks;
    }

    public boolean[][] gettSP() {
        return tSP;
    }

    public boolean[][][] gettSPD() {
        return tSPD;
    }

    public boolean[] getfX() {
        return fX;
    }

    public boolean[][][] getfXSV() {
        return fXSV;
    }

    public boolean[][][][] getfXSVD() {
        return fXSVD;
    }

    public double[] getUkL() {
        return ukL;
    }

    public double[] getUkX() {
        return ukX;
    }

    public double[] getuL() {
        return uL;
    }

    public double[] getuX() {
        return uX;
    }

    public double[][][] getmPSV() {
        return mPSV;
    }
}
