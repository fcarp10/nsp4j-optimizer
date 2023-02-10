package optimizer;

import java.util.Random;

import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import optimizer.algorithms.LauncherAlg;
import optimizer.elements.TrafficFlow;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.lp.LauncherLP;
import optimizer.results.Auxiliary;
import optimizer.results.ResultsManager;
import optimizer.scenarios.Journal;
import optimizer.utils.ConfigFiles;
import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class Manager {

   private static final Logger log = LoggerFactory.getLogger(Manager.class);
   private static boolean interrupted;
   private static Parameters pm;

   public static String readParameters(String graphNameForm) {
      String path = Auxiliary.getResourcesPath(graphNameForm + ".yml");
      String graphName = readYamlFile(path, graphNameForm);
      readTopologyFiles(path, graphName);
      determineUsedDemands(pm, false);
      return graphName;
   }

   public static String readYamlFile(String path, String graphNameForm) {
      String[] graphName = graphNameForm.split("_");
      try {
         pm = ConfigFiles.readParameters(path + graphNameForm + ".yml");
      } catch (Exception e) {
         printLog(log, ERROR, "error loading .yml file");
      }
      return graphName[0];
   }

   public static void readTopologyFiles(String path, String graphName) {
      String[] extensions = new String[] { ".dgs", ".gml" };
      boolean isLoaded = false;
      for (int i = 0; i < extensions.length; i++) {

         if (pm.initialize(path + graphName + extensions[i], path + graphName + ".txt",
               (boolean) pm.getGlobal(DIRECTED_EDGES))) {
            isLoaded = true;
            break;
         }
         printLog(log, WARNING, graphName + extensions[i] + " file not found");
      }
      if (!isLoaded) {
         printLog(log, ERROR, "error loading graph or paths files");
      }
      checkTopologyScale(pm);
      ResultsGUI.initialize(pm);
      printLog(log, INFO, "topology loaded");
   }

   private static void checkTopologyScale(Parameters pm) {
      double scalingX = (double) pm.getGlobal(X_SCALING);
      double scalingY = (double) pm.getGlobal(Y_SCALING);
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
            if ((int) node.getAttribute(NODE_TYPE) == NODE_TYPE_CLOUD
                  && node.getAttribute(longitudeLabel + "_gui") != null) {
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

   public static void determineUsedDemands(Parameters pm, boolean considerSubset) {
      Random rnd = new Random(pm.getSeed());
      for (TrafficFlow trafficFlow : pm.getTrafficFlows()) {
         trafficFlow.getAux().clear();
         for (int d = 0; d < trafficFlow.getDemands().size(); d++)
            trafficFlow.getAux().add(true);
         if (considerSubset) {
            double initialTrafficLoad = (double) pm.getGlobal().get(INITIAL_TRAFFIC_LOAD);
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
      String outputFileName;
      if (sce.getName().equals(LP) || sce.getName().equals(FF) || sce.getName().equals(RF)
            || sce.getName().equals(GRD)) {
         try {
            switch (sce.getName()) {
               case LP:
                  readParameters(sce.getInputFileName());
                  rm = new ResultsManager(pm.getGraphName());
                  outputFileName = pm.getGraphName() + sce.getName() + sce.getObjFunc();
                  LauncherLP.run(pm, sce, rm, null, null, outputFileName, true);
                  break;

               case FF:
                  readParameters(sce.getInputFileName());
                  rm = new ResultsManager(pm.getGraphName());
                  outputFileName = pm.getGraphName() + "_" + FF + "_" + sce.getObjFunc();
                  LauncherAlg.run(pm, sce, rm, null, outputFileName, false);
                  break;

               case RF:
                  readParameters(sce.getInputFileName());
                  rm = new ResultsManager(pm.getGraphName());
                  for (int i = 0; i < 10; i++) {
                     outputFileName = pm.getGraphName() + "_" + RF + "_" + sce.getObjFunc() + "_" + i;
                     LauncherAlg.run(pm, sce, rm, null, outputFileName, false);
                  }
                  break;

               case GRD:
                  readParameters(sce.getInputFileName());
                  rm = new ResultsManager(pm.getGraphName());
                  outputFileName = pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc();
                  LauncherAlg.run(pm, sce, rm, null, outputFileName, false);
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
      } else {
         Journal.run(pm, sce);
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
