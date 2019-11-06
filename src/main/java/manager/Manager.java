package manager;

import gui.WebClient;
import gui.WebServer;
import gui.elements.Scenario;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import lp.Constraints;
import lp.Model;
import lp.Variables;
import manager.elements.TrafficFlow;
import org.apache.commons.io.FilenameUtils;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import output.Auxiliary;
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

   public static void loadTopology(String fileName) {
      String path = getResourcePath(fileName);
      if (path != null)
         try {
            printLog(log, INFO, "loading topology");
            pm = ConfigFiles.readParameters(path, fileName + ".yml");
            pm.initialize(path);
            checkTopologyScale();
            WebServer.initialize(pm);
            printLog(log, INFO, "topology loaded");
         } catch (Exception e) {
            printLog(log, ERROR, "input parameters");
         }
   }

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

   private static void checkTopologyScale() {
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

   private static void specifyUsedTrafficDemands(boolean isInitialPlacement) {
      for (TrafficFlow trafficFlow : pm.getTrafficFlows()) {
         trafficFlow.getAux().clear();
         for (int d = 0; d < trafficFlow.getDemands().size(); d++)
            trafficFlow.getAux().add(true);
         if (isInitialPlacement) {
            double initialTrafficLoad = (double) pm.getAux().get(INITIAL_TRAFFIC_LOAD);
            int index = (int) (trafficFlow.getDemands().size() * initialTrafficLoad);
            if (index > 1) //at least one traffic demand per traffic flow
               for (int d = index; d < trafficFlow.getDemands().size(); d++)
                  trafficFlow.getAux().set(d, false);
         }
      }
   }

   public static GRBModel start(Scenario sce, GRBModel initialModel) {
      try {
         interrupted = false;
         GRBModel importedModel = ResultsManager.importModel(getResourcePath(sce.getInputFileName()), sce.getInputFileName(), pm, sce);
         if (initialModel == null && importedModel != null)
            initialModel = importedModel;
         ResultsManager resultsManager = new ResultsManager(pm.getScenario());
         printLog(log, INFO, "initializing " + sce.getModel());
         if (INITIAL_PLACEMENT.equals(sce.getModel())) {
            specifyUsedTrafficDemands(true);
            initialModel = runLP(INITIAL_PLACEMENT, sce, sce.getObjectiveFunction(), resultsManager, null);
         } else {
            specifyUsedTrafficDemands(false);
            runLP(sce.getModel(), sce, sce.getObjectiveFunction(), resultsManager, initialModel);
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
      Model model = new Model(pm);
      printLog(log, INFO, "setting variables");
      Variables variables = new Variables(pm, model.getGrbModel(), scenario);
      variables.initializeAdditionalVariables(pm, model.getGrbModel(), scenario);
      model.setVariables(variables);
      printLog(log, INFO, "setting constraints");
      new Constraints(pm, model, scenario, initialModel);
      expr = generateExprForObjectiveFunction(model, scenario, objectiveFunction, initialModel);
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

   private static GRBLinExpr generateExprForObjectiveFunction(Model model, Scenario scenario, String objectiveFunction, GRBModel initialPlacement) throws GRBException {
      GRBLinExpr expr = new GRBLinExpr();
      String[] weights = scenario.getWeights().split("-");
      double serversWeight, linksWeight;
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
            linksWeight = Double.parseDouble(weights[0]) / pm.getLinks().size();
            serversWeight = Double.parseDouble(weights[1]) / pm.getServers().size();
            expr.add(model.linkCostsExpr(linksWeight));
            expr.add(model.serverCostsExpr(serversWeight));
            break;
         case COSTS_MIGRATIONS_OBJ:
            expr.add(model.linkCostsExpr(1.0));
            expr.add(model.serverCostsExpr(1.0));
            if (initialPlacement != null)
               expr.add(model.numOfMigrations(0.0, initialPlacement));
            else printLog(log, WARNING, "no init. placement");
            break;
         case UTILIZATION_OBJ:
            linksWeight = Double.parseDouble(weights[0]) / pm.getLinks().size();
            serversWeight = Double.parseDouble(weights[1]) / pm.getServers().size();
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            break;
         case MAX_UTILIZATION_OBJ:
            linksWeight = Double.parseDouble(weights[0]) / pm.getLinks().size();
            serversWeight = Double.parseDouble(weights[1]) / pm.getServers().size();
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            expr.add(model.maxUtilizationExpr(Double.parseDouble(weights[2])));
            break;
      }
      return expr;
   }

   private static Results generateResultsForLP(Model optModel, Scenario scenario, GRBModel initialModel) throws GRBException {
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
