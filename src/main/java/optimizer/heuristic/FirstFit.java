package optimizer.heuristic;


import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Server;
import manager.elements.Service;
import manager.elements.TrafficFlow;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static optimizer.Definitions.*;

public class FirstFit {

   private static final Logger log = LoggerFactory.getLogger(FirstFit.class);

   protected boolean[][][] zSPD; // binary, routing per demand
   protected boolean[][][][] fXSVD; // binary, placement per demand
   protected Map<String, Double> uL; // link utilization
   protected Map<String, Double> uX; // server utilization
   protected boolean[] fX;
   protected boolean[][][] fXSV;
   protected boolean[][] zSP;
   protected double[] oX;
   protected double[][] oSV;
   protected double[][][] qSDP;
   protected double[][][] dSVX;
   protected double[] mS;
   protected Parameters pm;

   public FirstFit(Parameters pm) {
      this.pm = pm;
      zSPD = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
      fXSVD = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()][pm.getDemandsTrafficFlow()];
      uL = new HashMap<>();
      for (Edge link : pm.getLinks())
         uL.put(link.getId(), 0.0);
      uX = new HashMap<>();
      for (Server server : pm.getServers())
         uX.put(server.getId(), 0.0);
   }

   public void run() {

      for (int s = 0; s < pm.getServices().size(); s++) { // for every service
         Service service = pm.getServices().get(s);
         TrafficFlow tf = service.getTrafficFlow();
         for (int d = 0; d < tf.getDemands().size(); d++) { // for every traffic demand
            List<Integer> availablePaths = new ArrayList<>();
            int trafficDemand = tf.getDemands().get(d);
            for (int p = 0; p < tf.getPaths().size(); p++) // for every admissible path
               if (checkIfFreePathResources(tf.getPaths().get(p), trafficDemand)) // add paths with free path link resources
                  availablePaths.add(p);
            if (availablePaths.isEmpty()) { // if no path found, block
               // TO-DO blocking !!
               log.error("No available path found for [s][d] = [" + s + "][" + d + "]");
               continue;
            }
            for (Integer p : availablePaths) {
               Path pathFound = tf.getPaths().get(p);

               List<List<Integer>> listAvailableServersPerFunction = new ArrayList<>(); // find available servers for every function
               for (int v = 0; v < service.getFunctions().size(); v++) { // for every function
                  List<Integer> alreadyUsedServers = getUsedServersForFunction(s, v, d, pathFound); // get list of server where function already exists for the path
                  List<Integer> availableServers = new ArrayList<>();
                  Function function = service.getFunctions().get(v);
                  if (!alreadyUsedServers.isEmpty()) { // if function already exist...
                     for (int x = 0; x < alreadyUsedServers.size(); x++)
                        if (checkIfFreeServerResources(pm.getServers().get(x), trafficDemand, function)) // if free resources, save it
                           availableServers.add(x);
                  }
                  if (!availableServers.isEmpty()) { // if there are available servers, save them
                     listAvailableServersPerFunction.add(availableServers);
                     continue;
                  }
                  if (alreadyUsedServers.isEmpty()) { // if function does not exist yet in the path...
                     listAvailableServersPerFunction.add(getAvailableServers(pathFound, trafficDemand, function));
                  }
               }

               boolean noServerForFunctions = false;
               for (int v = 0; v < service.getFunctions().size(); v++) // check if all functions have at least one available server
                  if (listAvailableServersPerFunction.get(v).isEmpty()) {
                     noServerForFunctions = true;
                     log.error("No available servers for function [s][d][v] = [" + s + "][" + d + "][" + v + "]");
                  }
               if (!noServerForFunctions) { // if there are servers, make a pre-assigment of traffic
                  for (int v = 0; v < service.getFunctions().size(); v++) { // pre-assign traffic to servers
                     List<Integer> availableServers = listAvailableServersPerFunction.get(v);
                     for (Integer availableServer : availableServers)
                        assignFunctionAndTrafficToServer(s, availableServer, v, d);
                  }
                  assignTrafficDemandToPath(s, p, d);
                  break;
               } else {
                  // TO-DO blocking !!
               }
            }
         }
      }
   }

   protected void generateRestOfVariablesForResults() {

      fX = new boolean[pm.getServers().size()];
      fXSV = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
      zSP = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()];
      oX = new double[pm.getServers().size()];
      oSV = new double[pm.getServices().size()][pm.getServiceLength()];
      qSDP = new double[pm.getServices().size()][pm.getDemandsTrafficFlow()][pm.getPathsTrafficFlow()];
      dSVX = new double[pm.getServices().size()][pm.getServiceLength()][pm.getServers().size()];
      mS = new double[pm.getServices().size()];

      fXgenerate();
      fXSVgenerate();
      zSPgenerate();
      oXgenerate();
      oSVgenerate();
   }

   private boolean checkIfFreePathResources(Path path, double trafficDemand) {
      boolean isAvailable = true;
      for (Edge pathLink : path.getEdgePath())
         if (uL.get(pathLink.getId()) + (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)) >= 1.0) {
            isAvailable = false;
            break;
         }
      return isAvailable;
   }

   private List<Integer> getUsedServersForFunction(int s, int v, int d, Path path) {
      List<Integer> foundServers = new ArrayList<>();
      for (int n = 0; n < path.getNodePath().size(); n++)
         for (int x = 0; x < pm.getServers().size(); x++) {
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
               if (fXSVD[x][s][v][d]) {
                  foundServers.add(x);
               }
         }
      return foundServers;
   }

   private List<Integer> getAvailableServers(Path path, int trafficDemand, Function function) {
      List<Integer> availableServers = new ArrayList<>();
      for (int n = 0; n < path.getNodePath().size(); n++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
               if (checkIfFreeServerResources(pm.getServers().get(x), trafficDemand, function))
                  availableServers.add(x);
      return availableServers;
   }

   private boolean checkIfFreeServerResources(Server server, double trafficDemand, Function function) {
      return uX.get(server.getId()) + ((trafficDemand * (double) function.getAttribute(FUNCTION_LOAD_RATIO)) / server.getCapacity()) <= 1;
   }

   private void assignTrafficDemandToPath(int s, int p, int d) {
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
      for (Edge pathLink : path.getEdgePath())
         uL.put(pathLink.getId(), uL.get(pathLink.getId()) + (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)));
      zSPD[s][p][d] = true;
   }

   private void assignFunctionAndTrafficToServer(int s, int x, int v, int d) {
      Service service = pm.getServices().get(s);
      Server server = pm.getServers().get(x);
      double trafficDemand = service.getTrafficFlow().getDemands().get(d);
      Function function = service.getFunctions().get(v);
      fXSVD[x][s][v][d] = true;
      uX.put(server.getId(), uX.get(server.getId()) + (trafficDemand * (double) function.getAttribute(FUNCTION_LOAD_RATIO) / server.getCapacity()));
   }

   private void fXgenerate() {
      for (int x = 0; x < pm.getServers().size(); x++) {
         outerLoop:
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (fXSVD[x][s][v][d]) {
                     fX[x] = true;
                     break outerLoop;
                  }
      }
   }

   private void fXSVgenerate() {
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (fXSVD[x][s][v][d]) {
                     fXSV[x][s][v] = true;
                     break;
                  }
   }

   private void zSPgenerate() {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               if (zSPD[s][p][d]) {
                  zSP[s][p] = true;
                  break;
               }
   }

   private void oXgenerate() {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) == null) {
            if (fX[x])
               oX[x] = (double) pm.getAux().get(SERVER_IDLE_ENERGY_COST) + (uX.get(pm.getServers().get(x).getId()) * (double) pm.getAux().get(SERVER_UTIL_ENERGY_COST));
         } else
            oX[x] = 0.0;
   }

   private void oSVgenerate() {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) != null)
                  if (fXSV[x][s][v])
                     oSV[s][v] = (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_CHARGES);
   }

   private void qSDPgenerate(GRBModel initialPlacement) throws GRBException {

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               if (zSPD[s][p][d]) {
                  double serviceDelay = 0;
                  Service service = pm.getServices().get(s);
                  Path path = service.getTrafficFlow().getPaths().get(p);

                  // propagation delay
                  double pathDelay = 0.0;
                  for (Edge link : path.getEdgePath())
                     pathDelay += (double) link.getAttribute(LINK_DELAY) * 1000; // in ms
                  serviceDelay += pathDelay;

                  // processing delay
                  for (int n = 0; n < path.getNodePath().size(); n++)
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
                           for (int v = 0; v < service.getFunctions().size(); v++)
                              if (fXSVD[x][s][v][d]) {
                                 Function function = service.getFunctions().get(v);
                                 double ratio = (double) function.getAttribute(FUNCTION_LOAD_RATIO)
                                         * (double) function.getAttribute(FUNCTION_PROCESS_TRAFFIC_DELAY)
                                         / (int) function.getAttribute(FUNCTION_MAX_CAP_SERVER);
                                 double processinDelay = 0;
                                 for (int d1 = 0; d1 < service.getTrafficFlow().getDemands().size(); d1++)
                                    if (service.getTrafficFlow().getAux().get(d1))
                                       if (fXSVD[x][s][v][d1])
                                          processinDelay += ratio * service.getTrafficFlow().getDemands().get(d1);
                                 processinDelay += (double) function.getAttribute(FUNCTION_MIN_PROCESS_DELAY);
                                 processinDelay += (double) function.getAttribute(FUNCTION_PROCESS_DELAY) * uX.get(pm.getServers().get(x).getId());
                                 serviceDelay += processinDelay;
                                 dSVX[s][v][x] = processinDelay;
                              }

                  // migration delay
                  double maxDelay = 0;
                  for (int n = 0; n < path.getNodePath().size(); n++)
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
                           for (int v = 0; v < service.getFunctions().size(); v++)
                              if (initialPlacement.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 1.0 && !fXSV[x][s][v]) {
                                 double delay = (double) service.getFunctions().get(v).getAttribute(FUNCTION_MIGRATION_DELAY);
                                 if (delay > maxDelay)
                                    maxDelay = delay;
                              }
                  serviceDelay += maxDelay;
                  mS[s] = maxDelay;
                  double profit = 0;
                  for (int v = 0; v < service.getFunctions().size(); v++)
                     profit += (double) service.getFunctions().get(v).getAttribute(FUNCTION_CHARGES);
                  double qosPenalty = (double) pm.getAux().get(QOS_PENALTY_RATIO) * profit; // in $/h

                  qSDP[s][d][p] = ((serviceDelay / service.getMaxDelay()) - 1) * qosPenalty; // in $/h
               }
   }


}
