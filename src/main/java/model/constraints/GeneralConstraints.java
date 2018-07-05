package model.constraints;

import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import model.Model;
import model.Variables;
import org.graphstream.graph.Node;

public class GeneralConstraints {

    private Model model;
    private Variables variables;
    private Parameters parameters;

    public GeneralConstraints(Model model) throws GRBException {
        this.model = model;
        this.parameters = model.getParameters();
        this.variables = model.getVariables();
        this.setLinkUtilizationExpr();
        this.setServerUtilizationExpr();
        this.countNumberOfUsedServers();
        this.onePathPerDemand();
        this.activatePathForService();
        this.pathsConstrainedByFunctions();
        this.functionPlacement();
        this.oneFunctionPerDemand();
        this.mappingFunctionsWithDemands();
        this.functionSequenceOrder();
    }

    private void setLinkUtilizationExpr() throws GRBException {
        for (int l = 0; l < parameters.getLinks().size(); l++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                    if (!parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).contains(parameters.getLinks().get(l)))
                        continue;
                    for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm(parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                / (double) parameters.getLinks().get(l).getAttribute("capacity"), variables.tSPD[s][p][d]);
                }
            model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uL[l], "setLinkUtilizationExpr");
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
            model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uX[x], "setServerUtilizationExpr");
            setLinearCostFunctions(expr, variables.ukX[x]);
        }
    }

    private void setLinearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {
        for (int l = 0; l < variables.linearCostFunctions.getValues().size(); l++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(variables.linearCostFunctions.getValues().get(l)[0], expr);
            expr2.addConstant(variables.linearCostFunctions.getValues().get(l)[1]);
            model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar, "setLinearCostFunctions");
        }
    }

    private void countNumberOfUsedServers() throws GRBException {

        for (int x = 0; x < parameters.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                    expr.addTerm(1.0 / parameters.getTotalNumberOfFunctionsAux(), variables.fXSV[x][s][v]);
            model.getGrbModel().addConstr(variables.fX[x], GRB.GREATER_EQUAL, expr, "countNumberOfUsedServers");
        }
    }

    private void onePathPerDemand() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    expr.addTerm(1.0, variables.tSPD[s][p][d]);
                model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "onePathPerDemand");
            }
    }

    private void activatePathForService() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    model.getGrbModel().addConstr(variables.tSPD[s][p][d], GRB.LESS_EQUAL, variables.tSP[s][p], "activatePathForService");

        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    expr.addTerm(1.0, variables.tSPD[s][p][d]);
                model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.tSP[s][p], "activatePathForService");
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
                    model.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, "pathsConstrainedByFunctions");
                } else
                    model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "pathsConstrainedByFunctions");
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
                        model.getGrbModel().addConstr(variables.tSPD[s][p][d], GRB.LESS_EQUAL, expr, "functionPlacement");
                    }
    }

    private void oneFunctionPerDemand() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int x = 0; x < parameters.getServers().size(); x++)
                        expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
                    model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "oneFunctionPerDemand");
                }
    }

    private void mappingFunctionsWithDemands() throws GRBException {

        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < parameters.getServers().size(); x++)
                    for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        model.getGrbModel().addConstr(variables.fXSVD[x][s][v][d], GRB.LESS_EQUAL, variables.fXSV[x][s][v], "mappingFunctionsWithDemands");

        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < parameters.getServers().size(); x++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
                    model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.fXSV[x][s][v], "mappingFunctionsWithDemands");
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
                            model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "functionSequenceOrder");
                        }
                    }
            }
    }
}
