package lp;

import gui.elements.Scenario;
import gurobi.*;
import lp.constraints.AdditionalConstraints;
import lp.constraints.ExtraConstraints;
import lp.constraints.GeneralConstraints;
import lp.constraints.ModelSpecificConstraints;
import manager.Parameters;
import manager.elements.Function;
import output.Auxiliary;
import output.Definitions;

import static output.Definitions.*;

public class Constraints {

   private OptimizationModel model;
   private Variables vars;
   private Parameters pm;

   public Constraints(Parameters pm, OptimizationModel model, Scenario scenario, GRBModel initialModel) {
      try {
         this.pm = pm;
         this.model = model;
         this.vars = model.getVariables();
         // create link and server load expressions
         GRBLinExpr[] linkLoadExpr = createLinkLoadExpr();
         GRBLinExpr[] serverLoadExpr = createServerLoadExpr();
         // Generate constraints
         new GeneralConstraints(pm, model, scenario);
         new AdditionalConstraints(pm, model, scenario, initialModel, linkLoadExpr);
         new ModelSpecificConstraints(pm, model, scenario, initialModel);
         new ExtraConstraints(pm, model, scenario);
         // create link and server utilization expressions
         GRBLinExpr[] luExpr = createLinkUtilizationExpr(linkLoadExpr);
         GRBLinExpr[] xuExpr = createServerUtilizationExpr(serverLoadExpr);
         // constraint link utilization
         for (int l = 0; l < pm.getLinks().size(); l++)
            model.getGrbModel().addConstr(luExpr[l], GRB.EQUAL, vars.uL[l], uL
                    + "[" + pm.getLinks().get(l).getId() + "]");
         // constraint server utilization if no dimensioning
         if (!scenario.getObjectiveFunction().equals(SERVER_DIMENSIONING))
            for (int x = 0; x < pm.getServers().size(); x++)
               model.getGrbModel().addConstr(xuExpr[x], GRB.EQUAL, vars.uX[x], uX
                       + "[" + pm.getServers().get(x).getId() + "]");
         // set linear cost functions constraints
         if (scenario.getObjectiveFunction().equals(COSTS_OBJ))
            linearCostFunctions(luExpr, vars.kL);
         if (scenario.getObjectiveFunction().equals(COSTS_OBJ)
                 || scenario.getObjectiveFunction().equals(NUM_SERVERS_COSTS_OBJ))
            linearCostFunctions(xuExpr, vars.kX);
         // set max utilization constraint
         if (scenario.getObjectiveFunction().equals(MAX_UTILIZATION_OBJ)) maxUtilization();
         // set dimensioning constraints
         if (scenario.getObjectiveFunction().equals(SERVER_DIMENSIONING)) dimensioning(serverLoadExpr);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   ////////////////////////////////////// Core constraints ///////////////////////////////////////
   private GRBLinExpr[] createLinkLoadExpr() {
      GRBLinExpr[] expressions = new GRBLinExpr[pm.getLinks().size()];
      for (int l = 0; l < pm.getLinks().size(); l++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               if (!pm.getServices().get(s).getTrafficFlow().getPaths().get(p).contains(pm.getLinks().get(l)))
                  continue;
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm((double) pm.getServices().get(s).getTrafficFlow().getDemands().get(d), vars.zSPD[s][p][d]);
            }
         expressions[l] = expr;
      }
      return expressions;
   }

   private GRBLinExpr[] createServerLoadExpr() {
      GRBLinExpr[] expressions = new GRBLinExpr[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               Function function = pm.getServices().get(s).getFunctions().get(v);
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm((pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                          * (double) function.getAttribute(FUNCTION_LOAD_RATIO)), vars.fXSVD[x][s][v][d]);
               expr.addTerm((double) function.getAttribute(FUNCTION_LOAD_RATIO) * (int) function.getAttribute(FUNCTION_OVERHEAD)
                       , vars.fXSV[x][s][v]);
            }
         expressions[x] = expr;
      }
      return expressions;
   }

   private GRBLinExpr[] createLinkUtilizationExpr(GRBLinExpr[] linkLoadExpr) throws GRBException {
      GRBLinExpr[] luExprs = new GRBLinExpr[pm.getLinks().size()];
      for (int l = 0; l < pm.getLinks().size(); l++) {
         GRBLinExpr expr = new GRBLinExpr();
         expr.multAdd(1.0 / (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY), linkLoadExpr[l]);
         luExprs[l] = expr;
      }
      return luExprs;
   }

   private GRBLinExpr[] createServerUtilizationExpr(GRBLinExpr[] serverLoadExpr) throws GRBException {
      GRBLinExpr[] xuExprs = new GRBLinExpr[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++) {
         GRBLinExpr expr = new GRBLinExpr();
         expr.multAdd(1.0 / pm.getServers().get(x).getCapacity(), serverLoadExpr[x]);
         xuExprs[x] = expr;
      }
      return xuExprs;
   }

   private void linearCostFunctions(GRBLinExpr[] exprs, GRBVar[] grbVar) throws GRBException {
      for (int e = 0; e < exprs.length; e++)
         for (int c = 0; c < Auxiliary.costFunctions.getValues().size(); c++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(Auxiliary.costFunctions.getValues().get(c)[0], exprs[e]);
            expr2.addConstant(Auxiliary.costFunctions.getValues().get(c)[1]);
            model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar[e], COSTS_OBJ);
         }
   }

   private void maxUtilization() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         model.getGrbModel().addConstr(vars.uX[x], GRB.LESS_EQUAL, vars.uMax, Definitions.uMax);
      for (int l = 0; l < pm.getLinks().size(); l++)
         model.getGrbModel().addConstr(vars.uL[l], GRB.LESS_EQUAL, vars.uMax, Definitions.uMax);
   }

   private void dimensioning(GRBLinExpr[] serverLoadExpr) throws GRBException {
      for (int n = 0; n < pm.getNodes().size(); n++) {
         GRBLinExpr expr1 = new GRBLinExpr();
         expr1.addTerm((int) pm.getAux(SERVER_DIMENSIONING_CAPACITY), vars.xN[n]);
         GRBLinExpr expr2 = new GRBLinExpr();
         expr2.multAdd((double) pm.getAux(OVERPROVISIONING_SERVER_CAPACITY), serverLoadExpr[n]);
         model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, expr1, SERVER_DIMENSIONING);
      }
   }
}
