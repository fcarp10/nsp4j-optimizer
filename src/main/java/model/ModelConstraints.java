package model;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import org.graphstream.graph.Node;

public class ModelConstraints {

    private ModelParameters mp;

    public ModelConstraints(ModelParameters modelParameters, int numOfReplicas) throws GRBException {
        this.mp = modelParameters;

        this.onePathPerDemand();
        this.activatePathForService();
        this.pathsConstrainedByReplicas(numOfReplicas);
        this.pathsConstrainedByFunctions();
        this.functionPlacement();
        this.oneFunctionPerDemand();
        this.mappingFunctionsWithDemands();
        this.functionSequenceOrder();
    }

    private void onePathPerDemand() throws GRBException {
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    expr.addTerm(1.0, mp.rSPD[s][p][d]);
                mp.grbModel.addConstr(expr, GRB.EQUAL, 1.0, "One path per traffic demand");
            }
    }

    private void activatePathForService() throws GRBException {
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    mp.grbModel.addConstr(mp.rSPD[s][p][d], GRB.LESS_EQUAL, mp.rSP[s][p], "Activate path for service chain");

        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    expr.addTerm(1.0, mp.rSPD[s][p][d]);
                mp.grbModel.addConstr(expr, GRB.GREATER_EQUAL, mp.rSP[s][p], "Activate path for service chain");
            }
    }

    private void pathsConstrainedByReplicas(int numOfReplicas) throws GRBException {
        for (int s = 0; s < mp.ip.getServices().size(); s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                expr.addTerm(1.0, mp.rSP[s][p]);
            mp.grbModel.addConstr(expr, GRB.GREATER_EQUAL, mp.ip.getMinReplicas() + 1, "Parallel paths constrained by replicas");
            mp.grbModel.addConstr(expr, GRB.LESS_EQUAL, numOfReplicas + 1, "Parallel paths constrained by replicas");
        }
    }

    private void pathsConstrainedByFunctions() throws GRBException {
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int x = 0; x < mp.ip.getServers().size(); x++)
                    expr.addTerm(1.0, mp.fXSV[x][s][v]);
                if (mp.ip.getServices().get(s).getFunctions().get(v).isReplicable()) {
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                        expr2.addTerm(1.0, mp.rSP[s][p]);
                    mp.grbModel.addConstr(expr, GRB.EQUAL, expr2, "Paths constrained by functions");
                } else
                    mp.grbModel.addConstr(expr, GRB.EQUAL, 1.0, "Paths constrained by functions");
            }
    }

    private void functionPlacement() throws GRBException {
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++) {
                        GRBLinExpr expr = new GRBLinExpr();
                        for (int n = 0; n < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++)
                            for (int x = 0; x < mp.ip.getServers().size(); x++)
                                if (mp.ip.getServers().get(x).getNodeParent().equals(mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n)))
                                    expr.addTerm(1.0, mp.fXSVD[x][s][v][d]);
                        mp.grbModel.addConstr(mp.rSPD[s][p][d], GRB.LESS_EQUAL, expr, "Function placement");
                    }
    }

    private void oneFunctionPerDemand() throws GRBException {
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int x = 0; x < mp.ip.getServers().size(); x++)
                        expr.addTerm(1.0, mp.fXSVD[x][s][v][d]);
                    mp.grbModel.addConstr(expr, GRB.EQUAL, 1.0, "One function per trafiic demand");
                }
    }

    private void mappingFunctionsWithDemands() throws GRBException {

        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < mp.ip.getServers().size(); x++)
                    for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        mp.grbModel.addConstr(mp.fXSVD[x][s][v][d], GRB.LESS_EQUAL, mp.fXSV[x][s][v], "Mapping functions with traffic demands");

        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < mp.ip.getServers().size(); x++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm(1.0, mp.fXSVD[x][s][v][d]);
                    mp.grbModel.addConstr(expr, GRB.GREATER_EQUAL, mp.fXSV[x][s][v], "Mapping functions with traffic demands");
                }
    }

    private void functionSequenceOrder() throws GRBException {
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int v = 1; v < mp.ip.getServices().get(s).getFunctions().size(); v++) {
                        for (int n = 0; n < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++) {
                            GRBLinExpr expr = new GRBLinExpr();
                            GRBLinExpr expr2 = new GRBLinExpr();
                            Node currentNode = mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n);
                            for (int pointer = -1; pointer < n; pointer++) {
                                Node pastNode = mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(pointer + 1);
                                for (int x = 0; x < mp.ip.getServers().size(); x++)
                                    if (mp.ip.getServers().get(x).getNodeParent().equals(pastNode))
                                        expr.addTerm(1.0, mp.fXSVD[x][s][v - 1][d]);
                            }
                            for (int x = 0; x < mp.ip.getServers().size(); x++)
                                if (mp.ip.getServers().get(x).getNodeParent().equals(currentNode))
                                    expr.addTerm(-1.0, mp.fXSVD[x][s][v][d]);

                            expr2.addConstant(-1);
                            expr2.addTerm(1.0, mp.rSPD[s][p][d]);
                            mp.grbModel.addConstr(expr, GRB.GREATER_EQUAL, expr2, "Function sequence order");

                        }
                    }
            }
    }


}
