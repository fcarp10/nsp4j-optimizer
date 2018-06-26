package model;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Model {

    private static final Logger log = LoggerFactory.getLogger(Model.class);
    private ParametersModel pm;

    public Model(ParametersModel parametersModel) {
        this.pm = parametersModel;
    }

    public void setObjectiveFunction(GRBLinExpr expr) throws GRBException {
        pm.grbModel.setObjective(expr, GRB.MINIMIZE);
    }

    public GRBLinExpr usedServersExpr() {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            expr.addTerm(1.0 / pm.ip.getServers().size(), pm.fX[x]);
        return expr;
    }

    public GRBLinExpr linkUtilizationCostsExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int l = 0; l < pm.ip.getLinks().size(); l++)
            expr.addTerm(weight / pm.ip.getLinks().size(), pm.ukL[l]);
        return expr;
    }

    public GRBLinExpr serverUtilizationCostsExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            expr.addTerm(weight / pm.ip.getServers().size(), pm.ukX[x]);
        return expr;
    }

    public GRBLinExpr linkUtilizationExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int l = 0; l < pm.ip.getLinks().size(); l++)
            expr.addTerm(weight / pm.ip.getLinks().size(), pm.uL[l]);
        return expr;
    }

    public GRBLinExpr serverUtilizationExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            expr.addTerm(weight / pm.ip.getServers().size(), pm.uX[x]);
        return expr;
    }

    public GRBLinExpr migrations(ResultsModel initialResultsModel, double weight) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            for (int s = 0; s < pm.ip.getServices().size(); s++)
                for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++) {
                    if (initialResultsModel.getPm().fXSV[x][s][v].get(GRB.DoubleAttr.X) == 0) continue;
                    expr.addConstant(1.0);
                    expr.addTerm(-1.0, pm.fXSV[x][s][v]);
                }
        expr.multAdd(weight / pm.ip.getAuxTotalNumberOfFunctions(), expr);
        return expr;
    }

    public GRBLinExpr replications(double weight) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++) {
                GRBLinExpr expr2 = new GRBLinExpr();
                for (int x = 0; x < pm.ip.getServers().size(); x++)
                    expr2.addTerm(1.0, pm.fXSV[x][s][v]);
                expr.add(expr2);
                expr.addConstant(-1.0);
            }
        expr.multAdd(weight / pm.ip.getAuxTotalNumberOfFunctions(), expr);
        return expr;
    }

    public double run() throws GRBException {
        pm.grbModel.optimize();
        if (pm.grbModel.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
            pm.grbModel.computeIIS();
            log.error("Model is not feasible");
            return -1;
        } else return pm.grbModel.get(GRB.DoubleAttr.ObjVal);
    }

    public void finishModel() throws GRBException {
        pm.grbModel.dispose();
        pm.grbEnv.dispose();
    }
}
