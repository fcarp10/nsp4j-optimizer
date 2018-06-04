package model;

import filemanager.ConfigFiles;
import filemanager.InputParameters;
import gui.WebApp;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import launcher.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import results.ClientResults;
import results.Results;
import results.ResultsFiles;


public class LauncherModel {

    private static final Logger log = LoggerFactory.getLogger(LauncherModel.class);

    public static void startLinkOptimization() throws GRBException {

        InputParameters ip = ConfigFiles.readInputParameters("/../" + Launcher.configFile);
        ip.initializeParameters("/../");
        new WebApp().initializeResults();

        ParametersModel pm = new ParametersModel(ip);
        pm.initializeVariables();
        Model model = new Model(pm);
        GRBLinExpr expr = new GRBLinExpr();
        // add link costs expressions to the model
        expr.add(model.exprLinkCosts(pm.ip.getBeta()));
        // set objective function
        model.setObjectiveFunction(expr);
        // initialize general constraints
        ConstraintsModel constraintsModel = new ConstraintsModel(pm);
        // set link utilization constraints with costs functions (true)
        constraintsModel.setLinkUtilizationExpr(true);
        // set server utilization constraints without costs functions (false)
        constraintsModel.setServerUtilizationExpr(false);
        double objVal = model.run();
        generateResults(pm, objVal);
        model.finishModel();
    }

    private static void generateResults(ParametersModel pm, double objVal) throws GRBException {
        Results results = null;
        if(objVal!= -1) {
            ResultsModel resultsModel = new ResultsModel(pm);
            new ResultsFiles(pm.ip.getNetworkFile(), pm.ip.getAlpha() + "-" + pm.ip.getBeta());
            results = resultsModel.generate(objVal);
        }
        ClientResults.updateResultsToWebApp(results);
    }
}
