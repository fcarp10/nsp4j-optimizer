package optimizer.heuristic;


import manager.Parameters;
import manager.elements.Function;
import manager.elements.Server;
import manager.elements.Service;
import manager.elements.TrafficFlow;
import optimizer.results.Auxiliary;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static optimizer.Definitions.*;

public class FirstFit {

   private static final Logger log = LoggerFactory.getLogger(FirstFit.class);

   protected Parameters pm;
   protected Heuristic heu;

   public FirstFit(Parameters pm, Heuristic heuristic) {
      this.pm = pm;
      this.heu = heuristic;
   }

   public void run(String objFunc) {

      final boolean requiresOrdering = objFunc.equals(OPEX_SERVERS_OBJ) || objFunc.equals(FUNCTIONS_CHARGES_OBJ) || objFunc.equals(QOS_PENALTIES_OBJ);

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
               Auxiliary.printLog(log, ERROR, "No available path found for [s][d] = [" + s + "][" + d + "]");
               continue;
            }

            Map<Integer, List<List<Integer>>> admissiblePaths = findAdmissiblePaths(availablePaths, s, d);
            if (admissiblePaths.isEmpty())
               // TO-DO blocking
               Auxiliary.printLog(log, ERROR, "No path available");

            List<Integer> paths = new ArrayList<>(admissiblePaths.keySet());

            if (requiresOrdering) { // order set of admissible paths depending on the objective function
               boolean cloudPathsFirst = false;
               if (objFunc.equals(OPEX_SERVERS_OBJ))
                  cloudPathsFirst = true;
               paths = orderPaths(paths, cloudPathsFirst, tf);
            }

            for (Integer p : paths) {
               List<List<Integer>> listAvailableServersPerFunction = admissiblePaths.get(p);

               // check if available aggregated server capacity for all functions
               if (!checkIfFreeResourcesForAllFunctions(service, trafficDemand, listAvailableServersPerFunction))
                  continue;

               for (int v = 0; v < service.getFunctions().size(); v++) { // assign traffic to servers
                  List<Integer> availableServers = listAvailableServersPerFunction.get(v);

                  if (requiresOrdering) { // order set of available servers depending on the objective function
                     boolean cloudServersFirst = false;
                     if (objFunc.equals(OPEX_SERVERS_OBJ))
                        cloudServersFirst = true;
                     availableServers = selectServers(availableServers, cloudServersFirst);
                  }

                  boolean isFunctionAllocated = false;
                  for (Integer xAvailable : availableServers)
                     if (checkIfFreeServerResources(s, xAvailable, v, d)) {
                        assignFunctionAndTrafficToServer(s, xAvailable, v, d);
                        isFunctionAllocated = true;
                        break;
                     }
                  if (!isFunctionAllocated)
                     Auxiliary.printLog(log, ERROR, "function not allocated");
               }
               assignTrafficDemandToPath(s, p, d);
               break; // go to the next traffic demand
            }
         }

         // add synchronization traffic
         addSynchronizationTraffic(s);
      }
   }

   private Map<Integer, List<List<Integer>>> findAdmissiblePaths(List<Integer> availablePaths, int s, int d) {
      Service service = pm.getServices().get(s);
      TrafficFlow tf = service.getTrafficFlow();
      Map<Integer, List<List<Integer>>> admissiblePaths = new HashMap<>();
      for (Integer p : availablePaths) {

         Path pathFound = tf.getPaths().get(p);
         List<List<Integer>> listAvailableServersPerFunction = new ArrayList<>(); // find available servers for every function

         for (int v = 0; v < service.getFunctions().size(); v++) { // for every function

            List<Integer> availableServersPerFunction = new ArrayList<>();

            List<Integer> alreadyUsedServers = getUsedServersForFunction(s, v, pathFound); // get list of server where function already exists for the path
            List<Integer> availableServers = new ArrayList<>();

            if (!alreadyUsedServers.isEmpty()) // if function already exist...
               for (int x = 0; x < alreadyUsedServers.size(); x++)
                  if (checkIfFreeServerResources(s, x, v, d)) // if free resources, save it
                     availableServers.add(x);

            if (!availableServers.isEmpty()) // if there are available servers with functions already, save them
               availableServersPerFunction = availableServers;
            else
               availableServersPerFunction = getAvailableServers(s, p, d, v); // if function does not exist yet in the path, get available servers for placement

            listAvailableServersPerFunction.add(availableServersPerFunction);
         }

         boolean noServerForFunctions = false;
         for (int v = 0; v < service.getFunctions().size(); v++) // check if all functions have at least one available server
            if (listAvailableServersPerFunction.get(v).isEmpty())
               noServerForFunctions = true;

         if (!noServerForFunctions)// if there are servers, add path
            admissiblePaths.put(p, listAvailableServersPerFunction);

      }
      return admissiblePaths;
   }

   private void addSynchronizationTraffic(int s) {

      Service service = pm.getServices().get(s);
      for (int v = 0; v < service.getFunctions().size(); v++) {
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int y = 0; y < pm.getServers().size(); y++) {
               if (pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent())) continue;
               if (heu.fXSV[x][s][v] && heu.fXSV[y][s][v]) {

                  heu.gSVXY[s][v][x][y] = true;
                  // calculate the sync traffic
                  double traffic = 0;
                  for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
                     traffic += service.getTrafficFlow().getDemands().get(d);
                  double syncTraffic = traffic * (double) service.getFunctions().get(v).getAttribute(FUNCTION_SYNC_LOAD_RATIO);

                  // search an available path for the sync traffic
                  boolean foundSyncPath = false;
                  for (int p = 0; p < pm.getPaths().size(); p++) {
                     Path path = pm.getPaths().get(p);
                     if (path.getNodePath().get(0).equals(pm.getServers().get(x).getParent())
                             & path.getNodePath().get(path.getNodePath().size() - 1)
                             .equals(pm.getServers().get(y).getParent()))
                        if (checkIfFreePathResources(path, syncTraffic)) {
                           addSyncTraffic(path, syncTraffic, s, v, p);
                           foundSyncPath = true;
                           break;
                        }
                  }
                  if (!foundSyncPath) {
                     // TO-DO implement blocking
                     Auxiliary.printLog(log, ERROR, "No available path found for sync traffic");
                  }
               }
            }
      }
   }

   private List<Integer> selectServers(List<Integer> servers, boolean cloudFirst) {

      List<Integer> chosenServers = new ArrayList<>();

      if (cloudFirst) {
         for (Integer x : servers)
            if (pm.getServers().get(x).getParent().hasAttribute(NODE_CLOUD))
               chosenServers.add(x);

         int serverIndex = -1;
         for (int i = 0; i < servers.size(); i++) {
            if (!servers.get(i).equals(chosenServers.get(chosenServers.size() - 1))) continue;
            serverIndex = i + 1;
         }

         for (int i = serverIndex; i < servers.size(); i++)
            chosenServers.add(servers.get(i));

      } else
         chosenServers = servers;

      return chosenServers;
   }


   private List<Integer> orderPaths(List<Integer> paths, boolean connectingCloudFirst, TrafficFlow tf) {
      List<Integer> pathsConnectingEdge = new ArrayList<>();
      List<Integer> pathsConnectingCloud = new ArrayList<>();

      for (Integer p : paths) {
         boolean connectsCloud = false;
         for (Node node : tf.getPaths().get(p).getNodePath())
            if (node.hasAttribute(NODE_CLOUD))
               connectsCloud = true;
         if (connectsCloud)
            pathsConnectingCloud.add(p);
         else pathsConnectingEdge.add(p);

      }

      List<Integer> orderedPaths = new ArrayList<>();
      if (connectingCloudFirst) {
         orderedPaths.addAll(pathsConnectingCloud);
         orderedPaths.addAll(pathsConnectingEdge);
      } else {
         orderedPaths.addAll(pathsConnectingEdge);
         orderedPaths.addAll(pathsConnectingCloud);
      }

      return orderedPaths;
   }

   private boolean checkIfFreeResourcesForAllFunctions(Service service, int trafficDemand, List<List<Integer>> listAvailableServersPerFunction) {
      boolean availableResources = false;
      // TO-DO
      return availableResources;
   }

   private boolean checkIfFreePathResources(Path path, double trafficDemand) {
      boolean isAvailable = true;
      for (Edge pathLink : path.getEdgePath())
         if (heu.uL.get(pathLink.getId()) + (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)) >= 1.0) {
            isAvailable = false;
            break;
         }
      return isAvailable;
   }

   private List<Integer> getUsedServersForFunction(int s, int v, Path path) {
      List<Integer> foundServers = new ArrayList<>();
      for (int n = 0; n < path.getNodePath().size(); n++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n))) {
               if (heu.fXSV[x][s][v])
                  foundServers.add(x);
               break;
            }
      return foundServers;
   }

   private List<Integer> getAvailableServers(int s, int p, int d, int v) {
      Service service = pm.getServices().get(s);
      Path path = service.getTrafficFlow().getPaths().get(p);
      List<Integer> availableServers = new ArrayList<>();
      for (int n = 0; n < path.getNodePath().size(); n++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
               if (checkIfFreeServerResources(s, x, v, d))
                  availableServers.add(x);
      return availableServers;
   }

   private boolean checkIfFreeServerResources(int s, int x, int v, int d) {
      Service service = pm.getServices().get(s);
      Server server = pm.getServers().get(x);
      double trafficDemand = service.getTrafficFlow().getDemands().get(d);
      Function function = service.getFunctions().get(v);
      int functionOverhead = 0;
      if (!heu.fXSV[x][s][v])
         functionOverhead = (int) function.getAttribute(FUNCTION_OVERHEAD);
      double resourcesToAdd = functionOverhead + (trafficDemand * (double) function.getAttribute(FUNCTION_LOAD_RATIO));
      return heu.uX.get(server.getId()) + (resourcesToAdd / server.getCapacity()) <= 1.0;
   }

   private void assignTrafficDemandToPath(int s, int p, int d) {
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
      for (Edge pathLink : path.getEdgePath())
         heu.uL.put(pathLink.getId(), heu.uL.get(pathLink.getId()) + (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)));
      heu.zSPD[s][p][d] = true;
   }

   private void assignFunctionAndTrafficToServer(int s, int x, int v, int d) {
      Service service = pm.getServices().get(s);
      Server server = pm.getServers().get(x);
      double trafficDemand = service.getTrafficFlow().getDemands().get(d);
      Function function = service.getFunctions().get(v);
      heu.fXSVD[x][s][v][d] = true;
      if (!heu.fXSV[x][s][v]) {
         heu.fXSV[x][s][v] = true;
         int functionOverhead = (int) function.getAttribute(FUNCTION_OVERHEAD);
         heu.uX.put(server.getId(), heu.uX.get(server.getId()) + ((double) functionOverhead / server.getCapacity()));
      }
      heu.uX.put(server.getId(), heu.uX.get(server.getId()) + (trafficDemand * (double) function.getAttribute(FUNCTION_LOAD_RATIO) / server.getCapacity()));
   }

   private void addSyncTraffic(Path path, double trafficDemand, int s, int v, int p) {
      for (Edge pathLink : path.getEdgePath())
         heu.uL.put(pathLink.getId(), heu.uL.get(pathLink.getId()) + (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)));
      heu.hSVP[s][v][p] = true;
   }

}
