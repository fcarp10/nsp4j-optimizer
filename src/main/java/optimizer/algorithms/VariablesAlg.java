package optimizer.algorithms;

import static optimizer.Definitions.FUNCTIONS_CHARGES_OBJ;
import static optimizer.Definitions.FUNCTION_CHARGES;
import static optimizer.Definitions.FUNCTION_LOAD_RATIO;
import static optimizer.Definitions.FUNCTION_MAX_CAP_SERVER;
import static optimizer.Definitions.FUNCTION_MAX_DELAY;
import static optimizer.Definitions.FUNCTION_MIN_PROCESS_DELAY;
import static optimizer.Definitions.FUNCTION_PROCESS_DELAY;
import static optimizer.Definitions.FUNCTION_PROCESS_TRAFFIC_DELAY;
import static optimizer.Definitions.LINK_DELAY;
import static optimizer.Definitions.NODE_CLOUD;
import static optimizer.Definitions.OPEX_SERVERS_OBJ;
import static optimizer.Definitions.QOS_PENALTIES_OBJ;
import static optimizer.Definitions.QOS_PENALTY_RATIO;
import static optimizer.Definitions.SERVER_IDLE_ENERGY_COST;
import static optimizer.Definitions.SERVER_UTIL_ENERGY_COST;
import static optimizer.Definitions.SERVICE_DOWNTIME;

import java.util.HashMap;
import java.util.Map;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Path;

import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Server;
import manager.elements.Service;
import optimizer.results.Auxiliary;

public class VariablesAlg {

   public boolean[][][] zSPD;
   public boolean[][][][] fXSVD;
   public boolean[] fX;
   public boolean[][][] fXSV;
   public boolean[][] zSP;
   public double[] lu;
   public double[] xu;
   public double[] oX;
   public double[][] oSV;
   public double[][][] qSDP;
   public boolean[][][] hSVP;
   public boolean[][][][] gSVXY;
   public double objVal;
   public Parameters pm;
   public Map<String, Double> uL;
   public Map<String, Double> uX;
   public boolean[][] zSPinitial;
   public boolean[][][] zSPDinitial;
   public boolean[][][] fXSVinitial;
   public boolean[][][][] fXSVDinitial;
   private String objFunc;

   public VariablesAlg(Parameters pm, GRBModel initialPlacementModel) {
      this.pm = pm;
      zSP = Auxiliary.zSPvarsFromInitialModel(pm, initialPlacementModel);
      zSPD = Auxiliary.zSPDvarsFromInitialModel(pm, initialPlacementModel);
      fXSV = Auxiliary.fXSVvarsFromInitialModel(pm, initialPlacementModel);
      fXSVD = Auxiliary.fXSVDvarsFromInitialModel(pm, initialPlacementModel);
   }

   public VariablesAlg(Parameters pm, VariablesAlg initialPlacementVars) {
      this.pm = pm;
      zSP = Auxiliary.zSPvarsFromInitialModel(pm, initialPlacementVars);
      zSPD = Auxiliary.zSPDvarsFromInitialModel(pm, initialPlacementVars);
      fXSV = Auxiliary.fXSVvarsFromInitialModel(pm, initialPlacementVars);
      fXSVD = Auxiliary.fXSVDvarsFromInitialModel(pm, initialPlacementVars);
   }

   public VariablesAlg(Parameters pm, VariablesAlg initialPlacementVars, String objFunc) {
      this.pm = pm;
      this.objFunc = objFunc;
      zSP = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()];
      zSPD = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
      fXSV = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
      fXSVD = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()][pm
            .getDemandsTrafficFlow()];
      hSVP = new boolean[pm.getServices().size()][pm.getServiceLength()][pm.getPaths().size()];
      gSVXY = new boolean[pm.getServices().size()][pm.getServiceLength()][pm.getServers().size()][pm.getServers()
            .size()];
      uL = new HashMap<>();
      for (Edge link : pm.getLinks())
         uL.put(link.getId(), 0.0);
      uX = new HashMap<>();
      for (Server server : pm.getServers())
         uX.put(server.getId(), 0.0);
      zSPinitial = Auxiliary.zSPvarsFromInitialModel(pm, initialPlacementVars);
      zSPDinitial = Auxiliary.zSPDvarsFromInitialModel(pm, initialPlacementVars);
      fXSVinitial = Auxiliary.fXSVvarsFromInitialModel(pm, initialPlacementVars);
      fXSVDinitial = Auxiliary.fXSVDvarsFromInitialModel(pm, initialPlacementVars);
   }

   public void generateRestOfVariablesForResults() {
      lu = new double[pm.getLinks().size()];
      for (int l = 0; l < pm.getLinks().size(); l++)
         lu[l] = uL.get(pm.getLinks().get(l).getId());
      xu = new double[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++)
         xu[x] = uX.get(pm.getServers().get(x).getId());
      fX = new boolean[pm.getServers().size()];
      oX = new double[pm.getServers().size()];
      oSV = new double[pm.getServices().size()][pm.getServiceLength()];
      qSDP = new double[pm.getServices().size()][pm.getDemandsTrafficFlow()][pm.getPathsTrafficFlow()];
      fXgenerate();
      oXgenerate();
      oSVgenerate();
      qSDPgenerate();
      generateObjValue();
   }

   private void fXgenerate() {
      for (int x = 0; x < pm.getServers().size(); x++) {
         outerLoop: for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (fXSVD[x][s][v][d]) {
                     fX[x] = true;
                     break outerLoop;
                  }
      }
   }

   private void oXgenerate() {
      for (int x = 0; x < pm.getServers().size(); x++)
         if (pm.getServers().get(x).getParent().getAttribute(NODE_CLOUD) == null) {
            if (fX[x])
               oX[x] = (double) pm.getAux().get(SERVER_IDLE_ENERGY_COST)
                     + (uX.get(pm.getServers().get(x).getId()) * (double) pm.getAux().get(SERVER_UTIL_ENERGY_COST));
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

   private void qSDPgenerate() {

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               if (zSPD[s][p][d]) {
                  Service service = pm.getServices().get(s);
                  double serviceDelay = getCurrentServiceDelay(s, d, p);
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

   public double getCurrentServiceDelay(int s, int d, int p) {
      double serviceDelay = 0;
      Service service = pm.getServices().get(s);
      Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
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
                     double processingDelay = 0;
                     for (int d1 = 0; d1 < service.getTrafficFlow().getDemands().size(); d1++)
                        if (service.getTrafficFlow().getAux().get(d1))
                           if (fXSVD[x][s][v][d1])
                              processingDelay += ratio * service.getTrafficFlow().getDemands().get(d1);
                     processingDelay += (double) function.getAttribute(FUNCTION_MIN_PROCESS_DELAY);
                     processingDelay += (double) function.getAttribute(FUNCTION_PROCESS_DELAY)
                           * uX.get(pm.getServers().get(x).getId());
                     serviceDelay += processingDelay;
                  }
      // propagation delay
      double pathDelay = 0.0;
      for (Edge link : path.getEdgePath())
         pathDelay += (double) link.getAttribute(LINK_DELAY) * 1000; // in ms
      serviceDelay += pathDelay;
      // migration delay
      if (fXSVinitial != null) {
         double downtime = (double) service.getAttribute(SERVICE_DOWNTIME);
         double totalServiceDowntime = 0;
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int v = 0; v < service.getFunctions().size(); v++)
               if (fXSVinitial[x][s][v] && !fXSV[x][s][v])
                  totalServiceDowntime += downtime;
         serviceDelay += totalServiceDowntime; // in ms
      }
      return serviceDelay;
   }

   private void generateObjValue() {

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

   public double getObjVal() {
      return objVal;
   }
}
