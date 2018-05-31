package model;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Model {

    private static final Logger log = LoggerFactory.getLogger(Model.class);
    private ModelParameters mp;

    public Model(ModelParameters modelParameters) {
        this.mp = modelParameters;
    }

    public void setObjectiveFunction(GRBLinExpr expr) throws GRBException {
        mp.grbModel.setObjective(expr, GRB.MINIMIZE);
    }

    public GRBLinExpr getExprLinkCosts() throws GRBException {

        GRBLinExpr expr = new GRBLinExpr();
        for (int l = 0; l < mp.ip.getLinks().size(); l++)
            expr.addTerm((1 - mp.ip.getBeta()) / mp.ip.getLinks().size(), mp.lk[l]);
        setLinkUtilizationCosts();
        return expr;
    }

    private void setLinkUtilizationCosts() throws GRBException {

        for (int l = 0; l < mp.ip.getLinks().size(); l++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                    if (!mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).contains(mp.ip.getLinks().get(l)))
                        continue;
                    for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm(mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) / (double) mp.ip.getLinks().get(l).getAttribute("capacity"), mp.rSPD[s][p][d]);
                }
            mp.grbModel.addConstr(expr, GRB.EQUAL, mp.lu[l], "Link Utilization [" + l + "]");
            setLinearCostFunctions(expr, mp.lk[l]);
        }

    }

    public GRBLinExpr getExprServerCosts() throws GRBException {

        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < mp.ip.getServers().size(); x++)
            expr.addTerm(mp.ip.getBeta() / mp.ip.getServers().size(), mp.xk[x]);
        setServerUtilizationCosts();
        return expr;
    }

    private void setServerUtilizationCosts() throws GRBException {
        for (int x = 0; x < mp.ip.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++) {
                    for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                        expr.addTerm((mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                        * mp.ip.getServices().get(s).getFunctions().get(v).getLoad()) / mp.ip.getServers().get(x).getCapacity()
                                , mp.fXSVD[x][s][v][d]);
                    }
                }
            mp.grbModel.addConstr(expr, GRB.EQUAL, mp.xu[x], "Server Utilization [" + x + "]");
            setLinearCostFunctions(expr, mp.xk[x]);
        }
    }

    private void setLinearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {

        for (int l = 0; l < mp.linearCostFunctions.getValues().size(); l++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(mp.linearCostFunctions.getValues().get(l)[0], expr);
            expr2.addConstant(mp.linearCostFunctions.getValues().get(l)[1]);
            mp.grbModel.addConstr(expr2, GRB.LESS_EQUAL, grbVar, "Linear Cost function");
        }
    }

    public void run(int numOfReplicas) throws GRBException {

        mp.grbModel.optimize();
        if (mp.grbModel.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
            mp.grbModel.computeIIS();
            log.error("Model is not feasible");
            System.exit(0);
        }
    }

    public ModelResults generateResults(ModelResults initialResults) throws GRBException {
        ModelResults modelResults = new ModelResults(mp);

        if (initialResults != null) {
            modelResults.calculateNumberOfMigrations(initialResults);
            modelResults.calculateNumberOfReplications();
        }

        if (Launcher.debugging)
            modelResults.printSolution();
        else
            modelResults.printResults(mp.grbModel.get(GRB.DoubleAttr.ObjVal), modelResults);

        return modelResults;
    }

    public void finishModel() throws GRBException {
        mp.grbModel.dispose();
        mp.grbEnv.dispose();
    }
}
