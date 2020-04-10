package optimizer.heuristic;


import gurobi.GRBModel;
import manager.Parameters;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.results.Results;
import optimizer.results.ResultsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.convertInitialPlacement;
import static optimizer.results.Auxiliary.printLog;

public class LauncherHeu {

   private static final Logger log = LoggerFactory.getLogger(LauncherHeu.class);

   public static void run(Parameters pm, Scenario scenario, ResultsManager resultsManager, GRBModel initialPlacement) {

      printLog(log, INFO, "initializing heuristic");
      FirstFit firstFit = new FirstFit(pm);
      printLog(log, INFO, "running heuristic");
      long startTime = System.nanoTime();
      firstFit.run();
      long elapsedTime = System.nanoTime() - startTime;
      firstFit.generateRestOfVariablesForResults(convertInitialPlacement(pm, initialPlacement));
      Results results = generateResults(pm, scenario, firstFit, initialPlacement);
      results.setComputationTime((double) elapsedTime / 1000000000);
      resultsManager.exportJsonFile(generateFileName(pm), results);
      ResultsGUI.updateResults(results);
   }

   private static Results generateResults(Parameters pm, Scenario sc, FirstFit firstFit, GRBModel initialPlacement) {
      Results results = new Results(pm, sc);

      double[] linkUtilizationResults = new double[pm.getLinks().size()];
      for (int l = 0; l < pm.getLinks().size(); l++)
         linkUtilizationResults[l] = firstFit.uL.get(pm.getLinks().get(l).getId());
      results.setVariable(uL, linkUtilizationResults);

      double[] serverUtilizationResults = new double[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++)
         serverUtilizationResults[x] = firstFit.uX.get(pm.getServers().get(x).getId());
      results.setVariable(uX, serverUtilizationResults);

      results.setVariable(zSPD, firstFit.zSPD);
      results.setVariable(fXSVD, firstFit.fXSVD);
      results.setVariable(fX, firstFit.fX);
      results.setVariable(fXSV, firstFit.fXSV);
      results.setVariable(zSP, firstFit.zSP);
      results.setVariable(oX, firstFit.oX);
      results.setVariable(oSV, firstFit.oSV);
      results.setVariable(qSDP, firstFit.qSDP);
      results.setVariable(hSVP, firstFit.hSVP);
      results.initializeResults(0.0, convertInitialPlacement(pm, initialPlacement));
      return results;
   }

   private static String generateFileName(Parameters pm) {
      String fileName = pm.getScenario();
      fileName += "_first-fit";
      return fileName;
   }
}
