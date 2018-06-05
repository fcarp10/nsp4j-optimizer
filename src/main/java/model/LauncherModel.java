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
    private static ResultsModel resultsModel;

    public static void startOptimization(boolean areReplicas, boolean isInitialPlacement) throws GRBException {
        initializeModel();
        GRBLinExpr expr = new GRBLinExpr();
        expr.add(model.exprServerCosts(pm.ip.getAlpha()));
        expr.add(model.exprLinkCosts(pm.ip.getBeta()));
        model.setObjectiveFunction(expr);
        ConstraintsModel constraintsModel = new ConstraintsModel(pm);
        constraintsModel.setLinkUtilizationExpr();
        constraintsModel.setServerUtilizationExpr();
        double objVal = -1;
        boolean isRunnable = false;
        if (!areReplicas)
            constraintsModel.noParallelPaths();
        if (isInitialPlacement)
            isRunnable = true;
        else if (constraintsModel.variablesFromInitialPlacement(resultsModel))
            isRunnable = true;
        if (isRunnable)
            objVal = model.run();
        resultsModel = generateResults(pm, objVal);
        model.finishModel();
    }

    private static void initializeModel() {
        InputParameters ip = ConfigFiles.readInputParameters("/../" + Launcher.configFile);
        ip.initializeParameters("/../");
        new WebApp().initializeResults();
        pm = new ParametersModel(ip);
        pm.initializeVariables();
        model = new Model(pm);
    }

    private static ResultsModel generateResults(ParametersModel pm, double objVal) throws GRBException {
        ResultsModel resultsModel = null;
        Results results = null;
        if (objVal != -1) {
            resultsModel = new ResultsModel(pm);
            new ResultsFiles(pm.ip.getNetworkFile(), pm.ip.getAlpha() + "-" + pm.ip.getBeta());
            results = resultsModel.generate(objVal);
        }
        ClientResults.updateResultsToWebApp(results);
        return resultsModel;
    }
}
