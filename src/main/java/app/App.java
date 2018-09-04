package app;

import filemanager.ConfigFiles;
import filemanager.Parameters;
import gui.WebClient;
import gui.WebServer;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import learning.LearningModel;
import lp.OptimizationModel;
import results.ModelOutput;
import lp.Variables;
import lp.CommonConstraints;
import lp.SpecialConstraints;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import results.OutputFiles;
import results.Results;
import utils.Scenario;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static Parameters parameters;
    private static ModelOutput initialModelOutput;

    public static void start(Scenario scenario) {
        try {
            String path = FilenameUtils.getPath(App.class.getClassLoader().getResource(scenario.getInputFilesName() + ".yml").getFile());
            if (System.getProperty("os.name").equals("Mac OS X"))
                path = "/" + path;
            parameters = ConfigFiles.readParameters(path, scenario.getInputFilesName() + ".yml");
            parameters.initialize(path);
            new WebServer().initializeResults();
            OutputFiles outputFiles = initializeResultFiles();
            switch (scenario.getUseCase()) {
                case "all":
                    runLP("init", scenario, outputFiles);
                    runLP("mgr", scenario, outputFiles);
                    runLP("rep", scenario, outputFiles);
                    runLP("rep_mgr", scenario, outputFiles);
                    break;
                case "exp":
                    runLP("init", scenario, outputFiles);
                    double objValue = runLP("rep_mgr", scenario, outputFiles);
                    runRL("rep_mgr_rl", objValue, outputFiles);
                    break;
                default:
                    runLP(scenario.getUseCase(), scenario, outputFiles);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("The input files do not exist");
            WebClient.postMessage("Error: the input files do not exist");
        }
    }

    private static double runLP(String useCase, Scenario scenario, OutputFiles outputFiles) throws GRBException {
        GRBLinExpr expr;
        OptimizationModel optimizationModel = new OptimizationModel(parameters);
        Variables variables = new Variables(parameters, optimizationModel.getGrbModel());
        optimizationModel.setVariables(variables);
        new CommonConstraints(parameters, optimizationModel, scenario);
        new SpecialConstraints(parameters, optimizationModel, scenario, initialModelOutput);
        if (scenario.getUseCase().equals("all") || scenario.getUseCase().equals("exp") && useCase.equals("init"))
            expr = generateExprForObjectiveFunction(optimizationModel, "servers");
        else
            expr = generateExprForObjectiveFunction(optimizationModel, scenario.getObjective());
        optimizationModel.setObjectiveFunction(expr, scenario.isMaximization());
        double objVal = optimizationModel.run();
        ModelOutput modelOutput = generateModelOutput(optimizationModel);
        generateResults(modelOutput, useCase, objVal, outputFiles);
        if (useCase.equals("init"))
            initialModelOutput = modelOutput;
        else
            optimizationModel.finishModel();
        return objVal;
    }

    private static void runRL(String useCase, double objValue, OutputFiles outputFiles) {
        LearningModel learningModel = new LearningModel(parameters);
        double objVal = learningModel.run(initialModelOutput, objValue);
        ModelOutput modelOutput = generateModelOutput(learningModel);
        generateResults(modelOutput, useCase, objVal, outputFiles);
    }

    private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel optimizationModel, String objective) throws GRBException {
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

    private static OutputFiles initializeResultFiles() {
        StringBuilder title = new StringBuilder();
        for (Double d : parameters.getWeights())
            title.append("-").append(d);
        return new OutputFiles(parameters.getScenario(), title.toString());
    }

    private static ModelOutput generateModelOutput(OptimizationModel optimizationModel) {
        return new ModelOutput(parameters, optimizationModel);
    }

    private static ModelOutput generateModelOutput(LearningModel learningModel) {
        return new ModelOutput(parameters, learningModel);
    }

    private static void generateResults(ModelOutput modelOutput, String useCase, double objVal, OutputFiles outputFiles) {
        if (objVal >= 0) {
            if (initialModelOutput != null && !useCase.equals("init")) {
                modelOutput.calculateNumberOfMigrations(initialModelOutput);
                modelOutput.calculateNumberOfReplications();
            }
            Results results = modelOutput.generateResults(objVal);
            outputFiles.printSummary(results);
            outputFiles.print(results, useCase);
            WebClient.updateResultsToWebApp(modelOutput, results);
            WebClient.postMessage("Solution found");
        } else
            WebClient.postMessage("No solution");
    }
}
