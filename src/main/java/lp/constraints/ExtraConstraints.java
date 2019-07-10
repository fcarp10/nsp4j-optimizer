package lp.constraints;

import gui.elements.Scenario;
import gurobi.*;
import lp.OptimizationModel;
import lp.Variables;
import manager.Parameters;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import static output.Definitions.*;

public class ExtraConstraints {

   private OptimizationModel model;
   private Variables vars;
   private Parameters pm;

   public ExtraConstraints(Parameters pm, OptimizationModel model, Scenario scenario) {
      try {
         this.pm = pm;
         this.model = model;
         this.vars = model.getVariables();
         if (scenario.getConstraints().get(CONST_REP)) constraintReplications();
         if (scenario.getConstraints().get(FIX_SRC_DST)) fixSrcDst();
         if (scenario.getConstraints().get(USE_CLOUD)) useCloudServers();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   // Constraint replications
   private void constraintReplications() throws GRBException {
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

   // Fix src-dst functions
   private void fixSrcDst() throws GRBException {
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
         model.getGrbModel().addConstr(exprSrc, GRB.EQUAL, 1.0, FIX_SRC_DST);
         model.getGrbModel().addConstr(exprDst, GRB.EQUAL, 1.0, FIX_SRC_DST);
      }
   }

   // force to use cloud
   private void useCloudServers() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) != null)
            model.getGrbModel().addConstr(vars.fX[x], GRB.EQUAL, 1.0, SERVER_DIMENSIONING);
   }
}
