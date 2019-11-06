package lp.constraints;

import gui.elements.Scenario;
import gurobi.*;
import lp.Model;
import lp.Variables;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Service;
import org.graphstream.graph.Path;

import static output.Definitions.*;

public class AdditionalConstraints {

   private Model model;
   private Variables vars;
   private Parameters pm;

   public AdditionalConstraints(Parameters pm, Model model, Scenario scenario, GRBModel initialModel, GRBLinExpr[] linkLoadExpr) {
      try {
         this.pm = pm;
         this.model = model;
         this.vars = model.getVariables();
         if (scenario.getConstraints().get(SYNC_TRAFFIC)) SyncTraffic(linkLoadExpr);
         if (scenario.getConstraints().get(SERV_DELAY)) ServDelay(initialModel);
         if (scenario.getConstraints().get(ONLY_CLOUD)) useOnlyCloudServers();
         if (scenario.getConstraints().get(ONLY_EDGE)) useOnlyEdgeServers();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   // synchronization traffic
   private void SyncTraffic(GRBLinExpr[] linkLoadExpr) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int y = 0; y < pm.getServers().size(); y++) {
                  if (pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent())) continue;
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.fXSV[x][s][v], SYNC_TRAFFIC);
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.fXSV[y][s][v], SYNC_TRAFFIC);
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm(1.0, vars.fXSV[x][s][v]);
                  expr.addTerm(1.0, vars.fXSV[y][s][v]);
                  expr.addConstant(-1.0);
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.GREATER_EQUAL, expr, SYNC_TRAFFIC);
                  expr = new GRBLinExpr();
                  for (int p = 0; p < pm.getPaths().size(); p++) {
                     Path pa = pm.getPaths().get(p);
                     if (pa.getNodePath().get(0).equals(pm.getServers().get(x).getParent())
                             & pa.getNodePath().get(pa.getNodePath().size() - 1)
                             .equals(pm.getServers().get(y).getParent()))
                        expr.addTerm(1.0, vars.hSVP[s][v][p]);
                  }
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, expr, SYNC_TRAFFIC);
                  model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, 1.0, SYNC_TRAFFIC);
               }
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int n = 0; n < pm.getNodes().size(); n++)
               for (int m = 0; m < pm.getNodes().size(); m++) {
                  if (n == m) continue;
                  GRBLinExpr expr = new GRBLinExpr();
                  for (int p = 0; p < pm.getPaths().size(); p++) {
                     Path path = pm.getPaths().get(p);
                     if (path.getNodePath().get(0).equals(pm.getNodes().get(n))
                             & path.getNodePath().get(path.getNodePath().size() - 1).equals(pm.getNodes().get(m)))
                        expr.addTerm(1.0, vars.hSVP[s][v][p]);
                  }
                  GRBLinExpr expr2 = new GRBLinExpr();
                  for (int x = 0; x < pm.getServers().size(); x++)
                     for (int y = 0; y < pm.getServers().size(); y++)
                        if (pm.getServers().get(x).getParent().equals(pm.getNodes().get(n))
                                && pm.getServers().get(y).getParent().equals(pm.getNodes().get(m)))
                           expr2.addTerm(1.0, vars.gSVXY[s][v][x][y]);
                  model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, SYNC_TRAFFIC);
               }
      for (int l = 0; l < pm.getLinks().size(); l++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getPaths().size(); p++) {
            if (!pm.getPaths().get(p).contains(pm.getLinks().get(l)))
               continue;
            for (int s = 0; s < pm.getServices().size(); s++) {
               double traffic = 0;
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  traffic += pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                  double trafficScaled = traffic * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_SYNC_LOAD_RATIO);
                  expr.addTerm(trafficScaled, vars.hSVP[s][v][p]);
               }
            }
         }
         linkLoadExpr[l].add(expr);
      }
   }

   // service delay
   private void ServDelay(GRBModel initialModel) throws GRBException {
      int bigM = 10000000;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               Service service = pm.getServices().get(s);
               Path path = service.getTrafficFlow().getPaths().get(p);
               GRBLinExpr processDelayGlobalExpr = new GRBLinExpr();
               for (int n = 0; n < path.getNodePath().size(); n++)
                  for (int x = 0; x < pm.getServers().size(); x++)
                     if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
                        for (int v = 0; v < service.getFunctions().size(); v++) {
                           Function function = service.getFunctions().get(v);
                           double ratio = (double) function.getAttribute(FUNCTION_LOAD_RATIO)
                                   * (double) function.getAttribute(FUNCTION_PROCESS_TRAFFIC_DELAY)
                                   / (int) function.getAttribute(FUNCTION_MAX_CAP_SERVER);
                           GRBLinExpr loadDelayExpr = new GRBLinExpr();
                           for (int d1 = 0; d1 < service.getTrafficFlow().getDemands().size(); d1++)
                              loadDelayExpr.addTerm(ratio * service.getTrafficFlow().getDemands().get(d1), vars.fXSVD[x][s][v][d1]);
                           GRBLinExpr processDelayExpr1 = new GRBLinExpr();
                           processDelayExpr1.addTerm((double) function.getAttribute(FUNCTION_MIN_PROCESS_DELAY), vars.fXSV[x][s][v]);
                           GRBLinExpr processDelayExpr2 = new GRBLinExpr();
                           processDelayExpr2.addTerm((double) function.getAttribute(FUNCTION_PROCESS_DELAY), vars.uX[x]);
                           GRBLinExpr processDelayExpr = new GRBLinExpr();
                           processDelayExpr.add(loadDelayExpr);
                           processDelayExpr.add(processDelayExpr1);
                           processDelayExpr.add(processDelayExpr2);
                           for (int d1 = 0; d1 < service.getTrafficFlow().getDemands().size(); d1++) {
                              GRBLinExpr processConstraintExpr1 = new GRBLinExpr();
                              processConstraintExpr1.addTerm(-bigM, vars.fXSVD[x][s][v][d1]);
                              processConstraintExpr1.addConstant(bigM);
                              processConstraintExpr1.addTerm(1.0, vars.dSVXD[s][v][x][d1]);
                              model.getGrbModel().addConstr(processDelayExpr, GRB.LESS_EQUAL, processConstraintExpr1, FUNCTION_PROCESS_TRAFFIC_DELAY);
                              GRBLinExpr processConstraintExpr2 = new GRBLinExpr();
                              processConstraintExpr2.addTerm((double) function.getAttribute(FUNCTION_MAX_DELAY), vars.fXSVD[x][s][v][d1]);
                              model.getGrbModel().addConstr(vars.dSVXD[s][v][x][d1], GRB.LESS_EQUAL, processConstraintExpr2, FUNCTION_PROCESS_TRAFFIC_DELAY);
                           }
                           processDelayGlobalExpr.addTerm(1.0, vars.dSVXD[s][v][x][d]);
                           model.getGrbModel().addConstr(processDelayExpr, GRB.EQUAL, vars.dSVX[s][v][x], FUNCTION_PROCESS_TRAFFIC_DELAY);
                        }
               GRBLinExpr pathDelayExpr = new GRBLinExpr();
               pathDelayExpr.addTerm((double) service.getAttribute(SERVICE_MAX_DELAY), vars.zSPD[s][p][d]);
               pathDelayExpr.addConstant(bigM);
               pathDelayExpr.addTerm(-bigM, vars.zSPD[s][p][d]);
               GRBLinExpr totalDelayExpr = new GRBLinExpr();
               totalDelayExpr.add(processDelayGlobalExpr); // adds processing delay
               totalDelayExpr.add(linkDelayExpr(s, p)); // adds propagation delay
               if (initialModel != null)
                  totalDelayExpr.add(migrationDelayExpr(initialModel, s, d, p)); // adds migration delay
               String constrName = SERV_DELAY + "[s][p][d] --> " + "[" + s + "]"
                       + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "[" + d + "]";
               model.getGrbModel().addConstr(totalDelayExpr, GRB.LESS_EQUAL, pathDelayExpr, constrName);
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

   private GRBLinExpr migrationDelayExpr(GRBModel initialModel, int s, int d, int p) throws GRBException {
      Service service = pm.getServices().get(s);
      Path path = service.getTrafficFlow().getPaths().get(p);
      for (int n = 0; n < path.getNodePath().size(); n++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
               for (int v = 0; v < service.getFunctions().size(); v++)
                  if (initialModel.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 1.0) {
                     GRBLinExpr linExpr = new GRBLinExpr();
                     double delay = (double) service.getFunctions().get(v).getAttribute(FUNCTION_MIGRATION_DELAY);
                     linExpr.addTerm(-delay, vars.fXSV[x][s][v]);
                     linExpr.addConstant(delay);
                     model.getGrbModel().addConstr(linExpr, GRB.LESS_EQUAL, vars.mS[s], FUNCTION_MIGRATION_DELAY);
                  }
      GRBLinExpr linExpr = new GRBLinExpr();
      linExpr.addTerm(1.0, vars.mS[s]);
      return linExpr;
   }

   // use only cloud servers
   private void useOnlyCloudServers() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) == null)
            model.getGrbModel().addConstr(vars.fX[x], GRB.EQUAL, 0.0, SERVER_DIMENSIONING);
   }

   // use only edge servers
   private void useOnlyEdgeServers() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) != null)
            model.getGrbModel().addConstr(vars.fX[x], GRB.EQUAL, 0.0, SERVER_DIMENSIONING);
   }
}
