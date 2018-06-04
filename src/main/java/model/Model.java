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

    public GRBLinExpr exprLinkCosts(double weight) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        for (int l = 0; l < pm.ip.getLinks().size(); l++)
            expr.addTerm(weight / pm.ip.getLinks().size(), pm.lk[l]);
        return expr;
    }

    public GRBLinExpr exprServerCosts(double weight) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            expr.addTerm(weight / pm.ip.getServers().size(), pm.xk[x]);
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
