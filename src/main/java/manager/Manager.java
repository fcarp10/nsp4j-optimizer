package manager;

import gui.WebClient;
import gui.WebServer;
import gui.elements.Scenario;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
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
import utils.ConfigFiles;
import utils.GraphManager;
import utils.KShortestPathGenerator;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static results.Auxiliary.*;

public class Manager {

    private static final Logger log = LoggerFactory.getLogger(Manager.class);
    private static Parameters parameters;

    private static String getResourcePath(String fileName) {
        try {
            String path = FilenameUtils.getPath(Manager.class.getClassLoader().getResource("scenarios/" + fileName + ".yml").getFile());
            if (System.getProperty("os.name").equals("Mac OS X") || System.getProperty("os.name").equals("Linux"))
                path = "/" + path;
            return path;
        } catch (Exception e) {
            printLog(log, ERROR, "input file not found");
            return null;
        }
    }

    private static boolean readParameters(String pathFile, String fileName) {
        try {
            printLog(log, INFO, "loading topology");
            parameters = ConfigFiles.readParameters(pathFile, fileName + ".yml");
            parameters.initialize(pathFile);
            checkTopologyScale();
            new WebServer().initialize(parameters);
            printLog(log, INFO, "topology loaded");
            return true;
        } catch (Exception e) {
            printLog(log, ERROR, "reading the input parameters file");
            return false;
        }
    }

    private static void checkTopologyScale() {
        double scalingX = (double) parameters.getAux("scaling_x");
        double scalingY = (double) parameters.getAux("scaling_y");
        if (scalingX != 1.0 || scalingY != 1.0) {
            for (Node node : parameters.getNodes()) {
                int value = (int) Math.round((int) node.getAttribute("y") * scalingY);
                node.setAttribute("y", value);
            }
            for (Node node : parameters.getNodes()) {
                int value = (int) Math.round((int) node.getAttribute("x") * scalingX);
                node.setAttribute("x", value);
            }
        }
    }

    public static void loadTopology(String fileName) {
        String path = getResourcePath(fileName);
        if (path != null)
            readParameters(path, fileName);
    }

    public static Output start(Scenario scenario, Output initialOutput) {
        try {
            ResultFileWriter resultFileWriter = initializeResultFiles();
            printLog(log, INFO, "initializing model");
            switch (scenario.getModel()) {
                case ALL_OPT_MODELS_STRING:
                    initialOutput = runLP(ALL_OPT_MODELS[0], scenario, resultFileWriter, null);
                    for (int i = 1; i < ALL_OPT_MODELS.length; i++)
                        runLP(ALL_OPT_MODELS[i], scenario, resultFileWriter, initialOutput);
                    break;
                case MIGRATION_REPLICATION_RL_MODEL:
                    initialOutput = runLP(INITIAL_PLACEMENT_MODEL, scenario, resultFileWriter, null);
                    Output output = runLP(MIGRATION_REPLICATION_MODEL, scenario, resultFileWriter, initialOutput);
                    runRL(MIGRATION_REPLICATION_RL_MODEL, scenario, output.getObjVal(), resultFileWriter, initialOutput);
                    break;
                case INITIAL_PLACEMENT_MODEL:
                    initialOutput = runLP(INITIAL_PLACEMENT_MODEL, scenario, resultFileWriter, null);
                    break;
                default:
                    runLP(scenario.getModel(), scenario, resultFileWriter, initialOutput);
                    break;
            }
            printLog(log, INFO, "ready");
        } catch (Exception e) {
            printLog(log, ERROR, "something went wrong with the model");
        }
        return initialOutput;
    }

    public static void generatePaths(Scenario scenario) {
        String path = getResourcePath(scenario.getInputFileName());
        if (path != null)
            try {
                Graph graph = GraphManager.importTopology(path, scenario.getInputFileName());
                printLog(log, INFO, "generating paths");
                KShortestPathGenerator kShortestPathGenerator = new KShortestPathGenerator(graph, 10, 5, path, scenario.getInputFileName());
                kShortestPathGenerator.run();
                printLog(log, INFO, "paths generated");
            } catch (Exception e) {
                printLog(log, ERROR, "reading the topology file");
            }
    }

    private static Output runLP(String model, Scenario scenario, ResultFileWriter resultFileWriter, Output initialPlacement) throws GRBException {
        GRBLinExpr expr;
        OptimizationModel optimizationModel = new OptimizationModel(parameters);
        printLog(log, INFO, "setting variables");
        Variables variables = new Variables(parameters, optimizationModel.getGrbModel());
        optimizationModel.setVariables(variables);
        printLog(log, INFO, "setting constraints");
        new Constraints(parameters, optimizationModel, scenario, initialPlacement);
        if (scenario.getModel().equals(ALL_OPT_MODELS) || scenario.getModel().equals(MIGRATION_REPLICATION_RL_MODEL) && model.equals(INITIAL_PLACEMENT_MODEL))
            expr = generateExprForObjectiveFunction(optimizationModel, NUM_OF_SERVERS_OBJ);
        else
            expr = generateExprForObjectiveFunction(optimizationModel, scenario.getObjectiveFunction());
        optimizationModel.setObjectiveFunction(expr, scenario.isMaximization());
        printLog(log, INFO, "running model");
        double objVal = optimizationModel.run();
        Output output = new Output(parameters, scenario);
        output.setOptimizationModelResults(optimizationModel);
        submitResults(output, model, objVal, resultFileWriter, initialPlacement);
        return output;
    }

    private static Output runRL(String model, Scenario scenario, double objValue, ResultFileWriter resultFileWriter, Output initialPlacement) {
        LearningModel learningModel = new LearningModel(parameters);
        double objVal = learningModel.run(initialPlacement, objValue);
        Output output = new Output(parameters, scenario);
        output.setLearningModelResults(learningModel);
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
            output.prepareVariablesForJsonFile(objVal, initialPlacement);
            resultFileWriter.createJsonForResults(parameters.getScenario() + "_" + model, output);
            WebClient.updateResultsToWebApp(output);
        }
    }
}
