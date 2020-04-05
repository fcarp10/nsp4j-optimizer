package optimizer.heuristic;


import gurobi.GRBException;
import gurobi.GRBModel;
import manager.Parameters;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.results.Results;
import optimizer.results.ResultsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.convertInitialPlacement;
import static optimizer.results.Auxiliary.printLog;

public class LauncherHeu {

   private static final Logger log = LoggerFactory.getLogger(LauncherHeu.class);

   public static void run(Parameters pm, Scenario scenario, ResultsManager resultsManager, GRBModel initialPlacement) throws GRBException {

      printLog(log, INFO, "initializing heuristic");
      FirstFit firstFit = new FirstFit(pm);
      printLog(log, INFO, "running heuristic");
      long startTime = System.nanoTime();
      firstFit.run();
      long elapsedTime = System.nanoTime() - startTime;
      firstFit.generateRestOfVariablesForResults(initialPlacement);
      Results results = generateResults(pm, scenario, firstFit, initialPlacement);
      results.setComputationTime((double) elapsedTime / 1000000000);
      resultsManager.exportJsonFile(generateFileName(pm), results);
      ResultsGUI.updateResults(results);
   }

   private static Results generateResults(Parameters pm, Scenario sc, FirstFit firstFit, GRBModel initialPlacement) {
      Results results = new Results(pm, sc);
      results.setVariable(zSPD, firstFit.zSPD);
      results.setVariable(fXSVD, firstFit.fXSVD);
      List<Double> luList = new ArrayList<>(firstFit.uL.values());
      double[] luArray = new double[luList.size()];
      for (int i = 0; i < luList.size(); i++) luArray[i] = luList.get(i);
      results.setVariable(uL, luArray);
      List<Double> xuList = new ArrayList<>(firstFit.uX.values());
      double[] xuArray = new double[xuList.size()];
      for (int i = 0; i < xuList.size(); i++) xuArray[i] = xuList.get(i);
      results.setVariable(uX, xuArray);
      results.setVariable(fX, firstFit.fX);
      results.setVariable(fXSV, firstFit.fXSV);
      results.setVariable(zSP, firstFit.zSP);
      results.initializeResults(0.0, convertInitialPlacement(pm, initialPlacement));
      return results;
   }

   private static String generateFileName(Parameters pm) {
      String fileName = pm.getScenario();
      fileName += "_first-fit";
      return fileName;
   }
}
