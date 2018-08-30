package lp;

import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Variables {

    public GRBVar[][] tSP;
    public GRBVar[][][] tSPD;
    public GRBVar[] fX;
    public GRBVar[][][] fXSV;
    public GRBVar[][][][] fXSVD;
    public GRBVar[] ukL;
    public GRBVar[] ukX;
    public GRBVar[] uL;
    public GRBVar[] uX;
    public GRBVar[][][] mPSV;

    public Variables(Parameters pm, GRBModel grbModel) {
        try {
            tSP = new GRBVar[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    tSP[s][p] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "tSP[" + s + "][" + p + "]");

            tSPD = new GRBVar[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        tSPD[s][p][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "tSPD[" + s + "][" + p + "][" + d + "]");

            fX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                fX[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "fX[" + x + "]");

            fXSV = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        fXSV[x][s][v] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "fXSV[" + x + "][" + s + "][" + v + "]");

            fXSVD = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            fXSVD[x][s][v][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "fXSVD[" + x + "][" + s + "][" + v + "][" + d + "]");

            uL = new GRBVar[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                uL[l] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "uL[" + l + "]");

            uX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                uX[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "uX[" + x + "]");

            ukL = new GRBVar[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                ukL[l] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "ukL[" + l + "]");

            ukX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                ukX[x] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "ukX[" + x + "]");

            mPSV = new GRBVar[pm.getPaths().size()][pm.getServices().size()][pm.getServiceLengthAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < pm.getPaths().size(); p++)
                        mPSV[p][s][v] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "mPSV[" + p + "][" + s + "][" + v + "]");

            grbModel.update();
        } catch (Exception ignored) {
        }
    }
}
