package optimizer.lp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gurobi.*;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Service;
import optimizer.gui.Scenario;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;


public class ModelSpecificConstraints {

   private static final Logger log = LoggerFactory.getLogger(ModelSpecificConstraints.class);
   private Model model;
   private Variables vars;
   private Parameters pm;

   public ModelSpecificConstraints(Parameters pm, Model model, Scenario sc, GRBModel initialPlacement) {
      try {
         this.pm = pm;
         this.model = model;
         this.vars = model.getVars();

         // create link and server load expressions
         GRBLinExpr[] linkLoadExpr = createLinkLoadExpr();
         GRBLinExpr[] serverLoadExpr = createServerLoadExpr();

         // set dimensioning constraints
         if (sc.getObjFunc().equals(SERVER_DIMENSIONING)) dimensioning(serverLoadExpr);

         // set max utilization constraint
         if (sc.getObjFunc().equals(UTIL_COSTS_MAX_UTIL_OBJ)) maxUtilization();

         // set monetary costs
         if (sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
                 || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {
            opexServers();
            functionsCharges();
            qosPenalties(initialPlacement);
         }

         // rest of specific constraints
         if (sc.getConstraints().get(SYNC_TRAFFIC)) syncTraffic(linkLoadExpr);
         if (sc.getConstraints().get(MAX_SERV_DELAY)) constraintMaxServiceDelay(initialPlacement);
         if (sc.getConstraints().get(CLOUD_ONLY)) useOnlyCloudServers();
         if (sc.getConstraints().get(EDGE_ONLY)) useOnlyEdgeServers();
         if (sc.getConstraints().get(SINGLE_PATH)) singlePath();
         if (sc.getConstraints().get(SET_INIT_PLC)) setInitPlc(initialPlacement);
         if (sc.getConstraints().get(FORCE_SRC_DST)) forceSrcDst();
         if (sc.getConstraints().get(CONST_REP)) constRep();

         // create link and server utilization expressions
         GRBLinExpr[] luExpr = createLinkUtilizationExpr(linkLoadExpr);
         GRBLinExpr[] xuExpr = createServerUtilizationExpr(serverLoadExpr);

         // set linear utilization cost functions constraints
         if (sc.getObjFunc().equals(NUM_SERVERS_UTIL_COSTS_OBJ) || sc.getObjFunc().equals(UTIL_COSTS_OBJ)
                 || sc.getObjFunc().equals(UTIL_COSTS_MAX_UTIL_OBJ)) {
            linearUtilCostFunctions(luExpr, vars.kL);
            linearUtilCostFunctions(xuExpr, vars.kX);
         }

         // constraint link utilization
         for (int l = 0; l < pm.getLinks().size(); l++)
            model.getGrbModel().addConstr(luExpr[l], GRB.EQUAL, vars.uL[l], uL
                    + "[" + pm.getLinks().get(l).getId() + "]");

         // constraint server utilization if no dimensioning
         if (!sc.getObjFunc().equals(SERVER_DIMENSIONING))
            for (int x = 0; x < pm.getServers().size(); x++)
               model.getGrbModel().addConstr(xuExpr[x], GRB.EQUAL, vars.uX[x], uX + "[x] --> " + "[" + x + "]");
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

   private void linearUtilCostFunctions(GRBLinExpr[] exprs, GRBVar[] grbVar) throws GRBException {
      CostFunctions costFunctions = getLinearCostFunctions();
      for (int e = 0; e < exprs.length; e++)
         for (int c = 0; c < costFunctions.getValues().size(); c++) {
            GRBLinExpr expr = new GRBLinExpr();
            expr.multAdd(costFunctions.getValues().get(c)[0], exprs[e]);
            expr.addConstant(costFunctions.getValues().get(c)[1]);
            model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, grbVar[e], UTIL_COSTS_OBJ);
         }
   }

   private void maxUtilization() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         model.getGrbModel().addConstr(vars.uX[x], GRB.LESS_EQUAL, vars.uMax, uMax);
      for (int l = 0; l < pm.getLinks().size(); l++)
         model.getGrbModel().addConstr(vars.uL[l], GRB.LESS_EQUAL, vars.uMax, uMax);
   }

   private void opexServers() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) == null) {
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm((double) pm.getAux().get(SERVER_IDLE_ENERGY_COST), vars.fX[x]);
            expr.addTerm((double) pm.getAux().get(SERVER_UTIL_ENERGY_COST), vars.uX[x]);
            model.getGrbModel().addConstr(expr, GRB.EQUAL, vars.oX[x], oX);
         } else {
            model.getGrbModel().addConstr(vars.oX[x], GRB.EQUAL, 0, oX);
         }
   }

   private void functionsCharges() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) != null) {
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm((double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_CHARGES), vars.fXSV[x][s][v]); // in $/h
                  model.getGrbModel().addConstr(vars.oSV[s][v], GRB.EQUAL, expr, oSV); // to be updated from the paper (not affecting to the results because only one server in the cloud so no replicas will be replicated within the same server)
               }
   }

   private void qosPenalties(GRBModel initialPlacement) throws GRBException {

      for (int s = 0; s < pm.getServices().size(); s++) {
         Service service = pm.getServices().get(s);
         double bigM = 0;
         bigM += getMaxPathDelay(service.getTrafficFlow().getPaths()); // in ms
         bigM += getMaxProcessingDelay(service.getFunctions()) * service.getFunctions().size(); // in ms
         bigM += getMaxMigrationDelay(service.getFunctions()); // in ms

         for (int p = 0; p < service.getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
               if (service.getTrafficFlow().getAux().get(d)) {
                  GRBLinExpr serviceDelayExpr = serviceDelayExpr(s, p, d, initialPlacement); // in ms

                  // linearization of delay and routing variables
                  model.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.LESS_EQUAL, serviceDelayExpr, ySDP); // (31a)
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm(bigM, vars.zSPD[s][p][d]);
                  model.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.LESS_EQUAL, expr, ySDP); // (31b)
                  expr = new GRBLinExpr();
                  expr.addTerm(bigM, vars.zSPD[s][p][d]);
                  expr.addConstant(-bigM);
                  expr.add(serviceDelayExpr);
                  model.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.GREATER_EQUAL, expr, ySDP); // (31c)

                  // delay / max_delay
                  double maxDelay = 0;
                  maxDelay += service.getMaxPropagationDelay();
                  for (int v = 0; v < service.getFunctions().size(); v++)
                     maxDelay += (double) service.getFunctions().get(v).getAttribute(FUNCTION_MAX_DELAY);

                  expr = new GRBLinExpr();
                  expr.addTerm(1.0 / maxDelay, vars.ySDP[s][d][p]); // ratio
                  expr.addTerm(-1.0, vars.zSPD[s][p][d]);

                  // qos_penalty value
                  double profit = 0;
                  for (int v = 0; v < service.getFunctions().size(); v++)
                     profit += (double) service.getFunctions().get(v).getAttribute(FUNCTION_CHARGES);
                  double qosPenalty = (double) pm.getAux().get(QOS_PENALTY_RATIO) * profit; // in $/h

                  GRBLinExpr expr2 = new GRBLinExpr();
                  expr2.multAdd(qosPenalty, expr); // in $/h
                  model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, vars.qSDP[s][d][p], qSDP);
                  if (maxDelay > bigM)
                     printLog(log, WARNING, "max. service delay is not bounding");
                  double upperBound = ((bigM / maxDelay) - 1) * qosPenalty;
                  model.getGrbModel().addConstr(vars.qSDP[s][d][p], GRB.LESS_EQUAL, upperBound, qSDP);
               } else {
                  model.getGrbModel().addConstr(vars.qSDP[s][d][p], GRB.EQUAL, 0.0, qSDP);
                  model.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.EQUAL, 0.0, ySDP);
               }
      }
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
                  if (pm.getServices().get(s).getTrafficFlow().getAux().get(d))
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

   private void constraintMaxServiceDelay(GRBModel initialPlacement) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         double bigM = 0;
         bigM += getMaxPathDelay(pm.getServices().get(s).getTrafficFlow().getPaths()); // in ms
         bigM += getMaxProcessingDelay(pm.getServices().get(s).getFunctions()) * pm.getServices().get(s).getFunctions().size(); // in ms
         bigM += getMaxMigrationDelay(pm.getServices().get(s).getFunctions()); // in ms
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               if (pm.getServices().get(s).getTrafficFlow().getAux().get(d)) {
                  GRBLinExpr pathDelayExpr = new GRBLinExpr();
                  pathDelayExpr.addTerm(pm.getServices().get(s).getMaxDelay(), vars.zSPD[s][p][d]); // <-- TO BE UPDATED
                  pathDelayExpr.addConstant(bigM);
                  pathDelayExpr.addTerm(-bigM, vars.zSPD[s][p][d]);
                  String constrName = MAX_SERV_DELAY + "[s][p][d] --> " + "[" + s + "]"
                          + pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath() + "[" + d + "]";
                  model.getGrbModel().addConstr(serviceDelayExpr(s, p, d, initialPlacement), GRB.LESS_EQUAL, pathDelayExpr, constrName);
               }
      }
   }

   private GRBLinExpr serviceDelayExpr(int s, int p, int d, GRBModel initialPlacement) throws GRBException {
      GRBLinExpr serviceDelayExpr = new GRBLinExpr();
      serviceDelayExpr.add(propagationDelayExpr(s, p)); // adds propagation delay in ms
      serviceDelayExpr.add(processingDelayExpr(s, p, d)); // adds processing delay in ms
      if (initialPlacement != null)
         serviceDelayExpr.add(migrationDelayExpr(initialPlacement, s, p)); // adds migration delay in ms
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
                     if (service.getTrafficFlow().getAux().get(d1))
                        loadDelayExpr.addTerm(ratio * service.getTrafficFlow().getDemands().get(d1), vars.fXSVD[x][s][v][d1]);
                  GRBLinExpr processDelayExpr1 = new GRBLinExpr();
                  processDelayExpr1.addTerm((double) function.getAttribute(FUNCTION_MIN_PROCESS_DELAY), vars.fXSV[x][s][v]);
                  GRBLinExpr processDelayExpr2 = new GRBLinExpr();
                  processDelayExpr2.addTerm((double) function.getAttribute(FUNCTION_PROCESS_DELAY), vars.uX[x]);
                  GRBLinExpr processDelayExpr = new GRBLinExpr();
                  processDelayExpr.add(loadDelayExpr); // d_pro_q (first term)
                  processDelayExpr.add(processDelayExpr1); // d_pro_q (second term)
                  processDelayExpr.add(processDelayExpr2); // D_pro_x * ux
                  for (int d1 = 0; d1 < service.getTrafficFlow().getDemands().size(); d1++)
                     if (service.getTrafficFlow().getAux().get(d1)) {
                        GRBLinExpr processConstraintExpr1 = new GRBLinExpr();
                        processConstraintExpr1.addTerm(-(double) function.getAttribute(FUNCTION_MAX_DELAY), vars.fXSVD[x][s][v][d1]);
                        processConstraintExpr1.addConstant((double) function.getAttribute(FUNCTION_MAX_DELAY));
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

   private GRBLinExpr propagationDelayExpr(int s, int p) {
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      GRBLinExpr linkDelayExpr = new GRBLinExpr();
      double pathDelay = 0.0;
      for (Edge link : path.getEdgePath()) pathDelay += (double) link.getAttribute(LINK_DELAY) * 1000; // from sec to ms
//      linkDelayExpr.addTerm(pathDelay, vars.zSP[s][p]);
      linkDelayExpr.addConstant(pathDelay);
      return linkDelayExpr;
   }

   private GRBLinExpr migrationDelayExpr(GRBModel initialModel, int s, int p) throws GRBException {
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
      model.getGrbModel().addConstr(vars.mS[s], GRB.LESS_EQUAL, getMaxMigrationDelay(service.getFunctions()), FUNCTION_MIGRATION_DELAY);
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

   // read linear cost functions
   private CostFunctions getLinearCostFunctions() {
      CostFunctions costFunctions = null;
      TypeReference<CostFunctions> typeReference = new TypeReference<>() {
      };
      InputStream inputStream = TypeReference.class.getResourceAsStream("/aux_files/linear-cost-functions.yml");
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
         costFunctions = mapper.readValue(inputStream, typeReference);
      } catch (IOException e) {
         e.printStackTrace();
      }
      return costFunctions;
   }

   // get the longest propagation path delay
   private double getMaxPathDelay(List<Path> paths) {
      double maxPathDelay = 0;
      for (Path p : paths) {
         double pathDelay = 0;
         for (Edge e : p.getEdgePath())
            pathDelay += (double) e.getAttribute(LINK_DELAY) * 1000; // in ms
         if (pathDelay > maxPathDelay)
            maxPathDelay = pathDelay;
      }
      return maxPathDelay;
   }

   // get the maximum processing delay
   private double getMaxProcessingDelay(List<Function> functions) {
      double maxProcessingDelay = 0;
      for (Function f : functions)
         if ((double) f.getAttribute(FUNCTION_MAX_DELAY) > maxProcessingDelay)
            maxProcessingDelay = (double) f.getAttribute(FUNCTION_MAX_DELAY);
      return maxProcessingDelay;
   }

   // get the maximum migration delay
   private double getMaxMigrationDelay(List<Function> functions) {
      double maxMigrationDelay = 0;
      for (Function f : functions)
         if ((double) f.getAttribute(FUNCTION_MIGRATION_DELAY) > maxMigrationDelay)
            maxMigrationDelay = (double) f.getAttribute(FUNCTION_MIGRATION_DELAY);
      return maxMigrationDelay;
   }
}
