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
               (boolean) pm.getAux(DIRECTED_EDGES), (boolean) pm.getAux(ALL_NODES_TO_CLOUD))) {
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
      Random rnd = new Random(pm.getSeed());
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
      ResultsManager rm;
      String graphNameShort;
      try {
         switch (sce.getName()) {
            case LP:
               boolean exportMST = true;
               rm = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               String outputFileName = pm.getGraphName() + sce.getName() + sce.getObjFunc();
               LauncherLP.run(pm, sce, rm, null, null, outputFileName, exportMST);
               break;

            case FF:
               rm = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               outputFileName = pm.getGraphName() + "_" + FF + "_" + sce.getObjFunc();
               LauncherAlg.run(pm, sce, rm, null, outputFileName, false);
               break;

            case RF:
               rm = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               for (int i = 0; i < 10; i++) {
                  outputFileName = pm.getGraphName() + "_" + RF + "_" + sce.getObjFunc() + "_" + i;
                  LauncherAlg.run(pm, sce, rm, null, outputFileName, false);
               }
               break;

            case GRD:
               rm = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               outputFileName = pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc();
               LauncherAlg.run(pm, sce, rm, null, outputFileName, false);
               break;

            case CUSTOM_1:
               exportMST = true;
               rm = new ResultsManager(pm.getGraphName());
               graphNameShort = readInputParameters(sce.getInputFileName(), false);
               GRBModel initModel = rm.loadInitialPlacement(
                     Auxiliary.getResourcesPath(graphNameShort + "_init-lp.mst", null) + graphNameShort + "_init-lp",
                     pm, sce);
               GRBModel initSol = rm.loadModel(
                     Auxiliary.getResourcesPath(pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc() + ".mst", null)
                           + pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc(),
                     pm, sce, false);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc();
               LauncherLP.run(pm, sce, rm, initModel, initSol, outputFileName, exportMST);
               break;

            case CUSTOM_2:
               rm = new ResultsManager(sce.getInputFileName());
               exportMST = false;
               boolean edgeOnly;

               edgeOnly = true;
               // 1 - init-low [LP][MGR-REP][null]
               GRBModel initLowLP = runCustomLP(sce, NUM_SERVERS, LOW, NULL_STRING, rm, null, exportMST, edgeOnly);
               VariablesAlg initLowLPAlg = new VariablesAlg(pm, initLowLP);
               // 2 - init-high-pred [LP][MGR-REP][null]
               GRBModel initHighPredLP = runCustomLP(sce, NUM_SERVERS, HIGH_PRED, NULL_STRING, rm, null, exportMST,
                     edgeOnly);
               VariablesAlg initHighPredLPAlg = new VariablesAlg(pm, initHighPredLP);
               edgeOnly = false;

               // 3 - high [LP][MGR/REP/MGR-REP][init-low]
               runCustomLP(sce, MGR, HIGH, INIT_LOW, rm, initLowLP, exportMST, edgeOnly);
               runCustomLP(sce, REP, HIGH, INIT_LOW, rm, initLowLP, exportMST, edgeOnly);
               runCustomLP(sce, MGR_REP, HIGH, INIT_LOW, rm, initLowLP, exportMST, edgeOnly);
               // 3 - high [LP][MGR/REP/MGR-REP][init-high-pred]
               runCustomLP(sce, MGR, HIGH, INIT_HIGH_PRED, rm, initHighPredLP, exportMST, edgeOnly);
               runCustomLP(sce, REP, HIGH, INIT_HIGH_PRED, rm, initHighPredLP, exportMST, edgeOnly);
               runCustomLP(sce, MGR_REP, HIGH, INIT_HIGH_PRED, rm, initHighPredLP, exportMST, edgeOnly);

               // 4 - high [FF][MGR-REP][init-low]
               runCustomAlg(sce, FF, MGR_REP, HIGH, INIT_LOW, rm, initLowLPAlg, exportMST, edgeOnly);
               // 4 - high [FF][MGR-REP][init-high-pred]
               runCustomAlg(sce, FF, MGR_REP, HIGH, INIT_HIGH_PRED, rm, initHighPredLPAlg, exportMST, edgeOnly);

               // 5 - high [RF][MGR-REP][init-low]
               for (int i = 0; i < 10; i++)
                  runCustomAlg(sce, RF, MGR_REP, HIGH, INIT_LOW + "_" + i, rm, initLowLPAlg, exportMST, edgeOnly);
               // 5 - high [RF][MGR-REP][init-high-pred]
               for (int i = 0; i < 10; i++)
                  runCustomAlg(sce, RF, MGR_REP, HIGH, INIT_HIGH_PRED + "_" + i, rm, initHighPredLPAlg, exportMST,
                        edgeOnly);

               // 6 - high [GRD][MGR-REP][init-low]
               runCustomAlg(sce, GRD, MGR_REP, HIGH, INIT_LOW, rm, initLowLPAlg, exportMST, edgeOnly);
               // 6 - high [GRD][MGR-REP][init-high-pred]
               runCustomAlg(sce, GRD, MGR_REP, HIGH, INIT_HIGH_PRED, rm, initHighPredLPAlg, exportMST, edgeOnly);

               break;

            case CUSTOM_3:
               rm = new ResultsManager(sce.getInputFileName());
               exportMST = false;

               edgeOnly = true;
               // 1 - init-low [GRD][MGR-REP][null]
               initLowLPAlg = runCustomAlg(sce, GRD, MGR_REP, LOW, NULL_STRING, rm, null, exportMST, edgeOnly);
               // 2 - init-high-pred [GRD][MGR-REP][null]
               initHighPredLPAlg = runCustomAlg(sce, GRD, MGR_REP, HIGH_PRED, NULL_STRING, rm, null, exportMST,
                     edgeOnly);
               edgeOnly = false;

               // 4 - high [FF][MGR-REP][init-low]
               runCustomAlg(sce, FF, MGR_REP, HIGH, INIT_LOW, rm, initLowLPAlg, exportMST, edgeOnly);
               // 4 - high [FF][MGR-REP][init-high-pred]
               runCustomAlg(sce, FF, MGR_REP, HIGH, INIT_HIGH_PRED, rm, initHighPredLPAlg, exportMST, edgeOnly);

               // 5 - high [RF][MGR-REP][init-low]
               for (int i = 0; i < 10; i++)
                  runCustomAlg(sce, RF, MGR_REP, HIGH, INIT_LOW + "_" + i, rm, initLowLPAlg, exportMST, edgeOnly);
               // 5 - high [RF][MGR-REP][init-high-pred]
               for (int i = 0; i < 10; i++)
                  runCustomAlg(sce, RF, MGR_REP, HIGH, INIT_HIGH_PRED + "_" + i, rm, initHighPredLPAlg, exportMST,
                        edgeOnly);

               // 6 - high [GRD][MGR-REP][init-low]
               runCustomAlg(sce, GRD, MGR_REP, HIGH, INIT_LOW, rm, initLowLPAlg, exportMST, edgeOnly);
               // 6 - high [GRD][MGR-REP][init-high-pred]
               runCustomAlg(sce, GRD, MGR_REP, HIGH, INIT_HIGH_PRED, rm, initHighPredLPAlg, exportMST, edgeOnly);

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

   public static GRBModel runCustomLP(Scenario sce, String objFunc, String inputFileExtension,
         String outputFileExtension, ResultsManager resultsManager, GRBModel initPlacementModel, boolean exportMST,
         boolean edgeOnly) throws GRBException {
      readInputParameters(sce.getInputFileName() + "_" + inputFileExtension, false);
      sce.setObjFunc(objFunc);
      if (edgeOnly) {
         sce.setConstraint(EDGE_ONLY, true);
         Auxiliary.removeCapacityOfCloudLinks(pm);
      } else {
         sce.setConstraint(EDGE_ONLY, false);
         Auxiliary.restoreCapacityOfCloudLinks(pm);
      }
      String outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_" + outputFileExtension;
      return LauncherLP.run(pm, sce, resultsManager, initPlacementModel, null, outputFileName, exportMST);
   }

   public static VariablesAlg runCustomAlg(Scenario sce, String alg, String objFunc, String inputFileExtension,
         String outputFileExtension, ResultsManager resultsManager, VariablesAlg initPlacementVars, boolean exportMST,
         boolean edgeOnly) {
      readInputParameters(sce.getInputFileName() + "_" + inputFileExtension, false);
      sce.setName(alg);
      sce.setObjFunc(objFunc);
      if (edgeOnly) {
         Auxiliary.removeCapacityOfCloudServers(pm); // constraint to only edge
         Auxiliary.removeCapacityOfCloudLinks(pm);
      } else {
         Auxiliary.restoreCapacityOfCloudServers(pm); // remove constraint to only edge
         Auxiliary.restoreCapacityOfCloudLinks(pm);
      }
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
