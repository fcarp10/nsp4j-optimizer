package model;

import app.Launcher;
import filemanager.ConfigFiles;
import filemanager.Parameters;
import gui.WebClient;
import gui.WebServer;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import model.constraints.GeneralConstraints;
import model.constraints.SpecificConstraints;
import org.apache.commons.io.FilenameUtils;
import results.Files;
import results.Results;


public class Optimizer {

    private static Parameters parameters;
    private static Model model;
    private static Output initialOutput;

    public Optimizer() {

        String path  = FilenameUtils.getPath(getClass().getClassLoader().getResource(Launcher.configFile).getFile());
        parameters = ConfigFiles.readParameters(path, Launcher.configFile);
        parameters.initialize(path);
        new WebServer().initializeResults();
    }

    public static void start(String useCase, String objectiveFunction, String objective) throws GRBException {
        Files files = initializeResultFiles();
        boolean isMaximization = false;
        if (objective.equals("max"))
            isMaximization = true;

        if (useCase.equals("all")) {
            run("servers", "init", isMaximization, files);
            run("mgr", objectiveFunction, isMaximization, files);
            run("rep", objectiveFunction, isMaximization, files);
            run("rep_mgr", objectiveFunction, isMaximization, files);
        } else
            run(useCase, objectiveFunction, isMaximization, files);
    }

    private static void run(String useCase, String objectiveFunction, boolean isMaximization, Files files) throws GRBException {
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
            output = generateResults(useCase, objVal, files);
            if (output == null)
                model.finishModel();
            else
                initialOutput = output;
        } else {
            if (initialOutput != null) {
                model.setObjectiveFunction(expr, isMaximization);
                objVal = model.run();
                generateResults(useCase, objVal, files);
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

    private static Files initializeResultFiles() {
        StringBuilder title = new StringBuilder();
        for (Double d : parameters.getWeights())
            title.append("-").append(d);
        return new Files(parameters.getNetworkFile(), title.toString());
    }

    private static Output generateResults(String useCase, double objVal, Files files) throws GRBException {
        Output output = null;
        Results results = null;
        if (objVal >= 0) {
            output = new Output(model);
            if (initialOutput != null) {
                output.calculateNumberOfMigrations(initialOutput);
                output.calculateNumberOfReplications();
            }
            results = output.generateResults(objVal);
            files.printSummary(results);
            files.print(results, useCase);
        }
        if (objVal >= 0) {
            WebClient.updateResultsToWebApp(output, results);
            WebClient.postMessage("Solution found");
        } else if (output == null)
            WebClient.postMessage("Please, run initial placement first");
        else
            WebClient.postMessage("Model is not feasible");
        return output;
    }
}
