package optimizer.algorithms;

import manager.Parameters;
import optimizer.Definitions;
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

   public static void run(Parameters pm, Scenario scenario, ResultsManager resultsManager,
         boolean[][][] initialPlacement) {

      VariablesAlg vars = new VariablesAlg(pm, initialPlacement);

      long startTime = System.nanoTime();
      if (scenario.getAlgorithm().equals(DRL)) {
         printLog(log, INFO, "first placement using first-fit");
         Heuristic heuristic = new Heuristic(pm, vars, scenario.getObjFunc(), RANDOM_FIT);
         heuristic.allocateAllServices();
         vars.generateRestOfVariablesForResults(initialPlacement, scenario.getObjFunc());
         float initialObjVal = (float) vars.getObjVal();
         printLog(log, INFO, "starting DRL [" + initialObjVal + "]");
         PlacementModel2 placementModel2 = new PlacementModel2(null, pm, vars, initialPlacement, scenario.getObjFunc(), heuristic);
         placementModel2.run();
         // String routingModelConf = resultsManager.importConfDrlFile(ROUTING_DRL_CONF_FILE);
         // String placementModelConf = resultsManager.importConfDrlFile(PLACEMENT_DRL_CONF_FILE);
         // PlacementModel placementModel = new PlacementModel(placementModelConf, pm, vars, initialPlacement,
         //       scenario.getObjFunc(), heuristic);
         // RoutingModel routingModel = new RoutingModel(routingModelConf, pm, vars, initialPlacement,
         //       scenario.getObjFunc(), heuristic, placementModel);
         // Auxiliary.printLog(log, INFO, "running discovery DRL phase...");
         // double bestFoundObjVal = routingModel.run(1);
         // if (bestFoundObjVal == initialObjVal)
         //    Auxiliary.printLog(log, INFO, "no new solutions found");
         // else {
         //    Auxiliary.printLog(log, INFO, "finding optimal DRL solution");
         //    routingModel.run(0);
         //    resultsManager.exportJsonObject(ROUTING_DRL_CONF_FILE, routingModel.getConf().toJson());
         //    resultsManager.exportJsonObject(PLACEMENT_DRL_CONF_FILE, placementModel.getConf().toJson());
         // }
      } else {
         printLog(log, INFO, "running heuristics...");
         Heuristic heuristic = new Heuristic(pm, vars, scenario.getObjFunc(), scenario.getAlgorithm());
         heuristic.allocateAllServices();
      }
      long elapsedTime = System.nanoTime() - startTime;

      vars.generateRestOfVariablesForResults(initialPlacement, scenario.getObjFunc());
      Auxiliary.printLog(log, INFO, "finished [" + vars.objVal + "]");
      Results results = generateResults(pm, scenario, vars, initialPlacement);
      results.setComputationTime((double) elapsedTime / 1000000000);
      String fileName = generateFileName(pm, scenario.getObjFunc());
      resultsManager.exportJsonObject(fileName, results);
      exportResultsToMST(pm, resultsManager, fileName, vars);
      ResultsGUI.updateResults(results);
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

      Auxiliary.printLog(log, INFO, "exporting results...");
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

   private static String generateFileName(Parameters pm, String objFunc) {
      String fileName = pm.getScenario();
      fileName += "_first-fit_" + objFunc;
      return fileName;
   }
}
