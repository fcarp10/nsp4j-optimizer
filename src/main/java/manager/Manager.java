package manager;

import gui.WebClient;
import gui.WebServer;
import gui.elements.Scenario;
import gurobi.GRB;
import org.graphstream.graph.Graph;
import results.ResultFileWriter;
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
import utils.ConfigFiles;
import utils.GraphManager;
import utils.KShortestPathGenerator;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static results.Auxiliary.*;

public class Manager {

    private static final Logger log = LoggerFactory.getLogger(Manager.class);
    private static Parameters parameters;
    private static Output initialPlacement;

    private static String getResourcePath(String fileName) {
        try {
            String path = FilenameUtils.getPath(Manager.class.getClassLoader().getResource(fileName + ".yml").getFile());
            if (System.getProperty("os.name").equals("Mac OS X") || System.getProperty("os.name").equals("Linux"))
                path = "/" + path;
            return path;
        } catch (Exception e) {
            printLog(ERROR, "input file not found");
            return null;
        }
    }

    private static boolean readParameters(String pathFile, String fileName) {
        try {
            parameters = ConfigFiles.readParameters(pathFile, fileName + ".yml");
            parameters.initialize(pathFile);
            new WebServer().initialize(parameters);
            return true;
        } catch (Exception e) {
            printLog(ERROR, "reading the input parameters file");
            return false;
        }
    }

    public static void start(Scenario scenario) {
        String path = getResourcePath(scenario.getInputFileName());
        if (path != null & readParameters(path, scenario.getInputFileName()))
            try {
                ResultFileWriter resultFileWriter = initializeResultFiles();
                switch (scenario.getModel()) {
                    case ALL_OPT_MODELS_STRING:
                        initialPlacement = runLP(ALL_OPT_MODELS[0], scenario, resultFileWriter, null);
                        for (int i = 1; i < ALL_OPT_MODELS.length; i++)
                            runLP(ALL_OPT_MODELS[i], scenario, resultFileWriter, initialPlacement);
                        break;
                    case MIGRATION_REPLICATION_RL_MODEL:
                        initialPlacement = runLP(INITIAL_PLACEMENT_MODEL, scenario, resultFileWriter, null);
                        Output output = runLP(MIGRATION_REPLICATION_MODEL, scenario, resultFileWriter, initialPlacement);
                        runRL(MIGRATION_REPLICATION_RL_MODEL, output.getCost(), resultFileWriter, initialPlacement);
                        break;
                    case INITIAL_PLACEMENT_MODEL:
                        initialPlacement = runLP(INITIAL_PLACEMENT_MODEL, scenario, resultFileWriter, null);
                        break;
                    default:
                        runLP(scenario.getModel(), scenario, resultFileWriter, initialPlacement);
                        break;
                }
            } catch (Exception e) {
                printLog(ERROR, "something went wrong with the model");
            }
    }

    public static void generatePaths(Scenario scenario) {
        String path = getResourcePath(scenario.getInputFileName());
        if (path != null)
            try {
                Graph graph = GraphManager.importTopology(path, scenario.getInputFileName());
                KShortestPathGenerator kShortestPathGenerator = new KShortestPathGenerator(graph, 10, 5, path, scenario.getInputFileName());
                kShortestPathGenerator.run();
                printLog(INFO, "paths generated");
            } catch (Exception e) {
                printLog(ERROR, "reading the topology file");
            }
    }

    private static void printLog(String status, String message) {
        switch (status) {
            case ERROR:
                log.error(message);
                break;
            case INFO:
                log.info(message);
                break;
        }
        WebClient.postMessage(status + message);
    }

    private static Output runLP(String model, Scenario scenario, ResultFileWriter resultFileWriter, Output initialPlacement) throws GRBException {
        GRBLinExpr expr;
        OptimizationModel optimizationModel = new OptimizationModel(parameters);
        Variables variables = new Variables(parameters, optimizationModel.getGrbModel());
        optimizationModel.setVariables(variables);
        new Constraints(parameters, optimizationModel, scenario, initialPlacement);
        if (scenario.getModel().equals(ALL_OPT_MODELS) || scenario.getModel().equals(MIGRATION_REPLICATION_RL_MODEL) && model.equals(INITIAL_PLACEMENT_MODEL))
            expr = generateExprForObjectiveFunction(optimizationModel, NUM_OF_SERVERS_OBJ);
        else
            expr = generateExprForObjectiveFunction(optimizationModel, scenario.getObjectiveFunction());
        optimizationModel.setObjectiveFunction(expr, scenario.isMaximization());
        double objVal = optimizationModel.run();
        Output output = new Output(parameters, scenario, optimizationModel);
        submitResults(output, model, objVal, resultFileWriter, initialPlacement);
        return output;
    }

    private static Output runRL(String model, double objValue, ResultFileWriter resultFileWriter, Output initialPlacement) {
        LearningModel learningModel = new LearningModel(parameters);
        double objVal = learningModel.run(initialPlacement, objValue);
        Output output = new Output(parameters, learningModel);
        submitResults(output, model, objVal, resultFileWriter, initialPlacement);
        return output;
    }

    private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel optimizationModel, String objective) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        double weightLinks = parameters.getWeights()[0] / parameters.getLinks().size();
        double weightServers = parameters.getWeights()[1] / parameters.getServers().size();
        double weightServiceDelays = parameters.getWeights()[2] / (parameters.getPaths().size() * 100);
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

    private static void submitResults(Output output, String model, double objVal, ResultFileWriter resultFileWriter, Output initialPlacement) {
        if (objVal >= 0) {
            Results results = output.generateResults(objVal, initialPlacement);
            resultFileWriter.createJsonForResults(parameters.getScenario() + "_" + model, results);
            WebClient.updateResultsToWebApp(output, results);
            printLog(INFO, "solution found");
        } else
            printLog(INFO, "no solution found");
    }
}
