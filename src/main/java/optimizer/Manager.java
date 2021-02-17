package optimizer;

import gurobi.GRBException;
import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.TrafficFlow;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.algorithms.LauncherAlg;
import optimizer.algorithms.VariablesAlg;
import optimizer.lp.LauncherLP;
import optimizer.results.Auxiliary;
import optimizer.results.ResultsManager;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConfigFiles;

import java.util.Random;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class Manager {

   private static final Logger log = LoggerFactory.getLogger(Manager.class);
   private static boolean interrupted;
   private static Parameters pm;
   private static Long seed;

   public static String readInputParameters(String graphNameForm, boolean considerSubsetOfDemands) {

      String rootPath = Auxiliary.getResourcesPath(graphNameForm + ".yml", null);
      String[] graphName = graphNameForm.split("_");
      try {
         pm = ConfigFiles.readParameters(rootPath + graphNameForm + ".yml");
      } catch (Exception e) {
         printLog(log, ERROR, "error loading .yml file");
      }
      String[] extensions = new String[] { ".dgs", ".gml" };
      boolean isLoaded = false;
      for (int i = 0; i < extensions.length; i++)
         if (pm.initialize(rootPath + graphName[0] + extensions[i], rootPath + graphName[0] + ".txt",
               (boolean) pm.getAux(DIRECTED_EDGES))) {
            isLoaded = true;
            break;
         }
      if (!isLoaded) {
         printLog(log, ERROR, "error loading graph or paths files");
         return null;
      }
      checkTopologyScale(pm);
      ResultsGUI.initialize(pm);
      printLog(log, INFO, "topology loaded");
      seed = pm.getSeed();
      considerSubsetOfDemands(pm, considerSubsetOfDemands);
      return graphName[0];
   }

   private static void checkTopologyScale(Parameters pm) {
      double scalingX = (double) pm.getAux(X_SCALING);
      double scalingY = (double) pm.getAux(Y_SCALING);
      if (scalingX != 1.0 || scalingY != 1.0) {
         String longitudeLabel, latitudeLabel;
         if (pm.getNodes().get(0).getAttribute(LONGITUDE_LABEL_1) != null) {
            longitudeLabel = LONGITUDE_LABEL_1;
            latitudeLabel = LATITUDE_LABEL_1;
         } else {
            longitudeLabel = LONGITUDE_LABEL_2;
            latitudeLabel = LATITUDE_LABEL_2;
         }
         for (Node node : pm.getNodes()) {
            if (node.getAttribute(NODE_CLOUD) != null && node.getAttribute(longitudeLabel + "_gui") != null) {
               node.setAttribute(longitudeLabel + "_gui",
                     (double) node.getAttribute(longitudeLabel + "_gui") * scalingX);
               node.setAttribute(latitudeLabel + "_gui", (double) node.getAttribute(latitudeLabel + "_gui") * scalingY);
            } else {
               node.setAttribute(longitudeLabel, (double) node.getAttribute(longitudeLabel) * scalingX);
               node.setAttribute(latitudeLabel, (double) node.getAttribute(latitudeLabel) * scalingY);
            }
         }
      }
   }

   private static void considerSubsetOfDemands(Parameters pm, boolean considerSubset) {
      Random rnd = new Random(seed);
      for (TrafficFlow trafficFlow : pm.getTrafficFlows()) {
         trafficFlow.getAux().clear();
         for (int d = 0; d < trafficFlow.getDemands().size(); d++)
            trafficFlow.getAux().add(true);
         if (considerSubset) {
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
      ResultsManager resultsManager;
      String graphNameShort;
      try {
         switch (sce.getName()) {
            case LP:
               boolean exportMST = true;
               resultsManager = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               String outputFileName = pm.getGraphName() + sce.getName() + sce.getObjFunc();
               LauncherLP.run(pm, sce, resultsManager, null, null, outputFileName, exportMST);
               break;

            case FF:
               resultsManager = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               outputFileName = pm.getGraphName() + "_" + FF + "_" + sce.getObjFunc();
               LauncherAlg.run(pm, sce, resultsManager, null, outputFileName, false);
               break;

            case RF:
               resultsManager = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               for (int i = 0; i < 10; i++) {
                  outputFileName = pm.getGraphName() + "_" + RF + "_" + sce.getObjFunc() + "_" + i;
                  LauncherAlg.run(pm, sce, resultsManager, null, outputFileName, false);
               }
               break;

            case GRD:
               resultsManager = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               outputFileName = pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc();
               LauncherAlg.run(pm, sce, resultsManager, null, outputFileName, false);
               break;

            case CUSTOM_1:
               exportMST = true;
               resultsManager = new ResultsManager(pm.getGraphName());
               graphNameShort = readInputParameters(sce.getInputFileName(), false);
               GRBModel initModel = resultsManager.loadInitialPlacement(
                     Auxiliary.getResourcesPath(graphNameShort + "_init-lp.mst", null) + graphNameShort + "_init-lp",
                     pm, sce);
               GRBModel initSol = resultsManager.loadModel(
                     Auxiliary.getResourcesPath(pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc() + ".mst", null)
                           + pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc(),
                     pm, sce, false);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc();
               LauncherLP.run(pm, sce, resultsManager, initModel, initSol, outputFileName, exportMST);
               break;

            case CUSTOM_2:
               resultsManager = new ResultsManager(sce.getInputFileName());
               String algorithm;
               VariablesAlg initVars;
               exportMST = true;
               // 1 - low [RF]
               readInputParameters(sce.getInputFileName() + "_low", false);
               algorithm = RF;
               sce.setName(algorithm);
               sce.setObjFunc(MGR_REP);
               outputFileName = pm.getGraphName() + "_" + algorithm + "_" + sce.getObjFunc();
               Auxiliary.removeCapacityOfCloudServers(pm); // constraint to only edge
               initVars = LauncherAlg.run(pm, sce, resultsManager, null, outputFileName, exportMST);
               GRBModel lowModel = resultsManager
                     .loadModel(Auxiliary.getResourcesPath(outputFileName + ".mst", resultsManager.getResultsFolder())
                           + outputFileName, pm, sce, true); // load low model from file as GRBModel
               Auxiliary.restoreCapacityOfCloudServers(pm); // remove constraint only edge

               runCustomSequenceLP(sce, resultsManager, lowModel);
               runCustomSequenceAlg(sce, resultsManager, initVars);
               break;

            case CUSTOM_3:
               resultsManager = new ResultsManager(sce.getInputFileName());
               exportMST = false;

               // 1 - low [RF]
               readInputParameters(sce.getInputFileName() + "_low", false);
               algorithm = RF;
               sce.setName(algorithm);
               sce.setObjFunc(MGR_REP);
               Auxiliary.removeCapacityOfCloudServers(pm); // constraint to only edge
               outputFileName = pm.getGraphName() + "_" + algorithm + "_" + sce.getObjFunc();
               initVars = LauncherAlg.run(pm, sce, resultsManager, null, outputFileName, exportMST);
               Auxiliary.restoreCapacityOfCloudServers(pm); // remove constraint only edge

               runCustomSequenceAlg(sce, resultsManager, initVars);
               break;

            default:
               printLog(log, INFO, "no algorithm selected");
               break;
         }
         printLog(log, INFO, "backend is ready");
      } catch (Exception e) {
         e.printStackTrace();
         printLog(log, ERROR, "something went wrong");
      }
   }

   public static void runCustomSequenceLP(Scenario sce, ResultsManager resultsManager, GRBModel initModel)
         throws GRBException {

      boolean exportMST = false;

      // 2 - high [LP][MGR][init-low]
      runCustomLP(sce, MGR, "high", "init-low", resultsManager, initModel, exportMST);
      // 2 - high [LP][REP][init-low]
      runCustomLP(sce, REP, "high", "init-low", resultsManager, initModel, exportMST);
      // 2 - high [LP][MGR-REP][init-low]
      runCustomLP(sce, MGR_REP, "high", "init-low", resultsManager, initModel, exportMST);

      // 3 - high-pred [LP][MGR][init-low]
      GRBModel highPredMgrModel = runCustomLP(sce, MGR, "high-pred", "init-low", resultsManager, initModel, exportMST);
      // 3 - high [LP][MGR][init-high-pred]
      runCustomLP(sce, MGR, "high", "init-high-pred", resultsManager, highPredMgrModel, exportMST);

      // 4 - high-pred [LP][REP][init-low]
      GRBModel highPredRepModel = runCustomLP(sce, REP, "high-pred", "init-low", resultsManager, initModel, exportMST);
      // 4 - high [LP][REP][init-high-pred]
      runCustomLP(sce, REP, "high", "init-high-pred", resultsManager, highPredRepModel, exportMST);

      // 5 - high-pred [LP][MGR-REP][init-low]
      GRBModel highPredMgrRepModel = runCustomLP(sce, MGR_REP, "high-pred", "init-low", resultsManager, initModel,
            exportMST);
      // 5 - high traffic [LP][MGR-REP][init-high-pred]
      runCustomLP(sce, MGR_REP, "high", "init-high-pred", resultsManager, highPredMgrRepModel, exportMST);
   }

   public static void runCustomSequenceAlg(Scenario sce, ResultsManager resultsManager, VariablesAlg initVars) {

      VariablesAlg highPredVars;
      boolean exportMST = false;

      // 2 - high [FF][MGR_REP][init-low]
      runCustomAlg(sce, FF, MGR_REP, "high", "init-low", resultsManager, initVars, exportMST);
      // 2 - high [RF][MGR_REP][init-low]
      for (int i = 0; i < 10; i++)
         runCustomAlg(sce, RF, MGR_REP, "high", "init-low_" + i, resultsManager, initVars, exportMST);
      // 2 - high [GRD][MGR_REP][init-low]
      runCustomAlg(sce, GRD, MGR_REP, "high", "init-low", resultsManager, initVars, exportMST);

      // 3 - high-pred [FF][MGR_REP][init-low]
      highPredVars = runCustomAlg(sce, FF, MGR_REP, "high-pred", "init-low", resultsManager, initVars, exportMST);
      // 3 - high [FF][MGR_REP][init-high-pred]
      highPredVars = new VariablesAlg(pm, highPredVars);
      runCustomAlg(sce, FF, MGR_REP, "high", "init-high-pred", resultsManager, highPredVars, exportMST);
      for (int i = 0; i < 10; i++) {
         // 3 - high-pred [RF][MGR_REP][init-low]
         highPredVars = runCustomAlg(sce, RF, MGR_REP, "high-pred", "init-low_" + i, resultsManager, initVars,
               exportMST);
         // 3 - high [RF][MGR_REP][init-high-pred]
         highPredVars = new VariablesAlg(pm, highPredVars);
         runCustomAlg(sce, RF, MGR_REP, "high", "init-high-pred_" + i, resultsManager, highPredVars, exportMST);
      }
      // 3 - high-pred [GRD][MGR_REP][init-low]
      highPredVars = runCustomAlg(sce, GRD, MGR_REP, "high-pred", "init-low", resultsManager, initVars, exportMST);
      // 3 - high [GRD][MGR_REP][init-high-pred]
      highPredVars = new VariablesAlg(pm, highPredVars);
      runCustomAlg(sce, GRD, MGR_REP, "high", "init-high-pred", resultsManager, highPredVars, exportMST);
   }

   public static GRBModel runCustomLP(Scenario sce, String objFunc, String inputFileExtension,
         String outputFileExtension, ResultsManager resultsManager, GRBModel initPlacementModel, boolean exportMST)
         throws GRBException {
      readInputParameters(sce.getInputFileName() + "_" + inputFileExtension, false);
      sce.setObjFunc(objFunc);
      String outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_" + outputFileExtension;
      return LauncherLP.run(pm, sce, resultsManager, initPlacementModel, null, outputFileName, exportMST);
   }

   public static VariablesAlg runCustomAlg(Scenario sce, String alg, String objFunc, String inputFileExtension,
         String outputFileExtension, ResultsManager resultsManager, VariablesAlg initPlacementVars, boolean exportMST) {
      readInputParameters(sce.getInputFileName() + "_" + inputFileExtension, false);
      sce.setName(alg);
      sce.setObjFunc(objFunc);
      String outputFileName = pm.getGraphName() + "_" + alg + "_" + sce.getObjFunc() + "_" + outputFileExtension;
      return LauncherAlg.run(pm, sce, resultsManager, initPlacementVars, outputFileName, exportMST);
   }

   public static void terminate() {
      interrupted = true;
   }

   public static void reset() {
      interrupted = false;
   }

   public static boolean isInterrupted() {
      return interrupted;
   }
}
