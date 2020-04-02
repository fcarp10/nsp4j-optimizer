package optimizer.heuristic;


import manager.Parameters;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.results.Results;
import optimizer.results.ResultsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static optimizer.Definitions.INFO;
import static optimizer.results.Auxiliary.printLog;

public class LauncherHeu {

   private static final Logger log = LoggerFactory.getLogger(LauncherHeu.class);

   public static void run(Parameters pm, Scenario scenario, ResultsManager resultsManager) {

      printLog(log, INFO, "initializing heuristic");
      FirstFit firstFit = new FirstFit(pm);
      printLog(log, INFO, "running heuristic");
      long startTime = System.nanoTime();
      firstFit.run();
      long elapsedTime = System.nanoTime() - startTime;
      Results results = generateResults(pm, scenario, firstFit);
      results.setComputationTime((double) elapsedTime / 1000000000);
      resultsManager.exportJsonFile(generateFileName(pm), results);
      ResultsGUI.updateResults(results);
   }

   private static Results generateResults(Parameters pm, Scenario sc, FirstFit firstFit) {
      Results results = new Results(pm, sc);


      return results;
   }

   private static String generateFileName(Parameters pm) {
      String fileName = pm.getScenario();
      fileName += "_first-fit";
      return fileName;
   }
}
