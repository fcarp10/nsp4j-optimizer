package lp;


import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import results.ModelOutput;
import utils.Scenario;

public class SpecialConstraints {

    private Parameters pm;
    private OptimizationModel optimizationModel;
    private Variables variables;

    public SpecialConstraints(Parameters pm, OptimizationModel optimizationModel, Scenario scenario, ModelOutput initialModelOutput) throws GRBException {
        this.pm = pm;
        this.optimizationModel = optimizationModel;
        this.variables = optimizationModel.getVariables();
        if (scenario.getConstraints().get("noParallelPaths"))
            noParallelPaths();
        if (scenario.getConstraints().get("initialPlacementAsConstraints"))
            initialPlacementAsConstraints(initialModelOutput);
        if (scenario.getConstraints().get("reroutingMigration"))
            reroutingMigration(initialModelOutput);
    }

    private void noParallelPaths() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                expr.addTerm(1.0, variables.tSP[s][p]);
            optimizationModel.getGrbModel().addConstr(expr, GRB.EQUAL, 1, "noParallelPaths");
        }
    }

    private void initialPlacementAsConstraints(ModelOutput initialModelOutput) throws GRBException {
        if (initialModelOutput != null)
            for (int x = 0; x < initialModelOutput.getfXSV().length; x++)
                for (int s = 0; s < initialModelOutput.getfXSV()[x].length; s++)
                    for (int v = 0; v < initialModelOutput.getfXSV()[x][s].length; v++)
                        if (initialModelOutput.getfXSV()[x][s][v])
                            optimizationModel.getGrbModel().addConstr(variables.fXSV[x][s][v], GRB.EQUAL, 1, "initialPlacementAsConstraints");
    }

    private void reroutingMigration(ModelOutput initialModelOutput) throws GRBException {
        if (initialModelOutput != null) {
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < pm.getPaths().size(); p++)
                        for (int x = 0; x < pm.getServers().size(); x++)
                            for (int y = 0; y < pm.getServers().size(); y++) {
                                if (x == y)
                                    continue;
                                if (!pm.getPaths().get(p).getNodePath().get(0).equals(pm.getServers().get(x).getNodeParent())
                                        | !pm.getPaths().get(p).getNodePath().get(pm.getPaths().get(p).getNodePath().size() - 1).equals(pm.getServers().get(y).getNodeParent()))
                                    continue;
                                GRBLinExpr expr = new GRBLinExpr();
                                double fXSV = initialModelOutput.getfXSV()[x][s][v] ? 1.0 : 0.0;
                                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                                    expr.addTerm(pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                            * pm.getServices().get(s).getFunctions().get(v).getLoad()
                                            * fXSV, variables.fXSV[y][s][v]);
                                optimizationModel.getGrbModel().addConstr(variables.mPSV[p][s][v], GRB.LESS_EQUAL, expr, "reroutingMigration");
                            }

            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int x = 0; x < pm.getServers().size(); x++) {
                        double trafficToMigrate = 0;
                        double fXSV = initialModelOutput.getfXSV()[x][s][v] ? 1.0 : 0.0;
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            trafficToMigrate += pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                    * pm.getServices().get(s).getFunctions().get(v).getLoad() * fXSV;
                        GRBLinExpr expr = new GRBLinExpr();
                        for (int p = 0; p < pm.getPaths().size(); p++)
                            expr.addTerm(1.0, variables.mPSV[p][s][v]);
                        GRBLinExpr expr2 = new GRBLinExpr();
                        expr2.addConstant(trafficToMigrate);
                        expr2.addTerm(-trafficToMigrate, variables.fXSV[x][s][v]);
                        optimizationModel.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "reroutingMigration");
                    }
        }
    }
}
