package model;

import filemanager.ConfigFiles;
import filemanager.InputParameters;
import gui.WebApp;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import launcher.Launcher;
import results.ClientResults;
import results.Results;
import results.ResultsFiles;


public class LauncherModel {

    private static ParametersModel pm;
    private static Model model;
    private static ResultsModel resultsInitialModel;

    public static void startOptimization(String useCase, String objective) throws GRBException {
        initializeModel();
        GRBLinExpr expr = new GRBLinExpr();
        switch (objective) {
            case "costs":
                expr.add(model.exprServerUtilizationCosts(pm.ip.getAlpha()));
                expr.add(model.exprLinkUtilizationCosts(pm.ip.getBeta()));
                break;
            case "utilization":
                expr.add(model.exprServerUtilization(pm.ip.getAlpha()));
                expr.add(model.exprLinkUtilization(pm.ip.getBeta()));
                break;
            case "servers":
                break;
        }
        model.setObjectiveFunction(expr);
        ConstraintsModel constraintsModel = new ConstraintsModel(pm);
        constraintsModel.setLinkUtilizationExpr();
        constraintsModel.setServerUtilizationExpr();
        double objVal = -1;
        ResultsModel resultsModel = null;
        switch (useCase) {
            case "init":
                constraintsModel.noParallelPaths();
                objVal = model.run();
                resultsModel = generateResultModel(pm, objVal);
                submitResultsToGUI(resultsModel, objVal);
                if (resultsModel == null)
                    model.finishModel();
                else
                    resultsInitialModel = resultsModel;
                break;
            case "mgr":
                if (resultsInitialModel != null) {
                    constraintsModel.noParallelPaths();
                    constraintsModel.setMigrationCosts(resultsInitialModel);
                    objVal = model.run();
                    resultsModel = generateResultModel(pm, objVal);
                    if (resultsModel != null)
                        resultsModel.calculateNumberOfMigrations(resultsInitialModel);
                }
                submitResultsToGUI(resultsModel, objVal);
                model.finishModel();
                break;
            case "rep":
                if (resultsInitialModel != null) {
                    constraintsModel.setVariablesFromInitialPlacementAsConstraints(resultsInitialModel);
                    objVal = model.run();
                    resultsModel = generateResultModel(pm, objVal);
                    if (resultsModel != null)
                        resultsModel.calculateNumberOfReplications();
                }
                submitResultsToGUI(resultsModel, objVal);
                model.finishModel();
                break;
            case "both":
                if (resultsInitialModel != null) {
                    constraintsModel.setMigrationCosts(resultsInitialModel);
                    objVal = model.run();
                    resultsModel = generateResultModel(pm, objVal);
                    if (resultsModel != null) {
                        resultsModel.calculateNumberOfMigrations(resultsInitialModel);
                        resultsModel.calculateNumberOfReplications();
                    }
                }
                submitResultsToGUI(resultsModel, objVal);
                model.finishModel();
                break;
        }
    }

    private static void initializeModel() {
        InputParameters ip = ConfigFiles.readInputParameters(Launcher.configFile);
        ip.initializeParameters();
        new WebApp().initializeResults();
        pm = new ParametersModel(ip);
        pm.initializeVariables();
        model = new Model(pm);
    }

    private static ResultsModel generateResultModel(ParametersModel pm, double objVal) {
        ResultsModel resultsModel = null;
        if (objVal >= 0) {
            resultsModel = new ResultsModel(pm);
            new ResultsFiles(pm.ip.getNetworkFile(), pm.ip.getAlpha() + "-" + pm.ip.getBeta());
        }
        return resultsModel;
    }

    private static void submitResultsToGUI(ResultsModel resultsModel, double objVal) throws GRBException {
        Results results;
        if (objVal >= 0 ) {
            results = resultsModel.generate(objVal);
            ClientResults.updateResultsToWebApp(results);
            ClientResults.postMessage("Solution found");
        } else if (resultsModel == null){
            ClientResults.postMessage("Please, run initial placement first");
        } else
            ClientResults.postMessage("Model is not feasible");
    }
}
