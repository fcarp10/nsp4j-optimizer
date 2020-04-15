package optimizer.heuristic;


import manager.Parameters;
import manager.elements.Function;
import manager.elements.Server;
import manager.elements.Service;
import optimizer.results.Auxiliary;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static optimizer.Definitions.*;

public class Heuristic {

   private static final Logger log = LoggerFactory.getLogger(Heuristic.class);

   protected boolean[][][] zSPD;
   protected boolean[][][][] fXSVD;
   protected boolean[] fX;
   protected boolean[][][] fXSV;
   protected boolean[][] zSP;
   protected double[] lu;
   protected double[] xu;
   protected double[] oX;
   protected double[][] oSV;
   protected double[][][] qSDP;
   protected boolean[][][] hSVP;
   protected boolean[][][][] gSVXY;
   protected double objVal;
   protected Parameters pm;
   protected Map<String, Double> uL;
   protected Map<String, Double> uX;

   public Heuristic(Parameters pm, boolean[][][] initialPlacement) {
      this.pm = pm;
      fXSV = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
      if (initialPlacement != null)
         copyInitialPlacement(initialPlacement);
      zSPD = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
      fXSVD = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()][pm.getDemandsTrafficFlow()];
      hSVP = new boolean[pm.getServices().size()][pm.getServiceLength()][pm.getPaths().size()];
      gSVXY = new boolean[pm.getServices().size()][pm.getServiceLength()][pm.getServers().size()][pm.getServers().size()];
      uL = new HashMap<>();
      for (Edge link : pm.getLinks())
         uL.put(link.getId(), 0.0);
      uX = new HashMap<>();
      for (Server server : pm.getServers())
         uX.put(server.getId(), 0.0);
   }

   private void copyInitialPlacement(boolean[][][] initialPlacement) {

      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            System.arraycopy(initialPlacement[x][s], 0, fXSV[x][s], 0, pm.getServices().get(s).getFunctions().size());
   }

   protected void generateRestOfVariablesForResults(boolean[][][] initialPlacement, String objFunc) {

      lu = new double[pm.getLinks().size()];
      for (int l = 0; l < pm.getLinks().size(); l++)
         lu[l] = uL.get(pm.getLinks().get(l).getId());

      xu = new double[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++)
         xu[x] = uX.get(pm.getServers().get(x).getId());

      fX = new boolean[pm.getServers().size()];
      zSP = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()];
      oX = new double[pm.getServers().size()];
      oSV = new double[pm.getServices().size()][pm.getServiceLength()];
      qSDP = new double[pm.getServices().size()][pm.getDemandsTrafficFlow()][pm.getPathsTrafficFlow()];

      fXgenerate();
      zSPgenerate();
      oXgenerate();
      oSVgenerate();
      qSDPgenerate(initialPlacement);
      generateObjValue(objFunc);
      Auxiliary.printLog(log, INFO, "finished [" + objVal + "]");
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

   private void qSDPgenerate(boolean[][][] initialPlacement) {

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               if (zSPD[s][p][d]) {
                  double serviceDelay = 0;
                  Service service = pm.getServices().get(s);
                  Path path = service.getTrafficFlow().getPaths().get(p);

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
                              }

                  // propagation delay
                  double pathDelay = 0.0;
                  for (Edge link : path.getEdgePath())
                     pathDelay += (double) link.getAttribute(LINK_DELAY) * 1000; // in ms
                  serviceDelay += pathDelay;

                  // migration delay
                  if (initialPlacement != null) {
                     double downtime = (double) service.getAttribute(SERVICE_DOWNTIME);
                     double totalServiceDowntime = 0;
                     for (int x = 0; x < pm.getServers().size(); x++)
                        for (int v = 0; v < service.getFunctions().size(); v++)
                           if (initialPlacement[x][s][v] && !fXSV[x][s][v])
                              totalServiceDowntime += downtime;
                     serviceDelay += totalServiceDowntime; // in ms
                  }

                  double maxDelay = 0;
                  maxDelay += service.getMaxPropagationDelay();
                  for (int v = 0; v < service.getFunctions().size(); v++)
                     maxDelay += (double) service.getFunctions().get(v).getAttribute(FUNCTION_MAX_DELAY);

                  double profit = 0;
                  for (int v = 0; v < service.getFunctions().size(); v++)
                     profit += (double) service.getFunctions().get(v).getAttribute(FUNCTION_CHARGES);
                  double qosPenalty = (double) pm.getAux().get(QOS_PENALTY_RATIO) * profit; // in $/h

                  if (serviceDelay > maxDelay)
                     qSDP[s][d][p] = ((serviceDelay / maxDelay) - 1) * qosPenalty; // in $/h
               }
   }

   private void generateObjValue(String objFunc) {

      double opex = 0;
      double charges = 0;
      double penalties = 0;

      for (int x = 0; x < pm.getServers().size(); x++)
         opex += oX[x];
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            charges += oSV[s][v];
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               penalties += qSDP[s][d][p];

      switch (objFunc) {
         case OPEX_SERVERS_OBJ:
            objVal = opex;
            break;
         case FUNCTIONS_CHARGES_OBJ:
            objVal = charges;
            break;
         case QOS_PENALTIES_OBJ:
            objVal = penalties;
            break;
         default:
            objVal = opex + charges + penalties;
            break;
      }
   }

   public boolean[][] getzSP() {
      return zSP;
   }

   public boolean[][][] getzSPD() {
      return zSPD;
   }

   public boolean[][][][] getfXSVD() {
      return fXSVD;
   }

   public boolean[] getfX() {
      return fX;
   }

   public boolean[][][] getfXSV() {
      return fXSV;
   }

   public double[] getoX() {
      return oX;
   }

   public double[][] getoSV() {
      return oSV;
   }

   public double[][][] getqSDP() {
      return qSDP;
   }

   public boolean[][][] gethSVP() {
      return hSVP;
   }

   public boolean[][][][] getgSVXY() {
      return gSVXY;
   }

   public double[] getLu() {
      return lu;
   }

   public double[] getXu() {
      return xu;
   }

   public double getObjVal() {
      return objVal;
   }
}
