package lp.constraints;


import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import lp.OptimizationModel;
import lp.Output;
import lp.Variables;
import utils.Scenario;

public class UseCasesConstraints {

    private OptimizationModel optimizationModel;
    private Variables variables;
    private Parameters parameters;

    public UseCasesConstraints(OptimizationModel optimizationModel, Scenario scenario, Output initialOutput) throws GRBException {
        this.optimizationModel = optimizationModel;
        this.parameters = optimizationModel.getParameters();
        this.variables = optimizationModel.getVariables();
        if (scenario.getConstraints().get("noParallelPaths"))
            noParallelPaths();
        if (scenario.getConstraints().get("initialPlacementAsConstraints"))
            initialPlacementAsConstraints(initialOutput);
        if (scenario.getConstraints().get("reroutingMigration"))
            reroutingMigration(initialOutput);
    }

    private void noParallelPaths() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                expr.addTerm(1.0, variables.tSP[s][p]);
            optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, 1, "noParallelPaths");
        }
    }

    private void initialPlacementAsConstraints(Output initialOutput) throws GRBException {
        if (initialOutput != null)
            for (int x = 0; x < initialOutput.getfXSV().length; x++)
                for (int s = 0; s < initialOutput.getfXSV()[x].length; s++)
                    for (int v = 0; v < initialOutput.getfXSV()[x][s].length; v++)
                        if (initialOutput.getfXSV()[x][s][v])
                            optimizationModel.getGrbModel().addConstr(variables.fXSV[x][s][v], GRB.EQUAL, 1, "initialPlacementAsConstraints");
    }

    private void reroutingMigration(Output initialOutput) throws GRBException {
        if (initialOutput != null) {
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < parameters.getPaths().size(); p++)
                        for (int x = 0; x < parameters.getServers().size(); x++)
                            for (int y = 0; y < parameters.getServers().size(); y++) {
                                if (x == y)
                                    continue;
                                if (!parameters.getPaths().get(p).getNodePath().get(0).equals(parameters.getServers().get(x).getNodeParent())
                                        | !parameters.getPaths().get(p).getNodePath().get(parameters.getPaths().get(p).getNodePath().size() - 1).equals(parameters.getServers().get(y).getNodeParent()))
                                    continue;
                                GRBLinExpr expr = new GRBLinExpr();
                                double fXSV = initialOutput.getfXSV()[x][s][v] ? 1.0 : 0.0;
                                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                                    expr.addTerm(parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                            * parameters.getServices().get(s).getFunctions().get(v).getLoad()
                                            * fXSV, variables.fXSV[y][s][v]);
                                optimizationModel.getGrbModel().addConstr(variables.mPSV[p][s][v], GRB.LESS_EQUAL, expr, "reroutingMigration");
                            }

            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                    for (int x = 0; x < parameters.getServers().size(); x++) {
                        double trafficToMigrate = 0;
                        double fXSV = initialOutput.getfXSV()[x][s][v] ? 1.0 : 0.0;
                        for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            trafficToMigrate += parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                    * parameters.getServices().get(s).getFunctions().get(v).getLoad() * fXSV;
                        GRBLinExpr expr = new GRBLinExpr();
                        for (int p = 0; p < parameters.getPaths().size(); p++)
                            expr.addTerm(1.0, variables.mPSV[p][s][v]);
                        GRBLinExpr expr2 = new GRBLinExpr();
                        expr2.addConstant(trafficToMigrate);
                        expr2.addTerm(-trafficToMigrate, variables.fXSV[x][s][v]);
                        optimizationModel.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "reroutingMigration");
                    }
        }
    }
}
