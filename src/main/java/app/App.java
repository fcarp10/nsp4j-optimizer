package app;

import filemanager.ConfigFiles;
import filemanager.Parameters;
import gui.WebClient;
import gui.WebServer;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import learning.LearningModel;
import lp.OptimizationModel;
import lp.Output;
import lp.Variables;
import lp.constraints.CommonConstraints;
import lp.constraints.UseCasesConstraints;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import results.Files;
import results.Results;
import utils.Scenario;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static Parameters parameters;
    private static OptimizationModel optimizationModel;
    private static Output initialOutput;

    public static void start(Scenario scenario) {
        try {
            String path = FilenameUtils.getPath(App.class.getClassLoader().getResource(scenario.getInputFilesName() + ".yml").getFile());
            if (System.getProperty("os.name").equals("Mac OS X"))
                path = "/" + path;
            parameters = ConfigFiles.readParameters(path, scenario.getInputFilesName() + ".yml");
            parameters.initialize(path);
            new WebServer().initializeResults();
            Files files = initializeResultFiles();
            switch (scenario.getUseCase()) {
                case "all":
                    runLP("init", scenario, files);
                    runLP("mgr", scenario, files);
                    runLP("rep", scenario, files);
                    runLP("rep_mgr", scenario, files);
                    break;
                case "exp":
                    runLP("init", scenario, files);
                    double optimumValue = runLP("rep_mgr", scenario, files);
                    runRL(optimumValue);
                    break;
                default:
                    runLP(scenario.getUseCase(), scenario, files);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("The input files do not exist");
            WebClient.postMessage("Error: the input files do not exist");
        }
    }

    private static double runLP(String useCase, Scenario scenario, Files files) throws GRBException {
        GRBLinExpr expr;
        optimizationModel = new OptimizationModel(parameters);
        Variables variables = new Variables();
        variables.initializeVariables(parameters, optimizationModel.getGrbModel());
        optimizationModel.setVariables(variables);
        new CommonConstraints(optimizationModel, scenario);
        new UseCasesConstraints(optimizationModel, scenario, initialOutput);
        if (scenario.getUseCase().equals("all") || scenario.getUseCase().equals("exp") && useCase.equals("init"))
            expr = generateExprForObjectiveFunction("servers");
        else
            expr = generateExprForObjectiveFunction(scenario.getObjective());
        optimizationModel.setObjectiveFunction(expr, scenario.isMaximization());
        double objVal = optimizationModel.run();
        Output output = generateResults(useCase, objVal, files);
        if (useCase.equals("init"))
            initialOutput = output;
        else
            optimizationModel.finishModel();
        return objVal;
    }

    private static void runRL(double maxReward) throws GRBException {
        LearningModel learningModel = new LearningModel(parameters, maxReward);
        learningModel.run(initialOutput);
    }

    private static GRBLinExpr generateExprForObjectiveFunction(String objective) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        double linksWeights = parameters.getWeights()[0] / parameters.getLinks().size();
        double serversWeights = parameters.getWeights()[1] / parameters.getServers().size();
        switch (objective) {
            case "servers":
                expr.add(optimizationModel.usedServersExpr());
                break;
            case "costs":
                expr.add(optimizationModel.linkUtilizationCostsExpr(linksWeights));
                expr.add(optimizationModel.serverUtilizationCostsExpr(serversWeights));
                break;
            case "utilization":
                expr.add(optimizationModel.linkUtilizationExpr(linksWeights));
                expr.add(optimizationModel.serverUtilizationExpr(serversWeights));
                break;
        }
        return expr;
    }

    private static Files initializeResultFiles() {
        StringBuilder title = new StringBuilder();
        for (Double d : parameters.getWeights())
            title.append("-").append(d);
        return new Files(parameters.getScenario(), title.toString());
    }

    private static Output generateResults(String useCase, double objVal, Files files) throws GRBException {
        Output output = null;
        Results results = null;
        if (objVal >= 0) {
            output = new Output(optimizationModel);
            if (initialOutput != null && !useCase.equals("init")) {
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
        } else
            WebClient.postMessage("No solution");
        return output;
    }
}
