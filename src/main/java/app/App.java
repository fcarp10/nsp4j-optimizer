package app;

import elements.ResultFileWriter;
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
import results.Results;
import elements.Scenario;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static results.Auxiliary.*;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static Parameters parameters;
    private static Output initialOutput;

    public static void start(Scenario scenario) {
        try {
            String path = FilenameUtils.getPath(App.class.getClassLoader().getResource(scenario.getInputFileName() + ".yml").getFile());
            if (System.getProperty("os.name").equals("Mac OS X") || System.getProperty("os.name").equals("Linux"))
                path = "/" + path;
            parameters = ConfigFiles.readParameters(path, scenario.getInputFileName() + ".yml");
            parameters.initialize(path);
            new WebServer().initialize(parameters.getServers());
            ResultFileWriter resultFileWriter = initializeResultFiles();
            switch (scenario.getModel()) {
                case ALL_OPT_MODELS:
                    runLP(INITIAL_PLACEMENT_MODEL, scenario, resultFileWriter);
                    runLP(MIGRATION_MODEL, scenario, resultFileWriter);
                    runLP(REPLICATION_MODEL, scenario, resultFileWriter);
                    runLP(MIGRATION_REPLICATION_MODEL, scenario, resultFileWriter);
                    break;
                case MIGRATION_REPLICATION_RL_MODEL:
                    runLP(INITIAL_PLACEMENT_MODEL, scenario, resultFileWriter);
                    double objValue = runLP(MIGRATION_REPLICATION_MODEL, scenario, resultFileWriter);
                    runRL(MIGRATION_REPLICATION_RL_MODEL, objValue, resultFileWriter);
                    break;
                default:
                    runLP(scenario.getModel(), scenario, resultFileWriter);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("The input files do not exist");
            WebClient.postMessage("Error: the input files do not exist");
        }
    }

    private static double runLP(String model, Scenario scenario, ResultFileWriter resultFileWriter) throws GRBException {
        GRBLinExpr expr;
        OptimizationModel optimizationModel = new OptimizationModel(parameters);
        Variables variables = new Variables(parameters, optimizationModel.getGrbModel());
        optimizationModel.setVariables(variables);
        new Constraints(parameters, optimizationModel, scenario, initialOutput);
        if (scenario.getModel().equals(ALL_OPT_MODELS) || scenario.getModel().equals(MIGRATION_REPLICATION_RL_MODEL) && model.equals(INITIAL_PLACEMENT_MODEL))
            expr = generateExprForObjectiveFunction(optimizationModel, NUM_OF_SERVERS_OBJ);
        else
            expr = generateExprForObjectiveFunction(optimizationModel, scenario.getObjectiveFunction());
        optimizationModel.setObjectiveFunction(expr, scenario.isMaximization());
        double objVal = optimizationModel.run();
        Output output = new Output(parameters, scenario, optimizationModel);
        submitResults(output, model, objVal, resultFileWriter);
        if (model.equals(INITIAL_PLACEMENT_MODEL))
            initialOutput = output;
        else
            optimizationModel.finishModel();
        return objVal;
    }

    private static void runRL(String model, double objValue, ResultFileWriter resultFileWriter) {
        LearningModel learningModel = new LearningModel(parameters);
        double objVal = learningModel.run(initialOutput, objValue);
        Output output = new Output(parameters, learningModel);
        submitResults(output, model, objVal, resultFileWriter);
    }

    private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel optimizationModel, String objective) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        double weightLinks = parameters.getWeights()[0] / parameters.getLinks().size();
        double weightServers = parameters.getWeights()[1] / parameters.getServers().size();
        double weightServiceDelays = parameters.getWeights()[2] / parameters.getPaths().size();
        switch (objective) {
            case NUM_OF_SERVERS_OBJ:
                expr.add(optimizationModel.usedServersExpr());
                break;
            case COSTS_OBJ:
                expr.add(optimizationModel.linkCostsExpr(weightLinks));
                expr.add(optimizationModel.serverCostsExpr(weightServers));
                expr.add(optimizationModel.serviceDelayExpr(weightServiceDelays));
                break;
            case UTILIZATION_OBJ:
                expr.add(optimizationModel.linkUtilizationExpr(weightLinks));
                expr.add(optimizationModel.serverUtilizationExpr(weightServers));
                break;
        }
        return expr;
    }

    private static ResultFileWriter initializeResultFiles() {
        NumberFormat formatter = new DecimalFormat("#.##");
        StringBuilder title = new StringBuilder();
        title.append(formatter.format(parameters.getWeights()[0]));
        if (parameters.getWeights().length > 1)
            for (int i = 1; i < parameters.getWeights().length; i++)
                title.append("-").append(formatter.format(parameters.getWeights()[i]));
        return new ResultFileWriter(title.toString());
    }

    private static void submitResults(Output output, String model, double objVal, ResultFileWriter resultFileWriter) {
        if (objVal >= 0) {
            Results results = output.generateResults(objVal, initialOutput);
            resultFileWriter.createJsonForResults(parameters.getScenario() + "_" + model, results);
            WebClient.updateResultsToWebApp(output, results);
            WebClient.postMessage("Solution found");
        } else
            WebClient.postMessage("No solution");
    }
}
