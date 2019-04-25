package lp.constraints;

import gui.elements.Scenario;
import gurobi.*;
import lp.OptimizationModel;
import lp.Variables;
import manager.Parameters;

import static output.Definitions.*;

public class ModelSpecificConstraints {

   private OptimizationModel model;
   private Variables vars;
   private Parameters pm;

   public ModelSpecificConstraints(Parameters pm, OptimizationModel model, Scenario scenario, GRBModel initialModel) {
      try {
         this.pm = pm;
         this.model = model;
         this.vars = model.getVariables();
         if (scenario.getConstraints().get(IP)) IP();
         if (scenario.getConstraints().get(IP_MGR)) IPMGR();
         if (scenario.getConstraints().get(REP)) REP(initialModel);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   // Count number of used servers
   private void IP() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++) {
         GRBLinExpr expr = new GRBLinExpr();
         GRBLinExpr expr2 = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               expr.addTerm(1.0 / pm.getTotalNumFunctions(), vars.fXSV[x][s][v]);
               expr2.addTerm(1.0, vars.fXSV[x][s][v]);
            }
         model.getGrbModel().addConstr(vars.fX[x], GRB.GREATER_EQUAL, expr, IP);
         model.getGrbModel().addConstr(vars.fX[x], GRB.LESS_EQUAL, expr2, IP);
      }
   }

   // No parallel paths
   private void IPMGR() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            expr.addTerm(1.0, vars.zSP[s][p]);
         model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, IP_MGR);
      }
   }

   // Initial placement as constraints
   private void REP(GRBModel initialModel) throws GRBException {
      if (initialModel != null) {
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (initialModel.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]")
                          .get(GRB.DoubleAttr.X) == 1.0)
                     model.getGrbModel().addConstr(vars.fXSV[x][s][v], GRB.EQUAL, 1, REP);
      }
   }
}
