package optimizer;

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

      String[] graphName = graphNameForm.split("_");
      String extensionGraph = ".dgs";
      String[] graphNameExtension = graphNameForm.split("\\.");
      if (graphNameExtension.length > 1) {
         graphName[0] = graphNameExtension[0];
         extensionGraph = "." + graphNameExtension[1];
      }
      try {
         String path = Auxiliary.getResourcePath(graphNameForm + ".yml");
         pm = ConfigFiles.readParameters(path + graphNameForm + ".yml");
         pm.initialize(path + graphName[0] + extensionGraph, path + graphName[0] + ".txt",
               (boolean) pm.getAux(DIRECTED_EDGES));
         checkTopologyScale(pm);
         ResultsGUI.initialize(pm);
         printLog(log, INFO, "topology loaded");
         seed = pm.getSeed();
      } catch (Exception e) {
         printLog(log, ERROR, "error loading input parameters");
      }
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
               resultsManager = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               String outputFileName = pm.getGraphName() + sce.getName() + sce.getObjFunc();
               LauncherLP.run(pm, sce, resultsManager, null, null, outputFileName);
               break;

            case FF:
               resultsManager = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               outputFileName = pm.getGraphName() + "_" + FF + "_" + sce.getObjFunc();
               LauncherAlg.run(pm, sce, resultsManager, null, outputFileName);
               break;

            case RF:
               resultsManager = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               for (int i = 0; i < 10; i++) {
                  outputFileName = pm.getGraphName() + "_" + RF + "_" + sce.getObjFunc() + "_" + i;
                  LauncherAlg.run(pm, sce, resultsManager, null, outputFileName);
               }
               break;

            case GRD:
               resultsManager = new ResultsManager(pm.getGraphName());
               readInputParameters(sce.getInputFileName(), false);
               outputFileName = pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc();
               LauncherAlg.run(pm, sce, resultsManager, null, outputFileName);
               break;

            case CUSTOM_1:
               resultsManager = new ResultsManager(pm.getGraphName());
               graphNameShort = readInputParameters(sce.getInputFileName(), false);
               GRBModel initModel = resultsManager.loadInitialPlacement(
                     Auxiliary.getResourcePath(graphNameShort + "_init-lp.mst") + graphNameShort + "_init-lp", pm, sce);
               GRBModel initSol = resultsManager.loadModel(
                     Auxiliary.getResourcePath(pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc() + ".mst")
                           + pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc(),
                     pm, sce, false);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc();
               LauncherLP.run(pm, sce, resultsManager, initModel, initSol, outputFileName);
               break;

            case CUSTOM_2:
               resultsManager = new ResultsManager(sce.getInputFileName());

               /************* 1. low traffic ***************/
               readInputParameters(sce.getInputFileName() + "_low", false);
               sce.setObjFunc(NUM_SERVERS);
               sce.setConstraint(EDGE_ONLY, true);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc();
               GRBModel lowModel = LauncherLP.run(pm, sce, resultsManager, null, null, outputFileName);
               sce.setConstraint(EDGE_ONLY, false);
               /*******************************************/

               /********** 2.1 high traffic (MGR) *********/
               readInputParameters(sce.getInputFileName() + "_high", false);
               sce.setObjFunc(MGR);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_init-low";
               LauncherLP.run(pm, sce, resultsManager, lowModel, null, outputFileName);
               /*******************************************/

               /********* 2.2 high traffic (REP) **********/
               readInputParameters(sce.getInputFileName() + "_high", false);
               sce.setObjFunc(REP);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_init-low";
               LauncherLP.run(pm, sce, resultsManager, lowModel, null, outputFileName);
               /*******************************************/

               /********* 2.3 high traffic (MGR-REP) **********/
               readInputParameters(sce.getInputFileName() + "_high", false);
               sce.setObjFunc(MGR_REP);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_init-low";
               LauncherLP.run(pm, sce, resultsManager, lowModel, null, outputFileName);
               /*******************************************/

               /******* 3.1.1 high-pred traffic (MGR) *****/
               readInputParameters(sce.getInputFileName() + "_high-pred", false);
               sce.setObjFunc(MGR);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_init-low";
               GRBModel highPredMgrModel = LauncherLP.run(pm, sce, resultsManager, lowModel, null, outputFileName);
               /*******************************************/

               /******** 3.1.2 high traffic (MGR) *********/
               readInputParameters(sce.getInputFileName() + "_high", false);
               sce.setObjFunc(MGR);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_init-high-pred";
               LauncherLP.run(pm, sce, resultsManager, highPredMgrModel, null, outputFileName);
               /*******************************************/

               /******* 3.2.1 high-pred traffic (REP) *****/
               readInputParameters(sce.getInputFileName() + "_high-pred", false);
               sce.setObjFunc(REP);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_init-low";
               GRBModel highPredRepModel = LauncherLP.run(pm, sce, resultsManager, lowModel, null, outputFileName);
               /*******************************************/

               /******** 3.2.2 high traffic (REP) *********/
               readInputParameters(sce.getInputFileName() + "_high", false);
               sce.setObjFunc(REP);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_init-high-pred";
               LauncherLP.run(pm, sce, resultsManager, highPredRepModel, null, outputFileName);
               /*******************************************/

               /******* 3.3.1 high-pred traffic (MGR-REP) *****/
               readInputParameters(sce.getInputFileName() + "_high-pred", false);
               sce.setObjFunc(MGR_REP);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_init-low";
               GRBModel highPredMgrRepModel = LauncherLP.run(pm, sce, resultsManager, lowModel, null, outputFileName);
               /*******************************************/

               /******** 3.3.2 high traffic (MGR-REP) *********/
               readInputParameters(sce.getInputFileName() + "_high", false);
               sce.setObjFunc(MGR_REP);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_init-high-pred";
               LauncherLP.run(pm, sce, resultsManager, highPredMgrRepModel, null, outputFileName);
               /*******************************************/

               break;

            case CUSTOM_3:
               resultsManager = new ResultsManager(sce.getInputFileName());

               /************* 1. low traffic ***************/
               readInputParameters(sce.getInputFileName() + "_low", false);
               sce.setObjFunc(NUM_SERVERS);
               sce.setConstraint(EDGE_ONLY, true);
               outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc();
               lowModel = LauncherLP.run(pm, sce, resultsManager, null, null, outputFileName);
               sce.setConstraint(EDGE_ONLY, false);
               /*******************************************/

               /*********** 2.1 high traffic ************/
               /******************** FF ******************/
               readInputParameters(sce.getInputFileName() + "_high", false);
               String algorithm = FF;
               sce.setName(algorithm);
               sce.setObjFunc(MGR);
               VariablesAlg initModelVars = new VariablesAlg(pm, lowModel);
               outputFileName = pm.getGraphName() + "_" + algorithm + "_init-low";
               LauncherAlg.run(pm, sce, resultsManager, initModelVars, outputFileName);

               /******************** RF ******************/
               readInputParameters(sce.getInputFileName() + "_high", false);
               algorithm = RF;
               sce.setName(algorithm);
               sce.setObjFunc(MGR);
               initModelVars = new VariablesAlg(pm, lowModel);
               for (int i = 0; i < 10; i++) {
                  outputFileName = pm.getGraphName() + "_" + algorithm + "_init-low_" + i;
                  LauncherAlg.run(pm, sce, resultsManager, initModelVars, outputFileName);
               }

               /******************* GRD *****************/
               readInputParameters(sce.getInputFileName() + "_high", false);
               algorithm = GRD;
               sce.setName(algorithm);
               sce.setObjFunc(MGR);
               initModelVars = new VariablesAlg(pm, lowModel);
               outputFileName = pm.getGraphName() + "_" + algorithm + "_init-low";
               LauncherAlg.run(pm, sce, resultsManager, initModelVars, outputFileName);
               /*****************************************/

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
