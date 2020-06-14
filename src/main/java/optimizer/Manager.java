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

   public static void readInputParameters(String graphNameForm) {
      String graphName = graphNameForm;
      String extensionGraph = ".dgs";
      String[] graphNameExtension = graphNameForm.split("\\.");
      if (graphNameExtension.length > 1) {
         graphName = graphNameExtension[0];
         extensionGraph = "." + graphNameExtension[1];
      }
      try {
         String path = Auxiliary.getResourcePath(graphName + ".yml");
         pm = ConfigFiles.readParameters(path + graphName + ".yml");
         pm.initialize(path + graphName + extensionGraph, path + graphName + ".txt",
               (boolean) pm.getAux(DIRECTED_EDGES));
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
               node.setAttribute(longitudeLabel, (double) node.getAttribute(longitudeLabel + "_gui") * scalingX);
               node.setAttribute(latitudeLabel, (double) node.getAttribute(latitudeLabel + "_gui") * scalingY);
            } else {
               node.setAttribute(longitudeLabel, (double) node.getAttribute(longitudeLabel) * scalingX);
               node.setAttribute(latitudeLabel, (double) node.getAttribute(latitudeLabel) * scalingY);
            }
         }
      }
   }

   private static void specifyUsedTrafficDemands(Parameters pm, boolean isLowLoad) {
      Random rnd = new Random(seed);
      for (TrafficFlow trafficFlow : pm.getTrafficFlows()) {
         trafficFlow.getAux().clear();
         for (int d = 0; d < trafficFlow.getDemands().size(); d++)
            trafficFlow.getAux().add(true);
         if (isLowLoad) {
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
         ResultsManager resultsManager = new ResultsManager(pm.getGraphName());
         String pathFile = Auxiliary.getResourcePath(pm.getGraphName() + "_" + INIT_LP + ".mst");
         GRBModel initModel = resultsManager.loadInitialPlacement(pathFile + pm.getGraphName() + "_" + INIT_LP, pm,
               sce);
         boolean isLowLoad = false;
         switch (sce.getAlgorithm()) {
            case INITHEU_FF_10RF_GRD:
               // 1. initial placement LP
               sce.setAlgorithm(INTI_GRD);
               specifyUsedTrafficDemands(pm, true);
               VariablesAlg vars = new VariablesAlg(pm, null, sce.getObjFunc());
               LauncherAlg.run(pm, sce, resultsManager, vars, -1, false);
               VariablesAlg varsInitPlacement = new VariablesAlg(pm, vars);
               // 2. ff
               sce.setAlgorithm(FF);
               specifyUsedTrafficDemands(pm, false);
               vars = new VariablesAlg(pm, varsInitPlacement, sce.getObjFunc());
               LauncherAlg.run(pm, sce, resultsManager, vars, -1, false);
               // 3. rf
               sce.setAlgorithm(RF);
               for (int i = 0; i < 10; i++) {
                  vars = new VariablesAlg(pm, varsInitPlacement, sce.getObjFunc());
                  LauncherAlg.run(pm, sce, resultsManager, vars, i, false);
               }
               // 4. grd
               sce.setAlgorithm(GRD);
               vars = new VariablesAlg(pm, varsInitPlacement, sce.getObjFunc());
               LauncherAlg.run(pm, sce, resultsManager, vars, -1, false);
               break;
            case INITLP_FF_10RF_GRD_LP:
               if (initModel == null) {
                  // 1. initial placement LP
                  sce.setAlgorithm(INIT_LP);
                  specifyUsedTrafficDemands(pm, true);
                  initModel = LauncherLP.run(pm, sce, resultsManager, null, null);
               }
               varsInitPlacement = new VariablesAlg(pm, initModel);
               // 2. ff
               sce.setAlgorithm(FF);
               specifyUsedTrafficDemands(pm, false);
               LauncherAlg.run(pm, sce, resultsManager, varsInitPlacement, -1, false);
               // 3. rf
               sce.setAlgorithm(RF);
               for (int i = 0; i < 10; i++)
                  LauncherAlg.run(pm, sce, resultsManager, varsInitPlacement, i, false);
               // 4. grd
               sce.setAlgorithm(GRD);
               LauncherAlg.run(pm, sce, resultsManager, varsInitPlacement, -1, true);
               // 5. lp
               pathFile = resultsManager.getResultsFolder() + "/" + pm.getGraphName() + "_" + GRD + "_"
                     + sce.getObjFunc();
               GRBModel initSol = resultsManager.loadModel(pathFile, pm, sce, false);
               sce.setAlgorithm(LP);
               LauncherLP.run(pm, sce, resultsManager, initModel, initSol);
               break;
            case INIT_LP:
               isLowLoad = true;
            case DIMEN:
            case LP:
               specifyUsedTrafficDemands(pm, isLowLoad);
               pathFile = Auxiliary.getResourcePath(pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc() + ".mst");
               initSol = resultsManager.loadModel(pathFile + pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc(), pm,
                     sce, false);
               // make sure no initial model is loaded when launching initial placement
               if (sce.getAlgorithm().equals(INIT_LP))
                  initModel = null;
               LauncherLP.run(pm, sce, resultsManager, initModel, initSol);
               break;
            case FF:
            case RF:
            case GRD:
               specifyUsedTrafficDemands(pm, false);
               varsInitPlacement = new VariablesAlg(pm, initModel);
               LauncherAlg.run(pm, sce, resultsManager, varsInitPlacement, -1, false);
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

   public static void stop() {
      interrupted = true;
   }

   public static boolean isInterrupted() {
      return interrupted;
   }
}
