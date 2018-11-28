package lp;

import gurobi.GRB;
import gurobi.GRBModel;
import gurobi.GRBVar;
import manager.Parameters;
import output.Definitions;

public class Variables {

   // Elementary
   public GRBVar[][] rSP;
   public GRBVar[][][] rSPD;
   public GRBVar[][][] pXSV;
   public GRBVar[][][][] pXSVD;
   public GRBVar[] kL;
   public GRBVar[] kX;
   public GRBVar[] uL;
   public GRBVar[] uX;
   // Additional
   public GRBVar[] pX;
   public GRBVar[][][][] gSVXY;
   public GRBVar[][][] sSVP;
   public GRBVar[] dS;
   public GRBVar[][][] dSPX;

   public Variables(Parameters pm, GRBModel model) {
      try {
         rSP = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               rSP[s][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                       , Definitions.rSP + "[" + s + "][" + p + "]");
         rSPD = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  rSPD[s][p][d] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                          , Definitions.rSPD + "[" + s + "][" + p + "][" + d + "]");
         pXSV = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  pXSV[x][s][v] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                          , Definitions.pXSV + "[" + x + "][" + s + "][" + v + "]");
         pXSVD = new GRBVar[pm.getServers().size()][pm.getServices().size()]
                 [pm.getServiceLength()][pm.getDemandsTrafficFlow()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     pXSVD[x][s][v][d] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                             , Definitions.pXSVD + "[" + x + "][" + s + "][" + v + "][" + d + "]");
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
         pX = new GRBVar[pm.getServers().size()];
         for (int x = 0; x < pm.getServers().size(); x++)
            this.pX[x] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
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
         sSVP = new GRBVar[pm.getServices().size()][pm.getServiceLength()][pm.getPaths().size()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int p = 0; p < pm.getPaths().size(); p++)
                  sSVP[s][v][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                          , Definitions.sSVP + "[" + s + "][" + v + "][" + p + "]");
         dS = new GRBVar[pm.getServices().size()];
         for (int s = 0; s < pm.getServices().size(); s++)
            dS[s] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, Definitions.dS + "[" + s + "]");
         dSPX = new GRBVar[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getServers().size()];
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int x = 0; x < pm.getServers().size(); x++)
                  dSPX[s][p][x] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY
                          , Definitions.dSPX + "[" + s + "][" + p + "][" + x + "]");
         model.update();
      } catch (Exception ignored) {
      }
   }
}
