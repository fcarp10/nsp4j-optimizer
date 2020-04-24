package optimizer.algorithms;

import manager.Parameters;
import optimizer.Definitions;
import optimizer.algorithms.learning.PlacementModel;
import optimizer.algorithms.learning.RoutingModel;
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

      VariablesAlg variablesAlg = new VariablesAlg(pm, initialPlacement);

      long startTime = System.nanoTime();
      if (scenario.getAlgorithm().equals(DRL)) {
         printLog(log, INFO, "first placement using first-fit");
         Heuristic heuristic = new Heuristic(pm, variablesAlg, scenario.getObjFunc(), FIRST_FIT);
         heuristic.allocateAllServices();
         variablesAlg.generateRestOfVariablesForResults(initialPlacement, scenario.getObjFunc());
         printLog(log, INFO, "running reallocation using DRL...");
         
         String routingModelConf = resultsManager.importConfDrlFile(ROUTING_DRL_CONF_FILE);
         String placementModelConf = resultsManager.importConfDrlFile(PLACEMENT_DRL_CONF_FILE);
         PlacementModel placementModel = new PlacementModel(placementModelConf, pm, variablesAlg, initialPlacement,
               scenario.getObjFunc(), heuristic);
         RoutingModel routingModel = new RoutingModel(routingModelConf, pm, variablesAlg, initialPlacement, scenario.getObjFunc(),
               heuristic, placementModel);
         routingModel.run();
         resultsManager.exportJsonObject(ROUTING_DRL_CONF_FILE, routingModel.getConf().toJson());
         resultsManager.exportJsonObject(PLACEMENT_DRL_CONF_FILE, placementModel.getConf().toJson());
      } else {
         printLog(log, INFO, "running heuristics...");
         Heuristic heuristic = new Heuristic(pm, variablesAlg, scenario.getObjFunc(), scenario.getAlgorithm());
         heuristic.allocateAllServices();
      }
      long elapsedTime = System.nanoTime() - startTime;

      variablesAlg.generateRestOfVariablesForResults(initialPlacement, scenario.getObjFunc());
      Auxiliary.printLog(log, INFO, "finished [" + variablesAlg.objVal + "]");
      Results results = generateResults(pm, scenario, variablesAlg, initialPlacement);
      results.setComputationTime((double) elapsedTime / 1000000000);
      String fileName = generateFileName(pm, scenario.getObjFunc());
      resultsManager.exportJsonObject(fileName, results);
      exportResultsToMST(pm, resultsManager, fileName, variablesAlg);
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