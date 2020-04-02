package optimizer.heuristic;


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

import static optimizer.Definitions.FUNCTION_LOAD_RATIO;
import static optimizer.Definitions.LINK_CAPACITY;

public class FirstFit {

   private static final Logger log = LoggerFactory.getLogger(FirstFit.class);

   public Boolean[][][] zSPD; // binary, routing per demand
   public Boolean[][][][] fXSVD; // binary, placement per demand
   public Map<String, Double> uL; // link utilization
   public Map<String, Double> uX; // server utilization
   private Parameters pm;

   public FirstFit(Parameters pm) {
      this.pm = pm;
      zSPD = new Boolean[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
      fXSVD = new Boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()][pm.getDemandsTrafficFlow()];
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
               if (checkIfFreePathResources(tf.getPaths().get(p), tf.getDemands().get(d))) // add paths with free path link resources
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

   private boolean checkIfFreePathResources(Path path, double trafficDemand) {
      boolean isAvailable = true;
      for (Edge pathLink : path.getEdgePath())
         if (uL.get(pathLink.getId()) + (trafficDemand / (double) pathLink.getAttribute(LINK_CAPACITY)) >= 1.0) {
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
      TrafficFlow trafficFlow = pm.getServices().get(s).getTrafficFlow();
      Path path = trafficFlow.getPaths().get(p);
      for (Edge pathLink : path.getEdgePath())
         uL.put(pathLink.getId(), uL.get(pathLink.getId()) + (trafficFlow.getDemands().get(d) / (double) pathLink.getAttribute(LINK_CAPACITY)));
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
}
