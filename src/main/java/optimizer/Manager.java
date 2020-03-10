package optimizer;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.TrafficFlow;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.lp.Model;
import optimizer.lp.Variables;
import optimizer.lp.constraints.GeneralConstraints;
import optimizer.results.Auxiliary;
import optimizer.results.Results;
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

import static optimizer.Parameters.*;
import static optimizer.results.Auxiliary.printLog;

public class Manager {

   private static final Logger log = LoggerFactory.getLogger(Manager.class);
   private static Parameters pm;
   private static boolean interrupted;

   public static void loadTopology(String fileName) {
      String path = getResourcePath(fileName);
      if (path != null)
         try {
            pm = ConfigFiles.readParameters(path, fileName + ".yml");
            pm.initialize(path);
            checkTopologyScale();
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
      Random rnd = new Random(pm.getSeed());
      for (TrafficFlow trafficFlow : pm.getTrafficFlows()) {
         trafficFlow.getAux().clear();
         for (int d = 0; d < trafficFlow.getDemands().size(); d++)
            trafficFlow.getAux().add(true);
         if (isInitialPlacement) {
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

   public static GRBModel start(Scenario sce, GRBModel initialModel) {
      try {
         interrupted = false;
         GRBModel importedModel = ResultsManager.importModel(getResourcePath(sce.getInputFileName()), sce.getInputFileName(), pm);
         if (initialModel == null && importedModel != null)
            initialModel = importedModel;
         ResultsManager resultsManager = new ResultsManager(pm.getScenario());
         printLog(log, INFO, "initializing " + sce.getModel());
         if (INITIAL_PLACEMENT.equals(sce.getModel())) {
            specifyUsedTrafficDemands(true);
            initialModel = runLP(INITIAL_PLACEMENT, sce, sce.getObjFunc(), resultsManager, null);
         } else {
            specifyUsedTrafficDemands(false);
            runLP(sce.getModel(), sce, sce.getObjFunc(), resultsManager, initialModel);
         }
         printLog(log, INFO, "backend is ready");
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
      Variables variables = new Variables(pm, model.getGrbModel());
      variables.initializeAdditionalVariables(pm, model.getGrbModel(), scenario);
      model.setVars(variables);
      printLog(log, INFO, "setting constraints");
      new GeneralConstraints(pm, model, scenario, initialModel);
      expr = generateExprForObjectiveFunction(model, objectiveFunction, initialModel);
      model.setObjectiveFunction(expr, scenario.isMaximization());
      printLog(log, INFO, "running model");
      long startTime = System.nanoTime();
      Double objVal = model.run();
      long elapsedTime = System.nanoTime() - startTime;
      Results results;
      if (objVal != null) {
         results = generateResultsForLP(model, scenario, initialModel);
         results.setComputationTime((double) elapsedTime / 1000000000);
         resultsManager.exportJsonFile(generateFileName(modelName, scenario), results);
         if (modelName.equals(INITIAL_PLACEMENT))
            resultsManager.exportModel(model.getGrbModel(), scenario.getInputFileName());
         ResultsGUI.updateResults(results);
      }
      return model.getGrbModel();
   }

   private static GRBLinExpr generateExprForObjectiveFunction(Model model, String objectiveFunction, GRBModel initialPlacement) throws GRBException {
      GRBLinExpr expr = new GRBLinExpr();
      double serversWeight, linksWeight;
      Integer BIG_M = 100000;
      switch (objectiveFunction) {
         case SERVER_DIMENSIONING:
            expr.add(model.dimensioningExpr());
            break;
         case NUM_SERVERS_OBJ:
            expr.add(model.numUsedServersExpr());
            break;
         case NUM_SERVERS_UTIL_COSTS_OBJ:
            expr.add(model.numUsedServersExpr());
            serversWeight = 1.0 / pm.getServers().size();
            expr.add(model.serverCostsExpr(serversWeight));
            break;
         case UTIL_COSTS_OBJ:
            linksWeight = (double) pm.getAux().get(LINKS_WEIGHT) / pm.getLinks().size();
            serversWeight = (double) pm.getAux().get(SERVERS_WEIGHT) / pm.getServers().size();
            expr.add(model.linkCostsExpr(linksWeight));
            expr.add(model.serverCostsExpr(serversWeight));
            break;
         case UTIL_COSTS_MIGRATIONS_OBJ:
            expr.add(model.linkCostsExpr(1.0));
            expr.add(model.serverCostsExpr(1.0));
            if (initialPlacement != null)
               expr.add(model.numOfMigrations(0.0, initialPlacement));
            else printLog(log, WARNING, "no initial placement");
            break;
         case UTIL_COSTS_MAX_UTIL_OBJ:
            linksWeight = (double) pm.getAux().get(LINKS_WEIGHT) / pm.getLinks().size();
            serversWeight = (double) pm.getAux().get(SERVERS_WEIGHT) / pm.getServers().size();
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            expr.add(model.maxUtilizationExpr((double) pm.getAux().get(MAXU_WEIGHT)));
            break;
         case UTILIZATION_OBJ:
            linksWeight = (double) pm.getAux().get(LINKS_WEIGHT) / pm.getLinks().size();
            serversWeight = (double) pm.getAux().get(SERVERS_WEIGHT) / pm.getServers().size();
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            break;
         case OPEX_SERVERS_OBJ:
            expr.add(model.opexServersExpr());
            expr.add(model.qosPenaltiesExpr(1.0 / BIG_M)); // because delay is not constrained
            break;
         case FUNCTIONS_CHARGES_OBJ:
            expr.add(model.functionsChargesExpr());
            expr.add(model.qosPenaltiesExpr(1.0 / BIG_M)); // because delay is not constrained
            break;
         case QOS_PENALTIES_OBJ:
            expr.add(model.qosPenaltiesExpr(1.0));
            break;
         case ALL_MONETARY_COSTS_OBJ:
            expr.add(model.opexServersExpr());
            expr.add(model.functionsChargesExpr());
            expr.add(model.qosPenaltiesExpr(1.0));
            break;
      }
      return expr;
   }

   private static Results generateResultsForLP(Model optModel, Scenario sc, GRBModel initialModel) throws GRBException {
      Results results = new Results(pm, sc);
      // general variables
      results.setVariable(zSP, Auxiliary.grbVarsToBooleans(optModel.getVars().zSP));
      results.setVariable(zSPD, Auxiliary.grbVarsToBooleans(optModel.getVars().zSPD));
      results.setVariable(fX, Auxiliary.grbVarsToBooleans(optModel.getVars().fX));
      results.setVariable(fXSV, Auxiliary.grbVarsToBooleans(optModel.getVars().fXSV));
      results.setVariable(fXSVD, Auxiliary.grbVarsToBooleans(optModel.getVars().fXSVD));
      results.setVariable(uL, Auxiliary.grbVarsToDoubles(optModel.getVars().uL));
      results.setVariable(uX, Auxiliary.grbVarsToDoubles(optModel.getVars().uX));

      // model specific variables
      if (sc.getObjFunc().equals(SERVER_DIMENSIONING))
         results.setVariable(xN, Auxiliary.grbVarsToDoubles(optModel.getVars().xN));
      if (sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
              || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {
         results.setVariable(oX, Auxiliary.grbVarsToDoubles(optModel.getVars().oX));
         results.setVariable(oSV, Auxiliary.grbVarsToDoubles(optModel.getVars().oSV));
         results.setVariable(qSDP, Auxiliary.grbVarsToDoubles(optModel.getVars().qSDP));
      }

      // traffic sync variables
      if (sc.getConstraints().get(SYNC_TRAFFIC)) {
         results.setVariable(gSVXY, Auxiliary.grbVarsToBooleans(optModel.getVars().gSVXY));
         results.setVariable(hSVP, Auxiliary.grbVarsToBooleans(optModel.getVars().hSVP));
      }

      // service delay variables
      if (sc.getConstraints().get(MAX_SERV_DELAY) || sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
              || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {
         results.setVariable(dSVX, Auxiliary.grbVarsToDoubles(optModel.getVars().dSVX));
         results.setVariable(dSVXD, Auxiliary.grbVarsToDoubles(optModel.getVars().dSVXD));
         results.setVariable(mS, Auxiliary.grbVarsToDoubles(optModel.getVars().mS));
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

   private static String generateFileName(String model, Scenario sc) {
      String fileName = pm.getScenario() + "_";
      if (model.equals(INITIAL_PLACEMENT))
         fileName += model;
      else if (sc.getConstraints().get(CLOUD_ONLY))
         fileName += "co";
      else if (sc.getConstraints().get(EDGE_ONLY))
         fileName += "eo";
      else
         fileName += "ec";

      switch (sc.getObjFunc()) {
         case OPEX_SERVERS_OBJ:
            fileName += "_op";
            break;
         case FUNCTIONS_CHARGES_OBJ:
            fileName += "_ch";
            break;
         case QOS_PENALTIES_OBJ:
            fileName += "_pe";
            break;
         case ALL_MONETARY_COSTS_OBJ:
            fileName += "_all";
            break;
      }
      return fileName;
   }

   public static boolean isInterrupted() {
      return interrupted;
   }
}
