package optimizer.lp;

import gurobi.GRB;
import gurobi.GRBModel;
import gurobi.GRBVar;
import optimizer.Definitions;
import optimizer.gui.Scenario;

import static optimizer.Definitions.*;

public class Variables {

   // general variables
   public GRBVar[][] zSP; // binary, routing per path
   public GRBVar[][][] zSPD; // binary, routing per demand
   public GRBVar[] fX; // binary, true if server is used
   public GRBVar[][][] fXSV; // binary, placement per server
   public GRBVar[][][][] fXSVD; // binary, placement per demand
   public GRBVar[] uL; // link utilization
   public GRBVar[] uX; // server utilization

   // model specific variables
   public GRBVar[] xN; // integer, num servers per node
   public GRBVar[] kL; // link cost utilization
   public GRBVar[] kX; // server cost utilization
   public GRBVar uMax; // max utilization
   public GRBVar[] oX; // operational server cost
   public GRBVar[][] oSV; // operational function cost
   public GRBVar[][][] qSDP; // qos penalty cost
   public GRBVar[][][] ySDP; // aux variable for delay qos penalty cost

   // service delay variables
   public GRBVar[][][][] dSVXD; // continuous, aux variable for processing delay

   // synchronization traffic variables
   public GRBVar[][][][] gSVXY; //binary, aux synchronization traffic
   public GRBVar[][][] hSVP; // binary, traffic synchronization

   public Variables(manager.Parameters pm, GRBModel model) {
      try {
         zSP = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               zSP[s][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                       , Definitions.zSP + "[" + s + "][" + p + "]");

         zSPD = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  zSPD[s][p][d] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                          , Definitions.zSPD + "[" + s + "][" + p + "][" + d + "]");

         fX = new GRBVar[pm.getServers().size()];
         for (int x = 0; x < pm.getServers().size(); x++)
            fX[x] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                    , Definitions.fX + "[" + x + "]");

         fXSV = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  fXSV[x][s][v] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                          , Definitions.fXSV + "[" + x + "][" + s + "][" + v + "]");

         fXSVD = new GRBVar[pm.getServers().size()][pm.getServices().size()]
                 [pm.getServiceLength()][pm.getDemandsTrafficFlow()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     fXSVD[x][s][v][d] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                             , Definitions.fXSVD + "[" + x + "][" + s + "][" + v + "][" + d + "]");

         uL = new GRBVar[pm.getLinks().size()];
         for (int l = 0; l < pm.getLinks().size(); l++)
            uL[l] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS
                    , Definitions.uL + "[" + l + "]");

         uX = new GRBVar[pm.getServers().size()];
         for (int x = 0; x < pm.getServers().size(); x++)
            uX[x] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS
                    , Definitions.uX + "[" + x + "]");

         model.update();
      } catch (Exception ignored) {
      }
   }

   public void initializeAdditionalVariables(manager.Parameters pm, GRBModel model, Scenario sc) {
      try {

         // if model is dimensioning number of servers
         if (sc.getObjFunc().equals(SERVER_DIMENSIONING)) {
            xN = new GRBVar[pm.getNodes().size()];
            for (int n = 0; n < pm.getNodes().size(); n++)
               this.xN[n] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER
                       , Definitions.xN + "[" + n + "]");
         }

         // if model is optimizing with utilization costs
         if (sc.getObjFunc().equals(NUM_SERVERS_UTIL_COSTS_OBJ) || sc.getObjFunc().equals(UTIL_COSTS_OBJ)
                 || sc.getObjFunc().equals(UTIL_COSTS_MAX_UTIL_OBJ)) {
            kL = new GRBVar[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
               kL[l] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                       , Definitions.kL + "[" + l + "]");

            kX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
               kX[x] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                       , Definitions.kX + "[" + x + "]");
         }

         // if model is optimizing max utilization
         if (sc.getObjFunc().equals(UTIL_COSTS_MAX_UTIL_OBJ)) {
            uMax = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS
                    , Definitions.uMax);
         }

         // if model is optimizing with monetary costs
         if (sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
                 || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {
            oX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
               oX[x] = model.addVar(0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                       , Definitions.oX + "[" + x + "]");
            oSV = new GRBVar[pm.getServices().size()][pm.getServiceLength()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  oSV[s][v] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                          , Definitions.oSV + "[" + s + "][" + v + "]");
            qSDP = new GRBVar[pm.getServices().size()][pm.getDemandsTrafficFlow()][pm.getPathsTrafficFlow()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                     qSDP[s][d][p] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                             , Definitions.qSDP + "[" + s + "][" + d + "][" + p + "]");
            ySDP = new GRBVar[pm.getServices().size()][pm.getDemandsTrafficFlow()][pm.getPathsTrafficFlow()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                     ySDP[s][d][p] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                             , Definitions.ySDP + "[" + s + "][" + d + "][" + p + "]");
         }

         // if model is considering synchronization traffic
         if (sc.getConstraints().get(SYNC_TRAFFIC)) {
            gSVXY = new GRBVar[pm.getServices().size()][pm.getServiceLength()]
                    [pm.getServers().size()][pm.getServers().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int x = 0; x < pm.getServers().size(); x++)
                     for (int y = 0; y < pm.getServers().size(); y++)
                        if (!pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent()))
                           gSVXY[s][v][x][y] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                                   , Definitions.gSVXY + "[" + s + "][" + v + "][" + x + "][" + y + "]");

            hSVP = new GRBVar[pm.getServices().size()][pm.getServiceLength()][pm.getPaths().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int p = 0; p < pm.getPaths().size(); p++)
                     hSVP[s][v][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                             , Definitions.hSVP + "[" + s + "][" + v + "][" + p + "]");
         }

         // if model is considering delay constraints
         if (sc.getConstraints().get(MAX_SERV_DELAY) || sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
                 || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {

            dSVXD = new GRBVar[pm.getServices().size()][pm.getServiceLength()][pm.getServers().size()][pm.getDemandsTrafficFlow()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int x = 0; x < pm.getServers().size(); x++)
                     for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                        dSVXD[s][v][x][d] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                                , Definitions.dSVXD + "[" + s + "][" + v + "][" + x + "][" + d + "]");


         }
         model.update();
      } catch (Exception ignored) {
      }
   }
}
