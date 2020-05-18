package optimizer;

import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.TrafficFlow;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.algorithms.LauncherAlg;
import optimizer.lp.LauncherLP;
import optimizer.results.Auxiliary;
import optimizer.results.ResultsManager;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConfigFiles;
import utils.GraphManager;
import utils.KShortestPathGenerator;

import java.util.Random;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class Manager {

   private static final Logger log = LoggerFactory.getLogger(Manager.class);
   private static boolean interrupted;
   private static Parameters pm;
   private static Long seed;

   public static void readInputParameters(String fileName) {
      try {
         String path = Auxiliary.getResourcePath(fileName + ".yml");
         pm = ConfigFiles.readParameters(path, fileName + ".yml");
         pm.initialize(path);
         checkTopologyScale(pm);
         ResultsGUI.initialize(pm);
         printLog(log, INFO, "topology loaded");
         seed = pm.getSeed();
      } catch (Exception e) {
         printLog(log, ERROR, "error loading input parameters");
      }
   }

   private static void checkTopologyScale(Parameters pm) {
      double scalingX = (double) pm.getAux(X_SCALING);
      double scalingY = (double) pm.getAux(Y_SCALING);
      if (scalingX != 1.0 || scalingY != 1.0)
         for (Node node : pm.getNodes()) {
            String xAttr = "x", yAttr = "y";
            if (node.getAttribute(NODE_CLOUD) != null) {
               xAttr = "x_gui";
               yAttr = "y_gui";
            }
            node.setAttribute(xAttr, (double) node.getAttribute(xAttr) * scalingX);
            node.setAttribute(yAttr, (double) node.getAttribute(yAttr) * scalingY);
         }
   }

   private static void specifyUsedTrafficDemands(Parameters pm, Scenario sce) {
      Random rnd = new Random(seed);
      for (TrafficFlow trafficFlow : pm.getTrafficFlows()) {
         trafficFlow.getAux().clear();
         for (int d = 0; d < trafficFlow.getDemands().size(); d++)
            trafficFlow.getAux().add(true);
         if (sce.getAlgorithm().equals(INIT)) {
            double initialTrafficLoad = (double) pm.getAux().get(INITIAL_TRAFFIC_LOAD);
            double value;
            for (int d = 0; d < trafficFlow.getDemands().size(); d++) {
               value = rnd.nextDouble();
               if (value > initialTrafficLoad)
                  trafficFlow.getAux().set(d, false);
            }
            boolean allFalse = true;
            for (int d = 0; d < trafficFlow.getDemands().size(); d++)
               if (trafficFlow.getAux().get(d))
                  allFalse = false;
            if (allFalse)
               trafficFlow.getAux().set(0, true);
         }
      }
   }

   public static void main(Scenario sce) {
      readInputParameters(sce.getInputFileName());
      try {
         interrupted = false;
         ResultsManager resultsManager = new ResultsManager(pm.getScenario());
         if (sce.getAlgorithm().equals(ALL)) {
            // 1. initial placement
            sce.setAlgorithm(INIT);
            specifyUsedTrafficDemands(pm, sce);
            GRBModel initModel = LauncherLP.run(pm, sce, resultsManager, null, null);
            // 2. ff
            sce.setAlgorithm(FF);
            specifyUsedTrafficDemands(pm, sce);
            LauncherAlg.run(pm, sce, resultsManager, initModel, -1);
            // 3. rf
            sce.setAlgorithm(RF);
            for (int i = 0; i < 10; i++)
               LauncherAlg.run(pm, sce, resultsManager, initModel, i);
            // 4. heu
            sce.setAlgorithm(HEU);
            LauncherAlg.run(pm, sce, resultsManager, initModel, -1);
            // 5. lp
            String pathFile = resultsManager.getResultsFolder() + "/" + pm.getScenario() + "_heu_" + sce.getObjFunc();
            GRBModel initSol = resultsManager.loadModel(pathFile, pm, sce, false);
            sce.setAlgorithm(LP);
            LauncherLP.run(pm, sce, resultsManager, initModel, initSol);
         } else if (sce.getAlgorithm().equals(INIT) || sce.getAlgorithm().equals(DIMEN)
               || sce.getAlgorithm().equals(LP)) {
            specifyUsedTrafficDemands(pm, sce);
            String pathFile = Auxiliary.getResourcePath(pm.getScenario() + "_heu_" + sce.getObjFunc() + ".mst");
            GRBModel initSol = resultsManager.loadModel(pathFile + pm.getScenario() + "_heu_" + sce.getObjFunc(), pm,
                  sce, false);
            // make sure no initial model is loaded when launching initial placement
            GRBModel initModel;
            if (sce.getAlgorithm().equals(INIT))
               initModel = null;
            else {
               pathFile = Auxiliary.getResourcePath(pm.getScenario() + "_" + INIT + ".mst");
               initModel = resultsManager.loadInitialPlacement(pathFile + pm.getScenario() + "_" + INIT, pm, sce);
            }
            LauncherLP.run(pm, sce, resultsManager, initModel, initSol);
         } else {
            specifyUsedTrafficDemands(pm, sce);
            String pathFile = Auxiliary.getResourcePath(pm.getScenario() + "_" + INIT + ".mst");
            GRBModel initModel = resultsManager.loadInitialPlacement(pathFile + pm.getScenario() + "_" + INIT, pm, sce);
            LauncherAlg.run(pm, sce, resultsManager, initModel, -1);
         }
         printLog(log, INFO, "backend is ready");
      } catch (Exception e) {
         e.printStackTrace();
         printLog(log, ERROR, "something went wrong");
      }
   }

   public static void stop() {
      interrupted = true;
   }

   public static void generatePaths(Scenario sce) {
      String path = Auxiliary.getResourcePath(sce.getInputFileName() + ".yml");
      if (path != null)
         try {
            Graph graph = GraphManager.importTopology(path, sce.getInputFileName());
            printLog(log, INFO, "generating paths");
            KShortestPathGenerator k = new KShortestPathGenerator(graph, 10, 5, path, sce.getInputFileName());
            k.run();
            printLog(log, INFO, "paths generated");
         } catch (Exception e) {
            printLog(log, ERROR, "reading the topology file");
         }
   }

   public static boolean isInterrupted() {
      return interrupted;
   }
}
