package model;

import filemanager.Parameters;
import gurobi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Model {

    private static final Logger log = LoggerFactory.getLogger(Model.class);

    private GRBModel grbModel;
    private GRBEnv grbEnv;
    private Variables variables;
    private Parameters parameters;

    public Model(Parameters parameters) {
        this.parameters = parameters;
        try {
            grbEnv = new GRBEnv();
            grbEnv.set(GRB.IntParam.LogToConsole, 0);
            grbModel = new GRBModel(grbEnv);
            grbModel.getEnv().set(GRB.DoubleParam.MIPGap, parameters.getGap());
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public void setObjectiveFunction(GRBLinExpr expr) throws GRBException {
        grbModel.setObjective(expr, GRB.MINIMIZE);
    }

    public GRBLinExpr usedServersExpr() {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < parameters.getServers().size(); x++)
            expr.addTerm(1.0, variables.fX[x]);
        return expr;
    }

    public GRBLinExpr linkUtilizationCostsExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int l = 0; l < parameters.getLinks().size(); l++)
            expr.addTerm(weight, variables.ukL[l]);
        return expr;
    }

    public GRBLinExpr serverUtilizationCostsExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < parameters.getServers().size(); x++)
            expr.addTerm(weight, variables.ukX[x]);
        return expr;
    }

    public GRBLinExpr linkUtilizationExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int l = 0; l < parameters.getLinks().size(); l++)
            expr.addTerm(weight, variables.uL[l]);
        return expr;
    }

    public GRBLinExpr serverUtilizationExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < parameters.getServers().size(); x++)
            expr.addTerm(weight, variables.uX[x]);
        return expr;
    }

    public GRBLinExpr migrations(Output initialOutput, double weight) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < parameters.getServers().size(); x++)
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
                    if (initialOutput.getVariables().fXSV[x][s][v].get(GRB.DoubleAttr.X) == 0) continue;
                    expr.addConstant(weight);
                    expr.addTerm(-weight, this.variables.fXSV[x][s][v]);
                }
        return expr;
    }

    public GRBLinExpr replications(double weight) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
                GRBLinExpr expr2 = new GRBLinExpr();
                for (int x = 0; x < parameters.getServers().size(); x++)
                    expr2.addTerm(weight, variables.fXSV[x][s][v]);
                expr.add(expr2);
                expr.addConstant(-weight);
            }
        return expr;
    }

    public double run() throws GRBException {
        grbModel.optimize();
        if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
            grbModel.computeIIS();
            log.error("Model is not feasible");
            return -1;
        } else return grbModel.get(GRB.DoubleAttr.ObjVal);
    }

    public void finishModel() throws GRBException {
        grbModel.dispose();
        grbEnv.dispose();
    }

    public GRBModel getGrbModel() {
        return grbModel;
    }

    public GRBEnv getGrbEnv() {
        return grbEnv;
    }

    public Variables getVariables() {
        return variables;
    }

    public void setVariables(Variables variables) {
        this.variables = variables;
    }

    public Parameters getParameters() {
        return parameters;
    }
}
