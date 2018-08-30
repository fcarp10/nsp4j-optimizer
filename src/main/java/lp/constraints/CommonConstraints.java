package lp.constraints;

import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import lp.OptimizationModel;
import lp.Variables;
import org.graphstream.graph.Node;
import utils.Auxiliary;
import utils.Scenario;

public class CommonConstraints {

    private OptimizationModel optimizationModel;
    private Variables variables;
    private Parameters parameters;

    public CommonConstraints(OptimizationModel optimizationModel, Scenario scenario) throws GRBException {
        this.optimizationModel = optimizationModel;
        this.parameters = optimizationModel.getParameters();
        this.variables = optimizationModel.getVariables();
        if (scenario.getConstraints().get("setLinkUtilizationExpr")) {
            if (scenario.getUseCase().equals("mgr") || scenario.getUseCase().equals("rep_mgr"))
                setLinkUtilizationExpr(true);
            else setLinkUtilizationExpr(false);
        }
        if (scenario.getConstraints().get("setServerUtilizationExpr"))
            setServerUtilizationExpr();
        if (scenario.getConstraints().get("countNumberOfUsedServers"))
            countNumberOfUsedServers();
        if (scenario.getConstraints().get("onePathPerDemand"))
            onePathPerDemand();
        if (scenario.getConstraints().get("activatePathForService"))
            activatePathForService();
        if (scenario.getConstraints().get("pathsConstrainedByFunctions"))
            pathsConstrainedByFunctions();
        if (scenario.getConstraints().get("functionPlacement"))
            functionPlacement();
        if (scenario.getConstraints().get("oneFunctionPerDemand"))
            oneFunctionPerDemand();
        if (scenario.getConstraints().get("mappingFunctionsWithDemands"))
            mappingFunctionsWithDemands();
        if (scenario.getConstraints().get("functionSequenceOrder"))
            functionSequenceOrder();
    }

    private void setLinkUtilizationExpr(boolean isMigration) throws GRBException {
        for (int l = 0; l < parameters.getLinks().size(); l++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                    if (!parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).contains(parameters.getLinks().get(l)))
                        continue;
                    for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm((double) parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                / (int) parameters.getLinks().get(l).getAttribute("capacity"), variables.tSPD[s][p][d]);
                }
            if (isMigration)
                for (int s = 0; s < parameters.getServices().size(); s++)
                    for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                        for (int p = 0; p < parameters.getPaths().size(); p++) {
                            if (!parameters.getPaths().get(p).contains(parameters.getLinks().get(l)))
                                continue;
                            expr.addTerm(1.0 / (int) parameters.getLinks().get(l).getAttribute("capacity"), variables.mPSV[p][s][v]);
                        }

            optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uL[l], "setLinkUtilizationExpr");
            setLinearCostFunctions(expr, variables.ukL[l]);
        }
    }

    private void setServerUtilizationExpr() throws GRBException {
        for (int x = 0; x < parameters.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
                    for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                        expr.addTerm((parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                        * parameters.getServices().get(s).getFunctions().get(v).getLoad())
                                        / parameters.getServers().get(x).getCapacity()
                                , variables.fXSVD[x][s][v][d]);
                    }
                }
            optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uX[x], "setServerUtilizationExpr");
            setLinearCostFunctions(expr, variables.ukX[x]);
        }
    }

    private void setLinearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {
        for (int l = 0; l < Auxiliary.linearCostFunctions.getValues().size(); l++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(Auxiliary.linearCostFunctions.getValues().get(l)[0], expr);
            expr2.addConstant(Auxiliary.linearCostFunctions.getValues().get(l)[1]);
            optimizationModel.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar, "setLinearCostFunctions");
        }
    }

    private void countNumberOfUsedServers() throws GRBException {

        for (int x = 0; x < parameters.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
                    expr.addTerm(1.0 / parameters.getTotalNumberOfFunctionsAux(), variables.fXSV[x][s][v]);
                    expr2.addTerm(1.0, variables.fXSV[x][s][v]);
                }
            optimizationModel.getGrbModel().addConstr(variables.fX[x], GRB.GREATER_EQUAL, expr, "countNumberOfUsedServers");
            optimizationModel.getGrbModel().addConstr(variables.fX[x], GRB.LESS_EQUAL, expr2, "countNumberOfUsedServers");
        }
    }

    private void onePathPerDemand() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    expr.addTerm(1.0, variables.tSPD[s][p][d]);
                optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "onePathPerDemand");
            }
    }

    private void activatePathForService() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    optimizationModel.getGrbModel().addConstr(variables.tSPD[s][p][d], GRB.LESS_EQUAL, variables.tSP[s][p], "activatePathForService");

        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    expr.addTerm(1.0, variables.tSPD[s][p][d]);
                optimizationModel.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.tSP[s][p], "activatePathForService");
            }
    }

    private void pathsConstrainedByFunctions() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int x = 0; x < parameters.getServers().size(); x++)
                    expr.addTerm(1.0, variables.fXSV[x][s][v]);
                if (parameters.getServices().get(s).getFunctions().get(v).isReplicable()) {
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                        expr2.addTerm(1.0, variables.tSP[s][p]);
                    optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, "pathsConstrainedByFunctions");
                } else
                    optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "pathsConstrainedByFunctions");
            }
    }

    private void functionPlacement() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
                        GRBLinExpr expr = new GRBLinExpr();
                        for (int n = 0; n < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++)
                            for (int x = 0; x < parameters.getServers().size(); x++)
                                if (parameters.getServers().get(x).getNodeParent().equals(parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n)))
                                    expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
                        optimizationModel.getGrbModel().addConstr(variables.tSPD[s][p][d], GRB.LESS_EQUAL, expr, "functionPlacement");
                    }
    }

    private void oneFunctionPerDemand() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int x = 0; x < parameters.getServers().size(); x++)
                        expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
                    optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "oneFunctionPerDemand");
                }
    }

    private void mappingFunctionsWithDemands() throws GRBException {

        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < parameters.getServers().size(); x++)
                    for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        optimizationModel.getGrbModel().addConstr(variables.fXSVD[x][s][v][d], GRB.LESS_EQUAL, variables.fXSV[x][s][v], "mappingFunctionsWithDemands");

        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < parameters.getServers().size(); x++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
                    optimizationModel.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.fXSV[x][s][v], "mappingFunctionsWithDemands");
                }
    }

    private void functionSequenceOrder() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int v = 1; v < parameters.getServices().get(s).getFunctions().size(); v++) {
                        for (int n = 0; n < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++) {
                            GRBLinExpr expr = new GRBLinExpr();
                            GRBLinExpr expr2 = new GRBLinExpr();
                            Node nodeN = parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n);
                            for (int m = 0; m <= n; m++) {
                                Node nodeM = parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(m);
                                for (int x = 0; x < parameters.getServers().size(); x++)
                                    if (parameters.getServers().get(x).getNodeParent().equals(nodeM))
                                        expr.addTerm(1.0, variables.fXSVD[x][s][v - 1][d]);
                            }
                            for (int x = 0; x < parameters.getServers().size(); x++)
                                if (parameters.getServers().get(x).getNodeParent().equals(nodeN))
                                    expr.addTerm(-1.0, variables.fXSVD[x][s][v][d]);

                            expr2.addConstant(-1);
                            expr2.addTerm(1.0, variables.tSPD[s][p][d]);
                            optimizationModel.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "functionSequenceOrder");
                        }
                    }
            }
    }
}
