package lp;

import gurobi.GRB;
import gurobi.GRBModel;
import gurobi.GRBVar;
import manager.Parameters;
import output.Definitions;

public class Variables {
   // Objective
   public GRBVar[] kL; // link cost utilization
   public GRBVar[] kX; // server cost utilization
   public GRBVar[] uL; // link utilization
   public GRBVar[] uX; // server utilization
   public GRBVar uMax; // max utilization
   // Elementary
   public GRBVar[][] zSP; // binary, routing per path
   public GRBVar[][][] zSPD; // binary, routing per demand
   public GRBVar[][][] fXSV; // binary, placement per server
   public GRBVar[][][][] fXSVD; // binary, placement per demand
   // Additional
   public GRBVar[] fX; // binary, true if server is used
   public GRBVar[][][][] gSVXY; //binary, auxiliary variable
   public GRBVar[][][] hSVP; // binary, traffic synchronization variable
   public GRBVar[][][][] qSVXP; // continuous, traffic variable
   public GRBVar[][] dSP; // binary, service delay (auxiliary)


   public Variables(Parameters pm, GRBModel model) {
      try {
         // Objective
         uL = new GRBVar[pm.getLinks().size()];
         for (int l = 0; l < pm.getLinks().size(); l++)
            uL[l] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS
                    , Definitions.uL + "[" + l + "]");
         uX = new GRBVar[pm.getServers().size()];
         for (int x = 0; x < pm.getServers().size(); x++)
            uX[x] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS
                    , Definitions.uX + "[" + x + "]");
         kL = new GRBVar[pm.getLinks().size()];
         for (int l = 0; l < pm.getLinks().size(); l++)
            kL[l] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                    , Definitions.kL + "[" + l + "]");
         kX = new GRBVar[pm.getServers().size()];
         for (int x = 0; x < pm.getServers().size(); x++)
            kX[x] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                    , Definitions.kX + "[" + x + "]");
         uMax = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS
                 , Definitions.uMax);
         // Elementary
         zSP = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               zSP[s][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                       , Definitions.rSP + "[" + s + "][" + p + "]");
         zSPD = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  zSPD[s][p][d] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                          , Definitions.rSPD + "[" + s + "][" + p + "][" + d + "]");
         fXSV = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  fXSV[x][s][v] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                          , Definitions.pXSV + "[" + x + "][" + s + "][" + v + "]");
         fXSVD = new GRBVar[pm.getServers().size()][pm.getServices().size()]
                 [pm.getServiceLength()][pm.getDemandsTrafficFlow()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     fXSVD[x][s][v][d] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                             , Definitions.pXSVD + "[" + x + "][" + s + "][" + v + "][" + d + "]");
         // Additional
         fX = new GRBVar[pm.getServers().size()];
         for (int x = 0; x < pm.getServers().size(); x++)
            this.fX[x] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                    , Definitions.pX + "[" + x + "]");
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
                          , Definitions.sSVP + "[" + s + "][" + v + "][" + p + "]");
         dSP = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               dSP[s][p] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                       , Definitions.dSP + "[" + s + "][" + p + "]");
         qSVXP = new GRBVar[pm.getServices().size()][pm.getServiceLength()][pm.getServers().size()][pm.getPathsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int x = 0; x < pm.getServers().size(); x++)
                  for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                     qSVXP[s][v][x][p] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS
                             , Definitions.qSVXP + "[" + s + "][" + v + "][" + x + "][" + p + "]");
         model.update();
      } catch (Exception ignored) {
      }
   }
}
