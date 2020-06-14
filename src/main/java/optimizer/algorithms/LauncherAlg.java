package optimizer.algorithms;

import manager.Parameters;
import optimizer.Definitions;
import optimizer.algorithms.heuristics.HeuristicAlgorithm;
import optimizer.algorithms.learning.PlacementModel2;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.results.Auxiliary;
import optimizer.results.Results;
import optimizer.results.ResultsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class LauncherAlg {

   private static final Logger log = LoggerFactory.getLogger(LauncherAlg.class);

   public static VariablesAlg run(Parameters pm, Scenario sce, ResultsManager resultsManager,
         VariablesAlg initialPlacementVars, int iteration, boolean exportToMST) {
      VariablesAlg vars = new VariablesAlg(pm, initialPlacementVars, sce.getObjFunc());
      NetworkManager networkManager = new NetworkManager(pm, vars);
      HeuristicAlgorithm heuristicAlgorithm = new HeuristicAlgorithm(pm, vars, networkManager);
      long startTime = System.nanoTime();
      if (sce.getAlgorithm().equals(DRL)) {
         printLog(log, INFO, "first placement using random-fit");
         heuristicAlgorithm.allocateServices(RF);
         vars.generateRestOfVariablesForResults();
         printLog(log, INFO, "starting DRL [" + (float) vars.getObjVal() + "]");
         PlacementModel2 placementModel2 = new PlacementModel2(null, pm, vars, networkManager, heuristicAlgorithm);
         placementModel2.run(RF);
      } else if (sce.getAlgorithm().equals(GRD)) {
         printLog(log, INFO, "running heuristics...");
         heuristicAlgorithm.allocateServicesHeuristic(sce.getAlgorithm());
         // vars.generateRestOfVariablesForResults();
         // heuristicAlgorithm.optimizePlacement();
      } else {
         printLog(log, INFO, "running " + sce.getAlgorithm() + "...");
         heuristicAlgorithm.allocateServices(sce.getAlgorithm());
      }
      long elapsedTime = System.nanoTime() - startTime;
      vars.generateRestOfVariablesForResults();
      Auxiliary.printLog(log, INFO, "finished [" + Auxiliary.roundDouble(vars.objVal, 4) + "]");
      Auxiliary.printLog(log, INFO, "generating results...");
      Results results = generateResults(pm, sce, vars, Auxiliary.fXSVvarsFromInitialModel(pm, initialPlacementVars));
      results.setComputationTime((double) elapsedTime / 1000000000);
      String fileName = pm.getGraphName() + "_" + sce.getAlgorithm() + "_" + sce.getObjFunc();
      if (sce.getAlgorithm().equals(RF))
         fileName += iteration;
      resultsManager.exportJsonObject(fileName, results);
      if (exportToMST)
         exportResultsToMST(pm, resultsManager, fileName, vars);
      ResultsGUI.updateResults(results);
      Auxiliary.printLog(log, INFO, "done");
      return vars;
   }

   private static Results generateResults(Parameters pm, Scenario sc, VariablesAlg heu,
         boolean[][][] initialPlacement) {
      Results results = new Results(pm, sc);

      results.setVariable(uL, heu.lu);
      results.setVariable(uX, heu.xu);
      results.setVariable(zSPD, heu.zSPD);
      results.setVariable(fXSVD, heu.fXSVD);
      results.setVariable(fX, heu.fX);
      results.setVariable(fXSV, heu.fXSV);
      results.setVariable(zSP, heu.zSP);
      results.setVariable(oX, heu.oX);
      results.setVariable(oSV, heu.oSV);
      results.setVariable(qSDP, heu.qSDP);
      results.setVariable(hSVP, heu.hSVP);
      results.setVariable(gSVXY, heu.gSVXY);
      results.initializeResults(heu.objVal, initialPlacement);
      return results;
   }

   private static void exportResultsToMST(Parameters pm, ResultsManager rm, String fileName, VariablesAlg heu) {

      PrintWriter pw = rm.getPrinterFromPlainTextFile(fileName, ".mst");
      pw.println("# MIP start");

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            writeVarToFile(pw, Definitions.zSP + "[" + s + "][" + p + "] ", heu.zSP[s][p]);

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               writeVarToFile(pw, Definitions.zSPD + "[" + s + "][" + p + "][" + d + "] ", heu.zSPD[s][p][d]);

      for (int x = 0; x < pm.getServers().size(); x++)
         writeVarToFile(pw, Definitions.fX + "[" + x + "] ", heu.fX[x]);

      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               writeVarToFile(pw, Definitions.fXSV + "[" + x + "][" + s + "][" + v + "] ", heu.fXSV[x][s][v]);

      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  writeVarToFile(pw, Definitions.fXSVD + "[" + x + "][" + s + "][" + v + "][" + d + "] ",
                        heu.fXSVD[x][s][v][d]);

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int y = 0; y < pm.getServers().size(); y++)
                  if (!pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent()))
                     writeVarToFile(pw, Definitions.gSVXY + "[" + s + "][" + v + "][" + x + "][" + y + "] ",
                           heu.gSVXY[s][v][x][y]);

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int p = 0; p < pm.getPaths().size(); p++)
               writeVarToFile(pw, Definitions.hSVP + "[" + s + "][" + v + "][" + p + "] ", heu.hSVP[s][v][p]);
      pw.close();
   }

   private static void writeVarToFile(PrintWriter pw, String line, boolean var) {
      if (var)
         line += "1";
      else
         line += "0";
      pw.println(line);
   }
}
