package lp;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import manager.Parameters;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import output.Results;
import output.Auxiliary;
import gui.elements.Scenario;

public class Constraints {

    private OptimizationModel model;
    private Variables variables;
    private Parameters pm;

    public Constraints(Parameters pm, OptimizationModel optimizationModel, Scenario scenario, Results initialPlacement) throws GRBException {
        this.pm = pm;
        this.model = optimizationModel;
        this.variables = optimizationModel.getVariables();
        linkUtilization();
        serverUtilization();
        serviceDelay(initialPlacement);
        if (scenario.getConstraints().get("countNumberOfUsedServers")) countNumberOfUsedServers();
        if (scenario.getConstraints().get("onePathPerDemand")) onePathPerDemand();
        if (scenario.getConstraints().get("activatePathForService")) activatePathForService();
        if (scenario.getConstraints().get("pathsConstrainedByFunctions")) pathsConstrainedByFunctions();
        if (scenario.getConstraints().get("functionPlacement")) functionPlacement();
        if (scenario.getConstraints().get("oneFunctionPerDemand")) oneFunctionPerDemand();
        if (scenario.getConstraints().get("mappingFunctionsWithDemands")) mappingFunctionsWithDemands();
        if (scenario.getConstraints().get("functionSequenceOrder")) functionSequenceOrder();
        if (scenario.getConstraints().get("noParallelPaths")) noParallelPaths();
        if (scenario.getConstraints().get("initialPlacementAsConstraints"))
            initialPlacementAsConstraints(initialPlacement);
        if (scenario.getConstraints().get("synchronizationTraffic")) synchronizationTraffic();
    }

    private void linkUtilization() throws GRBException {
        for (int l = 0; l < pm.getLinks().size(); l++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                    if (!pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).contains(pm.getLinks().get(l)))
                        continue;
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm((double) pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                / (int) pm.getLinks().get(l).getAttribute("capacity"), variables.rSPD[s][p][d]);
                }
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < pm.getPaths().size(); p++) {
                        if (!pm.getPaths().get(p).contains(pm.getLinks().get(l)))
                            continue;
                        double traffic = 0;
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            traffic += pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                    * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load");
                        expr.addTerm(traffic / (int) pm.getLinks().get(l).getAttribute("capacity"), variables.sSVP[s][v][p]);
                    }
            model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uL[l], "linkUtilization");
            linearCostFunctions(expr, variables.kL[l]);
        }
    }

    private void serverUtilization() throws GRBException {
        for (int x = 0; x < pm.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                        expr.addTerm((pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                        * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load"))
                                        / pm.getServers().get(x).getCapacity()
                                , variables.pXSVD[x][s][v][d]);
                    }
                    expr.addTerm((double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load")
                                    * (int) pm.getAux("overhead") / pm.getServers().get(x).getCapacity()
                            , variables.pXSV[x][s][v]);
                }
            model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uX[x], "serverUtilization");
            linearCostFunctions(expr, variables.kX[x]);
        }
    }

    private void linearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {
        for (int l = 0; l < Auxiliary.costFunctions.getValues().size(); l++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(Auxiliary.costFunctions.getValues().get(l)[0], expr);
            expr2.addConstant(Auxiliary.costFunctions.getValues().get(l)[1]);
            model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar, "costFunctions");
        }
    }

    private void serviceDelay(Results initialPlacement) throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++) {
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                Path path = pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p);
                GRBLinExpr linkDelayExpr = new GRBLinExpr();
                double pathDelay = 0.0;
                for (int l = 0; l < path.getEdgePath().size(); l++)
                    pathDelay += (double) path.getEdgePath().get(l).getAttribute("delay");
                linkDelayExpr.addTerm(pathDelay, variables.rSP[s][p]);
                GRBLinExpr processingDelayExpr = new GRBLinExpr();
                for (int n = 0; n < path.getNodePath().size(); n++)
                    for (int x = 0; x < pm.getServers().size(); x++) {
                        if (!pm.getServers().get(x).getNodeParent().equals(path.getNodePath().get(n))) continue;
                        for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                                double load = pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                        * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load")
                                        / pm.getServers().get(x).getCapacity();
                                processingDelayExpr.addTerm(load * pm.getServers().get(x).getProcessingDelay()
                                        , variables.dSPX[s][p][x]);
                            }
                    }
                for (int x = 0; x < pm.getServers().size(); x++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        expr.addTerm(1.0, variables.pXSV[x][s][v]);
                    model.getGrbModel().addConstr(variables.dSPX[s][p][x], GRB.LESS_EQUAL, expr, "variableProcessingDelayExpr");
                    model.getGrbModel().addConstr(variables.dSPX[s][p][x], GRB.LESS_EQUAL, variables.rSP[s][p], "variableProcessingDelayExpr");
                    GRBLinExpr variableProcessingDelayExpr = new GRBLinExpr();
                    variableProcessingDelayExpr.addTerm(1.0, variables.rSP[s][p]);
                    GRBLinExpr expr2 = new GRBLinExpr();
                    expr2.multAdd(1.0 / pm.getServices().get(s).getFunctions().size(), expr);
                    variableProcessingDelayExpr.add(expr2);
                    variableProcessingDelayExpr.addConstant(-1.0);
                    model.getGrbModel().addConstr(variables.dSPX[s][p][x], GRB.GREATER_EQUAL, variableProcessingDelayExpr, "variableProcessingDelayExpr");
                }
                GRBLinExpr migrationDelayExpr = new GRBLinExpr();
                if (initialPlacement != null) {
                    for (int n = 0; n < path.getNodePath().size(); n++)
                        for (int x = 0; x < pm.getServers().size(); x++) {
                            if (!pm.getServers().get(x).getNodeParent().equals(path.getNodePath().get(n))) continue;
                            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                                    double load = pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                            * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load")
                                            / pm.getServers().get(x).getCapacity();
                                    double initialFunctionPlacement = 0;
                                    if (initialPlacement.getPlacement()[x][s][v]) initialFunctionPlacement = 1;
                                    double delay = load * (int) pm.getServices().get(s).getFunctions().get(v).getAttribute("delay");
                                    migrationDelayExpr.addTerm(delay, variables.dSPX[s][p][x]);
                                    migrationDelayExpr.addTerm(-delay * initialFunctionPlacement, variables.dSPX[s][p][x]);
                                }
                            }
                        }
                }
                GRBLinExpr serviceDelayExpr = new GRBLinExpr();
                serviceDelayExpr.add(linkDelayExpr);
                serviceDelayExpr.add(processingDelayExpr);
                serviceDelayExpr.add(migrationDelayExpr);
                model.getGrbModel().addConstr(serviceDelayExpr, GRB.EQUAL, variables.dSP[s][p], "serviceDelay");
            }
        }
    }

    private void countNumberOfUsedServers() throws GRBException {
        for (int x = 0; x < pm.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    expr.addTerm(1.0 / pm.getTotalNumberOfFunctionsAux(), variables.pXSV[x][s][v]);
                    expr2.addTerm(1.0, variables.pXSV[x][s][v]);
                }
            model.getGrbModel().addConstr(variables.pX[x], GRB.GREATER_EQUAL, expr, "countNumberOfUsedServers");
            model.getGrbModel().addConstr(variables.pX[x], GRB.LESS_EQUAL, expr2, "countNumberOfUsedServers");
        }
    }

    private void onePathPerDemand() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    expr.addTerm(1.0, variables.rSPD[s][p][d]);
                model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "onePathPerDemand");
            }
    }

    private void activatePathForService() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    model.getGrbModel().addConstr(variables.rSPD[s][p][d], GRB.LESS_EQUAL, variables.rSP[s][p], "activatePathForService");

        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    expr.addTerm(1.0, variables.rSPD[s][p][d]);
                model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.rSP[s][p], "activatePathForService");
            }
    }

    private void pathsConstrainedByFunctions() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int x = 0; x < pm.getServers().size(); x++)
                    expr.addTerm(1.0, variables.pXSV[x][s][v]);
                if ((boolean) pm.getServices().get(s).getFunctions().get(v).getAttribute("replicable")) {
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                        expr2.addTerm(1.0, variables.rSP[s][p]);
                    model.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, "pathsConstrainedByFunctions");
                } else
                    model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "pathsConstrainedByFunctions");
            }
    }

    private void functionPlacement() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                        GRBLinExpr expr = new GRBLinExpr();
                        for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++)
                            for (int x = 0; x < pm.getServers().size(); x++)
                                if (pm.getServers().get(x).getNodeParent().equals(pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n)))
                                    expr.addTerm(1.0, variables.pXSVD[x][s][v][d]);
                        model.getGrbModel().addConstr(variables.rSPD[s][p][d], GRB.LESS_EQUAL, expr, "functionPlacement");
                    }
    }

    private void oneFunctionPerDemand() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int x = 0; x < pm.getServers().size(); x++)
                        expr.addTerm(1.0, variables.pXSVD[x][s][v][d]);
                    model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "oneFunctionPerDemand");
                }
    }

    private void mappingFunctionsWithDemands() throws GRBException {

        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        model.getGrbModel().addConstr(variables.pXSVD[x][s][v][d], GRB.LESS_EQUAL, variables.pXSV[x][s][v], "mappingFunctionsWithDemands");

        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm(1.0, variables.pXSVD[x][s][v][d]);
                    model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.pXSV[x][s][v], "mappingFunctionsWithDemands");
                }
    }

    private void functionSequenceOrder() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int v = 1; v < pm.getServices().get(s).getFunctions().size(); v++) {
                        for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++) {
                            GRBLinExpr expr = new GRBLinExpr();
                            GRBLinExpr expr2 = new GRBLinExpr();
                            Node nodeN = pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n);
                            for (int m = 0; m <= n; m++) {
                                Node nodeM = pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(m);
                                for (int x = 0; x < pm.getServers().size(); x++)
                                    if (pm.getServers().get(x).getNodeParent().equals(nodeM))
                                        expr.addTerm(1.0, variables.pXSVD[x][s][v - 1][d]);
                            }
                            for (int x = 0; x < pm.getServers().size(); x++)
                                if (pm.getServers().get(x).getNodeParent().equals(nodeN))
                                    expr.addTerm(-1.0, variables.pXSVD[x][s][v][d]);

                            expr2.addConstant(-1);
                            expr2.addTerm(1.0, variables.rSPD[s][p][d]);
                            model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "functionSequenceOrder");
                        }
                    }
            }
    }

    private void noParallelPaths() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                expr.addTerm(1.0, variables.rSP[s][p]);
            model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, "noParallelPaths");
        }
    }

    private void initialPlacementAsConstraints(Results initialPlacement) throws GRBException {
        if (initialPlacement != null) {
            for (int x = 0; x < initialPlacement.getPlacement().length; x++)
                for (int s = 0; s < initialPlacement.getPlacement()[x].length; s++)
                    for (int v = 0; v < initialPlacement.getPlacement()[x][s].length; v++)
                        if (initialPlacement.getPlacement()[x][s][v])
                            model.getGrbModel().addConstr(variables.pXSV[x][s][v], GRB.EQUAL, 1, "initialPlacementAsConstraints");
        }
    }

    private void synchronizationTraffic() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++)
                    for (int y = 0; y < pm.getServers().size(); y++) {
                        if (x == y) continue;
                        model.getGrbModel().addConstr(variables.gSVXY[s][v][x][y], GRB.LESS_EQUAL, variables.pXSV[x][s][v], "synchronizationTraffic");
                        model.getGrbModel().addConstr(variables.gSVXY[s][v][x][y], GRB.LESS_EQUAL, variables.pXSV[y][s][v], "synchronizationTraffic");
                        GRBLinExpr expr = new GRBLinExpr();
                        expr.addTerm(1.0, variables.pXSV[x][s][v]);
                        expr.addTerm(1.0, variables.pXSV[y][s][v]);
                        expr.addConstant(-1.0);
                        model.getGrbModel().addConstr(variables.gSVXY[s][v][x][y], GRB.GREATER_EQUAL, expr, "synchronizationTraffic");
                        expr = new GRBLinExpr();
                        for (int p = 0; p < pm.getPaths().size(); p++)
                            if (pm.getPaths().get(p).getNodePath().get(0).equals(pm.getServers().get(x).getNodeParent())
                                    & pm.getPaths().get(p).getNodePath().get(pm.getPaths().get(p).getNodePath().size() - 1).equals(pm.getServers().get(y).getNodeParent()))
                                expr.addTerm(1.0, variables.sSVP[s][v][p]);
                        model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.gSVXY[s][v][x][y], "synchronizationTraffic");
                    }
    }
}
