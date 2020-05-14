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
import static optimizer.results.Auxiliary.printLog;

public class LauncherLP {

   private static final Logger log = LoggerFactory.getLogger(LauncherLP.class);

   public static void run(Parameters pm, Scenario sce, ResultsManager resultsManager, GRBModel initialModel,
         GRBModel initialSolution) throws GRBException {
      boolean[][][] initialPlacement = null;
      if (initialModel != null)
         initialPlacement = Auxiliary.fXSVvarsFromInitialModel(pm, initialModel);
      ModelLP modelLP = new ModelLP(pm, initialSolution);
      printLog(log, INFO, "setting variables");
      VariablesLP variablesLP = new VariablesLP(pm, modelLP.getGrbModel(), sce, initialSolution);
      modelLP.setVars(variablesLP);
      printLog(log, INFO, "setting constraints");
      new GeneralConstraints(pm, modelLP, sce, initialPlacement);
      GRBLinExpr expr = generateExprForObjectiveFunction(pm, modelLP, sce.getObjFunc());
      modelLP.setObjectiveFunction(expr, sce.isMaximization());
      printLog(log, INFO, "running model");
      long startTime = System.nanoTime();
      Double objVal = modelLP.run();
      long elapsedTime = System.nanoTime() - startTime;
      Results results;
      if (objVal != null) {
         results = generateResults(pm, modelLP, sce, initialPlacement);
         results.setComputationTime((double) elapsedTime / 1000000000);
         String outputFileName = pm.getScenario() + "_" + sce.getAlgorithm() + "_" + sce.getObjFunc();
         resultsManager.exportJsonObject(outputFileName, results);
         resultsManager.exportModel(modelLP.getGrbModel(), outputFileName);
         ResultsGUI.updateResults(results);
      }
   }

   private static GRBLinExpr generateExprForObjectiveFunction(Parameters pm, ModelLP modelLP, String objectiveFunction)
         throws GRBException {
      GRBLinExpr expr = new GRBLinExpr();
      double serversWeight, linksWeight;
      switch (objectiveFunction) {
         case SERVER_DIMENSIONING:
            expr.add(modelLP.dimensioningExpr());
            break;
         case NUM_SERVERS_OBJ:
            expr.add(modelLP.numUsedServersExpr());
            break;
         case NUM_SERVERS_UTIL_COSTS_OBJ:
            expr.add(modelLP.numUsedServersExpr());
            serversWeight = 1.0 / pm.getServers().size();
            expr.add(modelLP.serverCostsExpr(serversWeight));
            break;
         case UTIL_COSTS_OBJ:
            linksWeight = (double) pm.getAux().get(LINKS_WEIGHT) / pm.getLinks().size();
            serversWeight = (double) pm.getAux().get(SERVERS_WEIGHT) / pm.getServers().size();
            expr.add(modelLP.linkCostsExpr(linksWeight));
            expr.add(modelLP.serverCostsExpr(serversWeight));
            break;
         case UTIL_COSTS_MAX_UTIL_OBJ:
            linksWeight = (double) pm.getAux().get(LINKS_WEIGHT) / pm.getLinks().size();
            serversWeight = (double) pm.getAux().get(SERVERS_WEIGHT) / pm.getServers().size();
            expr.add(modelLP.linkUtilizationExpr(linksWeight));
            expr.add(modelLP.serverUtilizationExpr(serversWeight));
            expr.add(modelLP.maxUtilizationExpr((double) pm.getAux().get(MAXU_WEIGHT)));
            break;
         case UTILIZATION_OBJ:
            linksWeight = (double) pm.getAux().get(LINKS_WEIGHT) / pm.getLinks().size();
            serversWeight = (double) pm.getAux().get(SERVERS_WEIGHT) / pm.getServers().size();
            expr.add(modelLP.linkUtilizationExpr(linksWeight));
            expr.add(modelLP.serverUtilizationExpr(serversWeight));
            break;
         case OPEX_SERVERS_OBJ:
            expr.add(modelLP.opexServersExpr());
            break;
         case FUNCTIONS_CHARGES_OBJ:
            expr.add(modelLP.functionsChargesExpr());
            break;
         case QOS_PENALTIES_OBJ:
            expr.add(modelLP.qosPenaltiesExpr());
            break;
         case ALL_MONETARY_COSTS_OBJ:
            expr.add(modelLP.opexServersExpr());
            expr.add(modelLP.functionsChargesExpr());
            expr.add(modelLP.qosPenaltiesExpr());
            break;
      }
      return expr;
   }

   private static Results generateResults(Parameters pm, ModelLP optModelLP, Scenario sc,
         boolean[][][] initialPlacement) throws GRBException {
      Results results = new Results(pm, sc);
      // general variables
      results.setVariable(zSP, Auxiliary.grbVarsToBooleans(optModelLP.getVars().zSP));
      results.setVariable(zSPD, Auxiliary.grbVarsToBooleans(optModelLP.getVars().zSPD));
      results.setVariable(fX, Auxiliary.grbVarsToBooleans(optModelLP.getVars().fX));
      results.setVariable(fXSV, Auxiliary.grbVarsToBooleans(optModelLP.getVars().fXSV));
      results.setVariable(fXSVD, Auxiliary.grbVarsToBooleans(optModelLP.getVars().fXSVD));
      results.setVariable(uL, Auxiliary.grbVarsToDoubles(optModelLP.getVars().uL));
      results.setVariable(uX, Auxiliary.grbVarsToDoubles(optModelLP.getVars().uX));

      // model specific variables
      if (sc.getObjFunc().equals(SERVER_DIMENSIONING))
         results.setVariable(xN, Auxiliary.grbVarsToDoubles(optModelLP.getVars().xN));
      if (sc.getObjFunc().equals(OPEX_SERVERS_OBJ) || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ)
            || sc.getObjFunc().equals(QOS_PENALTIES_OBJ) || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {
         results.setVariable(oX, Auxiliary.grbVarsToDoubles(optModelLP.getVars().oX));
         results.setVariable(oSV, Auxiliary.grbVarsToDoubles(optModelLP.getVars().oSV));
         results.setVariable(qSDP, Auxiliary.grbVarsToDoubles(optModelLP.getVars().qSDP));
         results.setVariable(ySDP, Auxiliary.grbVarsToDoubles(optModelLP.getVars().ySDP));
      }

      // traffic sync variables
      if (sc.getConstraints().get(SYNC_TRAFFIC)) {
         results.setVariable(gSVXY, Auxiliary.grbVarsToBooleans(optModelLP.getVars().gSVXY));
         results.setVariable(hSVP, Auxiliary.grbVarsToBooleans(optModelLP.getVars().hSVP));
      }

      // service delay variables
      if (sc.getConstraints().get(MAX_SERV_DELAY) || sc.getObjFunc().equals(OPEX_SERVERS_OBJ)
            || sc.getObjFunc().equals(FUNCTIONS_CHARGES_OBJ) || sc.getObjFunc().equals(QOS_PENALTIES_OBJ)
            || sc.getObjFunc().equals(ALL_MONETARY_COSTS_OBJ)) {
         results.setVariable(dSVXD, Auxiliary.grbVarsToDoubles(optModelLP.getVars().dSVXD));
      }
      results.initializeResults(optModelLP.getObjVal(), initialPlacement);
      return results;
   }
}
