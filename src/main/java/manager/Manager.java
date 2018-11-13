package manager;

import gui.WebClient;
import gui.WebServer;
import gui.elements.Scenario;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import learning.LearningModel;
import lp.Constraints;
import lp.OptimizationModel;
import lp.Variables;
import org.apache.commons.io.FilenameUtils;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import results.Output;
import results.ResultFileWriter;
import utils.ConfigFiles;
import utils.GraphManager;
import utils.KShortestPathGenerator;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static results.Auxiliary.*;

public class Manager {

    private static final Logger log = LoggerFactory.getLogger(Manager.class);
    private static Parameters pm;

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
            pm = ConfigFiles.readParameters(pathFile, fileName + ".yml");
            pm.initialize(pathFile);
            checkTopologyScale();
            new WebServer().initialize(pm);
            printLog(log, INFO, "topology loaded");
            return true;
        } catch (Exception e) {
            printLog(log, ERROR, "reading the input parameters file");
            return false;
        }
    }

    private static void checkTopologyScale() {
        double scalingX = (double) pm.getAux("scaling_x");
        double scalingY = (double) pm.getAux("scaling_y");
        if (scalingX != 1.0 || scalingY != 1.0) {
            for (Node node : pm.getNodes()) {
                int value = (int) Math.round((int) node.getAttribute("y") * scalingY);
                node.setAttribute("y", value);
            }
            for (Node node : pm.getNodes()) {
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
        OptimizationModel optimizationModel = new OptimizationModel(pm);
        printLog(log, INFO, "setting variables");
        Variables variables = new Variables(pm, optimizationModel.getGrbModel());
        optimizationModel.setVariables(variables);
        printLog(log, INFO, "setting constraints");
        new Constraints(pm, optimizationModel, scenario, initialPlacement);
        if (scenario.getModel().equals(ALL_OPT_MODELS) || scenario.getModel().equals(MIGRATION_REPLICATION_RL_MODEL) && model.equals(INITIAL_PLACEMENT_MODEL))
            expr = generateExprForObjectiveFunction(optimizationModel, NUM_OF_SERVERS_OBJ);
        else
            expr = generateExprForObjectiveFunction(optimizationModel, scenario.getObjectiveFunction());
        optimizationModel.setObjectiveFunction(expr, scenario.isMaximization());
        printLog(log, INFO, "running model");
        double objVal = optimizationModel.run();
        Output output = generateOutput(optimizationModel, scenario, initialPlacement);
        submitResults(output, model, objVal, resultFileWriter);
        return output;
    }

    private static Output runRL(String model, Scenario scenario, double objValue, ResultFileWriter resultFileWriter, Output initialPlacement) throws GRBException {
        LearningModel learningModel = new LearningModel(pm);
        double objVal = learningModel.run(initialPlacement, objValue);
        Output output = new Output(pm, scenario);
//        output.setLearningModelResults(learningModel);
        submitResults(output, model, objVal, resultFileWriter);
        return output;
    }

    private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel optimizationModel, String objective) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        double weightLinks = pm.getWeights()[0] / pm.getLinks().size();
        double weightServers = pm.getWeights()[1] / pm.getServers().size();
        double weightServiceDelays = pm.getWeights()[2] / (pm.getPaths().size() * 100);
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
        title.append(formatter.format(pm.getWeights()[0]));
        if (pm.getWeights().length > 1)
            for (int i = 1; i < pm.getWeights().length; i++)
                title.append("-").append(formatter.format(pm.getWeights()[i]));
        return new ResultFileWriter(title.toString());
    }

    private static void submitResults(Output output, String model, double objVal, ResultFileWriter resultFileWriter) throws GRBException {
        if (objVal >= 0) {
            resultFileWriter.createJsonForResults(pm.getScenario() + "_" + model, output);
            WebClient.updateResultsToWebApp(output);
        }
    }

    private static Output generateOutput(OptimizationModel model, Scenario scenario, Output initialPlacement) throws GRBException {
        Output output = new Output(pm, scenario);
        output.setVariable("rSP", model.getVariables().rSP);
        output.setVariable("rSPD", model.getVariables().rSPD);
        output.setVariable("pXSV", model.getVariables().pXSV);
        output.setVariable("rXSVD", model.getVariables().pXSVD);
        output.setVariable("uL", model.getVariables().uL);
        output.setVariable("uX", model.getVariables().uX);
        output.setVariable("pX", model.getVariables().pX);
        output.setVariable("sSVP", model.getVariables().sSVP);
        output.setVariable("dSP", model.getVariables().dSP);
        output.prepareVariablesForJsonFile(model.getObjVal(), initialPlacement);
        return output;
    }
}
