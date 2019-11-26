package optimizer.lp.constraints;

import gurobi.*;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Service;
import optimizer.gui.Scenario;
import optimizer.lp.Model;
import optimizer.lp.Variables;
import optimizer.results.Auxiliary;
import org.graphstream.graph.Path;

import static optimizer.Parameters.*;


public class ModelSpecificConstraints {

   private final Integer BIG_M = 100000;
   private Model model;
   private Variables vars;
   private Parameters pm;

   public ModelSpecificConstraints(Parameters pm, Model model, Scenario sc, GRBModel initialPlacement
           , GRBLinExpr[] serverLoadExpr, GRBLinExpr[] linkLoadExpr, GRBLinExpr[] xuExpr, GRBLinExpr[] luExpr) {
      try {
         this.pm = pm;
         this.model = model;
         this.vars = model.getVars();

         // set dimensioning constraints
         if (sc.getObjFunc().equals(SERVER_DIMENSIONING)) dimensioning(serverLoadExpr);

         // set linear utilization cost functions constraints
         if (sc.getObjFunc().equals(NUM_SERVERS_UTIL_COSTS_OBJ) || sc.getObjFunc().equals(UTIL_COSTS_OBJ)
                 || sc.getObjFunc().equals(UTIL_COSTS_MIGRATIONS_OBJ) || sc.getObjFunc().equals(UTIL_COSTS_MAX_UTIL_OBJ)) {
            linearUtilCostFunctions(luExpr, vars.kL);
            linearUtilCostFunctions(xuExpr, vars.kX);
         }

         // set max utilization constraint
         if (sc.getObjFunc().equals(UTIL_COSTS_MAX_UTIL_OBJ)) maxUtilization();

         // set operational costs
         if (sc.getObjFunc().equals(OPER_COSTS_OBJ)) operationalCosts(initialPlacement);

         // rest of specific constraints
         if (sc.getConstraints().get(SYNC_TRAFFIC)) syncTraffic(linkLoadExpr);
         if (sc.getConstraints().get(SERV_DELAY)) constraintMaxServiceDelay(initialPlacement);
         if (sc.getConstraints().get(CLOUD_ONLY)) useOnlyCloudServers();
         if (sc.getConstraints().get(EDGE_ONLY)) useOnlyEdgeServers();
         if (sc.getConstraints().get(SINGLE_PATH)) singlePath();
         if (sc.getConstraints().get(SET_INIT_PLC)) setInitPlc(initialPlacement);
      } catch (Exception e) {
         e.printStackTrace();
      }
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

   private void linearUtilCostFunctions(GRBLinExpr[] exprs, GRBVar[] grbVar) throws GRBException {
      for (int e = 0; e < exprs.length; e++)
         for (int c = 0; c < Auxiliary.costFunctions.getValues().size(); c++) {
            GRBLinExpr expr = new GRBLinExpr();
            expr.multAdd(Auxiliary.costFunctions.getValues().get(c)[0], exprs[e]);
            expr.addConstant(Auxiliary.costFunctions.getValues().get(c)[1]);
            model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, grbVar[e], UTIL_COSTS_OBJ);
         }
   }

   private void maxUtilization() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         model.getGrbModel().addConstr(vars.uX[x], GRB.LESS_EQUAL, vars.uMax, uMax);
      for (int l = 0; l < pm.getLinks().size(); l++)
         model.getGrbModel().addConstr(vars.uL[l], GRB.LESS_EQUAL, vars.uMax, uMax);
   }

   private void operationalCosts(GRBModel initialPlacement) throws GRBException {

      // operational costs for using servers
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) == null) {
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm((double) pm.getAux().get(SERVER_IDLE_ENERGY_COST), vars.fX[x]);
            expr.addTerm((double) pm.getAux().get(SERVER_UTIL_ENERGY_COST), vars.uX[x]);
            expr.addConstant((double) pm.getAux().get(SERVER_OTHER_OPEX));
            model.getGrbModel().addConstr(vars.oX[x], GRB.EQUAL, expr, oX);
         }

      // operational costs for placing functions in the cloud
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++) {
               // calculate the longest holding time
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm(pm.getServices().get(s).getTrafficFlow().getHoldingTimes().get(d), vars.fXSVD[x][s][v][d]);
                  model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, vars.hSVX[s][v][x], oSV);
               }
               // calculate the opex of functions
               if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) != null) {
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm((double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_OPEX), vars.hSVX[s][v][x]);
                  model.getGrbModel().addConstr(vars.oSV[s][v], GRB.EQUAL, expr, oSV);
               }
            }

      // apply penalty costs for service delay
      applyQosPenaltyServiceDelay(initialPlacement);
   }

   // synchronization traffic
   private void syncTraffic(GRBLinExpr[] linkLoadExpr) throws GRBException {
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

   private void applyQosPenaltyServiceDelay(GRBModel initialPlacement) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               GRBLinExpr serviceDelayExpr = serviceDelayExpr(s, p, d, initialPlacement);
               // linearization of delay and routing variables
               model.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.LESS_EQUAL, serviceDelayExpr, ySDP);
               GRBLinExpr expr = new GRBLinExpr();
               expr.addTerm(BIG_M, vars.zSPD[s][p][d]);
               model.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.LESS_EQUAL, expr, ySDP);
               expr.addConstant(-BIG_M);
               expr.add(serviceDelayExpr);
               model.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.GREATER_EQUAL, expr, ySDP);
               // apply penalty costs
               expr = new GRBLinExpr();
               expr.addTerm((double) pm.getAux().get(QOS_PENALTY), vars.ySDP[s][d][p]);
               expr.addTerm(-(double) pm.getAux().get(QOS_PENALTY) *
                       (double) pm.getServices().get(s).getAttribute(SERVICE_MAX_DELAY), vars.zSPD[s][p][d]);
               model.getGrbModel().addConstr(vars.qSDP[s][d][p], GRB.GREATER_EQUAL, expr, OPER_COSTS_OBJ);
            }
   }

   private void constraintMaxServiceDelay(GRBModel initialPlacement) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               GRBLinExpr pathDelayExpr = new GRBLinExpr();
               pathDelayExpr.addTerm((double) pm.getServices().get(s).getAttribute(SERVICE_MAX_DELAY), vars.zSPD[s][p][d]);
               pathDelayExpr.addConstant(BIG_M);
               pathDelayExpr.addTerm(-BIG_M, vars.zSPD[s][p][d]);
               String constrName = SERV_DELAY + "[s][p][d] --> " + "[" + s + "]"
                       + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "[" + d + "]";
               model.getGrbModel().addConstr(serviceDelayExpr(s, p, d, initialPlacement), GRB.LESS_EQUAL, pathDelayExpr, constrName);
            }
   }

   private GRBLinExpr serviceDelayExpr(int s, int p, int d, GRBModel initialPlacement) throws GRBException {
      GRBLinExpr serviceDelayExpr = new GRBLinExpr();
      serviceDelayExpr.add(processingDelayExpr(s, p, d)); // adds processing delay
      serviceDelayExpr.add(linkDelayExpr(s, p)); // adds propagation delay
      if (initialPlacement != null)
         serviceDelayExpr.add(migrationDelayExpr(initialPlacement, s, d, p)); // adds migration delay
      return serviceDelayExpr;
   }

   private GRBLinExpr processingDelayExpr(int s, int p, int d) throws GRBException {
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
                     processConstraintExpr1.addTerm(-BIG_M, vars.fXSVD[x][s][v][d1]);
                     processConstraintExpr1.addConstant(BIG_M);
                     processConstraintExpr1.addTerm(1.0, vars.dSVXD[s][v][x][d1]);
                     model.getGrbModel().addConstr(processDelayExpr, GRB.LESS_EQUAL, processConstraintExpr1, FUNCTION_PROCESS_TRAFFIC_DELAY);
                     GRBLinExpr processConstraintExpr2 = new GRBLinExpr();
                     processConstraintExpr2.addTerm((double) function.getAttribute(FUNCTION_MAX_DELAY), vars.fXSVD[x][s][v][d1]);
                     model.getGrbModel().addConstr(vars.dSVXD[s][v][x][d1], GRB.LESS_EQUAL, processConstraintExpr2, FUNCTION_PROCESS_TRAFFIC_DELAY);
                  }
                  processDelayGlobalExpr.addTerm(1.0, vars.dSVXD[s][v][x][d]);
                  model.getGrbModel().addConstr(processDelayExpr, GRB.EQUAL, vars.dSVX[s][v][x], FUNCTION_PROCESS_TRAFFIC_DELAY);
               }
      return processDelayGlobalExpr;
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

   // Single path (mgr-only)
   private void singlePath() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            expr.addTerm(1.0, vars.zSP[s][p]);
         model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, SINGLE_PATH);
      }
   }

   // Initial placement as constraints (rep-only)
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
}
