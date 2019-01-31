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
import output.Definitions;
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

   private static String getResourcePath(String fileName) {
      try {
         String path = FilenameUtils.getPath(Manager.class.getClassLoader().getResource("scenarios/" + fileName + ".yml").getFile());
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
         printLog(log, ERROR, "reading the input parameters file");
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

   public static GRBModel start(Scenario sce, GRBModel initialModel) {
      try {
         if (initialModel == null)
            initialModel = ResultsManager.importModel(getResourcePath(sce.getInputFileName()), sce.getInputFileName(), pm);
         ResultsManager resultsManager = new ResultsManager(pm.getScenario());
         printLog(log, INFO, "initializing model");
         switch (sce.getModel()) {
            case INITIAL_PLACEMENT:
               initialModel = runLP(INITIAL_PLACEMENT, sce, sce.getObjectiveFunction(), resultsManager, null);
               break;
            default:
               runLP(sce.getModel(), sce, sce.getObjectiveFunction(), resultsManager, initialModel);
               break;
         }
         printLog(log, INFO, "ready");
      } catch (Exception e) {
         e.printStackTrace();
         printLog(log, ERROR, "something went wrong");
      }
      return initialModel;
   }

   public static void generatePaths(Scenario scenario) {
      String path = getResourcePath(scenario.getInputFileName());
      if (path != null)
         try {
            Graph graph = GraphManager.importTopology(path, scenario.getInputFileName());
            printLog(log, INFO, "generating paths");
            KShortestPathGenerator kShortestPathGenerator = new KShortestPathGenerator(graph, 10, 5, path, scenario.getInputFileName());
            kShortestPathGenerator.run();
            printLog(log, INFO, "paths generated");
         } catch (Exception e) {
            printLog(log, ERROR, "reading the topology file");
         }
   }

   private static GRBModel runLP(String modelName, Scenario scenario, String objectiveFunction, ResultsManager resultsManager, GRBModel initialModel) throws GRBException {
      GRBLinExpr expr;
      OptimizationModel model = new OptimizationModel(pm);
      printLog(log, INFO, "setting variables");
      Variables variables = new Variables(pm, model.getGrbModel());
      model.setVariables(variables);
      printLog(log, INFO, "setting constraints");
      new Constraints(pm, model, scenario, initialModel);
      expr = generateExprForObjectiveFunction(model, scenario, objectiveFunction);
      model.setObjectiveFunction(expr, scenario.isMaximization());
      printLog(log, INFO, "running model");
      double objVal = model.run();
      Results results;
      if (objVal != -1) {
         results = generateResults(model, scenario, initialModel);
         resultsManager.exportJsonFile(pm.getScenario() + "_" + modelName, results);
         if (modelName.equals(INITIAL_PLACEMENT))
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

   private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel model, Scenario scenario, String obj) throws GRBException {
      GRBLinExpr expr = new GRBLinExpr();
      String[] weights = scenario.getWeights().split("-");
      double linksWeight = Double.valueOf(weights[0]) / pm.getLinks().size();
      double serversWeight = Double.valueOf(weights[1]) / pm.getServers().size();
      switch (obj) {
         case NUM_OF_SERVERS_OBJ:
            expr.add(model.usedServersExpr());
            break;
         case COSTS_OBJ:
            expr.add(model.linkCostsExpr(linksWeight));
            expr.add(model.serverCostsExpr(serversWeight));

            break;
         case UTILIZATION_OBJ:
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            break;
         case NUM_DEDICATED_FUNCTIONS_OBJ:
            expr.add(model.numDedicatedFunctionsExpr());
            break;
         case MAX_UTILIZATION_OBJ:
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            expr.add(model.maxUtilizationExpr(Double.valueOf(weights[2])));
            break;
      }
      return expr;
   }

   private static Results generateResults(OptimizationModel model, Scenario scenario, GRBModel initialModel) throws GRBException {
      Results results = new Results(pm, scenario);
      results.setVariable(rSP, model.getVariables().rSP);
      results.setVariable(rSPD, model.getVariables().rSPD);
      results.setVariable(pXSV, model.getVariables().pXSV);
      results.setVariable(pXSVD, model.getVariables().pXSVD);
      results.setVariable(uL, model.getVariables().uL);
      results.setVariable(uX, model.getVariables().uX);
      results.setVariable(pX, model.getVariables().pX);
      results.setVariable(Definitions.dSPD, model.getVariables().dSPD);
      results.setVariable(nXSV, model.getVariables().nXSV);
      results.prepareVariablesForJsonFile(model.getObjVal(), initialModel);
      return results;
   }
}
