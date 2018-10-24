package app;

import filemanager.ConfigFiles;
import filemanager.Parameters;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import learning.LearningModel;
import lp.OptimizationModel;
import results.Output;
import lp.Variables;
import lp.Constraints;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import elements.OutputFiles;
import results.Results;
import elements.Scenario;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static Parameters parameters;
    private static Output initialOutput;

    public static void start(Scenario scenario) {
        try {
            String path = FilenameUtils.getPath(App.class.getClassLoader().getResource(scenario.getInputFilesName() + ".yml").getFile());
            if (System.getProperty("os.name").equals("Mac OS X") || System.getProperty("os.name").equals("Linux"))
                path = "/" + path;
            parameters = ConfigFiles.readParameters(path, scenario.getInputFilesName() + ".yml");
            parameters.initialize(path);
            new WebServer().initialize(parameters.getServers());
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
        new Constraints(parameters, optimizationModel, scenario, initialOutput);
        if (scenario.getUseCase().equals("all") || scenario.getUseCase().equals("exp") && useCase.equals("init"))
            expr = generateExprForObjectiveFunction(optimizationModel, "servers");
        else
            expr = generateExprForObjectiveFunction(optimizationModel, scenario.getObjective());
        optimizationModel.setObjectiveFunction(expr, scenario.isMaximization());
        double objVal = optimizationModel.run();
        Output output = generateModelOutput(optimizationModel);
        submitResults(output, useCase, objVal, outputFiles);
        if (useCase.equals("init"))
            initialOutput = output;
        else
            optimizationModel.finishModel();
        return objVal;
    }

    private static void runRL(String useCase, double objValue, OutputFiles outputFiles) {
        LearningModel learningModel = new LearningModel(parameters);
        double objVal = learningModel.run(initialOutput, objValue);
        Output output = generateModelOutput(learningModel);
        submitResults(output, useCase, objVal, outputFiles);
    }

    private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel optimizationModel, String objective) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        double weightLinks = parameters.getWeights()[0] / parameters.getLinks().size();
        double weightServers = parameters.getWeights()[1] / parameters.getServers().size();
        double weightServiceDelays = parameters.getWeights()[2] / parameters.getPaths().size();
        switch (objective) {
            case "servers":
                expr.add(optimizationModel.usedServersExpr());
                break;
            case "costs":
                expr.add(optimizationModel.linkCostsExpr(weightLinks));
                expr.add(optimizationModel.serverCostsExpr(weightServers));
                expr.add(optimizationModel.serviceDelayExpr(weightServiceDelays));
                break;
            case "utilization":
                expr.add(optimizationModel.linkUtilizationExpr(weightLinks));
                expr.add(optimizationModel.serverUtilizationExpr(weightServers));
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

    private static Output generateModelOutput(OptimizationModel optimizationModel) {
        return new Output(parameters, optimizationModel);
    }

    private static Output generateModelOutput(LearningModel learningModel) {
        return new Output(parameters, learningModel);
    }

    private static void submitResults(Output output, String useCase, double objVal, OutputFiles outputFiles) {
        if (objVal >= 0) {
            Results results = output.generateResults(objVal, initialOutput);
            outputFiles.printSummary(results);
            outputFiles.print(results, useCase);
            WebClient.updateResultsToWebApp(output, results);
            WebClient.postMessage("Solution found");
        } else
            WebClient.postMessage("No solution");
    }
}
