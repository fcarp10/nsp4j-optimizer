package lp;

import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import results.Output;
import results.Auxiliary;
import elements.Scenario;

public class Constraints {

    private OptimizationModel optimizationModel;
    private Variables variables;
    private Parameters pm;

    public Constraints(Parameters pm, OptimizationModel optimizationModel, Scenario scenario, Output initialOutput) throws GRBException {
        this.pm = pm;
        this.optimizationModel = optimizationModel;
        this.variables = optimizationModel.getVariables();
        linkUtilization();
        serverUtilization();
        serviceDelay();
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
            initialPlacementAsConstraints(initialOutput);
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
                                    * pm.getServices().get(s).getFunctions().get(v).getLoad();
                        expr.addTerm(traffic / (int) pm.getLinks().get(l).getAttribute("capacity"), variables.sSVP[s][v][p]);
                    }
            optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uL[l], "linkUtilization");
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
                                        * pm.getServices().get(s).getFunctions().get(v).getLoad())
                                        / pm.getServers().get(x).getCapacity()
                                , variables.pXSVD[x][s][v][d]);
                    }
                    expr.addTerm(pm.getServices().get(s).getFunctions().get(v).getLoad() * pm.getAux()[0] / pm.getServers().get(x).getCapacity()
                            , variables.pXSV[x][s][v]);
                }
            optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uX[x], "serverUtilization");
            linearCostFunctions(expr, variables.kX[x]);
        }
    }

    private void linearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {
        for (int l = 0; l < Auxiliary.linearCostFunctions.getValues().size(); l++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(Auxiliary.linearCostFunctions.getValues().get(l)[0], expr);
            expr2.addConstant(Auxiliary.linearCostFunctions.getValues().get(l)[1]);
            optimizationModel.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar, "linearCostFunctions");
        }
    }

    private void serviceDelay() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++) {
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                Path path = pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p);
                double pathDelay = 0.0;
                for (int l = 0; l < path.getEdgePath().size(); l++)
                    pathDelay += (double) path.getEdgePath().get(l).getAttribute("delay");
                GRBLinExpr linkDelayExpr = new GRBLinExpr();
                linkDelayExpr.addTerm(pathDelay, variables.rSP[s][p]);
                GRBLinExpr processingDelayExpr = new GRBLinExpr();
                for (int x = 0; x < pm.getServers().size(); x++) {
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                        double traffic = 0.0;
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            traffic += pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d);
                        processingDelayExpr.addTerm(traffic * pm.getServices().get(s).getFunctions().get(v).getLoad()
                                * pm.getServers().get(x).getProcessingDelay() / pm.getServers().get(x).getCapacity()
                                , variables.pXSV[x][s][v]);
                    }
                }
                GRBLinExpr migrationDelayExpr = new GRBLinExpr();

                GRBLinExpr serviceDelayExpr = new GRBLinExpr();
                serviceDelayExpr.add(linkDelayExpr);
                serviceDelayExpr.add(processingDelayExpr);
                serviceDelayExpr.add(migrationDelayExpr);
                optimizationModel.getGrbModel().addConstr(serviceDelayExpr, GRB.EQUAL, variables.dSP[s][p], "serviceDelay");
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
            optimizationModel.getGrbModel().addConstr(variables.pX[x], GRB.GREATER_EQUAL, expr, "countNumberOfUsedServers");
            optimizationModel.getGrbModel().addConstr(variables.pX[x], GRB.LESS_EQUAL, expr2, "countNumberOfUsedServers");
        }
    }

    private void onePathPerDemand() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    expr.addTerm(1.0, variables.rSPD[s][p][d]);
                optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "onePathPerDemand");
            }
    }

    private void activatePathForService() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    optimizationModel.getGrbModel().addConstr(variables.rSPD[s][p][d], GRB.LESS_EQUAL, variables.rSP[s][p], "activatePathForService");

        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    expr.addTerm(1.0, variables.rSPD[s][p][d]);
                optimizationModel.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.rSP[s][p], "activatePathForService");
            }
    }

    private void pathsConstrainedByFunctions() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int x = 0; x < pm.getServers().size(); x++)
                    expr.addTerm(1.0, variables.pXSV[x][s][v]);
                if (pm.getServices().get(s).getFunctions().get(v).isReplicable()) {
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                        expr2.addTerm(1.0, variables.rSP[s][p]);
                    optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, "pathsConstrainedByFunctions");
                } else
                    optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "pathsConstrainedByFunctions");
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
                        optimizationModel.getGrbModel().addConstr(variables.rSPD[s][p][d], GRB.LESS_EQUAL, expr, "functionPlacement");
                    }
    }

    private void oneFunctionPerDemand() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int x = 0; x < pm.getServers().size(); x++)
                        expr.addTerm(1.0, variables.pXSVD[x][s][v][d]);
                    optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "oneFunctionPerDemand");
                }
    }

    private void mappingFunctionsWithDemands() throws GRBException {

        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        optimizationModel.getGrbModel().addConstr(variables.pXSVD[x][s][v][d], GRB.LESS_EQUAL, variables.pXSV[x][s][v], "mappingFunctionsWithDemands");

        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm(1.0, variables.pXSVD[x][s][v][d]);
                    optimizationModel.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.pXSV[x][s][v], "mappingFunctionsWithDemands");
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
                            optimizationModel.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "functionSequenceOrder");
                        }
                    }
            }
    }

    private void noParallelPaths() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                expr.addTerm(1.0, variables.rSP[s][p]);
            optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, 1, "noParallelPaths");
        }
    }

    private void initialPlacementAsConstraints(Output initialOutput) throws GRBException {
        if (initialOutput != null)
            for (int x = 0; x < initialOutput.getpXSV().length; x++)
                for (int s = 0; s < initialOutput.getpXSV()[x].length; s++)
                    for (int v = 0; v < initialOutput.getpXSV()[x][s].length; v++)
                        if (initialOutput.getpXSV()[x][s][v])
                            optimizationModel.getGrbModel().addConstr(variables.pXSV[x][s][v], GRB.EQUAL, 1, "initialPlacementAsConstraints");
    }

    private void synchronizationTraffic() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++)
                    for (int y = 0; y < pm.getServers().size(); y++) {
                        if (x == y) continue;
                        optimizationModel.getGrbModel().addConstr(variables.gSVXY[s][v][x][y], GRB.LESS_EQUAL, variables.pXSV[x][s][v], "synchronizationTraffic");
                        optimizationModel.getGrbModel().addConstr(variables.gSVXY[s][v][x][y], GRB.LESS_EQUAL, variables.pXSV[y][s][v], "synchronizationTraffic");
                        GRBLinExpr expr = new GRBLinExpr();
                        expr.addTerm(1.0, variables.pXSV[x][s][v]);
                        expr.addTerm(1.0, variables.pXSV[y][s][v]);
                        expr.addConstant(-1.0);
                        optimizationModel.getGrbModel().addConstr(variables.gSVXY[s][v][x][y], GRB.GREATER_EQUAL, expr, "synchronizationTraffic");
                        expr = new GRBLinExpr();
                        for (int p = 0; p < pm.getPaths().size(); p++)
                            if (pm.getPaths().get(p).getNodePath().get(0).equals(pm.getServers().get(x).getNodeParent())
                                    & pm.getPaths().get(p).getNodePath().get(pm.getPaths().get(p).getNodePath().size() - 1).equals(pm.getServers().get(y).getNodeParent()))
                                expr.addTerm(1.0, variables.sSVP[s][v][p]);
                        optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, variables.gSVXY[s][v][x][y], "synchronizationTraffic");
                    }
    }
}
