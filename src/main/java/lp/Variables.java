package lp;

import gui.elements.Scenario;
import gurobi.GRB;
import gurobi.GRBModel;
import gurobi.GRBVar;
import manager.Parameters;
import output.Definitions;

import static output.Definitions.*;

public class Variables {
   // objective variables
   public GRBVar[] uL; // link utilization
   public GRBVar[] uX; // server utilization
   // model specific objective variables
   public GRBVar[] kL; // link cost utilization
   public GRBVar[] kX; // server cost utilization
   public GRBVar[] fX; // binary, true if server is used
   public GRBVar uMax; // max utilization
   // general variables
   public GRBVar[][] zSP; // binary, routing per path
   public GRBVar[][][] zSPD; // binary, routing per demand
   public GRBVar[][][] fXSV; // binary, placement per server
   public GRBVar[][][][] fXSVD; // binary, placement per demand
   // additional variables
   public GRBVar[][][] dSPD; // binary, service delay
   public GRBVar[][][][] ySVXD; // continuous, processing delay of a traffic demand
   public GRBVar[] mS; // integer, maximum migration delay for a service
   public GRBVar[][][][] gSVXY; //binary, synchronization traffic auxiliary variable
   public GRBVar[][][] hSVP; // binary, traffic synchronization variable

   public Variables(Parameters pm, GRBModel model, Scenario scenario) {
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
         // if model is using optimizing costs functions
         if (scenario.getObjectiveFunction().equals(COSTS_OBJ)) {
            kL = new GRBVar[pm.getLinks().size()];
            for (int l = 0; l < pm.getLinks().size(); l++)
               kL[l] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                       , Definitions.kL + "[" + l + "]");
         }
         if (scenario.getObjectiveFunction().equals(COSTS_OBJ)
                 || scenario.getObjectiveFunction().equals(NUM_SERVERS_COSTS_OBJ)) {
            kX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
               kX[x] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                       , Definitions.kX + "[" + x + "]");
         }
         // if model is optimizing max utilization
         if (scenario.getObjectiveFunction().equals(MAX_UTILIZATION_OBJ)) {
            uMax = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS
                    , Definitions.uMax);
         }
         model.update();
      } catch (Exception ignored) {
      }
   }

   public void initializeAdditionalVariables(Parameters pm, GRBModel model, Scenario scenario) {
      try {
         // if model is minimizing number of used servers
         if (scenario.getObjectiveFunction().equals(NUM_SERVERS_OBJ)
                 || scenario.getObjectiveFunction().equals(NUM_SERVERS_COSTS_OBJ)) {
            fX = new GRBVar[pm.getServers().size()];
            for (int x = 0; x < pm.getServers().size(); x++)
               this.fX[x] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                       , Definitions.fX + "[" + x + "]");
         }
         // if model is considering synchronization traffic
         if (scenario.getConstraints().get(ST)) {
            gSVXY = new GRBVar[pm.getServices().size()][pm.getServiceLength()]
                    [pm.getServers().size()][pm.getServers().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int x = 0; x < pm.getServers().size(); x++)
                     for (int y = 0; y < pm.getServers().size(); y++)
                        if (x != y)
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
         if (scenario.getConstraints().get(SD)) {
            dSPD = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     dSPD[s][p][d] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                             , Definitions.dSPD + "[" + s + "][" + p + "][" + d + "]");
            ySVXD = new GRBVar[pm.getServices().size()][pm.getServiceLength()][pm.getServers().size()][pm.getDemandsTrafficFlow()];
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int x = 0; x < pm.getServers().size(); x++)
                     for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                        ySVXD[s][v][x][d] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                                , Definitions.ySVXD + "[" + s + "][" + v + "][" + x + "][" + d + "]");
            mS = new GRBVar[pm.getServices().size()];
            for (int s = 0; s < pm.getServices().size(); s++)
               mS[s] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, Definitions.mS + "[" + s + "]");
         }
         model.update();
      } catch (Exception ignored) {
      }
   }
}
