package lp.constraints;

import gui.elements.Scenario;
import gurobi.*;
import lp.OptimizationModel;
import lp.Variables;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Service;
import org.graphstream.graph.Path;

import static output.Definitions.*;

public class AdditionalConstraints {

   private OptimizationModel model;
   private Variables vars;
   private Parameters pm;

   public AdditionalConstraints(Parameters pm, OptimizationModel model, Scenario scenario, GRBModel initialModel, GRBLinExpr[] luExpr) {
      try {
         this.pm = pm;
         this.model = model;
         this.vars = model.getVariables();
         if (scenario.getConstraints().get(ST)) ST(luExpr);
         if (scenario.getConstraints().get(SD)) SD(initialModel);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   // synchronization traffic
   private void ST(GRBLinExpr[] luExpr) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int y = 0; y < pm.getServers().size(); y++) {
                  if (x == y) continue;
                  if (pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent())) continue;
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.fXSV[x][s][v], ST);
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.fXSV[y][s][v], ST);
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm(1.0, vars.fXSV[x][s][v]);
                  expr.addTerm(1.0, vars.fXSV[y][s][v]);
                  expr.addConstant(-1.0);
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.GREATER_EQUAL, expr, ST);
                  expr = new GRBLinExpr();
                  for (int p = 0; p < pm.getPaths().size(); p++) {
                     Path pa = pm.getPaths().get(p);
                     if (pa.getNodePath().get(0).equals(pm.getServers().get(x).getParent())
                             & pa.getNodePath().get(pa.getNodePath().size() - 1)
                             .equals(pm.getServers().get(y).getParent()))
                        expr.addTerm(1.0, vars.hSVP[s][v][p]);
                  }
                  model.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, expr, ST);
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
                  double trafficScaled = traffic * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_SYNC_LOAD_RATIO)
                          / (int) pm.getLinks().get(l).getAttribute(LINK_CAPACITY);
                  expr.addTerm(trafficScaled, vars.hSVP[s][v][p]);
               }
            }
         }
         luExpr[l].add(expr);
      }
   }
   // service delay
   private void SD(GRBModel initialModel) throws GRBException {
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
                           linExpr4.addTerm((int) function.getAttribute(FUNCTION_PROCESS_DELAY), vars.fXSVD[x][s][v][d]);
                           model.getGrbModel().addConstr(vars.ySVXD[s][v][x][d], GRB.LESS_EQUAL, linExpr4, FUNCTION_PROCESS_DELAY);
                           model.getGrbModel().addConstr(vars.ySVXD[s][v][x][d], GRB.LESS_EQUAL, linExpr3, FUNCTION_PROCESS_DELAY);
                           linExpr4.addConstant(-(int) function.getAttribute(FUNCTION_PROCESS_DELAY));
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
              * processDelay / (int) service.getFunctions().get(v).getAttribute(FUNCTION_MAX_LOAD);
      for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
         linExpr.addTerm(ratio * service.getTrafficFlow().getDemands().get(d), vars.fXSVD[x][s][v][d]);
      return linExpr;
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
                     int delay = (int) service.getFunctions().get(v).getAttribute(FUNCTION_MIGRATION_DELAY);
                     linExpr.addTerm(-delay, vars.fXSV[x][s][v]);
                     linExpr.addConstant(delay);
                     model.getGrbModel().addConstr(linExpr, GRB.LESS_EQUAL, vars.mS[s], FUNCTION_MIGRATION_DELAY);
                  }
      GRBLinExpr linExpr = new GRBLinExpr();
      linExpr.addTerm(1.0, vars.mS[s]);
      return linExpr;
   }
}
