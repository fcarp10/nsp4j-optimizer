package lp;

import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Variables {

    // Elementary
    public GRBVar[][] rSP;
    public GRBVar[][][] rSPD;
    public GRBVar[][][] pXSV;
    public GRBVar[][][][] pXSVD;
    public GRBVar[] kL;
    public GRBVar[] kX;
    public GRBVar[] uL;
    public GRBVar[] uX;
    // Additional
    public GRBVar[] pX;
    public GRBVar[][][][] gSVXY;
    public GRBVar[][][] sSVP;
    public GRBVar[][] dSP;

    public Variables(Parameters pm, GRBModel grbModel) {
        try {
            rSP = new GRBVar[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    rSP[s][p] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "rSP[" + s + "][" + p + "]");
            rSPD = new GRBVar[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        rSPD[s][p][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "rSPD[" + s + "][" + p + "][" + d + "]");
            pXSV = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        pXSV[x][s][v] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "pXSV[" + x + "][" + s + "][" + v + "]");
            pXSVD = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            pXSVD[x][s][v][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "pXSVD[" + x + "][" + s + "][" + v + "][" + d + "]");
            uL = new GRBVar[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                uL[l] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "uL[" + l + "]");
            uX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                uX[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "uX[" + x + "]");
            kL = new GRBVar[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                kL[l] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "kL[" + l + "]");
            kX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                kX[x] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "kX[" + x + "]");
            pX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                this.pX[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "pX[" + x + "]");
            gSVXY = new GRBVar[pm.getServices().size()][pm.getServiceLengthAux()][pm.getServers().size()][pm.getServers().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int x = 0; x < pm.getServers().size(); x++)
                        for (int y = 0; y < pm.getServers().size(); y++)
                            gSVXY[s][v][x][y] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "gSVXY[" + s + "][" + v + "][" + x + "][" + y + "]");
            sSVP = new GRBVar[pm.getServices().size()][pm.getServiceLengthAux()][pm.getPaths().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < pm.getPaths().size(); p++)
                        sSVP[s][v][p] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "sSVP[" + s + "][" + v + "][" + p + "]");
            dSP = new GRBVar[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    dSP[s][p] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "dSP[" + s + "][" + p + "]");
            grbModel.update();
        } catch (Exception ignored) {
        }
    }
}
