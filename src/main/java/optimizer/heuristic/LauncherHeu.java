package optimizer.heuristic;


import manager.Parameters;
import optimizer.Definitions;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.results.Auxiliary;
import optimizer.results.Results;
import optimizer.results.ResultsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class LauncherHeu {

   private static final Logger log = LoggerFactory.getLogger(LauncherHeu.class);

   public static void run(Parameters pm, Scenario scenario, ResultsManager resultsManager, boolean[][][] initialPlacement) {

      printLog(log, INFO, "running heuristic");
      Heuristic heuristic = new Heuristic(pm, initialPlacement);

      FirstFit firstFit = new FirstFit(pm, heuristic);
      long startTime = System.nanoTime();
      firstFit.run(scenario.getObjFunc());
      long elapsedTime = System.nanoTime() - startTime;

      heuristic.generateRestOfVariablesForResults(initialPlacement, scenario.getObjFunc());
      Results results = generateResults(pm, scenario, heuristic, initialPlacement);
      results.setComputationTime((double) elapsedTime / 1000000000);
      String fileName = generateFileName(pm, scenario.getObjFunc());
      resultsManager.exportJsonFile(fileName, results);
      exportResultsToMST(pm, resultsManager, fileName, heuristic);
      ResultsGUI.updateResults(results);
   }

   private static Results generateResults(Parameters pm, Scenario sc, Heuristic heu, boolean[][][] initialPlacement) {
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

   private static void exportResultsToMST(Parameters pm, ResultsManager rm, String fileName, Heuristic heu) {

      Auxiliary.printLog(log, INFO, "exporting results...");
      File file = rm.createPlainTextFile(fileName, ".mst");
      rm.appendToPlainText(file, "# MIP start");

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            writeVarToFile(rm, file, Definitions.zSP + "[" + s + "][" + p + "] ", heu.getzSP()[s][p]);


      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               writeVarToFile(rm, file, Definitions.zSPD + "[" + s + "][" + p + "][" + d + "] ", heu.getzSPD()[s][p][d]);

      for (int x = 0; x < pm.getServers().size(); x++)
         writeVarToFile(rm, file, Definitions.fX + "[" + x + "] ", heu.getfX()[x]);

      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               writeVarToFile(rm, file, Definitions.fXSV + "[" + x + "][" + s + "][" + v + "] ", heu.getfXSV()[x][s][v]);

      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  writeVarToFile(rm, file, Definitions.fXSVD + "[" + x + "][" + s + "][" + v + "][" + d + "] ", heu.getfXSVD()[x][s][v][d]);

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++)
               for (int y = 0; y < pm.getServers().size(); y++)
                  if (!pm.getServers().get(x).getParent().equals(pm.getServers().get(y).getParent()))
                     writeVarToFile(rm, file, Definitions.gSVXY + "[" + s + "][" + v + "][" + x + "][" + y + "] ", heu.getgSVXY()[s][v][x][y]);

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int p = 0; p < pm.getPaths().size(); p++)
               writeVarToFile(rm, file, Definitions.hSVP + "[" + s + "][" + v + "][" + p + "] ", heu.gethSVP()[s][v][p]);
   }

   private static void writeVarToFile(ResultsManager rm, File file, String line, boolean var) {
      if (var)
         line += "1";
      else
         line += "0";
      rm.appendToPlainText(file, line);
   }

   private static String generateFileName(Parameters pm, String objFunc) {
      String fileName = pm.getScenario();
      fileName += "_first-fit_" + objFunc;
      return fileName;
   }
}
