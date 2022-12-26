package optimizer.lp;

import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import optimizer.Parameters;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.results.Auxiliary;
import optimizer.results.Results;
import optimizer.results.ResultsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

import java.time.Duration;
import java.time.LocalDateTime;

public class LauncherLP {

   private static final Logger log = LoggerFactory.getLogger(LauncherLP.class);

   public static GRBModel run(Parameters pm, Scenario sce, ResultsManager resultsManager, GRBModel initialModel,
         GRBModel initialSolution, String outputFileName, boolean exportMST) throws GRBException {
      boolean[][][] initialPlacement = null;
      if (initialModel != null)
         initialPlacement = Auxiliary.fXSVvarsFromInitialModel(pm, initialModel);
      ModelLP modelLP = new ModelLP(pm, initialSolution);
      printLog(log, INFO, "setting variables");
      VariablesLP variablesLP = new VariablesLP(pm, modelLP.getGrbModel(), sce, initialSolution);
      modelLP.setVars(variablesLP);
      printLog(log, INFO, "setting constraints");
      new Constraints(pm, modelLP, sce, initialPlacement);
      GRBLinExpr expr = generateExprForObjectiveFunction(pm, modelLP, sce.getObjFunc(), initialPlacement);
      modelLP.setObjectiveFunction(expr, sce.isMaximization());
      printLog(log, INFO, "running model");
      LocalDateTime startTime = LocalDateTime.now();
      Double objVal = modelLP.run();
      Duration durationComputation = Duration.between(startTime, LocalDateTime.now());
      Results results;
      if (objVal != null) {
         Auxiliary.printLog(log, INFO, "generating results...");
         results = generateResults(pm, modelLP, sce, initialPlacement);
         results.setComputationTime((double) durationComputation.getSeconds());
         resultsManager.exportJsonObject(outputFileName, results);
         if (exportMST)
            resultsManager.exportModel(modelLP.getGrbModel(), outputFileName);
         ResultsGUI.updateResults(results);
         Auxiliary.printLog(log, INFO, "done");
      }
      return modelLP.getGrbModel();
   }

   private static GRBLinExpr generateExprForObjectiveFunction(Parameters pm, ModelLP modelLP, String objectiveFunction,
         boolean[][][] initialPlacement) throws GRBException {
      GRBLinExpr expr = new GRBLinExpr();
      double serversWeight, linksWeight;
      switch (objectiveFunction) {
      case DIMEN:
         expr.add(modelLP.dimensioningExpr());
         break;
      case NUM_SERVERS:
         expr.add(modelLP.numUsedServersExpr());
         break;
      case NUM_SERVERS_AND_UTIL_COSTS:
         expr.add(modelLP.numUsedServersExpr());
         serversWeight = 1.0 / pm.getServers().size();
         expr.add(modelLP.serverCostsExpr(serversWeight));
         break;
      case UTIL_COSTS:
         linksWeight = (double) pm.getGlobal().get(LINKS_WEIGHT) / pm.getLinks().size();
         serversWeight = (double) pm.getGlobal().get(SERVERS_WEIGHT) / pm.getServers().size();
         expr.add(modelLP.linkCostsExpr(linksWeight));
         expr.add(modelLP.serverCostsExpr(serversWeight));
         break;
      case UTIL_COSTS_AND_MAX_UTIL:
         linksWeight = (double) pm.getGlobal().get(LINKS_WEIGHT) / pm.getLinks().size();
         serversWeight = (double) pm.getGlobal().get(SERVERS_WEIGHT) / pm.getServers().size();
         expr.add(modelLP.linkUtilizationExpr(linksWeight));
         expr.add(modelLP.serverUtilizationExpr(serversWeight));
         expr.add(modelLP.maxUtilizationExpr((double) pm.getGlobal().get(MAXU_WEIGHT)));
         break;
      case UTILIZATION:
         linksWeight = (double) pm.getGlobal().get(LINKS_WEIGHT) / pm.getLinks().size();
         serversWeight = (double) pm.getGlobal().get(SERVERS_WEIGHT) / pm.getServers().size();
         expr.add(modelLP.linkUtilizationExpr(linksWeight));
         expr.add(modelLP.serverUtilizationExpr(serversWeight));
         break;
      case OPEX_SERVERS:
         expr.add(modelLP.opexServersExpr());
         break;
      case FUNCTIONS_CHARGES:
         expr.add(modelLP.functionsChargesExpr());
         break;
      case QOS_PENALTIES:
         expr.add(modelLP.qosPenaltiesExpr());
         break;
      case ALL_MONETARY_COSTS:
         expr.add(modelLP.opexServersExpr());
         expr.add(modelLP.functionsChargesExpr());
         expr.add(modelLP.qosPenaltiesExpr());
         break;
      case UTILIZATION_CLOUD:
         serversWeight = (double) 1.0 / pm.getServers().size();
         expr.add(modelLP.serverUtilizationExpr(-serversWeight));
         expr.add(modelLP.numFunctionsInCloudExpr(1.0));
         break;
      case NUM_SERVERS_CLOUD:
         expr.add(modelLP.numUsedServersExpr());
         expr.add(modelLP.numFunctionsInCloudExpr(1.0));
         break;
      case MGR:
         expr.add(modelLP.numMigrations(1.0, initialPlacement));
         expr.add(modelLP.numReplications(0.001));
         expr.add(modelLP.numFunctionsInCloudExpr(0.001));
         break;
      case REP:
         expr.add(modelLP.numReplications(1.0));
         expr.add(modelLP.numMigrations(0.001, initialPlacement));
         expr.add(modelLP.numFunctionsInCloudExpr(0.001));
         break;
      case CLOUD:
         expr.add(modelLP.numFunctionsInCloudExpr(1.0));
         expr.add(modelLP.numMigrations(0.001, initialPlacement));
         expr.add(modelLP.numReplications(0.001));
         break;
      // case MGR_CLOUD:
      //    expr.add(modelLP.numMigrations(1.0, initialPlacement));
      //    expr.add(modelLP.numReplications(0.001));
      //    expr.add(modelLP.numFunctionsInCloudExpr(1.0));
      //    break;
      // case REP_CLOUD:
      //    expr.add(modelLP.numReplications(1.0));
      //    expr.add(modelLP.numMigrations(0.001, initialPlacement));
      //    expr.add(modelLP.numFunctionsInCloudExpr(1.0));
      //    break;
      case MGR_REP_CLOUD:
         expr.add(modelLP.numMigrations(1.0, initialPlacement));
         expr.add(modelLP.numReplications(1.0));
         expr.add(modelLP.numFunctionsInCloudExpr(1.0));
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
      if (sc.getObjFunc().equals(DIMEN))
         results.setVariable(xN, Auxiliary.grbVarsToDoubles(optModelLP.getVars().xN));
      if (sc.getObjFunc().equals(OPEX_SERVERS) || sc.getObjFunc().equals(FUNCTIONS_CHARGES)
            || sc.getObjFunc().equals(QOS_PENALTIES) || sc.getObjFunc().equals(ALL_MONETARY_COSTS)) {
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
      if (sc.getConstraints().get(MAX_SERV_DELAY) || sc.getObjFunc().equals(OPEX_SERVERS)
            || sc.getObjFunc().equals(FUNCTIONS_CHARGES) || sc.getObjFunc().equals(QOS_PENALTIES)
            || sc.getObjFunc().equals(ALL_MONETARY_COSTS)) {
         results.setVariable(dSVXD, Auxiliary.grbVarsToDoubles(optModelLP.getVars().dSVXD));
      }
      results.initializeResults(optModelLP.getObjVal(), initialPlacement);
      return results;
   }
}
