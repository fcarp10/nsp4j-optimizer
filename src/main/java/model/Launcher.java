package model;

import filemanager.ConfigFiles;
import filemanager.Parameters;
import gui.WebApp;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import launcher.App;
import model.constraints.GeneralConstraints;
import model.constraints.SpecificConstraints;
import results.Client;
import results.ResultFiles;
import results.Results;


public class Launcher {

    private static Parameters parameters;
    private static Model model;
    private static Output initialOutput;

    public Launcher() {
        parameters = ConfigFiles.readParameters(App.configFile);
        parameters.initialize();
        new WebApp().initializeResults();
    }

    public static void startOptimization(String useCase, String objectiveFunction, String objective) throws GRBException {
        ResultFiles resultFiles = initializeResultFiles();
        boolean isMaximization = false;
        if (objective.equals("max"))
            isMaximization = true;

        if (useCase.equals("all")) {
            runOptimization("servers", "init", isMaximization, resultFiles);
            runOptimization("mgr", objectiveFunction, isMaximization, resultFiles);
            runOptimization("rep", objectiveFunction, isMaximization, resultFiles);
            runOptimization("rep_mgr", objectiveFunction, isMaximization, resultFiles);
        } else
            runOptimization(useCase, objectiveFunction, isMaximization, resultFiles);
    }

    private static void runOptimization(String useCase, String objectiveFunction, boolean isMaximization, ResultFiles resultFiles) throws GRBException {
        double objVal;
        Output output;
        model = new Model(parameters);
        Variables variables = new Variables();
        variables.initializeVariables(parameters, model.getGrbModel());
        model.setVariables(variables);
        GeneralConstraints generalConstraints = new GeneralConstraints(model);
        SpecificConstraints specificConstraints = new SpecificConstraints(model);
        GRBLinExpr expr = generateExprForObjectiveFunction(objectiveFunction);
        switch (useCase) {
            case "init":
                setAllGeneralConstraints(generalConstraints, false);
                specificConstraints.noParallelPaths();
                break;
            case "mgr":
                if (initialOutput != null) {
                    setAllGeneralConstraints(generalConstraints, true);
                    specificConstraints.noParallelPaths();
                    specificConstraints.reRoutingMigration(initialOutput);
                }
                break;
            case "rep":
                setAllGeneralConstraints(generalConstraints, false);
                if (initialOutput != null)
                    specificConstraints.setVariablesFromInitialPlacementAsConstraints(initialOutput);
                break;
            case "both":
                if (initialOutput != null) {
                    setAllGeneralConstraints(generalConstraints, true);
                    specificConstraints.reRoutingMigration(initialOutput);
                }
        }
        if (useCase.equals("init")) {
            model.setObjectiveFunction(expr, isMaximization);
            objVal = model.run();
            output = generateResults(useCase, objVal, resultFiles);
            if (output == null)
                model.finishModel();
            else
                initialOutput = output;
        } else {
            if (initialOutput != null) {
                model.setObjectiveFunction(expr, isMaximization);
                objVal = model.run();
                generateResults(useCase, objVal, resultFiles);
            }
            model.finishModel();
        }
    }

    private static GRBLinExpr generateExprForObjectiveFunction(String objective) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        double linksWeights = parameters.getWeights()[0] / parameters.getLinks().size();
        double serversWeights = parameters.getWeights()[1] / parameters.getServers().size();
        switch (objective) {
            case "servers":
                expr.add(model.usedServersExpr());
                break;
            case "costs":
                expr.add(model.linkUtilizationCostsExpr(linksWeights));
                expr.add(model.serverUtilizationCostsExpr(serversWeights));
                break;
            case "utilization":
                expr.add(model.linkUtilizationExpr(linksWeights));
                expr.add(model.serverUtilizationExpr(serversWeights));
                break;
        }
        return expr;
    }

    private static void setAllGeneralConstraints(GeneralConstraints generalConstraints, boolean isMigration) throws GRBException {
        generalConstraints.setLinkUtilizationExpr(isMigration);
        generalConstraints.setServerUtilizationExpr();
        generalConstraints.countNumberOfUsedServers();
        generalConstraints.onePathPerDemand();
        generalConstraints.activatePathForService();
        generalConstraints.pathsConstrainedByFunctions();
        generalConstraints.functionPlacement();
        generalConstraints.oneFunctionPerDemand();
        generalConstraints.mappingFunctionsWithDemands();
        generalConstraints.functionSequenceOrder();
    }

    private static ResultFiles initializeResultFiles() {
        StringBuilder title = new StringBuilder();
        for (Double d : parameters.getWeights())
            title.append("-").append(d);
        return new ResultFiles(parameters.getNetworkFile(), title.toString());
    }

    private static Output generateResults(String useCase, double objVal, ResultFiles resultFiles) throws GRBException {
        Output output = null;
        Results results = null;
        if (objVal >= 0) {
            output = new Output(model);
            if (initialOutput != null) {
                output.calculateNumberOfMigrations(initialOutput);
                output.calculateNumberOfReplications();
            }
            results = output.generateResults(objVal);
            resultFiles.printSummary(results);
            resultFiles.print(results, useCase);
        }
        if (objVal >= 0) {
            Client.updateResultsToWebApp(output, results);
            Client.postMessage("Solution found");
        } else if (output == null)
            Client.postMessage("Please, run initial placement first");
        else
            Client.postMessage("Model is not feasible");
        return output;
    }
}
