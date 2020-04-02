package optimizer;

import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.TrafficFlow;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.heuristic.LauncherHeu;
import optimizer.lp.LauncherLP;
import optimizer.results.ResultsManager;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConfigFiles;
import utils.GraphManager;
import utils.KShortestPathGenerator;

import java.io.File;
import java.util.Random;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class Manager {

   private static final Logger log = LoggerFactory.getLogger(Manager.class);
   private static boolean interrupted;
   private static Parameters pm;

   public static void readInputParameters(String fileName) {
      String path = getResourcePath(fileName);
      if (path != null)
         try {
            pm = ConfigFiles.readParameters(path, fileName + ".yml");
            pm.initialize(path);
            checkTopologyScale(pm);
            ResultsGUI.initialize(pm);
            printLog(log, INFO, "topology loaded");

         } catch (Exception e) {
            printLog(log, ERROR, "input parameters");
         }
   }

   private static String getResourcePath(String fileName) {
      try {
         File file = new File(Manager.class.getClassLoader()
                 .getResource("scenarios/" + fileName + ".yml").getFile());
         String absolutePath = file.getAbsolutePath();
         String path = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
         if (System.getProperty("os.name").equals("Mac OS X") || System.getProperty("os.name").equals("Linux"))
            path = path + "/";
         else
            path = path + "\\";
         return path;
      } catch (Exception e) {
         printLog(log, ERROR, "input file not found");
         return null;
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
      Random rnd = new Random(pm.getSeed());
      for (TrafficFlow trafficFlow : pm.getTrafficFlows()) {
         trafficFlow.getAux().clear();
         for (int d = 0; d < trafficFlow.getDemands().size(); d++)
            trafficFlow.getAux().add(true);
         if (sce.getModel().equals(INITIAL_PLACEMENT)) {
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
      try {
         interrupted = false;
         GRBModel initialModel = ResultsManager.importModel(getResourcePath(sce.getInputFileName()), sce.getInputFileName(), pm);
         ResultsManager resultsManager = new ResultsManager(pm.getScenario());
         printLog(log, INFO, "initializing " + sce.getModel());

         specifyUsedTrafficDemands(pm, sce);
         if (sce.getModel().equals(HEURISTIC))
            LauncherHeu.run(pm, sce, resultsManager);
         else
            LauncherLP.run(pm, sce, resultsManager, initialModel);

         printLog(log, INFO, "backend is ready");
      } catch (Exception e) {
         e.printStackTrace();
         printLog(log, ERROR, "something went wrong");
      }
   }

   public static void stop() {
      interrupted = true;
   }

   public static void generatePaths(Scenario scenario) {
      String path = getResourcePath(scenario.getInputFileName());
      if (path != null)
         try {
            Graph graph = GraphManager.importTopology(path, scenario.getInputFileName());
            printLog(log, INFO, "generating paths");
            KShortestPathGenerator k = new KShortestPathGenerator(graph, 10
                    , 5, path, scenario.getInputFileName());
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
