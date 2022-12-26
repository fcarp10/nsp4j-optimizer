package optimizer.lp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gurobi.*;
import optimizer.Parameters;
import optimizer.elements.*;
import optimizer.gui.Scenario;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.*;

public class SpecificConstraints {

   private static final Logger log = LoggerFactory.getLogger(SpecificConstraints.class);
   private ModelLP modelLP;
   private VariablesLP vars;
   private Parameters pm;

   public SpecificConstraints(Parameters pm, ModelLP modelLP, Scenario sc, boolean[][][] initialPlacement) {
      try {
         this.pm = pm;
         this.modelLP = modelLP;
         this.vars = modelLP.getVars();

         // create link and server load expressions
         GRBLinExpr[] linkLoadExpr = createLinkLoadExpr();
         GRBLinExpr[] serverLoadExpr = createServerLoadExpr();

         // dimensioning
         if (vars.xN != null)
            dimensioning_num_servers(serverLoadExpr);
         if (vars.cLT != null)
            dimensioning_link_capacity(linkLoadExpr);
         if (vars.cXT != null)
            dimensioning_server_capacity(serverLoadExpr);

         // max utilization
         if (vars.uMax != null)
            maxUtilization();

         // monetary costs
         if (vars.oX != null)
            opexServers();
         if (vars.oSV != null)
            functionsCharges();
         if (vars.qSDP != null)
            qosPenalties(initialPlacement);

         // rest of specific constraints
         if (sc.getConstraints().get(SYNC_TRAFFIC))
            syncTraffic(linkLoadExpr);
         if (sc.getConstraints().get(MAX_SERV_DELAY))
            constraintMaxServiceDelay(initialPlacement);
         if (sc.getConstraints().get(CLOUD_ONLY))
            useOnlyCloudServers();
         if (sc.getConstraints().get(EDGE_ONLY))
            useOnlyEdgeServers();
         if (sc.getConstraints().get(SINGLE_PATH))
            singlePath();
         if (sc.getConstraints().get(SET_INIT_PLC))
            setInitPlc(initialPlacement);
         if (sc.getConstraints().get(FORCE_SRC_DST))
            forceSrcDst();
         if (sc.getConstraints().get(CONST_REP))
            constRep();
         if (sc.getConstraints().containsKey(PATHS_SERVERS_CLOUD))
            if (sc.getConstraints().get(PATHS_SERVERS_CLOUD))
               constraintPathsServersCloud();

         // create link and server utilization expressions
         GRBLinExpr[] luExpr = createLinkUtilizationExpr(linkLoadExpr);
         GRBLinExpr[] xuExpr = createServerUtilizationExpr(serverLoadExpr);

         // set linear utilization cost functions constraints
         if (vars.kL != null)
            linearUtilCostFunctions(luExpr, vars.kL);
         if (vars.kX != null)
            linearUtilCostFunctions(xuExpr, vars.kX);

         // constraint link utilization
         if (!sc.getObjFunc().equals(DIMEN_LINK_CAP)) // except when dimensioning
            for (int l = 0; l < pm.getLinks().size(); l++)
               modelLP.getGrbModel().addConstr(luExpr[l], GRB.EQUAL, vars.uL[l],
                     uL + "[" + pm.getLinks().get(l).getId() + "]");

         // constraint server utilization
         if (!sc.getObjFunc().equals(DIMEN_NUM_SERVERS)
               || !sc.getObjFunc().equals(DIMEN_SERVER_CAP)) // except when dimensioning
            for (int x = 0; x < pm.getServers().size(); x++)
               modelLP.getGrbModel().addConstr(xuExpr[x], GRB.EQUAL, vars.uX[x], uX + "[x] --> " + "[" + x + "]");

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void dimensioning_num_servers(GRBLinExpr[] serverLoadExpr) throws GRBException {
      for (int n = 0; n < pm.getNodes().size(); n++) {
         GRBLinExpr expr1 = new GRBLinExpr();
         expr1.addTerm((int) pm.getGlobal(SERVER_DIMENSIONING_CAPACITY), vars.xN[n]);
         GRBLinExpr expr2 = new GRBLinExpr();
         expr2.multAdd((double) pm.getGlobal(OVERPROVISIONING_NUM_SERVERS), serverLoadExpr[n]);
         modelLP.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, expr1, DIMEN_NUM_SERVERS);
      }
   }

   private void dimensioning_link_capacity(GRBLinExpr[] linkLoadExpr) throws GRBException {
      ArrayList<Integer> types = (ArrayList<Integer>) pm.getGlobal().get(LINK_CAPACITY_TYPES);
      for (int l = 0; l < pm.getLinks().size(); l++) {
         GRBLinExpr expr1 = new GRBLinExpr();
         expr1.multAdd(1.0, linkLoadExpr[l]);
         GRBLinExpr expr2 = new GRBLinExpr();
         for (int t = 0; t < types.size(); t++)
            expr2.addTerm((double) pm.getGlobal(OVERPROVISIONING_LINK_CAPACITY) * types.get(t), vars.cLT[l][t]);
         modelLP.getGrbModel().addConstr(expr1, GRB.LESS_EQUAL, expr2, DIMEN_LINK_CAP);
      }
      for (int l = 0; l < pm.getLinks().size(); l++) {
         GRBLinExpr expr1 = new GRBLinExpr();
         for (int t = 0; t < types.size(); t++)
            expr1.addTerm(1.0, vars.cLT[l][t]);
         modelLP.getGrbModel().addConstr(expr1, GRB.EQUAL, 1.0, DIMEN_LINK_CAP);
      }
   }

   private void dimensioning_server_capacity(GRBLinExpr[] serverLoadExpr) throws GRBException {
      ArrayList<Integer> types = (ArrayList<Integer>) pm.getGlobal().get(SERVER_CAPACITY_TYPES);
      for (int x = 0; x < pm.getServers().size(); x++) {
         GRBLinExpr expr1 = new GRBLinExpr();
         expr1.multAdd(1.0, serverLoadExpr[x]);
         GRBLinExpr expr2 = new GRBLinExpr();
         for (int t = 0; t < types.size(); t++)
            expr2.addTerm((double) pm.getGlobal(OVERPROVISIONING_SERVER_CAPACITY) * types.get(t), vars.cXT[x][t]);
         modelLP.getGrbModel().addConstr(expr1, GRB.LESS_EQUAL, expr2, DIMEN_SERVER_CAP);
      }
      for (int x = 0; x < pm.getServers().size(); x++) {
         GRBLinExpr expr1 = new GRBLinExpr();
         for (int t = 0; t < types.size(); t++)
            expr1.addTerm(1.0, vars.cXT[x][t]);
         modelLP.getGrbModel().addConstr(expr1, GRB.EQUAL, 1.0, DIMEN_SERVER_CAP);
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
                     expr.addTerm((double) pm.getServices().get(s).getTrafficFlow().getDemands().get(d),
                           vars.zSPD[s][p][d]);
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
               double overhead = (double) function.getAttribute(FUNCTION_OVERHEAD_RATIO)
                     * (int) function.getAttribute(FUNCTION_MAX_BW) * (int) function.getAttribute(FUNCTION_MAX_DEM)
                     * (double) function.getAttribute(FUNCTION_LOAD_RATIO);
               expr.addTerm(overhead, vars.fXSV[x][s][v]);
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
            modelLP.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, grbVar[e], UTIL_COSTS);
         }
   }

   private void maxUtilization() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         modelLP.getGrbModel().addConstr(vars.uX[x], GRB.LESS_EQUAL, vars.uMax, uMax);
      for (int l = 0; l < pm.getLinks().size(); l++)
         modelLP.getGrbModel().addConstr(vars.uL[l], GRB.LESS_EQUAL, vars.uMax, uMax);
   }

   private void opexServers() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) == null) {
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm((double) pm.getGlobal().get(SERVER_IDLE_ENERGY_COST), vars.fX[x]);
            expr.addTerm((double) pm.getGlobal().get(SERVER_UTIL_ENERGY_COST), vars.uX[x]);
            modelLP.getGrbModel().addConstr(expr, GRB.EQUAL, vars.oX[x], oX);
         } else {
            modelLP.getGrbModel().addConstr(vars.oX[x], GRB.EQUAL, 0, oX);
         }
   }

   private void functionsCharges() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) != null) {
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm((double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_CHARGES),
                        vars.fXSV[x][s][v]); // in $/h
                  modelLP.getGrbModel().addConstr(vars.oSV[s][v], GRB.EQUAL, expr, oSV); // to be updated from the paper
                                                                                         // (not affecting to the
                                                                                         // results because only one
                                                                                         // server in the cloud so no
                                                                                         // replicas will be replicated
                                                                                         // within the same server)
               }
   }

   private void qosPenalties(boolean[][][] initialPlacement) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service service = pm.getServices().get(s);
         double bigM = 0;
         bigM += getMaxPathDelay(service.getTrafficFlow().getPaths()); // in ms
         bigM += getMaxProcessingDelay(service.getFunctions()) * service.getFunctions().size(); // in ms
         bigM += getMaxServiceDowntime(service); // in ms

         for (int p = 0; p < service.getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
               if (service.getTrafficFlow().getAux().get(d)) {

                  // linearization of delay and routing variables
                  linearizationOfzSPDandDelay(initialPlacement);

                  // delay / max_delay
                  double maxDelay = 0;
                  maxDelay += service.getMaxPropagationDelay();
                  for (int v = 0; v < service.getFunctions().size(); v++)
                     maxDelay += (double) service.getFunctions().get(v).getAttribute(FUNCTION_MAX_DELAY);

                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm(1.0 / maxDelay, vars.ySDP[s][d][p]); // ratio
                  expr.addTerm(-1.0, vars.zSPD[s][p][d]);

                  // qos_penalty value
                  double profit = 0;
                  for (int v = 0; v < service.getFunctions().size(); v++)
                     profit += (double) service.getFunctions().get(v).getAttribute(FUNCTION_CHARGES);
                  double qosPenalty = (double) pm.getGlobal().get(QOS_PENALTY_RATIO) * profit; // in $/h

                  GRBLinExpr expr2 = new GRBLinExpr();
                  expr2.multAdd(qosPenalty, expr); // in $/h
                  modelLP.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, vars.qSDP[s][d][p], qSDP);
                  if (maxDelay > bigM)
                     printLog(log, WARNING, "max. service delay is not bounding");
                  double upperBound = ((bigM / maxDelay) - 1) * qosPenalty;
                  modelLP.getGrbModel().addConstr(vars.qSDP[s][d][p], GRB.LESS_EQUAL, upperBound, qSDP);
               } else {
                  modelLP.getGrbModel().addConstr(vars.qSDP[s][d][p], GRB.EQUAL, 0.0, qSDP);
                  modelLP.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.EQUAL, 0.0, ySDP);
               }
      }
   }

   private void linearizationOfzSPDandDelay(boolean[][][] initialPlacement) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service service = pm.getServices().get(s);
         double bigM = 0;
         bigM += getMaxPathDelay(service.getTrafficFlow().getPaths()); // in ms
         bigM += getMaxProcessingDelay(service.getFunctions()) * service.getFunctions().size(); // in ms
         bigM += getMaxServiceDowntime(service); // in ms

         for (int p = 0; p < service.getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
               if (service.getTrafficFlow().getAux().get(d)) {
                  GRBLinExpr serviceDelayExpr = serviceDelayExpr(s, p, d, initialPlacement); // in ms

                  // linearization of delay and routing variables
                  modelLP.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.LESS_EQUAL, serviceDelayExpr, ySDP);
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm(bigM, vars.zSPD[s][p][d]);
                  modelLP.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.LESS_EQUAL, expr, ySDP);
                  expr = new GRBLinExpr();
                  expr.addTerm(bigM, vars.zSPD[s][p][d]);
                  expr.addConstant(-bigM);
                  expr.add(serviceDelayExpr);
                  modelLP.getGrbModel().addConstr(vars.ySDP[s][d][p], GRB.GREATER_EQUAL, expr, ySDP);
               }
      }
   }

   private void constraintMaxServiceDelay(boolean[][][] initialPlacement) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service service = pm.getServices().get(s);
         double bigM = 0;
         bigM += getMaxPathDelay(service.getTrafficFlow().getPaths()); // in ms
         bigM += getMaxProcessingDelay(service.getFunctions()) * service.getFunctions().size(); // in ms
         bigM += getMaxServiceDowntime(service); // in ms

         for (int p = 0; p < service.getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
               if (service.getTrafficFlow().getAux().get(d)) {
                  GRBLinExpr serviceDelayExpr = serviceDelayExpr(s, p, d, initialPlacement); // in ms
                  GRBLinExpr pathDelayExpr = new GRBLinExpr();
                  pathDelayExpr.addTerm(pm.getServices().get(s).getMaxDelay(), vars.zSPD[s][p][d]);
                  pathDelayExpr.addConstant(bigM);
                  pathDelayExpr.addTerm(-bigM, vars.zSPD[s][p][d]);
                  modelLP.getGrbModel().addConstr(serviceDelayExpr, GRB.LESS_EQUAL, pathDelayExpr, MAX_SERV_DELAY);
               }
      }
   }

   private GRBLinExpr serviceDelayExpr(int s, int p, int d, boolean[][][] initialPlacement) throws GRBException {
      GRBLinExpr serviceDelayExpr = new GRBLinExpr();
      serviceDelayExpr.add(propagationDelayExpr(s, p)); // adds propagation delay in ms
      serviceDelayExpr.add(processingDelayExpr(s, p, d)); // adds processing delay in ms
      if (initialPlacement != null)
         serviceDelayExpr.add(migrationDelayExpr(initialPlacement, s)); // adds migration delay in ms
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
                        / ((int) function.getAttribute(FUNCTION_MAX_DEM) * (int) function.getAttribute(FUNCTION_MAX_BW)
                              * (double) function.getAttribute(FUNCTION_LOAD_RATIO));
                  GRBLinExpr loadDelayExpr = new GRBLinExpr();
                  for (int d1 = 0; d1 < service.getTrafficFlow().getDemands().size(); d1++)
                     if (service.getTrafficFlow().getAux().get(d1))
                        loadDelayExpr.addTerm(ratio * service.getTrafficFlow().getDemands().get(d1),
                              vars.fXSVD[x][s][v][d1]);
                  GRBLinExpr processDelayExpr1 = new GRBLinExpr();
                  processDelayExpr1.addTerm((double) function.getAttribute(FUNCTION_MIN_PROCESS_DELAY),
                        vars.fXSV[x][s][v]);
                  GRBLinExpr processDelayExpr2 = new GRBLinExpr();
                  processDelayExpr2.addTerm((double) function.getAttribute(FUNCTION_PROCESS_DELAY), vars.uX[x]);
                  GRBLinExpr processDelayExpr = new GRBLinExpr();
                  processDelayExpr.add(loadDelayExpr); // d_pro_q (first term)
                  processDelayExpr.add(processDelayExpr1); // d_pro_q (second term)
                  processDelayExpr.add(processDelayExpr2); // D_pro_x * ux
                  for (int d1 = 0; d1 < service.getTrafficFlow().getDemands().size(); d1++)
                     if (service.getTrafficFlow().getAux().get(d1)) {
                        GRBLinExpr processConstraintExpr1 = new GRBLinExpr();
                        processConstraintExpr1.addTerm(-(double) function.getAttribute(FUNCTION_MAX_DELAY),
                              vars.fXSVD[x][s][v][d1]);
                        processConstraintExpr1.addConstant((double) function.getAttribute(FUNCTION_MAX_DELAY));
                        processConstraintExpr1.addTerm(1.0, vars.dSVXD[s][v][x][d1]);
                        modelLP.getGrbModel().addConstr(processDelayExpr, GRB.LESS_EQUAL, processConstraintExpr1,
                              FUNCTION_PROCESS_TRAFFIC_DELAY);
                        GRBLinExpr processConstraintExpr2 = new GRBLinExpr();
                        processConstraintExpr2.addTerm((double) function.getAttribute(FUNCTION_MAX_DELAY),
                              vars.fXSVD[x][s][v][d1]);
                        modelLP.getGrbModel().addConstr(vars.dSVXD[s][v][x][d1], GRB.LESS_EQUAL, processConstraintExpr2,
                              FUNCTION_PROCESS_TRAFFIC_DELAY);
                     }
                  processDelayGlobalExpr.addTerm(1.0, vars.dSVXD[s][v][x][d]);
               }
      return processDelayGlobalExpr;
   }

   private GRBLinExpr propagationDelayExpr(int s, int p) {
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      GRBLinExpr linkDelayExpr = new GRBLinExpr();
      double pathDelay = 0.0;
      for (Edge link : path.getEdgePath())
         pathDelay += (double) link.getAttribute(LINK_DELAY) * 1000; // from sec to ms
      linkDelayExpr.addConstant(pathDelay);
      return linkDelayExpr;
   }

   private GRBLinExpr migrationDelayExpr(boolean[][][] initialModel, int s) {
      Service service = pm.getServices().get(s);
      double downtime = (double) service.getAttribute(SERVICE_DOWNTIME);
      GRBLinExpr linExpr = new GRBLinExpr();
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int v = 0; v < service.getFunctions().size(); v++)
            if (initialModel[x][s][v]) {
               linExpr.addTerm(-downtime, vars.fXSV[x][s][v]);
               linExpr.addConstant(downtime);
            }
      return linExpr;
   }

   // synchronization traffic
   private void syncTraffic(GRBLinExpr[] linkLoadExpr) throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int y = 0; y < pm.getServers().size(); y++) {
                  if (pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent()))
                     continue;
                  modelLP.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.fXSV[x][s][v],
                        gSVXY + "_1[s][v][x][y] --> " + "[" + s + "][" + v + "][" + x + "][" + y + "]");
                  modelLP.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, vars.fXSV[y][s][v],
                        gSVXY + "_2[s][v][x][y] --> " + "[" + s + "][" + v + "][" + x + "][" + y + "]");
                  GRBLinExpr expr = new GRBLinExpr();
                  expr.addTerm(1.0, vars.fXSV[x][s][v]);
                  expr.addTerm(1.0, vars.fXSV[y][s][v]);
                  expr.addConstant(-1.0);
                  modelLP.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.GREATER_EQUAL, expr,
                        gSVXY + "_3[s][v][x][y] --> " + "[" + s + "][" + v + "][" + x + "][" + y + "]");
                  expr = new GRBLinExpr();
                  for (int p = 0; p < pm.getPaths().size(); p++) {
                     Path pa = pm.getPaths().get(p);
                     if (pa.getNodePath().get(0).equals(pm.getServers().get(x).getParent()) & pa.getNodePath()
                           .get(pa.getNodePath().size() - 1).equals(pm.getServers().get(y).getParent()))
                        expr.addTerm(1.0, vars.hSVP[s][v][p]);
                  }
                  modelLP.getGrbModel().addConstr(vars.gSVXY[s][v][x][y], GRB.LESS_EQUAL, expr,
                        gSVXY + "_4[s][v][x][y] --> " + "[" + s + "][" + v + "][" + x + "][" + y + "]");
                  modelLP.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, 1.0,
                        gSVXY + "_3[s][v][x][y] --> " + "[" + s + "][" + v + "][" + x + "][" + y + "]");
               }
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int n = 0; n < pm.getNodes().size(); n++)
               for (int m = 0; m < pm.getNodes().size(); m++) {
                  if (n == m)
                     continue;
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
                  modelLP.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2,
                        SYNC_TRAFFIC + "[s][v][n][m] --> " + "[" + s + "][" + v + "][" + n + "][" + m + "]");
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
                  double trafficScaled = traffic
                        * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_SYNC_LOAD_RATIO);
                  expr.addTerm(trafficScaled, vars.hSVP[s][v][p]);
               }
            }
         }
         linkLoadExpr[l].add(expr);
      }
   }

   // use only cloud servers
   private void useOnlyCloudServers() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) == null)
            modelLP.getGrbModel().addConstr(vars.fX[x], GRB.EQUAL, 0.0, CLOUD_ONLY);
   }

   // use only edge servers
   private void useOnlyEdgeServers() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) != null)
            modelLP.getGrbModel().addConstr(vars.fX[x], GRB.EQUAL, 0.0, EDGE_ONLY);
   }

   // Single path (no replicas)
   private void singlePath() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            expr.addTerm(1.0, vars.zSP[s][p]);
         modelLP.getGrbModel().addConstr(expr, GRB.EQUAL, 1, SINGLE_PATH);
      }
   }

   // Initial placement as constraints (no migrations)
   private void setInitPlc(boolean[][][] initialPlacement) throws GRBException {
      if (initialPlacement != null) {
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (initialPlacement[x][s][v])
                     modelLP.getGrbModel().addConstr(vars.fXSV[x][s][v], GRB.EQUAL, 1, SET_INIT_PLC);
      }
   }

   // Constraint paths servers cloud
   private void constraintPathsServersCloud() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) != null)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                  GRBLinExpr expr = new GRBLinExpr();
                  for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                     if (pm.getServices().get(s).getTrafficFlow().getPaths().get(p)
                           .contains(pm.getServers().get(x).getParent()))
                        expr.addTerm(1.0, vars.zSP[s][p]);
                  modelLP.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, vars.fXSV[x][s][v], PATHS_SERVERS_CLOUD);
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
         modelLP.getGrbModel().addConstr(exprSrc, GRB.EQUAL, 1.0, FORCE_SRC_DST);
         modelLP.getGrbModel().addConstr(exprDst, GRB.EQUAL, 1.0, FORCE_SRC_DST);
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
         modelLP.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, minPaths, CONST_REP);
         modelLP.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxPaths, CONST_REP);
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
}
