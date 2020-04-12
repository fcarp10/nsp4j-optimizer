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

   public Variables(manager.Parameters pm, GRBModel model, Scenario sc, GRBModel initialSolution) {
      try {
         zSP = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               String varName = Definitions.zSP + "[" + s + "][" + p + "]";
               if (initialSolution != null)
                  zSP[s][p] = initialSolution.getVarByName(varName);
               else
                  zSP[s][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
            }

         zSPD = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                  String varName = Definitions.zSPD + "[" + s + "][" + p + "][" + d + "]";
                  if (initialSolution != null)
                     zSPD[s][p][d] = initialSolution.getVarByName(varName);
                  else
                     zSPD[s][p][d] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
               }

         fX = new GRBVar[pm.getServers().size()];
         for (int x = 0; x < pm.getServers().size(); x++) {
            String varName = Definitions.fX + "[" + x + "]";
            if (initialSolution != null)
               fX[x] = initialSolution.getVarByName(varName);
            else
               fX[x] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
         }

         fXSV = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                  String varName = Definitions.fXSV + "[" + x + "][" + s + "][" + v + "]";
                  if (initialSolution != null)
                     fXSV[x][s][v] = initialSolution.getVarByName(varName);
                  else
                     fXSV[x][s][v] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
               }

         fXSVD = new GRBVar[pm.getServers().size()][pm.getServices().size()]
                 [pm.getServiceLength()][pm.getDemandsTrafficFlow()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                     String varName = Definitions.fXSVD + "[" + x + "][" + s + "][" + v + "][" + d + "]";
                     if (initialSolution != null)
                        fXSVD[x][s][v][d] = initialSolution.getVarByName(varName);
                     else
                        fXSVD[x][s][v][d] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
                  }

         uL = new GRBVar[pm.getLinks().size()];
         for (int l = 0; l < pm.getLinks().size(); l++) {
            String varName = Definitions.uL + "[" + l + "]";
            if (initialSolution != null)
               uL[l] = initialSolution.getVarByName(varName);
            else
               uL[l] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, varName);
         }

         uX = new GRBVar[pm.getServers().size()];
         for (int x = 0; x < pm.getServers().size(); x++) {
            String varName = Definitions.uX + "[" + x + "]";
            if (initialSolution != null)
               uX[x] = initialSolution.getVarByName(varName);
            else
               uX[x] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, varName);
         }


         ///////////// additional variables //////////////

         // if model is dimensioning number of servers
         if (sc.getObjFunc().equals(SERVER_DIMENSIONING)) {
            xN = new GRBVar[pm.getNodes().size()];
            for (int n = 0; n < pm.getNodes().size(); n++) {
               String varName = Definitions.xN + "[" + n + "]";
               if (initialSolution != null)
                  xN[n] = initialSolution.getVarByName(varName);
               else
                  xN[n] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, varName);
            }
         }

         // if model is optimizing with utilization costs
         if (sc.getObjFunc().equals(NUM_SERVERS_UTIL_COSTS_OBJ) || sc.getObjFunc().equals(UTIL_COSTS_OBJ)
                 || sc.getObjFunc().equals(UTIL_COSTS_MAX_UTIL_OBJ)) {
            kL = new GRBVar[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++) {
               String varName = Definitions.kL + "[" + l + "]";
               if (initialSolution != null)
                  kL[l] = initialSolution.getVarByName(varName);
               else
                  kL[l] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
            }

            kX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++) {
               String varName = Definitions.kX + "[" + x + "]";
               if (initialSolution != null)
                  kX[x] = initialSolution.getVarByName(varName);
               else
                  kX[x] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
            }
         }

         // if model is optimizing max utilization
         if (sc.getObjFunc().equals(UTIL_COSTS_MAX_UTIL_OBJ)) {
            String varName = Definitions.uMax;
            if (initialSolution != null)
               uMax = initialSolution.getVarByName(varName);
            else
               uMax = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, varName);
         }

         // if model is optimizing with monetary costs
         if (sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
                 || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {

            oX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++) {
               String varName = Definitions.oX + "[" + x + "]";
               if (initialSolution != null)
                  oX[x] = initialSolution.getVarByName(varName);
               else
                  oX[x] = model.addVar(0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
            }

            oSV = new GRBVar[pm.getServices().size()][pm.getServiceLength()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                  String varName = Definitions.oSV + "[" + s + "][" + v + "]";
                  if (initialSolution != null)
                     oSV[s][v] = initialSolution.getVarByName(varName);
                  else
                     oSV[s][v] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
               }

            qSDP = new GRBVar[pm.getServices().size()][pm.getDemandsTrafficFlow()][pm.getPathsTrafficFlow()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
                     String varName = Definitions.qSDP + "[" + s + "][" + d + "][" + p + "]";
                     if (initialSolution != null)
                        qSDP[s][d][p] = initialSolution.getVarByName(varName);
                     else
                        qSDP[s][d][p] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
                  }

            ySDP = new GRBVar[pm.getServices().size()][pm.getDemandsTrafficFlow()][pm.getPathsTrafficFlow()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
                     String varName = Definitions.ySDP + "[" + s + "][" + d + "][" + p + "]";
                     if (initialSolution != null)
                        ySDP[s][d][p] = initialSolution.getVarByName(varName);
                     else
                        ySDP[s][d][p] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
                  }
         }

         // if model is considering synchronization traffic
         if (sc.getConstraints().get(SYNC_TRAFFIC)) {

            gSVXY = new GRBVar[pm.getServices().size()][pm.getServiceLength()]
                    [pm.getServers().size()][pm.getServers().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int x = 0; x < pm.getServers().size(); x++)
                     for (int y = 0; y < pm.getServers().size(); y++)
                        if (!pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent())) {
                           String varName = Definitions.gSVXY + "[" + s + "][" + v + "][" + x + "][" + y + "]";
                           if (initialSolution != null)
                              gSVXY[s][v][x][y] = initialSolution.getVarByName(varName);
                           else
                              gSVXY[s][v][x][y] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
                        }

            hSVP = new GRBVar[pm.getServices().size()][pm.getServiceLength()][pm.getPaths().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int p = 0; p < pm.getPaths().size(); p++) {
                     String varName = Definitions.hSVP + "[" + s + "][" + v + "][" + p + "]";
                     if (initialSolution != null)
                        hSVP[s][v][p] = initialSolution.getVarByName(varName);
                     else
                        hSVP[s][v][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
                  }
         }

         // if model is considering delay constraints
         if (sc.getConstraints().get(MAX_SERV_DELAY) || sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
                 || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {

            dSVXD = new GRBVar[pm.getServices().size()][pm.getServiceLength()][pm.getServers().size()][pm.getDemandsTrafficFlow()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int x = 0; x < pm.getServers().size(); x++)
                     for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                        String varName = Definitions.dSVXD + "[" + s + "][" + v + "][" + x + "][" + d + "]";
                        if (initialSolution != null)
                           dSVXD[s][v][x][d] = initialSolution.getVarByName(varName);
                        else
                           dSVXD[s][v][x][d] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
                     }
         }

         model.update();
      } catch (Exception ignored) {
      }
   }

}
