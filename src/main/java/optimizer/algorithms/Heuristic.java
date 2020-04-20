package optimizer.algorithms;


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

public class Heuristic {

   private static final Logger log = LoggerFactory.getLogger(Heuristic.class);

   protected Parameters pm;
   protected VariablesAlg vars;
   protected Random rnd;
   private String objFunc;
   private String alg;

   public Heuristic(Parameters pm, VariablesAlg variablesAlg, String objFunc, String algorithm) {
      this.pm = pm;
      this.vars = variablesAlg;
      rnd = new Random();
      this.objFunc = objFunc;
      this.alg = algorithm;
   }

   public void allocateAllServices() {

      assignFunctionsToServersFromInitialPlacement(); // add overhead of functions from initial placement

      for (int s = 0; s < pm.getServices().size(); s++) { // for every service
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) { // for every traffic demand

            List<Integer> availablePaths = getAvailablePaths(s, d); // get paths with enough path link resources
            Map<Integer, List<List<Integer>>> pathsMappingServers = findAdmissiblePaths(availablePaths, s, d); // get paths with enough servers resources
            List<Integer> paths = new ArrayList<>(pathsMappingServers.keySet());

            int pChosen = choosePath(paths, pm.getServices().get(s).getTrafficFlow());
            List<List<Integer>> listAvailableServersPerFunction = pathsMappingServers.get(pChosen);

            List<Integer> chosenServers = chooseServersForFunctionAllocation(s, d, pChosen, listAvailableServersPerFunction);
            addTrafficDemandToFunctionsToSpecificServers(s, d, chosenServers);

            addTrafficDemandToPath(s, pChosen, d);
         }
         removeUnusedFunctions(s); // remove unused servers from initial placement
         addSyncTraffic(s); // add synchronization traffic
      }
   }

   public boolean checkPathForReallocation(int s, int d, int p) {
      List<List<Integer>> availableServersPerFunction = findServersForFunctionsInPath(s, d, p);
      return availableServersPerFunction != null;
   }

   public void reallocateSpecificDemand(int s, int d, int pOld, int pNew) {

      removeTrafficDemandFromFunctions(s, d);
      removeTrafficDemandFromPath(s, pOld, d);

      List<List<Integer>> availableServersPerFunction = findServersForFunctionsInPath(s, d, pNew);
      List<Integer> chosenServers = chooseServersForFunctionAllocation(s, d, pNew, availableServersPerFunction);
      addTrafficDemandToFunctionsToSpecificServers(s, d, chosenServers);
      addTrafficDemandToPath(s, pNew, d);

      removeUnusedFunctions(s);
      removeSyncTraffic(s);

      addSyncTraffic(s);
   }

   private List<Integer> getUsedServersByDemand(int s, int d) {
      List<Integer> listOfUsedServers = new ArrayList<>();
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (vars.fXSVD[x][s][v][d])
               listOfUsedServers.add(x);
      return listOfUsedServers;
   }

   private void removeTrafficDemandFromFunctions(int s, int d) {
      List<Integer> listOfOldUsedServers = getUsedServersByDemand(s, d);
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
         int x = listOfOldUsedServers.get(v);
         unAssignDemandToFunctionToServer(s, x, v, d);
      }
   }

   private List<Integer> chooseServersForFunctionAllocation(int s, int d, int pChosen, List<List<Integer>> listAvailableServersPerFunction) {
      List<Integer> specificServers = new ArrayList<>();
      int lastPathNodeUsed = 0;
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
         List<Integer> availableServers = listAvailableServersPerFunction.get(v);
         int xChosen = chooseServerForFunction(availableServers, lastPathNodeUsed, s, pChosen, v, d);
         specificServers.add(xChosen);
         lastPathNodeUsed = getNodePathIndexFromServer(s, pChosen, xChosen); // save last node path used for ordering
      }
      return specificServers;
   }

   private void addTrafficDemandToFunctionsToSpecificServers(int s, int d, List<Integer> specificServers) {
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
         int xChosen = specificServers.get(v);
         assignDemandToFunctionToServer(s, xChosen, v, d);
      }
   }

   public void unAssignDemandToFunctionToServer(int s, int x, int v, int d) {
      Server server = pm.getServers().get(x);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d) * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_LOAD_RATIO);
      vars.fXSVD[x][s][v][d] = false;
      vars.uX.put(server.getId(), vars.uX.get(server.getId()) - (trafficDemand / server.getCapacity()));
   }

   public void assignDemandToFunctionToServer(int s, int x, int v, int d) {
      Server server = pm.getServers().get(x);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d) * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_LOAD_RATIO);
      vars.fXSVD[x][s][v][d] = true;
      if (!vars.fXSV[x][s][v])
         assignFunctionToServer(s, x, v);
      vars.uX.put(server.getId(), vars.uX.get(server.getId()) + (trafficDemand / server.getCapacity()));
   }

   public void assignFunctionsToServersFromInitialPlacement() {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               if (vars.fXSV[x][s][v])
                  assignFunctionToServer(s, x, v);
   }

   private void assignFunctionToServer(int s, int x, int v) {
      Service service = pm.getServices().get(s);
      Server server = pm.getServers().get(x);
      Function function = service.getFunctions().get(v);
      vars.fXSV[x][s][v] = true;
      double functionOverhead = (int) function.getAttribute(FUNCTION_OVERHEAD);
      vars.uX.put(server.getId(), vars.uX.get(server.getId()) + (functionOverhead / server.getCapacity()));
   }


   private List<Integer> getAvailablePaths(int s, int d) {
      Service service = pm.getServices().get(s);
      TrafficFlow tf = service.getTrafficFlow();
      List<Integer> availablePaths = new ArrayList<>();
      int trafficDemand = tf.getDemands().get(d);
      for (int p = 0; p < tf.getPaths().size(); p++) // for every admissible path
         if (checkIfFreePathResources(tf.getPaths().get(p), trafficDemand)) // add paths with free path link resources
            availablePaths.add(p);
      if (availablePaths.isEmpty()) { // if no path found, block
         // TO-DO blocking !!
         Auxiliary.printLog(log, ERROR, "no available path found for [s][d] = [" + s + "][" + d + "]");
         System.exit(-1);
      }
      return availablePaths;
   }

   private Integer chooseFirstFitPath(List<Integer> paths, TrafficFlow tf) {
      boolean cloudPathsFirst = false;
      if (objFunc.equals(OPEX_SERVERS_OBJ)) // order set of admissible paths depending on the objective function
         cloudPathsFirst = true;
      paths = orderPaths(paths, cloudPathsFirst, tf);
      return paths.get(0); // and take the first path
   }

   private Integer choosePath(List<Integer> paths, TrafficFlow tf) {

      int chosenPath = -1;
      if (alg.equals(FIRST_FIT) || alg.equals(FFP_RFX) || alg.equals(DRL))
         chosenPath = chooseFirstFitPath(paths, tf);
      else if (alg.equals(RANDOM_FIT) || alg.equals(RFP_FFX))
         chosenPath = paths.get(rnd.nextInt(paths.size()));

      return chosenPath;
   }

   private Integer chooseFirstFitServerForFunction(List<Integer> availableServers, int lastPathNodeUsed, int s, int p, int v, int d) {
      int chosenServer = -1;
      boolean cloudServersFirst = false;
      if (objFunc.equals(OPEX_SERVERS_OBJ)) // order set of available servers depending on the objective function
         cloudServersFirst = true;
      availableServers = selectServers(availableServers, cloudServersFirst);
      availableServers = removePreviousServersFromNodeIndec(availableServers, lastPathNodeUsed, s, p);

      for (Integer xAvailable : availableServers)
         if (checkIfFreeServerResources(s, xAvailable, v, d, 1))
            chosenServer = xAvailable;
      return chosenServer;
   }

   private Integer chooseServerForFunction(List<Integer> availableServers, int lastPathNodeUsed, int s, int p, int v, int d) {
      int chosenServer = -1;

      if (alg.equals(FIRST_FIT) || alg.equals(RFP_FFX) || alg.equals(DRL))
         chosenServer = chooseFirstFitServerForFunction(availableServers, lastPathNodeUsed, s, p, v, d);
      else if (alg.equals(RANDOM_FIT) || alg.equals(FFP_RFX))
         chosenServer = availableServers.get(rnd.nextInt(availableServers.size()));

      if (chosenServer == -1) {
         Auxiliary.printLog(log, ERROR, "function could not be allocated [s][d][p][v] = [" + s + "][" + d + "][" + p + "][" + v + "]");
         System.exit(-1);
      }
      return chosenServer;
   }


   private void removeUnusedFunctions(int s) {

      for (int x = 0; x < pm.getServers().size(); x++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            boolean usedServer = false;
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               if (vars.fXSVD[x][s][v][d]) {
                  usedServer = true;
                  break;
               }
            if (!usedServer && vars.fXSV[x][s][v])
               removeFunctionFromServer(s, x, v);
         }
   }

   private List<List<Integer>> findServersForFunctionsInPath(int s, int d, int p) {

      List<List<Integer>> availableServersPerFunction = new ArrayList<>(); // find available servers for every function
      Path availablePath = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) { // for every function
         List<Integer> chosenServers = new ArrayList<>();
         List<Integer> alreadyUsedServers = getUsedServersForFunction(s, v, availablePath); // get list of server where function already exists for the path
         if (!alreadyUsedServers.isEmpty()) // if function already exist, with enough resources, add it
            for (int server : alreadyUsedServers)
               if (checkIfFreeServerResources(s, server, v, d, pm.getServices().get(s).getFunctions().size()))
                  chosenServers.add(server);
         int nInitial = -1;
         if (chosenServers.isEmpty())
            nInitial = 0; // if no already existing function, get all available servers in the path
         else  // if existing function, get only the next servers in the path
            for (int n = 0; n < availablePath.getNodePath().size(); n++) {
               int lastServer = chosenServers.get(chosenServers.size() - 1);
               if (pm.getServers().get(lastServer).getParent().equals(availablePath.getNodePath().get(n))) {
                  nInitial = n + 1;
                  break;
               }
            }
         List<Integer> availableServers = getAvailableServers(s, p, d, v, nInitial); // get rest of available servers
         chosenServers.addAll(availableServers);
         availableServersPerFunction.add(chosenServers);
      }

      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) // check if all functions have at least one available server
         if (availableServersPerFunction.get(v).isEmpty())
            return null;

      return availableServersPerFunction;
   }

   private Map<Integer, List<List<Integer>>> findAdmissiblePaths(List<Integer> availablePaths, int s, int d) {

      Map<Integer, List<List<Integer>>> admissiblePaths = new HashMap<>();
      for (Integer p : availablePaths) {
         List<List<Integer>> availableServersPerFunction = findServersForFunctionsInPath(s, d, p);
         if (availableServersPerFunction != null)// if there are servers, add path
            admissiblePaths.put(p, availableServersPerFunction);
      }

      if (admissiblePaths.isEmpty()) {
         // TO-DO blocking
         Auxiliary.printLog(log, ERROR, "no admissible path available for [s][d] = [" + s + "][" + d + "]");
         System.exit(-1);
      }
      return admissiblePaths;
   }

   private void addSyncTraffic(int s) {

      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int y = 0; y < pm.getServers().size(); y++) {
               if (pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent())) continue;
               if (vars.fXSV[x][s][v] && vars.fXSV[y][s][v]) {

                  vars.gSVXY[s][v][x][y] = true;
                  // calculate the sync traffic
                  double syncTraffic = calculateSyncTraffic(s, v);

                  // search an available path for the sync traffic
                  boolean foundSyncPath = false;
                  for (int p = 0; p < pm.getPaths().size(); p++) {
                     Path path = pm.getPaths().get(p);
                     if (path.getNodePath().get(0).equals(pm.getServers().get(x).getParent()) & path.getNodePath().get(path.getNodePath().size() - 1).equals(pm.getServers().get(y).getParent()))
                        if (checkIfFreePathResources(path, syncTraffic)) {
                           assignSyncTraffic(s, v, p, syncTraffic);
                           foundSyncPath = true;
                           break;
                        }
                  }
                  if (!foundSyncPath)
                     Auxiliary.printLog(log, ERROR, "No available path found for sync traffic"); // TO-DO implement blocking
               }
            }
   }

   private void removeSyncTraffic(int s) {
      Service service = pm.getServices().get(s);
      for (int v = 0; v < service.getFunctions().size(); v++)
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int y = 0; y < pm.getServers().size(); y++) {
               if (pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent())) continue;
               if (vars.gSVXY[s][v][x][y]) {
                  // calculate the sync traffic
                  double syncTraffic = calculateSyncTraffic(s, v);
                  for (int p = 0; p < pm.getPaths().size(); p++)
                     if (vars.hSVP[s][v][p]) {
                        unAssignSyncTraffic(s, v, p, syncTraffic);
                        break;
                     }
               }
            }
   }

   private double calculateSyncTraffic(int s, int v) {
      Service service = pm.getServices().get(s);
      double syncTraffic = 0;
      for (int d = 0; d < service.getTrafficFlow().getDemands().size(); d++)
         syncTraffic += service.getTrafficFlow().getDemands().get(d);
      return syncTraffic * (double) service.getFunctions().get(v).getAttribute(FUNCTION_SYNC_LOAD_RATIO);
   }

   private void assignSyncTraffic(int s, int v, int p, double syncTraffic) {
      for (Edge pathLink : pm.getPaths().get(p).getEdgePath())
         vars.uL.put(pathLink.getId(), vars.uL.get(pathLink.getId()) + (syncTraffic / (int) pathLink.getAttribute(LINK_CAPACITY)));
      vars.hSVP[s][v][p] = true;
   }

   private void unAssignSyncTraffic(int s, int v, int p, double syncTraffic) {
      for (Edge pathLink : pm.getPaths().get(p).getEdgePath())
         vars.uL.put(pathLink.getId(), vars.uL.get(pathLink.getId()) - (syncTraffic / (int) pathLink.getAttribute(LINK_CAPACITY)));
      vars.hSVP[s][v][p] = false;
   }

   private int getNodePathIndexFromServer(int s, int p, int x) {
      int nodeIndex = -1;
      Service service = pm.getServices().get(s);
      Path path = service.getTrafficFlow().getPaths().get(p);
      for (int n = 0; n < path.getNodePath().size(); n++)
         if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
            nodeIndex = n;
      return nodeIndex;
   }

   private boolean checkIfPathTraversesCloudNode(int s, int p) {
      Service service = pm.getServices().get(s);
      Path path = service.getTrafficFlow().getPaths().get(p);
      for (int n = 0; n < path.getNodePath().size(); n++)
         if (path.getNodePath().get(n).hasAttribute(NODE_CLOUD))
            return true;
      return false;
   }

   private List<Integer> removePreviousServersFromNodeIndec(List<Integer> servers, int nodeIndex, int s, int p) {

      Service service = pm.getServices().get(s);
      Path path = service.getTrafficFlow().getPaths().get(p);

      int serverIndex = 0;
      for (int x = 0; x < servers.size(); x++)
         if (pm.getServers().get(servers.get(x)).getParent().equals(path.getNodePath().get(nodeIndex)))
            serverIndex = x;

      if (serverIndex > 0)
         return servers.subList(serverIndex, servers.size());

      return servers;
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

   private List<Integer> getUsedServersForFunction(int s, int v, Path path) {
      List<Integer> foundServers = new ArrayList<>();
      for (int n = 0; n < path.getNodePath().size(); n++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n))) {
               if (vars.fXSV[x][s][v])
                  foundServers.add(x);
               break;
            }
      return foundServers;
   }

   private List<Integer> getAvailableServers(int s, int p, int d, int v, int nInitial) {
      Service service = pm.getServices().get(s);
      Path path = service.getTrafficFlow().getPaths().get(p);
      List<Integer> availableServers = new ArrayList<>();
      for (int n = nInitial; n < path.getNodePath().size(); n++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
               if (checkIfFreeServerResources(s, x, v, d, service.getFunctions().size()))
                  availableServers.add(x);
      return availableServers;
   }

   private boolean checkIfFreePathResources(Path path, double trafficDemand) {
      boolean isAvailable = true;
      for (Edge pathLink : path.getEdgePath())
         if (vars.uL.get(pathLink.getId()) + (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)) >= 1.0) {
            isAvailable = false;
            break;
         }
      return isAvailable;
   }

   private boolean checkIfFreeServerResources(int s, int x, int v, int d, int numOfFunctions) {
      Service service = pm.getServices().get(s);
      Server server = pm.getServers().get(x);
      double trafficDemand = service.getTrafficFlow().getDemands().get(d);
      Function function = service.getFunctions().get(v);
      int functionOverhead = (int) function.getAttribute(FUNCTION_OVERHEAD);
      double trafficLoad = trafficDemand * (double) function.getAttribute(FUNCTION_LOAD_RATIO);
      double resourcesToAdd = (trafficLoad + functionOverhead) * numOfFunctions;
      return vars.uX.get(server.getId()) + (resourcesToAdd / server.getCapacity()) <= 1.0;
   }

   private void removeTrafficDemandFromPath(int s, int p, int d) {
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
      for (Edge pathLink : path.getEdgePath())
         vars.uL.put(pathLink.getId(), vars.uL.get(pathLink.getId()) - (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)));
      vars.zSPD[s][p][d] = false;
   }

   private void addTrafficDemandToPath(int s, int p, int d) {
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
      for (Edge pathLink : path.getEdgePath())
         vars.uL.put(pathLink.getId(), vars.uL.get(pathLink.getId()) + (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)));
      vars.zSPD[s][p][d] = true;
   }

   private void removeFunctionFromServer(int s, int x, int v) {
      Service service = pm.getServices().get(s);
      Server server = pm.getServers().get(x);
      Function function = service.getFunctions().get(v);
      vars.fXSV[x][s][v] = false;
      double functionOverhead = (int) function.getAttribute(FUNCTION_OVERHEAD);
      vars.uX.put(server.getId(), vars.uX.get(server.getId()) - (functionOverhead / server.getCapacity()));
   }
}
