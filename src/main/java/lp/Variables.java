package lp;

import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Variables {

    public GRBVar[][] sp;
    public GRBVar[][][] spd;
    public GRBVar[] x;
    public GRBVar[][][] xsv;
    public GRBVar[][][][] xsvd;
    public GRBVar[] kl;
    public GRBVar[] kx;
    public GRBVar[] ul;
    public GRBVar[] ux;
    public GRBVar[][][] svp;
    public GRBVar[][][][] svxy;

    public Variables(Parameters pm, GRBModel grbModel) {
        try {
            sp = new GRBVar[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    sp[s][p] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "sp[" + s + "][" + p + "]");

            spd = new GRBVar[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        spd[s][p][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "spd[" + s + "][" + p + "][" + d + "]");

            x = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                this.x[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x[" + x + "]");

            xsv = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        xsv[x][s][v] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "xsv[" + x + "][" + s + "][" + v + "]");

            xsvd = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()][pm.getDemandsPerTrafficFlowAux()];
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            xsvd[x][s][v][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "xsvd[" + x + "][" + s + "][" + v + "][" + d + "]");

            ul = new GRBVar[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                ul[l] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "ul[" + l + "]");

            ux = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                ux[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "ux[" + x + "]");

            kl = new GRBVar[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
                kl[l] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "kl[" + l + "]");

            kx = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
                kx[x] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "kx[" + x + "]");

            svp = new GRBVar[pm.getServices().size()][pm.getServiceLengthAux()][pm.getPaths().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < pm.getPaths().size(); p++)
                        svp[s][v][p] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "svp[" + s + "][" + v + "][" + p + "]");

            svxy = new GRBVar[pm.getServices().size()][pm.getServiceLengthAux()][pm.getServers().size()][pm.getServers().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int x = 0; x < pm.getServers().size(); x++)
                        for (int y = 0; y < pm.getServers().size(); y++)
                            svxy[s][v][x][y] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "svxy[" + s + "][" + v + "][" + x + "][" + y + "]");

            grbModel.update();
        } catch (Exception ignored) {
        }
    }
}
