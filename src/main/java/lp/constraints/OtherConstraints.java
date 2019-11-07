package lp.constraints;

import gui.Scenario;
import gurobi.*;
import lp.Model;
import lp.Variables;
import manager.Parameters;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import static output.Parameters.*;

public class OtherConstraints {

   private Model model;
   private Variables vars;
   private Parameters pm;

   public OtherConstraints(Parameters pm, Model model, Scenario scenario, GRBModel initialModel) {
      try {
         this.pm = pm;
         this.model = model;
         this.vars = model.getVariables();
         if (scenario.getConstraints().get(SINGLE_PATH)) singlePath();
         if (scenario.getConstraints().get(SET_INIT_PLC)) setInitPlc(initialModel);
         if (scenario.getConstraints().get(FORCE_SRC_DST)) forceSrcDst();
         if (scenario.getConstraints().get(CONST_REP)) constRep();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   // Single path
   private void singlePath() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            expr.addTerm(1.0, vars.zSP[s][p]);
         model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, SINGLE_PATH);
      }
   }

   // Initial placement as constraints
   private void setInitPlc(GRBModel initialModel) throws GRBException {
      if (initialModel != null) {
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (initialModel.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]")
                          .get(GRB.DoubleAttr.X) == 1.0)
                     model.getGrbModel().addConstr(vars.fXSV[x][s][v], GRB.EQUAL, 1, SET_INIT_PLC);
      }
   }

   // Fix src-dst functions
   private void forceSrcDst() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(0);
         Node srcNode = path.getNodePath().get(0);
         Node dstNode = path.getNodePath().get(path.getNodePath().size() - 1);
         GRBLinExpr exprSrc = new GRBLinExpr();
         GRBLinExpr exprDst = new GRBLinExpr();
         for (int x = 0; x < pm.getServers().size(); x++) {
            if (pm.getServers().get(x).getParent().getId().equals(srcNode.getId()))
               exprSrc.addTerm(1.0, vars.fXSV[x][s][0]);
            if (pm.getServers().get(x).getParent().getId().equals(dstNode.getId()))
               exprDst.addTerm(1.0, vars.fXSV[x][s][pm.getServices().get(s).getFunctions().size() - 1]);
         }
         model.getGrbModel().addConstr(exprSrc, GRB.EQUAL, 1.0, FORCE_SRC_DST);
         model.getGrbModel().addConstr(exprDst, GRB.EQUAL, 1.0, FORCE_SRC_DST);
      }
   }

   // Constraint replications
   private void constRep() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            expr.addTerm(1.0, vars.zSP[s][p]);
         int minPaths = (int) pm.getServices().get(s).getAttribute(SERVICE_MIN_PATHS);
         int maxPaths = (int) pm.getServices().get(s).getAttribute(SERVICE_MAX_PATHS);
         model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, minPaths, CONST_REP);
         model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxPaths, CONST_REP);
      }
   }
}
