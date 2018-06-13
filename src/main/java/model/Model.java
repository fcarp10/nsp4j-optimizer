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

    public GRBLinExpr exprLinkUtilizationCosts(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int l = 0; l < pm.ip.getLinks().size(); l++)
            expr.addTerm(weight / pm.ip.getLinks().size(), pm.ukL[l]);
        return expr;
    }

    public GRBLinExpr exprServerUtilizationCosts(double weight){
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            expr.addTerm(weight / pm.ip.getServers().size(), pm.ukX[x]);
        return expr;
    }

    public GRBLinExpr exprLinkUtilization(double weight){
        GRBLinExpr expr = new GRBLinExpr();
        for (int l = 0; l < pm.ip.getLinks().size(); l++)
            expr.addTerm(weight / pm.ip.getLinks().size(), pm.uL[l]);
        return expr;
    }

    public GRBLinExpr exprServerUtilization(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            expr.addTerm(weight / pm.ip.getServers().size(), pm.uX[x]);
        return expr;
    }

    public GRBLinExpr exprMigrationCosts(double weight){
        GRBLinExpr expr = new GRBLinExpr();
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                expr.addTerm(weight / pm.ip.getAuxTotalNumberOfFunctions(), pm.mkVS[s][v]);
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
