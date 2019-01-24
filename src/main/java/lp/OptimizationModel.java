package lp;

import gurobi.*;
import manager.Manager;
import manager.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import output.Auxiliary;

import static output.Auxiliary.*;
import static output.Definitions.*;

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
         grbModel.getEnv().set(GRB.DoubleParam.MIPGap, (double) parameters.getAux().get("gap"));
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

   public double run() throws GRBException {
      grbModel.optimize();
      if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
         objVal = Auxiliary.roundDouble(grbModel.get(GRB.DoubleAttr.ObjVal), 2);
         printLog(log, INFO, "finished [" + objVal + "]");
         return objVal;
      } else if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
         printLog(log, ERROR, "model is infeasible");
      } else if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.INF_OR_UNBD)
         printLog(log, ERROR, "solution is inf. or unbd.");
      else if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.INTERRUPTED)
         printLog(log, INFO, "optimization interrupted");
      else
         printLog(log, ERROR, "no solution [" + grbModel.get(GRB.IntAttr.Status) + "]");
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
               double newGap = Auxiliary.roundDouble(((objbst - objbnd) / objbnd) * 100, 2);
               if (newGap != gap) {
                  if (newGap <= 100)
                     printLog(log, INFO, "gap [" + newGap + "%]");
                  else
                     printLog(log, INFO, "reducing solution space");
                  gap = newGap;
               }
            }
            if (Manager.isInterrupted())
               abort();
         } catch (GRBException e) {
            e.printStackTrace();
         }
      }
   }
}
