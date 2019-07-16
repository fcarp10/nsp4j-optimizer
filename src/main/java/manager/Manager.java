package manager;

import gui.WebClient;
import gui.WebServer;
import gui.elements.Scenario;
import gurobi.GRB;
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
import output.Auxiliary;
import output.Definitions;
import output.Results;
import output.ResultsManager;
import utils.ConfigFiles;
import utils.GraphManager;
import utils.KShortestPathGenerator;

import static output.Auxiliary.printLog;
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
            double value = Math.round((double) node.getAttribute("y") * scalingY);
            node.setAttribute("y", value);
         }
         for (Node node : pm.getNodes()) {
            double value =  Math.round((double) node.getAttribute("x") * scalingX);
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
         interrupted = false;
         GRBModel importedModel = ResultsManager.importModel(getResourcePath(sce.getInputFileName()), sce.getInputFileName(), pm, sce);
         if (initialModel == null && importedModel != null)
            initialModel = importedModel;
         ResultsManager resultsManager = new ResultsManager(pm.getScenario());
         printLog(log, INFO, "initializing " + sce.getModel());
         switch (sce.getModel()) {
            case INITIAL_PLACEMENT:
               initialModel = runLP(INITIAL_PLACEMENT, sce, sce.getObjectiveFunction(), resultsManager, null);
               break;
            default:
               runLP(sce.getModel(), sce, sce.getObjectiveFunction(), resultsManager, initialModel);
               break;
            case ALL_CASES:
               if (initialModel == null)
                  Auxiliary.printLog(log, ERROR, "run initial placement first");
               else {
                  runLP(MIGRATION, sce, sce.getObjectiveFunction(), resultsManager, initialModel);
                  runLP(REPLICATION, sce, sce.getObjectiveFunction(), resultsManager, initialModel);
                  runLP(MIGRATION_REPLICATION, sce, sce.getObjectiveFunction(), resultsManager, initialModel);
               }
               break;
            case REINFORCEMENT_LEARNING:
               if (initialModel == null)
                  initialModel = runLP(INITIAL_PLACEMENT, sce, Definitions.NUM_SERVERS_OBJ, resultsManager, null);
               GRBModel mgrRepModel = runLP(MIGRATION_REPLICATION, sce, sce.getObjectiveFunction(), resultsManager, initialModel);
               runRL(sce, resultsManager, initialModel, mgrRepModel);
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

   private static GRBModel runLP(String modelName, Scenario scenario, String objectiveFunction, ResultsManager resultsManager, GRBModel initialModel) throws GRBException {
      GRBLinExpr expr;
      OptimizationModel model = new OptimizationModel(pm);
      printLog(log, INFO, "setting variables");
      Variables variables = new Variables(pm, model.getGrbModel(), scenario);
      variables.initializeAdditionalVariables(pm, model.getGrbModel(), scenario);
      model.setVariables(variables);
      printLog(log, INFO, "setting constraints");
      new Constraints(pm, model, scenario, initialModel);
      expr = generateExprForObjectiveFunction(model, scenario, objectiveFunction);
      model.setObjectiveFunction(expr, scenario.isMaximization());
      printLog(log, INFO, "running LP model");
      double objVal = model.run();
      Results results;
      if (objVal != -1) {
         results = generateResultsForLP(model, scenario, initialModel);
         resultsManager.exportJsonFile(generateFileName(scenario, modelName), results);
         if (modelName.equals(INITIAL_PLACEMENT))
            resultsManager.exportModel(model.getGrbModel(), scenario.getInputFileName());
         WebClient.updateResultsToWebApp(results);
      }
      return model.getGrbModel();
   }

   private static void runRL(Scenario scenario, ResultsManager resultsManager, GRBModel initialModel, GRBModel mgrRepModel) throws GRBException {
      String resultsFileName = pm.getScenario() + "_" + scenario.getModel();
      LearningModel learningModel = new LearningModel(pm);
      printLog(log, INFO, "running RL model");
      learningModel.run(initialModel, Auxiliary.roundDouble(mgrRepModel.get(GRB.DoubleAttr.ObjVal), 4), mgrRepModel);
      Results results = generateResultsForRL(learningModel, scenario, initialModel);
      resultsManager.exportJsonFile(resultsFileName, results);
      WebClient.updateResultsToWebApp(results);
   }

   private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel model, Scenario scenario, String objectiveFunction) throws GRBException {
      GRBLinExpr expr = new GRBLinExpr();
      String[] weights = scenario.getWeights().split("-");
      double linksWeight = Double.valueOf(weights[0]) / pm.getLinks().size();
      double serversWeight = Double.valueOf(weights[1]) / pm.getServers().size();
      switch (objectiveFunction) {
         case SERVER_DIMENSIONING:
            expr.add(model.dimensioningExpr());
            break;
         case NUM_SERVERS_OBJ:
            expr.add(model.usedServersExpr());
            break;
         case NUM_SERVERS_COSTS_OBJ:
            expr.add(model.usedServersExpr());
            serversWeight = 1.0 / pm.getServers().size();
            expr.add(model.serverCostsExpr(serversWeight));
            break;
         case COSTS_OBJ:
            expr.add(model.linkCostsExpr(linksWeight));
            expr.add(model.serverCostsExpr(serversWeight));
            break;
         case UTILIZATION_OBJ:
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            break;
         case MAX_UTILIZATION_OBJ:
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            expr.add(model.maxUtilizationExpr(Double.valueOf(weights[2])));
            break;
      }
      return expr;
   }

   private static Results generateResultsForLP(OptimizationModel optModel, Scenario scenario, GRBModel initialModel) throws GRBException {
      Results results = new Results(pm, scenario);
      // objective variables
      results.setVariable(uL, Auxiliary.grbVarsToDoubles(optModel.getVariables().uL));
      results.setVariable(uX, Auxiliary.grbVarsToDoubles(optModel.getVariables().uX));
      // model specific objective variables
      if (scenario.getObjectiveFunction().equals(SERVER_DIMENSIONING))
         results.setVariable(xN, Auxiliary.grbVarsToDoubles(optModel.getVariables().xN));
      if (scenario.getObjectiveFunction().equals(NUM_SERVERS_COSTS_OBJ)
              || scenario.getObjectiveFunction().equals(NUM_SERVERS_OBJ))
         results.setVariable(fX, Auxiliary.grbVarsToBooleans(optModel.getVariables().fX));
      // general variables
      results.setVariable(zSP, Auxiliary.grbVarsToBooleans(optModel.getVariables().zSP));
      results.setVariable(zSPD, Auxiliary.grbVarsToBooleans(optModel.getVariables().zSPD));
      results.setVariable(fXSV, Auxiliary.grbVarsToBooleans(optModel.getVariables().fXSV));
      results.setVariable(fXSVD, Auxiliary.grbVarsToBooleans(optModel.getVariables().fXSVD));
      // additional variables
      if (scenario.getConstraints().get(ST)) {
         results.setVariable(gSVXY, Auxiliary.grbVarsToBooleans(optModel.getVariables().gSVXY));
         results.setVariable(hSVP, Auxiliary.grbVarsToBooleans(optModel.getVariables().hSVP));
      }
      if (scenario.getConstraints().get(SD)) {
         results.setVariable(dSVX, Auxiliary.grbVarsToDoubles(optModel.getVariables().dSVX));
         results.setVariable(dSVXD, Auxiliary.grbVarsToDoubles(optModel.getVariables().dSVXD));
         results.setVariable(mS, Auxiliary.grbVarsToDoubles(optModel.getVariables().mS));
         results.setVariable(ySVX, Auxiliary.grbVarsToDoubles(optModel.getVariables().ySVX));
      }
      results.initializeResults(optModel.getObjVal(), convertInitialPlacement(initialModel));
      return results;
   }

   private static Results generateResultsForRL(LearningModel learningModel, Scenario scenario, GRBModel initialModel) throws GRBException {
      Results results = new Results(pm, scenario);
      results.setVariable(zSPD, learningModel.getzSPD());
      results.setVariable(fXSV, learningModel.getfXSV());
      results.setVariable(fXSVD, learningModel.getfXSVD());
      results.setVariable(uL, learningModel.getuL());
      results.setVariable(uX, learningModel.getuX());
      results.initializeResults(learningModel.getObjVal(), convertInitialPlacement(initialModel));
      return results;
   }

   private static boolean[][][] convertInitialPlacement(GRBModel initialModel) throws GRBException {
      boolean[][][] initialPlacement = null;
      if (initialModel != null) {
         initialPlacement = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  if (initialModel.getVarByName(fXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 1.0)
                     initialPlacement[x][s][v] = true;
      }
      return initialPlacement;
   }

   private static String generateFileName(Scenario scenario, String model) {
      String fileName = pm.getScenario() + "_";
      if (String.valueOf(scenario.getWeights()).equals("1.0-0.0-0.0"))
         fileName += "LLB_";
      if (String.valueOf(scenario.getWeights()).equals("0.0-1.0-0.0"))
         fileName += "XLB_";
      fileName += model;
      return fileName;
   }

   public static boolean isInterrupted() {
      return interrupted;
   }
}
