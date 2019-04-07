package lp;

import gui.elements.Scenario;
import gurobi.*;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Service;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import output.Auxiliary;

import java.util.List;

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
         maxUtilization();
         serviceDelay(initialModel);
         // General constraints
         if (scenario.getConstraints().get(RPC1)) RPC1();
         if (scenario.getConstraints().get(RPC2)) RPC2();
         if (scenario.getConstraints().get(PFC1)) PFC1();
         if (scenario.getConstraints().get(PFC2)) PFC2();
         if (scenario.getConstraints().get(FDC1)) FDC1();
         if (scenario.getConstraints().get(FDC2)) FDC2();
         if (scenario.getConstraints().get(FDC3)) FDC3();
         if (scenario.getConstraints().get(FDC4)) FDC4();
         // Model specific constraints
         if (scenario.getConstraints().get(IPC)) IPC();
         if (scenario.getConstraints().get(IPMGRC)) IPMGRC();
         if (scenario.getConstraints().get(REPC)) REPC(initialModel);
         // Extra constraints
         if (scenario.getConstraints().get(RC)) RC();
         if (scenario.getConstraints().get(FXC)) FXC();
         if (scenario.getConstraints().get(SDC)) SDC();
         if (scenario.getConstraints().get(DIC1)) DIC1();
         if (scenario.getConstraints().get(DVC1)) DVC1();
         if (scenario.getConstraints().get(DVC2)) DVC2();
         if (scenario.getConstraints().get(DVC3)) DVC3();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   ////////////////////////////////////// Core constraints ///////////////////////////////////////
   private void linkUtilization() throws GRBException {
      for (int l = 0; l < pm.getLinks().size(); l++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               if (!pm.getServices().get(s).getTrafficFlow().getPaths().get(p).contains(pm.getLinks().get(l)))
                  continue;
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm((double) pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                          / (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY), vars.zSPD[s][p][d]);
            }
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int p = 0; p < pm.getPaths().size(); p++) {
                  if (!pm.getPaths().get(p).contains(pm.getLinks().get(l)))
                     continue;
                  double traf = 0;
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     traf += pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                             * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_SYNC_LOAD_RATIO);
                  expr.addTerm(traf / (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY)
                          , vars.hSVP[s][v][p]);
               }
         model.getGrbModel().addConstr(expr, GRB.EQUAL, vars.uL[l], "link utilization");
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
                          * (double) function.getAttribute(FUNCTION_LOAD_RATIO))
                          / pm.getServers().get(x).getCapacity(), vars.fXSVD[x][s][v][d]);
               expr.addTerm((double) function.getAttribute(FUNCTION_LOAD_RATIO)
                               * (int) function.getAttribute(FUNCTION_OVERHEAD) / pm.getServers().get(x).getCapacity()
                       , vars.fXSV[x][s][v]);
            }
         model.getGrbModel().addConstr(expr, GRB.EQUAL, vars.uX[x], "server utilization");
         linearCostFunctions(expr, vars.kX[x]);
      }
   }

   private void linearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {
      for (int l = 0; l < Auxiliary.costFunctions.getValues().size(); l++) {
         GRBLinExpr expr2 = new GRBLinExpr();
         expr2.multAdd(Auxiliary.costFunctions.getValues().get(l)[0], expr);
         expr2.addConstant(Auxiliary.costFunctions.getValues().get(l)[1]);
         model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar, "cost functions");
      }
   }

   private void maxUtilization() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         model.getGrbModel().addConstr(vars.uX[x], GRB.LESS_EQUAL, vars.uMax, "maxU");
      for (int l = 0; l < pm.getLinks().size(); l++)
         model.getGrbModel().addConstr(vars.uL[l], GRB.LESS_EQUAL, vars.uMax, "maxU");
   }

   private void serviceDelay(GRBModel initialModel) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               Service service = pm.getServices().get(s);
               Path path = service.getTrafficFlow().getPaths().get(p);
               GRBLinExpr processDelayExpr = new GRBLinExpr();
               for (int n = 0; n < path.getNodePath().size(); n++)
                  for (int x = 0; x < pm.getServers().size(); x++)
                     if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
                        for (int v = 0; v < service.getFunctions().size(); v++) {
                           Function function = service.getFunctions().get(v);
                           GRBLinExpr linExpr3 = processingDelayExpr(s, v, x, (int) function.getAttribute(FUNCTION_PROCESS_DELAY));
                           GRBLinExpr linExpr4 = new GRBLinExpr();
                           linExpr4.addTerm((int) function.getAttribute(FUNCTION_MAX_DELAY), vars.fXSVD[x][s][v][d]);
                           model.getGrbModel().addConstr(vars.ySVXD[s][v][x][d], GRB.LESS_EQUAL, linExpr4, FUNCTION_PROCESS_DELAY);
                           model.getGrbModel().addConstr(vars.ySVXD[s][v][x][d], GRB.LESS_EQUAL, linExpr3, FUNCTION_PROCESS_DELAY);
                           linExpr4.addConstant(-(int) function.getAttribute(FUNCTION_MAX_DELAY));
                           linExpr4.add(linExpr3);
                           model.getGrbModel().addConstr(vars.ySVXD[s][v][x][d], GRB.GREATER_EQUAL, linExpr4, FUNCTION_PROCESS_DELAY);
                           processDelayExpr.addTerm(1.0, vars.ySVXD[s][v][x][d]);
                        }
               GRBLinExpr pathExpr = new GRBLinExpr();
               pathExpr.addTerm((int) service.getAttribute(SERVICE_MAX_DELAY), vars.zSPD[s][p][d]);
               pathExpr.addConstant(Integer.MAX_VALUE);
               pathExpr.addTerm(-Integer.MAX_VALUE, vars.zSPD[s][p][d]);
               GRBLinExpr totalDelayExpr = new GRBLinExpr();
               totalDelayExpr.add(processDelayExpr); // adds processing delay
               totalDelayExpr.add(linkDelayExpr(s, p)); // adds propagation delay
               if (initialModel != null)
                  totalDelayExpr.add(migrationDelayExpr(initialModel, s, d, p)); // adds migration delay
               model.getGrbModel().addConstr(totalDelayExpr, GRB.LESS_EQUAL, pathExpr, LINK_DELAY);
               model.getGrbModel().addConstr(totalDelayExpr, GRB.EQUAL, vars.dSPD[s][p][d], LINK_DELAY);
            }
   }

   private GRBLinExpr linkDelayExpr(int s, int p) {
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      GRBLinExpr linkDelayExpr = new GRBLinExpr();
      double pathDelay = 0.0;
      for (int l = 0; l < path.getEdgePath().size(); l++)
         pathDelay += (double) path.getEdgePath().get(l).getAttribute(LINK_DELAY);
      linkDelayExpr.addTerm(pathDelay, vars.zSP[s][p]);
      return linkDelayExpr;
   }

   private GRBLinExpr processingDelayExpr(int s, int v, int x, int processDelay) {
      GRBLinExpr linExpr = new GRBLinExpr();
      Service service = pm.getServices().get(s);
      double ratio = (double) service.getFunctions().get(v).getAttribute(FUNCTION_LOAD_RATIO)
              * processDelay / pm.getServers().get(x).getCapacity();
      for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
         linExpr.addTerm(ratio * service.getTrafficFlow().getDemands().get(d), vars.fXSVD[x][s][v][d]);
      return linExpr;
   }

   private GRBLinExpr migrationDelayExpr(GRBModel initialModel, int s, int d, int p) throws GRBException {
      GRBLinExpr linExpr = new GRBLinExpr();
      Service service = pm.getServices().get(s);
      Path path = service.getTrafficFlow().getPaths().get(p);
      for (int n = 0; n < path.getNodePath().size(); n++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
               for (int v = 0; v < service.getFunctions().size(); v++)
                  if (initialModel.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 0.0)
                     linExpr.addTerm(1.0, vars.ySVXD[s][v][x][d]);
      return linExpr;
   }
   ////////////////////////////////////////////////////////////////////////////////////////////

   ///////////////////////////////// Common constraints ///////////////////////////////////////
   // One path per demand
   private void RPC1() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               expr.addTerm(1.0, vars.zSPD[s][p][d]);
            model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, RPC1);
         }
   }

   // Activate path for service
   private void RPC2() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               model.getGrbModel().addConstr(vars.zSPD[s][p][d], GRB.LESS_EQUAL, vars.zSP[s][p], "");
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               expr.addTerm(1.0, vars.zSPD[s][p][d]);
            model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.zSP[s][p], RPC2);
         }
   }

   // Paths constrained by functions
   private void PFC1() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int x = 0; x < pm.getServers().size(); x++)
               expr.addTerm(1.0, vars.fXSV[x][s][v]);
            if ((boolean) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_REPLICABLE)) {
               GRBLinExpr expr2 = new GRBLinExpr();
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  expr2.addTerm(1.0, vars.zSP[s][p]);
               model.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, PFC1);
            } else
               model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, PFC1);
         }
   }

   // Function placement
   private void PFC2() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service service = pm.getServices().get(s);
         for (int p = 0; p < service.getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
               for (int v = 0; v < service.getFunctions().size(); v++) {
                  GRBLinExpr middleExpr = new GRBLinExpr();
                  for (int n = 0; n < service.getTrafficFlow().getPaths().get(p).getNodePath().size(); n++)
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (pm.getServers().get(x).getParent()
                                .equals(service.getTrafficFlow().getPaths().get(p).getNodePath().get(n)))
                           middleExpr.addTerm(1.0, vars.fXSVD[x][s][v][d]);
                  model.getGrbModel().addConstr(vars.zSPD[s][p][d], GRB.LESS_EQUAL, middleExpr, PFC2);
               }
      }
   }

   // One function per demand
   private void FDC1() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               GRBLinExpr expr = new GRBLinExpr();
               for (int x = 0; x < pm.getServers().size(); x++)
                  expr.addTerm(1.0, vars.fXSVD[x][s][v][d]);
               model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, FDC1);
            }
   }

   // Mapping functions with demands
   private void FDC2() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  model.getGrbModel().addConstr(vars.fXSVD[x][s][v][d], GRB.LESS_EQUAL, vars.fXSV[x][s][v], FDC2);
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++) {
               GRBLinExpr expr = new GRBLinExpr();
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm(1.0, vars.fXSVD[x][s][v][d]);
               model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.fXSV[x][s][v], FDC2);
            }
   }

   // Functions sequence order
   private void FDC3() throws GRBException {
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
                              expr.addTerm(1.0, vars.fXSVD[x][s][v - 1][d]);
                     }
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (pm.getServers().get(x).getParent().equals(nodeN))
                           expr.addTerm(-1.0, vars.fXSVD[x][s][v][d]);

                     expr2.addConstant(-1);
                     expr2.addTerm(1.0, vars.zSPD[s][p][d]);
                     model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, FDC3);
                  }
               }
         }
      }
   }

   // Synchronization traffic
   private void FDC4() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int y = 0; y < pm.getServers().size(); y++) {
                  if (x == y) continue;
                  if (pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent())) continue;
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.fXSV[x][s][v], FDC4);
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.fXSV[y][s][v], FDC4);
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm(1.0, vars.fXSV[x][s][v]);
                  expr.addTerm(1.0, vars.fXSV[y][s][v]);
                  expr.addConstant(-1.0);
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.GREATER_EQUAL, expr, FDC4);
                  expr = new GRBLinExpr();
                  for (int p = 0; p < pm.getPaths().size(); p++) {
                     Path pa = pm.getPaths().get(p);
                     if (pa.getNodePath().get(0).equals(pm.getServers().get(x).getParent())
                             & pa.getNodePath().get(pa.getNodePath().size() - 1)
                             .equals(pm.getServers().get(y).getParent()))
                        expr.addTerm(1.0, vars.hSVP[s][v][p]);
                  }
                  model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.gSVXY[s][v][x][y], FDC4);
               }
   }
   ///////////////////////////////////////////////////////////////////////////////////////////


   ////////////////////////// Model specific constraints ////////////////////////////////////

   /************************** Initial placement *******************************************/
   // Count number of used servers
   private void IPC() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++) {
         GRBLinExpr expr = new GRBLinExpr();
         GRBLinExpr expr2 = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               expr.addTerm(1.0 / pm.getTotalNumFunctions(), vars.fXSV[x][s][v]);
               expr2.addTerm(1.0, vars.fXSV[x][s][v]);
            }
         model.getGrbModel().addConstr(vars.fX[x], GRB.GREATER_EQUAL, expr, IPC);
         model.getGrbModel().addConstr(vars.fX[x], GRB.LESS_EQUAL, expr2, IPC);
      }
   }

   /******************** Initial placement and Migration model ******************************/
   // No parallel paths
   private void IPMGRC() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            expr.addTerm(1.0, vars.zSP[s][p]);
         model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, IPMGRC);
      }
   }

   /********************************* Replication  *****************************************/
   // Initial placement as constraints
   private void REPC(GRBModel initialModel) throws GRBException {
      if (initialModel != null) {
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (initialModel.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]")
                          .get(GRB.DoubleAttr.X) == 1.0)
                     model.getGrbModel().addConstr(vars.fXSV[x][s][v], GRB.EQUAL, 1, REPC);
      }
   }
   ///////////////////////////////////////////////////////////////////////////////////////////


   ////////////////////////////////// Extra constraints //////////////////////////////////////
   // Constraint replications
   private void RC() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            expr.addTerm(1.0, vars.zSP[s][p]);
         int minPaths = (int) pm.getServices().get(s).getAttribute(SERVICE_MIN_PATHS);
         int maxPaths = (int) pm.getServices().get(s).getAttribute(SERVICE_MAX_PATHS);
         model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, minPaths, RC);
         model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxPaths, RC);
      }
   }

   // Functions per server
   private void FXC() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int x = 0; x < pm.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               expr.addTerm(1.0, vars.fXSV[x][s][v]);
            int functionsServer = (int) pm.getServices().get(s).getAttribute(SERVICE_FUNCTIONS_PER_SERVER);
            model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, functionsServer, FXC);
         }
   }

   // Fix src-dst functions
   private void SDC() throws GRBException {
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
         model.getGrbModel().addConstr(exprSrc, GRB.EQUAL, 1.0, SDC);
         model.getGrbModel().addConstr(exprDst, GRB.EQUAL, 1.0, SDC);
      }
   }

   private void DIC1() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++) {
            Service service = pm.getServices().get(s);
            for (int v = 0; v < service.getFunctions().size(); v++) {
               double load = (double) service.getFunctions().get(v).getAttribute(FUNCTION_LOAD_RATIO);
               GRBLinExpr expr = new GRBLinExpr();
               for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm(load * service.getTrafficFlow().getDemands().get(d), vars.fXSVD[x][s][v][d]);
               int maxLoad = (int) service.getFunctions().get(v).getAttribute(FUNCTION_MAX_LOAD);
               int maxInt = (int) service.getFunctions().get(v).getAttribute(FUNCTION_MAX_INSTANCES);
               model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxLoad * maxInt, DIC1);
            }
         }
   }

   private void DVC1() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++) {
            Service service = pm.getServices().get(s);
            for (int v = 0; v < service.getFunctions().size(); v++) {
               double load = (double) service.getFunctions().get(v).getAttribute(FUNCTION_LOAD_RATIO);
               GRBLinExpr expr = new GRBLinExpr();
               GRBLinExpr expr2 = new GRBLinExpr();
               int maxLoad = (int) service.getFunctions().get(v).getAttribute(FUNCTION_MAX_LOAD);
               for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm(load * service.getTrafficFlow().getDemands().get(d), vars.fXSVD[x][s][v][d]);
               expr2.addTerm(maxLoad, vars.nXSV[x][s][v]);
               model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, DVC1);

            }
         }
   }

   private void DVC2() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++) {
               GRBLinExpr expr = new GRBLinExpr();
               GRBLinExpr expr2 = new GRBLinExpr();
               GRBLinExpr expr3 = new GRBLinExpr();
               int maxInst = (int) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_MAX_INSTANCES);
               expr.addTerm(1.0, vars.fXSV[x][s][v]);
               expr2.addTerm(1.0, vars.nXSV[x][s][v]);
               expr3.addTerm(maxInst, vars.fXSV[x][s][v]);
               model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, DVC2);
               model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, expr3, DVC2);
            }
   }

   private void DVC3() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++) {
            Service service = pm.getServices().get(s);
            for (int v = 0; v < service.getFunctions().size(); v++) {
               GRBLinExpr expr = new GRBLinExpr();
               GRBLinExpr expr2 = new GRBLinExpr();
               double load = (double) service.getFunctions().get(v).getAttribute(FUNCTION_LOAD_RATIO);
               int maxLoad = (int) service.getFunctions().get(v).getAttribute(FUNCTION_MAX_LOAD);
               for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm(load * service.getTrafficFlow().getDemands().get(d), vars.fXSVD[x][s][v][d]);
               expr2.addTerm(maxLoad, vars.nXSV[x][s][v]);
               model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, DVC3);
               expr.addConstant(maxLoad);
               model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, DVC3);

            }
         }
   }
}
