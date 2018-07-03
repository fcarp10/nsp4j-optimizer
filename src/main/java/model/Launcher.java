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
import results.Results;
import results.ResultFiles;


public class Launcher {

    private static Model model;
    private static Output initialOutput;

    public Launcher() {
        Parameters parameters = ConfigFiles.readParameters(App.configFile);
        parameters.initialize();
        new WebApp().initializeResults();
        model = new Model(parameters);
        Variables variables = new Variables();
        variables.initializeVariables(parameters, model.getGrbModel());
        model.setVariables(variables);
    }

    public static void startOptimization(String useCase, String objective) throws GRBException {
        double objVal;
        Output output;

        double linksWeights = model.getParameters().getWeights()[0] / model.getParameters().getLinks().size();
        double serversWeights = model.getParameters().getWeights()[1] / model.getParameters().getServers().size();

        new GeneralConstraints(model);
        SpecificConstraints specificConstraints = new SpecificConstraints(model);

        GRBLinExpr expr = new GRBLinExpr();
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

        switch (useCase) {
            case "init":
                specificConstraints.noParallelPaths();
                model.setObjectiveFunction(expr);
                objVal = model.run();
                output = generateResults(objVal);
                if (output == null)
                    model.finishModel();
                else
                    initialOutput = output;
                break;
            case "mgr":
                if (initialOutput != null) {
                    specificConstraints.noParallelPaths();
                    model.setObjectiveFunction(expr);
                    objVal = model.run();
                    output = generateResults(objVal);
                    if (output != null)
                        output.calculateNumberOfMigrations(initialOutput);
                }
                model.finishModel();
                break;
            case "rep":
                if (initialOutput != null) {
                    specificConstraints.setVariablesFromInitialPlacementAsConstraints(initialOutput);
                    model.setObjectiveFunction(expr);
                    objVal = model.run();
                    output = generateResults(objVal);
                    if (output != null)
                        output.calculateNumberOfReplications();
                }
                model.finishModel();
                break;
            case "both":
                if (initialOutput != null) {
                    model.setObjectiveFunction(expr);
                    objVal = model.run();
                    output = generateResults(objVal);
                    if (output != null) {
                        output.calculateNumberOfMigrations(initialOutput);
                        output.calculateNumberOfReplications();
                    }
                }
                model.finishModel();
                break;
        }
    }

    private static Output generateResults(double objVal) throws GRBException {
        Output output = null;
        Results results = null;
        StringBuilder title = new StringBuilder();
        for (Double d : model.getParameters().getWeights())
            title.append("-").append(d);
        if (objVal >= 0) {
            output = new Output(model);
            results = output.generateResults(objVal);
            ResultFiles resultFiles = new ResultFiles(model.getParameters().getNetworkFile(), title.toString());
            resultFiles.printSummary(results);
            resultFiles.print(results);
        }
        if (objVal >= 0) {
            Client.updateResultsToWebApp(results);
            Client.postMessage("Solution found");
        } else if (output == null)
            Client.postMessage("Please, run initial placement first");
        else
            Client.postMessage("Model is not feasible");
        return output;
    }
}
