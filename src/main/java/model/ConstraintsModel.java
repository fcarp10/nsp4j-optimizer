package model;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import org.graphstream.graph.Node;

public class ConstraintsModel {

    private ParametersModel pm;

    public ConstraintsModel(ParametersModel parametersModel) throws GRBException {
        this.pm = parametersModel;
        this.countNumberOfUsedServers();
        this.onePathPerDemand();
        this.activatePathForService();
        this.pathsConstrainedByFunctions();
        this.functionPlacement();
        this.oneFunctionPerDemand();
        this.mappingFunctionsWithDemands();
        this.functionSequenceOrder();
    }

    public void setLinkUtilizationExpr() throws GRBException {
        for (int l = 0; l < pm.ip.getLinks().size(); l++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < pm.ip.getServices().size(); s++)
                for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                    if (!pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).contains(pm.ip.getLinks().get(l)))
                        continue;
                    for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm(pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                / (double) pm.ip.getLinks().get(l).getAttribute("capacity"), pm.tSPD[s][p][d]);
                }
            pm.grbModel.addConstr(expr, GRB.EQUAL, pm.uL[l], "Link Utilization [" + l + "]");
            setLinearCostFunctions(expr, pm.ukL[l]);
        }
    }

    public void setServerUtilizationExpr() throws GRBException {
        for (int x = 0; x < pm.ip.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < pm.ip.getServices().size(); s++)
                for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++) {
                    for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                        expr.addTerm((pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                        * pm.ip.getServices().get(s).getFunctions().get(v).getLoad())
                                        / pm.ip.getServers().get(x).getCapacity()
                                , pm.fXSVD[x][s][v][d]);
                    }
                }
            pm.grbModel.addConstr(expr, GRB.EQUAL, pm.uX[x], "Server Utilization [" + x + "]");
            setLinearCostFunctions(expr, pm.ukX[x]);
        }
    }

    private void setLinearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {
        for (int l = 0; l < pm.linearCostFunctions.getValues().size(); l++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(pm.linearCostFunctions.getValues().get(l)[0], expr);
            expr2.addConstant(pm.linearCostFunctions.getValues().get(l)[1]);
            pm.grbModel.addConstr(expr2, GRB.LESS_EQUAL, grbVar, "Linear Cost function");
        }
    }

    public void setVariablesFromInitialPlacementAsConstraints(ResultsModel initialResultsModel) throws GRBException {
        for (int x = 0; x < initialResultsModel.getPm().fXSV.length; x++)
            for (int s = 0; s < initialResultsModel.getPm().fXSV[x].length; s++)
                for (int v = 0; v < initialResultsModel.getPm().fXSV[x][s].length; v++)
                    if (initialResultsModel.getPm().fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                        pm.grbModel.addConstr(pm.fXSV[x][s][v], GRB.EQUAL, 1, "initial placement");
    }

    public void noParallelPaths() throws GRBException {
        for (int s = 0; s < pm.ip.getServices().size(); s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                expr.addTerm(1.0, pm.tSP[s][p]);
            pm.grbModel.addConstr(expr, GRB.EQUAL, 1, "No parallel paths");
        }
    }

    private void countNumberOfUsedServers() throws GRBException {

        for (int x = 0; x < pm.ip.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < pm.ip.getServices().size(); s++)
                for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                    expr.addTerm(1 / pm.ip.getAuxTotalNumberOfFunctions(), pm.fXSV[x][s][v]);
            pm.grbModel.addConstr(pm.fX[x], GRB.GREATER_EQUAL, expr, "countNumberOfUsedServers");
        }
    }

    private void onePathPerDemand() throws GRBException {
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    expr.addTerm(1.0, pm.tSPD[s][p][d]);
                pm.grbModel.addConstr(expr, GRB.EQUAL, 1.0, "One path per traffic demand");
            }
    }

    private void activatePathForService() throws GRBException {
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    pm.grbModel.addConstr(pm.tSPD[s][p][d], GRB.LESS_EQUAL, pm.tSP[s][p], "Activate path for service chain");

        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    expr.addTerm(1.0, pm.tSPD[s][p][d]);
                pm.grbModel.addConstr(expr, GRB.GREATER_EQUAL, pm.tSP[s][p], "Activate path for service chain");
            }
    }

    private void pathsConstrainedByFunctions() throws GRBException {
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int x = 0; x < pm.ip.getServers().size(); x++)
                    expr.addTerm(1.0, pm.fXSV[x][s][v]);
                if (pm.ip.getServices().get(s).getFunctions().get(v).isReplicable()) {
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                        expr2.addTerm(1.0, pm.tSP[s][p]);
                    pm.grbModel.addConstr(expr, GRB.EQUAL, expr2, "Paths constrained by functions");
                } else
                    pm.grbModel.addConstr(expr, GRB.EQUAL, 1.0, "Paths constrained by functions");
            }
    }

    private void functionPlacement() throws GRBException {
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++) {
                        GRBLinExpr expr = new GRBLinExpr();
                        for (int n = 0; n < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++)
                            for (int x = 0; x < pm.ip.getServers().size(); x++)
                                if (pm.ip.getServers().get(x).getNodeParent().equals(pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n)))
                                    expr.addTerm(1.0, pm.fXSVD[x][s][v][d]);
                        pm.grbModel.addConstr(pm.tSPD[s][p][d], GRB.LESS_EQUAL, expr, "Function placement");
                    }
    }

    private void oneFunctionPerDemand() throws GRBException {
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int x = 0; x < pm.ip.getServers().size(); x++)
                        expr.addTerm(1.0, pm.fXSVD[x][s][v][d]);
                    pm.grbModel.addConstr(expr, GRB.EQUAL, 1.0, "One function per traffic demand");
                }
    }

    private void mappingFunctionsWithDemands() throws GRBException {

        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.ip.getServers().size(); x++)
                    for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        pm.grbModel.addConstr(pm.fXSVD[x][s][v][d], GRB.LESS_EQUAL, pm.fXSV[x][s][v], "Mapping functions with traffic demands");

        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.ip.getServers().size(); x++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm(1.0, pm.fXSVD[x][s][v][d]);
                    pm.grbModel.addConstr(expr, GRB.GREATER_EQUAL, pm.fXSV[x][s][v], "Mapping functions with traffic demands");
                }
    }

    private void functionSequenceOrder() throws GRBException {
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int v = 1; v < pm.ip.getServices().get(s).getFunctions().size(); v++) {
                        for (int n = 0; n < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++) {
                            GRBLinExpr expr = new GRBLinExpr();
                            GRBLinExpr expr2 = new GRBLinExpr();
                            Node currentNode = pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n);
                            for (int pointer = -1; pointer < n; pointer++) {
                                Node pastNode = pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(pointer + 1);
                                for (int x = 0; x < pm.ip.getServers().size(); x++)
                                    if (pm.ip.getServers().get(x).getNodeParent().equals(pastNode))
                                        expr.addTerm(1.0, pm.fXSVD[x][s][v - 1][d]);
                            }
                            for (int x = 0; x < pm.ip.getServers().size(); x++)
                                if (pm.ip.getServers().get(x).getNodeParent().equals(currentNode))
                                    expr.addTerm(-1.0, pm.fXSVD[x][s][v][d]);

                            expr2.addConstant(-1);
                            expr2.addTerm(1.0, pm.tSPD[s][p][d]);
                            pm.grbModel.addConstr(expr, GRB.GREATER_EQUAL, expr2, "Function sequence order");

                        }
                    }
            }
    }
}
