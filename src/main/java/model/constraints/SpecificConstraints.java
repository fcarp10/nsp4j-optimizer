package model.constraints;


import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import model.Model;
import model.Output;
import model.Variables;

public class SpecificConstraints {

    private Model model;
    private Variables variables;
    private Parameters parameters;

    public SpecificConstraints(Model model) {
        this.model = model;
        this.parameters = model.getParameters();
        this.variables = model.getVariables();
    }

    public void noParallelPaths() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                expr.addTerm(1.0, variables.tSP[s][p]);
            model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, "noParallelPaths");
        }
    }

    public void setVariablesFromInitialPlacementAsConstraints(Output initialOutput) throws GRBException {
        for (int x = 0; x < initialOutput.getVariables().fXSV.length; x++)
            for (int s = 0; s < initialOutput.getVariables().fXSV[x].length; s++)
                for (int v = 0; v < initialOutput.getVariables().fXSV[x][s].length; v++)
                    if (initialOutput.getVariables().fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                        model.getGrbModel().addConstr(variables.fXSV[x][s][v], GRB.EQUAL, 1, "setVariablesFromInitialPlacementAsConstraints");
    }

//    public void reRoutingFromPreviousPlacement(Output initialOutput) throws GRBException {
//        for (int s = 0; s < parameters.getServices().size(); s++)
//            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
//                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
//                    for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
//                        GRBLinExpr expr = new GRBLinExpr();
//                        for (int n = 0; n < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++)
//                            for (int x = 0; x < parameters.getServers().size(); x++)
//                                if (parameters.getServers().get(x).getNodeParent().equals(parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n)))
//                                    if (initialOutput.getVariables().fXSVD[x][s][v][d].get(GRB.DoubleAttr.X) == 1) {
//                                        expr.addConstant(1.0);
//                                        expr.addTerm(-1.0, variables.fXSVD[x][s][v][d]);
//                                    }
//                        model.getGrbModel().addConstr(variables.tSPD[s][p][d], GRB.LESS_EQUAL, expr, "reRoutingFromPreviousPlacement");
//                    }
//    }
}
