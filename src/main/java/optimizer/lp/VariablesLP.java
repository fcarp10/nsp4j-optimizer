package optimizer.lp;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;
import optimizer.Parameters;
import optimizer.Definitions;
import optimizer.gui.Scenario;

import static optimizer.Definitions.*;

public class VariablesLP {

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
   public GRBVar[][][][] gSVXY; // binary, aux synchronization traffic
   public GRBVar[][][] hSVP; // binary, traffic synchronization

   public VariablesLP(Parameters pm, GRBModel model, Scenario sc, GRBModel initialSolution) {
      try {

         zSP_init(pm, model, initialSolution);
         zSPD_init(pm, model, initialSolution);
         fX_init(pm, model, initialSolution);
         fXSV_init(pm, model, initialSolution);
         fXSVD_init(pm, model, initialSolution);
         uL_init(pm, model, initialSolution);
         uX_init(pm, model, initialSolution);

         /************ additional variables **********/
         // model dimension number of servers
         if (sc.getObjFunc().equals(DIMEN))
            xN_init(pm, model, initialSolution);
         // model optimizes utilization costs
         if (sc.getObjFunc().equals(NUM_SERVERS_AND_UTIL_COSTS) || sc.getObjFunc().equals(UTIL_COSTS)
               || sc.getObjFunc().equals(UTIL_COSTS_AND_MAX_UTIL)) {
            kL_init(pm, model, initialSolution);
            kX_init(pm, model, initialSolution);
         }
         // model optimizes max utilization
         if (sc.getObjFunc().equals(UTIL_COSTS_AND_MAX_UTIL))
            uMax_init(pm, model, initialSolution);
         // model optimizes opex costs
         if (sc.getObjFunc().equals(OPEX_SERVERS)) {
            oX_init(pm, model, initialSolution);
            dSVXD_init(pm, model, initialSolution);
         }
         // model optimizes charges
         if (sc.getObjFunc().equals(FUNCTIONS_CHARGES)) {
            oSV_init(pm, model, initialSolution);
            dSVXD_init(pm, model, initialSolution);
         }
         // model optimizes qos penalties
         if (sc.getObjFunc().equals(QOS_PENALTIES)) {
            qSDP_init(pm, model, initialSolution);
            ySDP_init(pm, model, initialSolution);
            dSVXD_init(pm, model, initialSolution);
         }
         // model optimizes all monetary costs
         if (sc.getObjFunc().equals(ALL_MONETARY_COSTS)) {
            oX_init(pm, model, initialSolution);
            oSV_init(pm, model, initialSolution);
            qSDP_init(pm, model, initialSolution);
            ySDP_init(pm, model, initialSolution);
            dSVXD_init(pm, model, initialSolution);
         }
         // model optimizes migrations or replications
         if (sc.getObjFunc().equals(MGR) || sc.getObjFunc().equals(REP) || sc.getObjFunc().equals(MGR_REP)) {
            // qSDP_init(pm, model, initialSolution);
            // ySDP_init(pm, model, initialSolution);
            // dSVXD_init(pm, model, initialSolution);
         }
         // model considers synchronization traffic
         if (sc.getConstraints().get(SYNC_TRAFFIC)) {
            gSVXY_init(pm, model, initialSolution);
            hSVP_init(pm, model, initialSolution);
         }
         // model constrainst max service delay
         if (sc.getConstraints().get(MAX_SERV_DELAY))
            dSVXD_init(pm, model, initialSolution);

         model.update();
      } catch (Exception ignored) {
      }
   }

   private void zSP_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      zSP = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()];
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
            String varName = Definitions.zSP + "[" + s + "][" + p + "]";
            if (initialSolution != null)
               zSP[s][p] = initialSolution.getVarByName(varName);
            else
               zSP[s][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
         }
   }

   private void zSPD_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
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
   }

   private void fX_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      fX = new GRBVar[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++) {
         String varName = Definitions.fX + "[" + x + "]";
         if (initialSolution != null)
            fX[x] = initialSolution.getVarByName(varName);
         else
            fX[x] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName);
      }
   }

   private void fXSV_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
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
   }

   private void fXSVD_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      fXSVD = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()][pm
            .getDemandsTrafficFlow()];
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
   }

   private void uL_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      uL = new GRBVar[pm.getLinks().size()];
      for (int l = 0; l < pm.getLinks().size(); l++) {
         String varName = Definitions.uL + "[" + l + "]";
         if (initialSolution != null)
            uL[l] = initialSolution.getVarByName(varName);
         else
            uL[l] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, varName);
      }
   }

   private void uX_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      uX = new GRBVar[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++) {
         String varName = Definitions.uX + "[" + x + "]";
         if (initialSolution != null)
            uX[x] = initialSolution.getVarByName(varName);
         else
            uX[x] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, varName);
      }
   }

   private void xN_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      xN = new GRBVar[pm.getNodes().size()];
      for (int n = 0; n < pm.getNodes().size(); n++) {
         String varName = Definitions.xN + "[" + n + "]";
         if (initialSolution != null)
            xN[n] = initialSolution.getVarByName(varName);
         else
            xN[n] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER, varName);
      }
   }

   private void kL_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      kL = new GRBVar[pm.getLinks().size()];
      for (int l = 0; l < pm.getLinks().size(); l++) {
         String varName = Definitions.kL + "[" + l + "]";
         if (initialSolution != null)
            kL[l] = initialSolution.getVarByName(varName);
         else
            kL[l] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
      }
   }

   private void kX_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      kX = new GRBVar[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++) {
         String varName = Definitions.kX + "[" + x + "]";
         if (initialSolution != null)
            kX[x] = initialSolution.getVarByName(varName);
         else
            kX[x] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
      }
   }

   private void uMax_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      String varName = Definitions.uMax;
      if (initialSolution != null)
         uMax = initialSolution.getVarByName(varName);
      else
         uMax = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, varName);
   }

   private void oX_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      oX = new GRBVar[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++) {
         String varName = Definitions.oX + "[" + x + "]";
         if (initialSolution != null)
            oX[x] = initialSolution.getVarByName(varName);
         else
            oX[x] = model.addVar(0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
      }
   }

   private void oSV_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      oSV = new GRBVar[pm.getServices().size()][pm.getServiceLength()];
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            String varName = Definitions.oSV + "[" + s + "][" + v + "]";
            if (initialSolution != null)
               oSV[s][v] = initialSolution.getVarByName(varName);
            else
               oSV[s][v] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName);
         }
   }

   private void qSDP_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
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
   }

   private void ySDP_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
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

   private void gSVXY_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      gSVXY = new GRBVar[pm.getServices().size()][pm.getServiceLength()][pm.getServers().size()][pm.getServers()
            .size()];
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
   }

   private void hSVP_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
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

   private void dSVXD_init(Parameters pm, GRBModel model, GRBModel initialSolution) throws GRBException {
      dSVXD = new GRBVar[pm.getServices().size()][pm.getServiceLength()][pm.getServers().size()][pm
            .getDemandsTrafficFlow()];
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
}
