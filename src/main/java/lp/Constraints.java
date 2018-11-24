package lp;

import gui.elements.Scenario;
import gurobi.*;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Server;
import manager.elements.Service;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import output.Auxiliary;

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
         linkUtilization();
         serverUtilization();
         serviceDelay(initialModel);
         if (scenario.getConstraints().get("countNumberOfUsedServers")) countNumberOfUsedServers();
         if (scenario.getConstraints().get("onePathPerDemand")) onePathPerDemand();
         if (scenario.getConstraints().get("activatePathForService")) activatePathForService();
         if (scenario.getConstraints().get("pathsConstrainedByFunctions")) pathsConstrainedByFunctions();
         if (scenario.getConstraints().get("functionPlacement")) functionPlacement();
         if (scenario.getConstraints().get("oneFunctionPerDemand")) oneFunctionPerDemand();
         if (scenario.getConstraints().get("mappingFunctionsWithDemands")) mappingFunctionsWithDemands();
         if (scenario.getConstraints().get("functionSequenceOrder")) functionSequenceOrder();
         if (scenario.getConstraints().get("noParallelPaths")) noParallelPaths();
         if (scenario.getConstraints().get("initialPlacementAsConstraints"))
            initialPlacementAsConstraints(initialModel);
         if (scenario.getConstraints().get("synchronizationTraffic")) synchronizationTraffic();
         if (scenario.getConstraints().get("constraintReplications")) constraintReplications();
         if (scenario.getConstraints().get("numFunctionsPerServer")) numFunctionsPerServer();
         if (scenario.getConstraints().get("fixSrcDstFunctions")) fixSrcDstFunctions();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void linkUtilization() throws GRBException {
      for (int l = 0; l < pm.getLinks().size(); l++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               if (!pm.getServices().get(s).getTrafficFlow().getPaths().get(p).contains(pm.getLinks().get(l)))
                  continue;
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm((double) pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                          / (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY), vars.rSPD[s][p][d]);
            }
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int p = 0; p < pm.getPaths().size(); p++) {
                  if (!pm.getPaths().get(p).contains(pm.getLinks().get(l)))
                     continue;
                  double traf = 0;
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     traf += pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                             * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(SYNC_LOAD);
                  expr.addTerm(traf / (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY)
                          , vars.sSVP[s][v][p]);
               }
         model.getGrbModel().addConstr(expr, GRB.EQUAL, vars.uL[l], "");
         linearCostFunctions(expr, vars.kL[l]);
      }
   }

   private void serverUtilization() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               Function function = pm.getServices().get(s).getFunctions().get(v);
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm((pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                          * (double) function.getAttribute(LOAD_FUNCTION))
                          / pm.getServers().get(x).getCapacity(), vars.pXSVD[x][s][v][d]);
               expr.addTerm((double) function.getAttribute(LOAD_FUNCTION)
                               * (int) function.getAttribute(OVERHEAD_FUNCTION) / pm.getServers().get(x).getCapacity()
                       , vars.pXSV[x][s][v]);
            }
         model.getGrbModel().addConstr(expr, GRB.EQUAL, vars.uX[x], "");
         linearCostFunctions(expr, vars.kX[x]);
      }
   }

   private void linearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {
      for (int l = 0; l < Auxiliary.costFunctions.getValues().size(); l++) {
         GRBLinExpr expr2 = new GRBLinExpr();
         expr2.multAdd(Auxiliary.costFunctions.getValues().get(l)[0], expr);
         expr2.addConstant(Auxiliary.costFunctions.getValues().get(l)[1]);
         model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar, "");
      }
   }

   private void serviceDelay(GRBModel initialModel) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
            Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
            GRBLinExpr linkDelayExpr = new GRBLinExpr();
            double pathDelay = 0.0;
            for (int l = 0; l < path.getEdgePath().size(); l++)
               pathDelay += (double) path.getEdgePath().get(l).getAttribute(LINK_DELAY);
            linkDelayExpr.addTerm(pathDelay, vars.rSP[s][p]);
            GRBLinExpr procDelayExpr = new GRBLinExpr();
            for (int n = 0; n < path.getNodePath().size(); n++)
               for (int x = 0; x < pm.getServers().size(); x++) {
                  Server server = pm.getServers().get(x);
                  if (!pm.getServers().get(x).getParent().equals(path.getNodePath().get(n))) continue;
                  for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                     for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                        double load = pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                                * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(LOAD_FUNCTION)
                                / server.getCapacity();
                        procDelayExpr.addTerm(load * server.getProcessDelay(), vars.dSPX[s][p][x]);
                     }
               }
            for (int x = 0; x < pm.getServers().size(); x++) {
               GRBLinExpr expr = new GRBLinExpr();
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  expr.addTerm(1.0, vars.pXSV[x][s][v]);
               model.getGrbModel().addConstr(vars.dSPX[s][p][x], GRB.LESS_EQUAL, expr, "");
               model.getGrbModel().addConstr(vars.dSPX[s][p][x], GRB.LESS_EQUAL, vars.rSP[s][p], "");
               GRBLinExpr varProcDelayExpr = new GRBLinExpr();
               varProcDelayExpr.addTerm(1.0, vars.rSP[s][p]);
               GRBLinExpr expr2 = new GRBLinExpr();
               expr2.multAdd(1.0 / pm.getServices().get(s).getFunctions().size(), expr);
               varProcDelayExpr.add(expr2);
               varProcDelayExpr.addConstant(-1.0);
               model.getGrbModel().addConstr(vars.dSPX[s][p][x], GRB.GREATER_EQUAL, varProcDelayExpr, "");
            }
            GRBLinExpr migrationDelayExpr = new GRBLinExpr();
            if (initialModel != null) {
               for (int n = 0; n < path.getNodePath().size(); n++)
                  for (int x = 0; x < pm.getServers().size(); x++) {
                     if (!pm.getServers().get(x).getParent().equals(path.getNodePath().get(n))) continue;
                     for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                        Function function = pm.getServices().get(s).getFunctions().get(v);
                        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                           double load = pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                                   * (double) function.getAttribute(LOAD_FUNCTION)
                                   / pm.getServers().get(x).getCapacity();
                           double initialFunctionPlacement = 0;
                           if (initialModel.getVarByName(pXSV + "[" + x + "][" + s + "][" + v + "]")
                                   .get(GRB.DoubleAttr.X) == 1.0)
                              initialFunctionPlacement = 1;
                           double delay = load * (int) function.getAttribute(PROCESS_DELAY);
                           migrationDelayExpr.addTerm(delay, vars.dSPX[s][p][x]);
                           migrationDelayExpr.addTerm(-delay * initialFunctionPlacement, vars.dSPX[s][p][x]);
                        }
                     }
                  }
            }
            GRBLinExpr serviceDelayExpr = new GRBLinExpr();
            serviceDelayExpr.add(linkDelayExpr);
            serviceDelayExpr.add(procDelayExpr);
            serviceDelayExpr.add(migrationDelayExpr);
            model.getGrbModel().addConstr(serviceDelayExpr, GRB.EQUAL, vars.dSP[s][p], "");
         }
      }
   }

   private void countNumberOfUsedServers() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++) {
         GRBLinExpr expr = new GRBLinExpr();
         GRBLinExpr expr2 = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               expr.addTerm(1.0 / pm.getTotalNumFunctions(), vars.pXSV[x][s][v]);
               expr2.addTerm(1.0, vars.pXSV[x][s][v]);
            }
         model.getGrbModel().addConstr(vars.pX[x], GRB.GREATER_EQUAL, expr, "");
         model.getGrbModel().addConstr(vars.pX[x], GRB.LESS_EQUAL, expr2, "");
      }
   }

   private void onePathPerDemand() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               expr.addTerm(1.0, vars.rSPD[s][p][d]);
            model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "");
         }
   }

   private void activatePathForService() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               model.getGrbModel().addConstr(vars.rSPD[s][p][d], GRB.LESS_EQUAL, vars.rSP[s][p], "");

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               expr.addTerm(1.0, vars.rSPD[s][p][d]);
            model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.rSP[s][p], "");
         }
   }

   private void pathsConstrainedByFunctions() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int x = 0; x < pm.getServers().size(); x++)
               expr.addTerm(1.0, vars.pXSV[x][s][v]);
            if ((boolean) pm.getServices().get(s).getFunctions().get(v).getAttribute(REPLICABLE_FUNCTION)) {
               GRBLinExpr expr2 = new GRBLinExpr();
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  expr2.addTerm(1.0, vars.rSP[s][p]);
               model.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, "");
            } else
               model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "");
         }
   }

   private void functionPlacement() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service se = pm.getServices().get(s);
         for (int p = 0; p < se.getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < se.getTrafficFlow().getDemands().size(); d++)
               for (int v = 0; v < se.getFunctions().size(); v++) {
                  GRBLinExpr expr = new GRBLinExpr();
                  for (int n = 0; n < se.getTrafficFlow().getPaths().get(p).getNodePath().size(); n++)
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (pm.getServers().get(x).getParent().equals(
                                pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(n)))
                           expr.addTerm(1.0, vars.pXSVD[x][s][v][d]);
                  model.getGrbModel().addConstr(vars.rSPD[s][p][d], GRB.LESS_EQUAL, expr, "");
               }
      }
   }

   private void oneFunctionPerDemand() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               GRBLinExpr expr = new GRBLinExpr();
               for (int x = 0; x < pm.getServers().size(); x++)
                  expr.addTerm(1.0, vars.pXSVD[x][s][v][d]);
               model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "");
            }
   }

   private void mappingFunctionsWithDemands() throws GRBException {

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  model.getGrbModel().addConstr(vars.pXSVD[x][s][v][d], GRB.LESS_EQUAL, vars.pXSV[x][s][v], "");

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++) {
               GRBLinExpr expr = new GRBLinExpr();
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm(1.0, vars.pXSVD[x][s][v][d]);
               model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.pXSV[x][s][v], "");
            }
   }

   private void functionSequenceOrder() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service se = pm.getServices().get(s);
         for (int d = 0; d < se.getTrafficFlow().getDemands().size(); d++) {
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
                              expr.addTerm(1.0, vars.pXSVD[x][s][v - 1][d]);
                     }
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (pm.getServers().get(x).getParent().equals(nodeN))
                           expr.addTerm(-1.0, vars.pXSVD[x][s][v][d]);

                     expr2.addConstant(-1);
                     expr2.addTerm(1.0, vars.rSPD[s][p][d]);
                     model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "");
                  }
               }
         }
      }
   }

   private void noParallelPaths() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            expr.addTerm(1.0, vars.rSP[s][p]);
         model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, "");
      }
   }

   private void initialPlacementAsConstraints(GRBModel initialModel) throws GRBException {
      if (initialModel != null) {
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (initialModel.getVarByName(pXSV + "[" + x + "][" + s + "][" + v + "]")
                          .get(GRB.DoubleAttr.X) == 1.0)
                     model.getGrbModel().addConstr(vars.pXSV[x][s][v], GRB.EQUAL, 1, "");
      }
   }

   private void synchronizationTraffic() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int y = 0; y < pm.getServers().size(); y++)
                  if (x != y) {
                     model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.pXSV[x][s][v], "");
                     model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.pXSV[y][s][v], "");
                     GRBLinExpr expr = new GRBLinExpr();
                     expr.addTerm(1.0, vars.pXSV[x][s][v]);
                     expr.addTerm(1.0, vars.pXSV[y][s][v]);
                     expr.addConstant(-1.0);
                     model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.GREATER_EQUAL, expr, "");
                     expr = new GRBLinExpr();
                     for (int p = 0; p < pm.getPaths().size(); p++) {
                        Path pa = pm.getPaths().get(p);
                        if (pa.getNodePath().get(0).equals(pm.getServers().get(x).getParent())
                                & pa.getNodePath().get(pa.getNodePath().size() - 1)
                                .equals(pm.getServers().get(y).getParent()))
                           expr.addTerm(1.0, vars.sSVP[s][v][p]);
                     }
                     model.getGrbModel().addConstr(expr, GRB.EQUAL, vars.gSVXY[s][v][x][y], "");
                  }
   }

   private void constraintReplications() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            expr.addTerm(1.0, vars.rSP[s][p]);
         int minPaths = (int) pm.getServices().get(s).getAttribute("minPaths");
         int maxPaths = (int) pm.getServices().get(s).getAttribute("maxPaths");
         model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, minPaths, "");
         model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxPaths, "");
      }
   }

   private void numFunctionsPerServer() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int x = 0; x < pm.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               expr.addTerm(1.0, vars.pXSV[x][s][v]);
            int functionsServer = (int) pm.getServices().get(s).getAttribute("functionsServer");
            model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, functionsServer, "");
         }
   }

   private void fixSrcDstFunctions() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(0);
         Node srcNode = path.getNodePath().get(0);
         Node dstNode = path.getNodePath().get(path.getNodePath().size() - 1);
         GRBLinExpr exprSrc = new GRBLinExpr();
         GRBLinExpr exprDst = new GRBLinExpr();
         for (int x = 0; x < pm.getServers().size(); x++) {
            if (pm.getServers().get(x).getParent().getId().equals(srcNode.getId()))
               exprSrc.addTerm(1.0, vars.pXSV[x][s][0]);
            if (pm.getServers().get(x).getParent().getId().equals(dstNode.getId()))
               exprDst.addTerm(1.0, vars.pXSV[x][s][pm.getServices().get(s).getFunctions().size() - 1]);
         }
         model.getGrbModel().addConstr(exprSrc, GRB.EQUAL, 1.0, "");
         model.getGrbModel().addConstr(exprDst, GRB.EQUAL, 1.0, "");
      }
   }
}
