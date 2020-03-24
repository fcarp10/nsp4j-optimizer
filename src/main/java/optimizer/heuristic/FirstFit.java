package optimizer.heuristic;


import manager.Parameters;
import manager.elements.Server;
import manager.elements.TrafficFlow;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static optimizer.Parameters.LINK_CAPACITY;

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

      for (int s = 0; s < pm.getServices().size(); s++) {
         TrafficFlow tf = pm.getServices().get(s).getTrafficFlow();
         for (int d = 0; d < tf.getDemands().size(); d++) {
            boolean pathFound = false;
            for (int p = 0; p < tf.getPaths().size(); p++)
               if (checkIfFreePathResources(tf.getPaths().get(p), tf.getDemands().get(d))) {
                  assignTrafficDemand(tf.getPaths().get(p), tf.getDemands().get(d));
                  zSPD[s][p][d] = true;
                  pathFound = true;
                  break;
               }
            if (!pathFound) {
               // TO-DO
               log.error("Path not found");
               System.exit(-1);
            }
         }
      }
   }

   private boolean checkIfFreePathResources(Path p, double trafficDemand) {
      boolean isAvailable = true;
      for (Edge pathLink : p.getEdgePath())
         if (uL.get(pathLink.getId()) + (trafficDemand / (double) pathLink.getAttribute(LINK_CAPACITY)) >= 1.0) {
            isAvailable = false;
            break;
         }
      return isAvailable;
   }

   private void assignTrafficDemand(Path p, double trafficDemand) {
      for (Edge pathLink : p.getEdgePath())
         uL.put(pathLink.getId(), uL.get(pathLink.getId()) + (trafficDemand / (double) pathLink.getAttribute(LINK_CAPACITY)));
   }
}
