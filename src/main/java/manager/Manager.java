package manager;

import gui.WebClient;
import gui.WebServer;
import gui.elements.Scenario;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import learning.LearningModel;
import lp.Constraints;
import lp.OptimizationModel;
import lp.Variables;
import org.apache.commons.io.FilenameUtils;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import output.Results;
import output.ResultsManager;
import utils.ConfigFiles;
import utils.GraphManager;
import utils.KShortestPathGenerator;

import static output.Auxiliary.*;
import static output.Definitions.*;

public class Manager {

   private static final Logger log = LoggerFactory.getLogger(Manager.class);
   private static Parameters pm;
   private static boolean interrupted;

   private static String getResourcePath(String fileName) {
      try {
         String path = FilenameUtils.getPath(Manager.class.getClassLoader()
                 .getResource("scenarios/" + fileName + ".yml").getFile());
         if (System.getProperty("os.name").equals("Mac OS X") || System.getProperty("os.name").equals("Linux"))
            path = "/" + path;
         return path;
      } catch (Exception e) {
         printLog(log, ERROR, "input file not found");
         return null;
      }
   }

   private static void readParameters(String pathFile, String fileName) {
      try {
         printLog(log, INFO, "loading topology");
         pm = ConfigFiles.readParameters(pathFile, fileName + ".yml");
         pm.initialize(pathFile);
         checkTopologyScale();
         WebServer.initialize(pm);
         printLog(log, INFO, "topology loaded");
      } catch (Exception e) {
         printLog(log, ERROR, "input parameters");
      }
   }

   private static void checkTopologyScale() {
      double scalingX = (double) pm.getAux("scaling_x");
      double scalingY = (double) pm.getAux("scaling_y");
      if (scalingX != 1.0 || scalingY != 1.0) {
         for (Node node : pm.getNodes()) {
            int value = (int) Math.round((int) node.getAttribute("y") * scalingY);
            node.setAttribute("y", value);
         }
         for (Node node : pm.getNodes()) {
            int value = (int) Math.round((int) node.getAttribute("x") * scalingX);
            node.setAttribute("x", value);
         }
      }
   }

   public static void loadTopology(String fileName) {
      String path = getResourcePath(fileName);
      if (path != null)
         readParameters(path, fileName);
   }

   public static GRBModel start(Scenario scen, GRBModel initialModel) {
      try {
         interrupted = false;
         GRBModel importedModel = ResultsManager.importModel(getResourcePath(scen.getInputFileName()), scen.getInputFileName(), pm);
         if (initialModel == null && importedModel != null)
            initialModel = importedModel;
         ResultsManager resultsManager = new ResultsManager(pm.getScenario());
         printLog(log, INFO, "initializing model");
         switch (scen.getModel()) {
            case ALL_OPT_MODELS_STRING:
               initialModel = runLP(ALL_OPT_MODELS[0], scen, resultsManager, null);
               for (int i = 1; i < ALL_OPT_MODELS.length; i++)
                  runLP(ALL_OPT_MODELS[i], scen, resultsManager, initialModel);
               break;
            case MIGRATION_REPLICATION_RL_MODEL:
               initialModel = runLP(INITIAL_PLACEMENT_MODEL, scen, resultsManager, null);
               GRBModel tmpModel = runLP(MIGRATION_REPLICATION_MODEL, scen, resultsManager, initialModel);
//               runRL(MIGRATION_REPLICATION_RL_MODEL, scenario, results.getObjVal(), resultsManager, initialModel);
               break;
            case INITIAL_PLACEMENT_MODEL:
               initialModel = runLP(INITIAL_PLACEMENT_MODEL, scen, resultsManager, null);
               break;
            default:
               runLP(scen.getModel(), scen, resultsManager, initialModel);
               break;
         }
         printLog(log, INFO, "ready");
      } catch (Exception e) {
         e.printStackTrace();
         printLog(log, ERROR, "something went wrong");
      }
      return initialModel;
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

   private static GRBModel runLP(String modelName, Scenario scenario, ResultsManager resultsManager, GRBModel initialModel) throws GRBException {
      GRBLinExpr expr;
      OptimizationModel model = new OptimizationModel(pm);
      printLog(log, INFO, "setting variables");
      Variables variables = new Variables(pm, model.getGrbModel());
      model.setVariables(variables);
      printLog(log, INFO, "setting constraints");
      new Constraints(pm, model, scenario, initialModel);
      expr = generateExprForObjectiveFunction(model, scenario);
      model.setObjectiveFunction(expr, scenario.isMaximization());
      printLog(log, INFO, "running model");
      double objVal = model.run();
      Results results;
      if (objVal != -1) {
         String resultsFileName = pm.getScenario() + "_" + modelName;
         results = generateResults(model, scenario, initialModel);
         resultsManager.exportJsonFile(resultsFileName, results);
         resultsManager.exportTextFile(resultsFileName, results);
         if (modelName.equals(INITIAL_PLACEMENT_MODEL))
            resultsManager.exportModel(model.getGrbModel(), scenario.getInputFileName());
         WebClient.updateResultsToWebApp(results);
      }
      return model.getGrbModel();
   }

   private static Results runRL(String model, Scenario scenario, double objValue, ResultsManager resultsManager, Results initialPlacement) throws GRBException {
      LearningModel learningModel = new LearningModel(pm);
      double objVal = learningModel.run(initialPlacement, objValue);
      Results results = new Results(pm, scenario);
//        output.setLearningModelResults(learningModel);
//      generateResults(results, model, objVal, resultsManager);
      return results;
   }

   private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel model, Scenario scenario) throws GRBException {
      GRBLinExpr expr = new GRBLinExpr();
      String[] weights = scenario.getWeights().split("-");
      double weightLinks = Double.valueOf(weights[0]) / pm.getLinks().size();
      double weightServers = Double.valueOf(weights[1]) / pm.getServers().size();
      double weightServiceDelays = Double.valueOf(weights[2]);
      switch (scenario.getObjectiveFunction()) {
         case NUM_OF_SERVERS_OBJ:
            expr.add(model.usedServersExpr());
            break;
         case COSTS_OBJ:
            expr.add(model.linkCostsExpr(weightLinks));
            expr.add(model.serverCostsExpr(weightServers));
            expr.add(model.serviceDelayExpr(weightServiceDelays));
            break;
         case UTILIZATION_OBJ:
            expr.add(model.linkUtilizationExpr(weightLinks));
            expr.add(model.serverUtilizationExpr(weightServers));
            break;
      }
      return expr;
   }

   private static Results generateResults(OptimizationModel model, Scenario scenario, GRBModel initialModel) throws GRBException {
      Results results = new Results(pm, scenario);
      // primary variables
      results.setVariable(rSP, model.getVariables().rSP);
      results.setVariable(rSPD, model.getVariables().rSPD);
      results.setVariable(pXSV, model.getVariables().pXSV);
      results.setVariable(pXSVD, model.getVariables().pXSVD);
      results.setVariable(uL, model.getVariables().uL);
      results.setVariable(uX, model.getVariables().uX);
      // secondary
      results.setVariable(pX, model.getVariables().pX);
      results.setVariable(gSVXY, model.getVariables().gSVXY);
      results.setVariable(sSVP, model.getVariables().sSVP);
      results.setVariable(dS, model.getVariables().dS);
      results.setVariable(dSPX, model.getVariables().dSPX);
      results.prepareVariablesForJsonFile(model.getObjVal(), initialModel);
      return results;
   }

   public static boolean isInterrupted() {
      return interrupted;
   }
}
