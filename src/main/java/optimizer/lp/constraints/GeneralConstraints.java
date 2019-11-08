package optimizer.lp.constraints;

import gurobi.*;
import manager.elements.Function;
import manager.elements.Service;
import optimizer.gui.Scenario;
import optimizer.lp.Model;
import optimizer.lp.Variables;
import org.graphstream.graph.Node;

import static optimizer.Parameters.*;

public class GeneralConstraints {

   private Model model;
   private Variables vars;
   private manager.Parameters pm;

   public GeneralConstraints(manager.Parameters pm, Model model, Scenario scenario, GRBModel initialPlacement) {
      try {
         this.pm = pm;
         this.model = model;
         this.vars = model.getVariables();

         // create link and server load expressions
         GRBLinExpr[] linkLoadExpr = createLinkLoadExpr();
         GRBLinExpr[] serverLoadExpr = createServerLoadExpr();

         // create link and server utilization expressions
         GRBLinExpr[] luExpr = createLinkUtilizationExpr(linkLoadExpr);
         GRBLinExpr[] xuExpr = createServerUtilizationExpr(serverLoadExpr);

         // Generate additional constraints
         new ModelSpecificConstraints(pm, model, scenario, initialPlacement, serverLoadExpr, linkLoadExpr, xuExpr, luExpr);
         new OtherConstraints(pm, model, scenario);

         // constraint link utilization
         for (int l = 0; l < pm.getLinks().size(); l++)
            model.getGrbModel().addConstr(luExpr[l], GRB.EQUAL, vars.uL[l], uL
                    + "[" + pm.getLinks().get(l).getId() + "]");

         // constraint server utilization if no dimensioning
         if (!scenario.getObjFunc().equals(SERVER_DIMENSIONING))
            for (int x = 0; x < pm.getServers().size(); x++)
               model.getGrbModel().addConstr(xuExpr[x], GRB.EQUAL, vars.uX[x], uX + "[x] --> " + "[" + x + "]");

         // create rest of constraints
         if (scenario.getConstraints().get(RP1)) RP1();
         if (scenario.getConstraints().get(RP2)) RP2();
         if (scenario.getConstraints().get(PF1)) PF1();
         if (scenario.getConstraints().get(PF2)) PF2();
         if (scenario.getConstraints().get(PF3)) PF3();
         if (scenario.getConstraints().get(FD1)) FD1();
         if (scenario.getConstraints().get(FD2)) FD2();
         if (scenario.getConstraints().get(FD3)) FD3();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private GRBLinExpr[] createLinkLoadExpr() {
      GRBLinExpr[] expressions = new GRBLinExpr[pm.getLinks().size()];
      for (int l = 0; l < pm.getLinks().size(); l++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               if (!pm.getServices().get(s).getTrafficFlow().getPaths().get(p).contains(pm.getLinks().get(l)))
                  continue;
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
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
                  if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                     expr.addTerm((pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                             * (double) function.getAttribute(FUNCTION_LOAD_RATIO)), vars.fXSVD[x][s][v][d]);
               expr.addTerm((int) function.getAttribute(FUNCTION_OVERHEAD), vars.fXSV[x][s][v]);
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

   // One path per demand
   private void RP1() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            if (pm.getServices().get(s).getTrafficFlow().getAux().get(d)) {
               GRBLinExpr expr = new GRBLinExpr();
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  expr.addTerm(1.0, vars.zSPD[s][p][d]);
               model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, RP1 + "[s][d] --> "
                       + "[" + s + "][" + d + "]");
            }
   }

   // Activate path for service
   private void RP2() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                  model.getGrbModel().addConstr(vars.zSPD[s][p][d], GRB.LESS_EQUAL, vars.zSP[s][p], RP2 + "[s][p][d] --> "
                          + "[" + s + "]" + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "[" + d + "]");
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                  expr.addTerm(1.0, vars.zSPD[s][p][d]);
            model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.zSP[s][p], RP2 + "[s][p] --> "
                    + "[" + s + "]" + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath());
         }
   }

   // Paths constrained by functions
   private void PF1() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int x = 0; x < pm.getServers().size(); x++)
               expr.addTerm(1.0, vars.fXSV[x][s][v]);
            if ((boolean) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_REPLICABLE)) {
               GRBLinExpr expr2 = new GRBLinExpr();
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  expr2.addTerm(1.0, vars.zSP[s][p]);
               model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, PF1 + "[s][v] --> "
                       + "[" + s + "][" + v + "]");
            } else
               model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, 1.0, PF1 + "[s][v] --> "
                       + "[" + s + "][" + v + "]");
         }
   }

   // Function placement
   private void PF2() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service service = pm.getServices().get(s);
         for (int p = 0; p < service.getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
               if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                  for (int v = 0; v < service.getFunctions().size(); v++) {
                     GRBLinExpr middleExpr = new GRBLinExpr();
                     for (int n = 0; n < service.getTrafficFlow().getPaths().get(p).getNodePath().size(); n++)
                        for (int x = 0; x < pm.getServers().size(); x++)
                           if (pm.getServers().get(x).getParent()
                                   .equals(service.getTrafficFlow().getPaths().get(p).getNodePath().get(n)))
                              middleExpr.addTerm(1.0, vars.fXSVD[x][s][v][d]);
                     model.getGrbModel().addConstr(vars.zSPD[s][p][d], GRB.LESS_EQUAL, middleExpr, PF2
                             + "[s][p][d][v] --> " + "[" + s + "]"
                             + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath()
                             + "[" + d + "][" + v + "]");
                  }
      }
   }

   // Count number of used servers
   private void PF3() throws GRBException {
      if (vars.fX != null)
         for (int x = 0; x < pm.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                  expr.addTerm(1.0 / pm.getTotalNumFunctions(), vars.fXSV[x][s][v]);
                  expr2.addTerm(1.0, vars.fXSV[x][s][v]);
               }
            model.getGrbModel().addConstr(vars.fX[x], GRB.GREATER_EQUAL, expr, PF3);
            model.getGrbModel().addConstr(vars.fX[x], GRB.LESS_EQUAL, expr2, PF3);
         }
   }

   // One function per demand
   private void FD1() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               if (pm.getServices().get(s).getTrafficFlow().getAux().get(d)) {
                  GRBLinExpr expr = new GRBLinExpr();
                  for (int x = 0; x < pm.getServers().size(); x++)
                     expr.addTerm(1.0, vars.fXSVD[x][s][v][d]);
                  model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, FD1 + "[s][v][d] --> "
                          + "[" + s + "][" + v + "][" + d + "]");
               }
   }

   // Mapping functions with demands
   private void FD2() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                     model.getGrbModel().addConstr(vars.fXSVD[x][s][v][d], GRB.LESS_EQUAL, vars.fXSV[x][s][v], FD2
                             + "[s][v][x][d] --> " + "[" + s + "][" + v + "][" + x + "][" + d + "]");
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++) {
               GRBLinExpr expr = new GRBLinExpr();
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
                     expr.addTerm(1.0, vars.fXSVD[x][s][v][d]);
               model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.fXSV[x][s][v], FD2 + "[s][v][x] --> "
                       + "[" + s + "][" + v + "][" + x + "]");
            }
   }

   // Functions sequence order
   private void FD3() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service se = pm.getServices().get(s);
         for (int d = 0; d < se.getTrafficFlow().getDemands().size(); d++)
            if (pm.getServices().get(s).getTrafficFlow().getAux().get(d)) {
               for (int p = 0; p < se.getTrafficFlow().getPaths().size(); p++)
                  for (int v = 1; v < se.getFunctions().size(); v++) {
                     for (int n = 0; n < se.getTrafficFlow().getPaths().get(p).getNodePath().size(); n++) {
                        GRBLinExpr expr = new GRBLinExpr();
                        GRBLinExpr expr2 = new GRBLinExpr();
                        Node nodeN = se.getTrafficFlow().getPaths().get(p).getNodePath().get(n);
                        for (int m = 0; m <= n; m++) {
                           Node nodeM = se.getTrafficFlow().getPaths().get(p).getNodePath().get(m);
                           for (int x = 0; x < pm.getServers().size(); x++)
                              if (pm.getServers().get(x).getParent().equals(nodeM))
                                 expr.addTerm(1.0, vars.fXSVD[x][s][v - 1][d]);
                        }
                        for (int x = 0; x < pm.getServers().size(); x++)
                           if (pm.getServers().get(x).getParent().equals(nodeN))
                              expr.addTerm(-1.0, vars.fXSVD[x][s][v][d]);

                        expr2.addConstant(-1);
                        expr2.addTerm(1.0, vars.zSPD[s][p][d]);
                        model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, FD3 + "[s][d][p][v][n] --> "
                                + "[" + s + "][" + d + "]"
                                + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath()
                                + "[" + v + "][" + se.getTrafficFlow().getPaths().get(p).getNodePath().get(n).getId() + "]");
                     }
                  }
            }
      }
   }
}
