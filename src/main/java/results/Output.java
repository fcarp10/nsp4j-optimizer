package results;

import filemanager.Parameters;
import gurobi.GRB;
import learning.LearningModel;
import lp.OptimizationModel;
import network.Server;
import org.graphstream.graph.Edge;
import services.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Output {

    private Parameters pm;
    private boolean[][] sp;
    private boolean[][][] spd;
    private boolean[] x;
    private boolean[][][] xsv;
    private boolean[][][][] xsvd;
    private double[] kl;
    private double[] kx;
    private double[] ul;
    private double[] ux;
    private double[][][] svp;

    public Output(Parameters pm, OptimizationModel optimizationModel) {
        this.pm = pm;
        try {
            sp = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    if (optimizationModel.getVariables().sp[s][p].get(GRB.DoubleAttr.X) == 1.0)
                        sp[s][p] = true;
            spd = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (optimizationModel.getVariables().spd[s][p][d].get(GRB.DoubleAttr.X) == 1.0)
                            spd[s][p][d] = true;
            x = new boolean[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                if (optimizationModel.getVariables().x[x].get(GRB.DoubleAttr.X) == 1.0)
                    this.x[x] = true;
            xsv = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        if (optimizationModel.getVariables().xsv[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                            xsv[x][s][v] = true;
            xsvd = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            if (optimizationModel.getVariables().xsvd[x][s][v][d].get(GRB.DoubleAttr.X) == 1.0)
                                xsvd[x][s][v][d] = true;
            ul = new double[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                ul[l] = optimizationModel.getVariables().ul[l].get(GRB.DoubleAttr.X);
            ux = new double[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                ux[x] = optimizationModel.getVariables().ux[x].get(GRB.DoubleAttr.X);
            kl = new double[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                kl[l] = optimizationModel.getVariables().kl[l].get(GRB.DoubleAttr.X);
            kx = new double[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                kx[x] = optimizationModel.getVariables().kx[x].get(GRB.DoubleAttr.X);
            svp = new double[pm.getPaths().size()][pm.getServices().size()][pm.getServiceLengthAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < pm.getPaths().size(); p++)
                        svp[p][s][v] = optimizationModel.getVariables().svp[p][s][v].get(GRB.DoubleAttr.X);
        } catch (Exception ignored) {
        }
    }

    public Output(Parameters pm, LearningModel learningModel) {
        this.pm = pm;
        this.sp = learningModel.gettSP();
        this.spd = learningModel.gettSPD();
        this.xsv = learningModel.getfXSV();
        this.xsvd = learningModel.getfXSVD();
        this.ux = learningModel.getuX();
        this.ul = learningModel.getuL();
        this.svp = learningModel.getmPSV();
    }

    public Results generateResults(double cost, Output initialOutput) {

        List<Double> lu = new ArrayList<>(linkUtilizationMap().values());
        List<Double> xu = new ArrayList<>(serverUtilizationMap().values());
        List<Integer> numOfFunctionsPerServer = numOfFunctionsPerServer();
        int numOfMigrations = 0;
        int numOfReplications = 0;
        if (initialOutput != null) {
            numOfMigrations = calculateNumberOfMigrations(initialOutput);
            numOfReplications = calculateNumberOfReplications();
        }
        return new Results(pm, lu, xu, numOfFunctionsPerServer, pm.getTotalTrafficAux()
                , Auxiliary.roundDouble(trafficOnLinks(), 2), Auxiliary.roundDouble(avgPathLength(), 2)
                , Auxiliary.roundDouble(cost, 4), numOfMigrations, numOfReplications
                , xsv, xsvd, sp, spd, svp);
    }

    private int calculateNumberOfMigrations(Output initialPlacement) {
        int numOfMigrations = 0;
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (initialPlacement.xsv[x][s][v] && !xsv[x][s][v])
                        numOfMigrations++;
        return numOfMigrations;
    }

    private int calculateNumberOfReplications() {
        int numOfReplicas = 0;
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                int numOfReplicasPerFunction = 0;
                for (int x = 0; x < pm.getServers().size(); x++)
                    if (xsv[x][s][v])
                        numOfReplicasPerFunction++;
                numOfReplicas += numOfReplicasPerFunction - 1;
            }
        return numOfReplicas;
    }

    public Map<Edge, Double> linkUtilizationMap() {
        Map<Edge, Double> linkMapResults = new HashMap<>();
        for (int l = 0; l < pm.getLinks().size(); l++)
            linkMapResults.put(pm.getLinks().get(l), Math.round(ul[l] * 10000.0) / 10000.0);
        return linkMapResults;
    }

    public Map<Server, Double> serverUtilizationMap() {
        Map<Server, Double> serverMapResults = new HashMap<>();
        for (int x = 0; x < pm.getServers().size(); x++)
            serverMapResults.put(pm.getServers().get(x), Math.round(ux[x] * 10000.0) / 10000.0);
        return serverMapResults;
    }

    public Map<Server, List<Function>> functionsPerServerMap() {
        Map<Server, List<Function>> functionsMap = new HashMap<>();
        for (int x = 0; x < pm.getServers().size(); x++) {
            List<Function> functions = new ArrayList<>();
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (xsv[x][s][v])
                        functions.add(pm.getServices().get(s).getFunctions().get(v));
            functionsMap.put(pm.getServers().get(x), functions);
        }
        return functionsMap;
    }

    private List<Integer> numOfFunctionsPerServer() {
        List<Integer> numOfFunctionsPerServer = new ArrayList<>();
        for (int x = 0; x < pm.getServers().size(); x++) {
            int numOfFunctions = 0;
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (this.x[x])
                        numOfFunctions++;
            numOfFunctionsPerServer.add(numOfFunctions);
        }
        return numOfFunctionsPerServer;
    }

    private double avgPathLength() {
        double avgPathLength = 0;
        int usedPaths = 0;
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (sp[s][p]) {
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
            trafficOnLinks += ul[l] * (int) pm.getLinks().get(l).getAttribute("capacity");
        return trafficOnLinks;
    }

    public boolean[][] getSp() {
        return sp;
    }

    public boolean[][][] getSpd() {
        return spd;
    }

    public boolean[] getX() {
        return x;
    }

    public boolean[][][] getXsv() {
        return xsv;
    }

    public boolean[][][][] getXsvd() {
        return xsvd;
    }

    public double[] getKl() {
        return kl;
    }

    public double[] getKx() {
        return kx;
    }

    public double[] getuL() {
        return ul;
    }

    public double[] getUx() {
        return ux;
    }

    public double[][][] getSvp() {
        return svp;
    }
}
