package optimizer.lp;


import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import manager.Parameters;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.results.Auxiliary;
import optimizer.results.Results;
import optimizer.results.ResultsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.convertInitialPlacement;
import static optimizer.results.Auxiliary.printLog;

public class LauncherLP {

   private static final Logger log = LoggerFactory.getLogger(LauncherLP.class);

   public static void run(Parameters pm, Scenario sce, ResultsManager resultsManager, GRBModel initialModel) throws GRBException {
      Model model = new Model(pm);
      printLog(log, INFO, "setting variables");
      Variables variables = new Variables(pm, model.getGrbModel());
      variables.initializeAdditionalVariables(pm, model.getGrbModel(), sce);
      model.setVars(variables);
      printLog(log, INFO, "setting constraints");
      new GeneralConstraints(pm, model, sce, initialModel);
      GRBLinExpr expr = generateExprForObjectiveFunction(pm, model, sce.getObjFunc());
      model.setObjectiveFunction(expr, sce.isMaximization());
      printLog(log, INFO, "running model");
      long startTime = System.nanoTime();
      Double objVal = model.run();
      long elapsedTime = System.nanoTime() - startTime;
      Results results;
      if (objVal != null) {
         results = generateResults(pm, model, sce, initialModel);
         results.setComputationTime((double) elapsedTime / 1000000000);
         resultsManager.exportJsonFile(generateFileName(pm, sce.getModel(), sce), results);
         if (sce.getModel().equals(INITIAL_PLACEMENT))
            resultsManager.exportModel(model.getGrbModel(), sce.getInputFileName());
         ResultsGUI.updateResults(results);
      }
   }

   private static GRBLinExpr generateExprForObjectiveFunction(Parameters pm, Model model, String objectiveFunction) throws GRBException {
      GRBLinExpr expr = new GRBLinExpr();
      double serversWeight, linksWeight;
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
            break;
         case FUNCTIONS_CHARGES_OBJ:
            expr.add(model.functionsChargesExpr());
            break;
         case QOS_PENALTIES_OBJ:
            expr.add(model.qosPenaltiesExpr());
            break;
         case ALL_MONETARY_COSTS_OBJ:
            expr.add(model.opexServersExpr());
            expr.add(model.functionsChargesExpr());
            expr.add(model.qosPenaltiesExpr());
            break;
      }
      return expr;
   }

   private static Results generateResults(Parameters pm, Model optModel, Scenario sc, GRBModel initialModel) throws GRBException {
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
         results.setVariable(ySDP, Auxiliary.grbVarsToDoubles(optModel.getVars().ySDP));
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
      results.initializeResults(optModel.getObjVal(), convertInitialPlacement(pm, initialModel));
      return results;
   }

   private static String generateFileName(Parameters pm, String model, Scenario sc) {
      String fileName = pm.getScenario();
      if (model.equals(INITIAL_PLACEMENT))
         fileName += "_" + model;
      fileName += "_" + sc.getObjFunc();
      return fileName;
   }
}
