package optimizer.lp;

import gurobi.*;
import manager.Parameters;
import optimizer.Manager;
import optimizer.results.Auxiliary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class Model {

   private static final Logger log = LoggerFactory.getLogger(Model.class);
   private GRBModel grbModel;
   private Variables vars;
   private Parameters pm;
   private double objVal;

   public Model(Parameters pm) {
      this.pm = pm;
      try {
         GRBEnv grbEnv = new GRBEnv();
//         grbEnv.set(GRB.IntParam.LogToConsole, 0);
         grbModel = new GRBModel(grbEnv);
         Callback cb = new Callback();
//         grbModel.set(GRB.IntParam.Method, 3);
         grbModel.setCallback(cb);
         grbModel.getEnv().set(GRB.DoubleParam.MIPGap, (double) pm.getAux().get("gap"));
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

   public GRBLinExpr dimensioningExpr() {
      GRBLinExpr expr = new GRBLinExpr();
      for (int n = 0; n < pm.getNodes().size(); n++)
         expr.addTerm(1.0, vars.xN[n]);
      return expr;
   }

   public GRBLinExpr numUsedServersExpr() {
      GRBLinExpr expr = new GRBLinExpr();
      for (int x = 0; x < pm.getServers().size(); x++)
         expr.addTerm(1.0, vars.fX[x]);
      return expr;
   }

   public GRBLinExpr linkCostsExpr(double weight) {
      GRBLinExpr expr = new GRBLinExpr();
      for (int l = 0; l < pm.getLinks().size(); l++)
         expr.addTerm(weight, vars.kL[l]);
      return expr;
   }

   public GRBLinExpr serverCostsExpr(double weight) {
      GRBLinExpr expr = new GRBLinExpr();
      for (int x = 0; x < pm.getServers().size(); x++)
         expr.addTerm(weight, vars.kX[x]);
      return expr;
   }

   public GRBLinExpr linkUtilizationExpr(double weight) {
      GRBLinExpr expr = new GRBLinExpr();
      for (int l = 0; l < pm.getLinks().size(); l++)
         expr.addTerm(weight, vars.uL[l]);
      return expr;
   }

   public GRBLinExpr serverUtilizationExpr(double weight) {
      GRBLinExpr expr = new GRBLinExpr();
      for (int x = 0; x < pm.getServers().size(); x++)
         expr.addTerm(weight, vars.uX[x]);
      return expr;
   }

   public GRBLinExpr maxUtilizationExpr(double weight) {
      GRBLinExpr expr = new GRBLinExpr();
      expr.addTerm(weight, vars.uMax);
      return expr;
   }

   public GRBLinExpr opexServersExpr() {
      GRBLinExpr expr = new GRBLinExpr();
      for (int x = 0; x < pm.getServers().size(); x++)
         expr.addTerm(1.0, vars.oX[x]);
      return expr;
   }

   public GRBLinExpr functionsChargesExpr() {
      GRBLinExpr expr = new GRBLinExpr();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            expr.addTerm(1.0, vars.oSV[s][v]);
      return expr;
   }

   public GRBLinExpr qosPenaltiesExpr() {
      GRBLinExpr expr = new GRBLinExpr();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               expr.addTerm(1.0, vars.qSDP[s][d][p]);
      return expr;
   }

   public GRBLinExpr numOfMigrations(double weight, GRBModel initialPlacement) {
      GRBLinExpr expr = new GRBLinExpr();
      try {
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (initialPlacement.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 1.0) {
                     expr.addConstant(weight);
                     expr.addTerm(-weight, vars.fXSV[x][s][v]);
                  }
      } catch (Exception e) {
         printLog(log, ERROR, e.getMessage());
      }
      return expr;
   }

   public Double run() throws GRBException {
      grbModel.optimize();
      if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL
              || grbModel.get(GRB.IntAttr.Status) == GRB.Status.INTERRUPTED) {
         objVal = grbModel.get(GRB.DoubleAttr.ObjVal);
         double objValLog = Auxiliary.roundDouble(objVal, 4);
         printLog(log, INFO, "finished [" + objValLog + "]");
         if (objVal > 100000)
            return null;
         return objVal;
      } else if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
         grbModel.computeIIS();
         printISS();
         printLog(log, ERROR, "model is infeasible");
      } else if (grbModel.get(GRB.IntAttr.Status) == GRB.Status.INF_OR_UNBD)
         printLog(log, ERROR, "solution is inf. or unbd.");
      else
         printLog(log, ERROR, "no solution [" + grbModel.get(GRB.IntAttr.Status) + "]");
      return null;
   }

   private void printISS() throws GRBException {
      printLog(log, INFO, "constraints in IIS: ");
      for (GRBConstr constr : grbModel.getConstrs())
         if (constr.get(GRB.IntAttr.IISConstr) > 0)
            printLog(log, INFO, constr.get(GRB.StringAttr.ConstrName));
      printLog(log, INFO, "variables in IIS: ");
      for (GRBVar var : grbModel.getVars())
         if (var.get(GRB.IntAttr.IISLB) > 0 || var.get(GRB.IntAttr.IISUB) > 0)
            printLog(log, INFO, var.get(GRB.StringAttr.VarName));
   }

   public GRBModel getGrbModel() {
      return grbModel;
   }

   public Variables getVars() {
      return vars;
   }

   public void setVars(Variables vars) {
      this.vars = vars;
   }

   public double getObjVal() {
      return objVal;
   }

   private class Callback extends GRBCallback {
      private boolean isPresolving = false;
      private double gap = Double.MAX_VALUE;

      Callback() {
      }

      @Override
      protected void callback() {
         try {
            if (where == GRB.CB_POLLING) {
               // Ignore polling callback
            } else if (where == GRB.CB_PRESOLVE && !isPresolving) {
               printLog(log, INFO, "presolving model");
               isPresolving = true;
            } else if (where == GRB.CB_MIPNODE) {
               double objbst = getDoubleInfo(GRB.CB_MIPNODE_OBJBST);
               double objbnd = getDoubleInfo(GRB.CB_MIPNODE_OBJBND);
               double numerator = Math.abs(objbnd - objbst);
               double denominator = Math.abs(objbst);
               if (denominator > 0) {
                  double newGap = (numerator / denominator) * 100;
                  if (newGap != gap) {
                     gap = newGap;
                     objbst = Auxiliary.roundDouble(objbst, 2);
                     objbnd = Auxiliary.roundDouble(objbnd, 2);
                     double showGap = Auxiliary.roundDouble(newGap, 2);
                     printLog(log, INFO, "[" + objbst + "-" + objbnd + "][" + showGap + "%]");
                  }
               }
            }
            if (Manager.isInterrupted())
               grbModel.terminate();
         } catch (GRBException e) {
            e.printStackTrace();
         }
      }
   }
}
