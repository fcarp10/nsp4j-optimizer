package lp;

import gurobi.*;
import manager.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import results.Auxiliary;

import static results.Auxiliary.ERROR;
import static results.Auxiliary.INFO;
import static results.Auxiliary.printLog;

public class OptimizationModel {

    private static final Logger log = LoggerFactory.getLogger(OptimizationModel.class);
    private GRBModel grbModel;
    private GRBEnv grbEnv;
    private Variables variables;
    private Parameters parameters;
    private double objVal;

    public OptimizationModel(Parameters parameters) {
        this.parameters = parameters;
        try {
            grbEnv = new GRBEnv();
            grbEnv.set(GRB.IntParam.LogToConsole, 0);
            grbModel = new GRBModel(grbEnv);
            Callback cb = new Callback();
            grbModel.setCallback(cb);
            grbModel.getEnv().set(GRB.DoubleParam.MIPGap, parameters.getGap());
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public void setObjectiveFunction(GRBLinExpr expr, boolean isMaximization) throws GRBException {
        if (!isMaximization)
            grbModel.setObjective(expr, GRB.MINIMIZE);
        else
            grbModel.setObjective(expr, GRB.MAXIMIZE);
    }

    public GRBLinExpr usedServersExpr() {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < parameters.getServers().size(); x++)
            expr.addTerm(1.0, variables.pX[x]);
        return expr;
    }

    public GRBLinExpr linkCostsExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int l = 0; l < parameters.getLinks().size(); l++)
            expr.addTerm(weight, variables.kL[l]);
        return expr;
    }

    public GRBLinExpr serverCostsExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < parameters.getServers().size(); x++)
            expr.addTerm(weight, variables.kX[x]);
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

    public GRBLinExpr serviceDelayExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                expr.addTerm(weight, variables.dSP[s][p]);
        return expr;
    }

    public double run() throws GRBException {
        grbModel.optimize();
        if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
            objVal = grbModel.get(GRB.DoubleAttr.ObjVal);
            printLog(log, INFO, "opt. finished [objVal --> " + objVal + "]");
            return objVal;
        } else if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
//            grbModel.computeIIS();
            printLog(log, ERROR, "model is infeasible");
        } else if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.INF_OR_UNBD)
            printLog(log, ERROR, "solution is inf. or unbd.");
        else
            printLog(log, ERROR, "no solution [status --> " + grbModel.get(GRB.IntAttr.Status) + "]");
        return -1;
    }

    public GRBModel getGrbModel() {
        return grbModel;
    }

    public Variables getVariables() {
        return variables;
    }

    public void setVariables(Variables variables) {
        this.variables = variables;
    }

    public double getObjVal() {
        return objVal;
    }

    private class Callback extends GRBCallback {

        private int status;
        private double gap;

        Callback() {
        }

        @Override
        protected void callback() {
            try {
                if (where == GRB.CB_PRESOLVE && status != GRB.CB_PRESOLVE) {
                    status = GRB.CB_PRESOLVE;
                    printLog(log, INFO, "pre-resolving model");
                } else if (where == GRB.CB_MIPNODE) {
                    double objbst = getDoubleInfo(GRB.CB_MIPNODE_OBJBST);
                    double objbnd = getDoubleInfo(GRB.CB_MIPNODE_OBJBND);
                    double newGap = ((objbst - objbnd) / objbnd) * 100;
                    if (newGap != gap) {
                        printLog(log, INFO, "gap [" + Auxiliary.roundDouble(newGap, 2) + "%]");
                        gap = newGap;
                    }
                }
            } catch (GRBException e) {
                e.printStackTrace();
            }
        }
    }
}
