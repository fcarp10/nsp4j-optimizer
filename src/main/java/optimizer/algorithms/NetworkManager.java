package optimizer.algorithms;

import static optimizer.Definitions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graphstream.graph.Edge;

import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import manager.Parameters;
import manager.elements.Function;
import manager.elements.Server;
import manager.elements.Service;
import manager.elements.TrafficFlow;
import optimizer.results.Auxiliary;

public class NetworkManager {

   private static final Logger log = LoggerFactory.getLogger(NetworkManager.class);

   protected Parameters pm;
   protected VariablesAlg vars;

   public NetworkManager(Parameters pm, VariablesAlg variablesAlg) {
      this.pm = pm;
      this.vars = variablesAlg;
   }

   public void addDemandToFunctionsToSpecificServers(int s, int d, List<Integer> specificServers) {
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
         int xChosen = specificServers.get(v);
         addDemandToFunctionToServer(s, xChosen, v, d);
      }
   }

   public void removeDemandToFunctionToServer(int s, int x, int v, int d) {
      Server server = pm.getServers().get(x);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
            * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_LOAD_RATIO);
      vars.fXSVD[x][s][v][d] = false;
      vars.uX.put(server.getId(), vars.uX.get(server.getId()) - (trafficDemand / server.getCapacity()));
   }

   public void removeDemandFromAllFunctionsToServer(int s, int d) {
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
         for (int x = 0; x < pm.getServers().size(); x++) {
            if (vars.fXSVD[x][s][v][d]) {
               removeDemandToFunctionToServer(s, x, v, d);
               break;
            }
         }
   }

   public void addDemandToFunctionToServer(int s, int x, int v, int d) {
      Server server = pm.getServers().get(x);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
            * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute(FUNCTION_LOAD_RATIO);
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

   public List<Integer> getAvailablePaths(int s, int d) {
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

   public void removeUnusedFunctions(int s) {
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

   public List<List<Integer>> findServersForFunctionsInPath(int s, int d, int p) {
      List<List<Integer>> availableServersPerFunction = new ArrayList<>();
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
         int nStartLimit = 0;
         int nEndLimit = pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().size() - 1;
         int index;
         if (v > 0)
            if ((index = getNodeIndexFromFunction(s, d, p, v - 1)) != -1)
               nStartLimit = index;
         if (v < pm.getServices().get(s).getFunctions().size() - 1)
            if ((index = getNodeIndexFromFunction(s, d, p, v + 1)) != -1)
               nEndLimit = index;
         List<Integer> availableServers = getAvailableServers(s, p, d, v, nStartLimit, nEndLimit);
         availableServersPerFunction.add(availableServers);
      }
      // check if functions have at least one server
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
         if (availableServersPerFunction.get(v).isEmpty())
            return null;
      return availableServersPerFunction;
   }

   public int getNodeIndexFromFunction(int s, int d, int p, int v) {
      int nodeIndex = -1;
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      for (int n = 0; n < path.getNodePath().size(); n++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
               if (vars.fXSVD[x][s][v][d])
                  nodeIndex = n;
      return nodeIndex;
   }

   public Map<Integer, List<List<Integer>>> findAdmissiblePaths(List<Integer> availablePaths, int s, int d) {
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

   public void addSyncTraffic(int s) {
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int y = 0; y < pm.getServers().size(); y++) {
               if (pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent()))
                  continue;
               if (vars.fXSV[x][s][v] && vars.fXSV[y][s][v]) {
                  vars.gSVXY[s][v][x][y] = true;
                  // calculate the sync traffic
                  double syncTraffic = calculateSyncTraffic(s, v);
                  // search an available path for the sync traffic
                  boolean foundSyncPath = false;
                  for (int p = 0; p < pm.getPaths().size(); p++) {
                     Path path = pm.getPaths().get(p);
                     if (path.getNodePath().get(0).equals(pm.getServers().get(x).getParent()) & path.getNodePath()
                           .get(path.getNodePath().size() - 1).equals(pm.getServers().get(y).getParent()))
                        if (checkIfFreePathResources(path, syncTraffic)) {
                           assignSyncTraffic(s, v, p, syncTraffic);
                           foundSyncPath = true;
                           break;
                        }
                  }
                  if (!foundSyncPath)
                     Auxiliary.printLog(log, ERROR, "No available path found for sync traffic"); // blocking
               }
            }
   }

   public void removeSyncTraffic(int s) {
      Service service = pm.getServices().get(s);
      for (int v = 0; v < service.getFunctions().size(); v++)
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int y = 0; y < pm.getServers().size(); y++) {
               if (pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent()))
                  continue;
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
         vars.uL.put(pathLink.getId(),
               vars.uL.get(pathLink.getId()) + (syncTraffic / (int) pathLink.getAttribute(LINK_CAPACITY)));
      vars.hSVP[s][v][p] = true;
   }

   private void unAssignSyncTraffic(int s, int v, int p, double syncTraffic) {
      for (Edge pathLink : pm.getPaths().get(p).getEdgePath())
         vars.uL.put(pathLink.getId(),
               vars.uL.get(pathLink.getId()) - (syncTraffic / (int) pathLink.getAttribute(LINK_CAPACITY)));
      vars.hSVP[s][v][p] = false;
   }

   public int getNodePathIndexFromServer(int s, int p, int x) {
      int nodeIndex = -1;
      Service service = pm.getServices().get(s);
      Path path = service.getTrafficFlow().getPaths().get(p);
      for (int n = 0; n < path.getNodePath().size(); n++)
         if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
            nodeIndex = n;
      return nodeIndex;
   }

   public List<Integer> removePreviousServersFromNodeIndex(List<Integer> servers, int nodeIndex, int s, int p) {
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

   private List<Integer> getAvailableServers(int s, int p, int d, int v, int nStartLimit, int nEndLimit) {
      Service service = pm.getServices().get(s);
      Path path = service.getTrafficFlow().getPaths().get(p);
      List<Integer> availableServers = new ArrayList<>();
      for (int n = nStartLimit; n <= nEndLimit; n++)
         for (int x = 0; x < pm.getServers().size(); x++)
            if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n))) {
               if (vars.fXSVD[x][s][v][d])
                  availableServers.add(x);
               else if (vars.fXSV[x][s][v] || !vars.fXSVD[x][s][v][d])
                  if (checkIfFreeServerResources(s, x, v, d, service.getFunctions().size()))
                     availableServers.add(x);
               break;
            }
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

   public boolean checkIfFreeServerResources(int s, int x, int v, int d, int numOfFunctions) {
      Service service = pm.getServices().get(s);
      Server server = pm.getServers().get(x);
      double trafficDemand = service.getTrafficFlow().getDemands().get(d);
      Function function = service.getFunctions().get(v);
      int functionOverhead = (int) function.getAttribute(FUNCTION_OVERHEAD);
      double trafficLoad = trafficDemand * (double) function.getAttribute(FUNCTION_LOAD_RATIO);
      double resourcesToAdd = (trafficLoad + functionOverhead) * numOfFunctions;
      return vars.uX.get(server.getId()) + (resourcesToAdd / server.getCapacity()) <= 1.0;
   }

   public void removeDemandFromPath(int s, int p, int d) {
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
      for (Edge pathLink : path.getEdgePath())
         vars.uL.put(pathLink.getId(),
               vars.uL.get(pathLink.getId()) - (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)));
      vars.zSPD[s][p][d] = false;
   }

   public void addDemandToPath(int s, int p, int d) {
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
      double trafficDemand = pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
      for (Edge pathLink : path.getEdgePath())
         vars.uL.put(pathLink.getId(),
               vars.uL.get(pathLink.getId()) + (trafficDemand / (int) pathLink.getAttribute(LINK_CAPACITY)));
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

   public int getUsedServerForFunction(int s, int d, int v) {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (vars.fXSVD[x][s][v][d])
            return x;
      return -1;
   }
}
