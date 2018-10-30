package learning;

import manager.Parameters;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import results.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import results.Auxiliary;

import java.util.*;

public class LearningModel {

    private static final Logger log = LoggerFactory.getLogger(LearningModel.class);
    private Parameters pm;
    private DeepQ deepQ;
    private final double THRESHOLD = 1.0;
    private double ux[];
    private double ul[];
    private boolean xsvd[][][][];
    private boolean xsv[][][];
    private boolean sp[][];
    private boolean spd[][][];
    private boolean svp[][][];

    public LearningModel(Parameters pm) {
        this.pm = pm;
        int inputLength = pm.getServers().size() * pm.getServices().size() * pm.getServiceLengthAux() + 1;
        int outputLength = pm.getServers().size() * pm.getServices().size() * pm.getServiceLengthAux();
        int hiddenLayerOut = 150;
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(0.0025)
                .updater(Updater.NESTEROVS)
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(inputLength)
                        .nOut(hiddenLayerOut)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nIn(hiddenLayerOut)
                        .nOut(outputLength)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.IDENTITY)
                        .build())
                .pretrain(false)
                .backprop(true)
                .build();
        deepQ = new DeepQ(conf, 100000, .99f, 1024, 100, 1024, inputLength);
    }

    public double run(Output initialPlacement, double minCost) {
        float[] input = generateInput(initialPlacement);
        int[] environment = generateEnvironment(initialPlacement);
        for (int i = 0; i < pm.getAux()[1]; i++)
            learn(input, environment, minCost, i);
        return reason(input, environment, minCost, 0);
    }

    private float[] generateInput(Output initialOutput) {
        List<float[]> inputList = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                    if (initialOutput.getrSPD()[s][p][d]) {
                        float[] individualInput = new float[2 + pm.getServiceLengthAux()];
                        individualInput[0] = pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d);
                        individualInput[1] = p;
                        for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                            for (int x = 0; x < pm.getServers().size(); x++)
                                if (initialOutput.getpXSVD()[x][s][v][d])
                                    individualInput[2 + v] = x;
                        inputList.add(individualInput);
                    }
                }
        float[] inputArray = new float[inputList.size() * (2 + pm.getServiceLengthAux()) + 1];
        for (int i = 0; i < inputList.size(); i++)
            if (inputList.get(i).length >= 0)
                System.arraycopy(inputList.get(i), 0, inputArray, i * 4, inputList.get(i).length);
        return inputArray;
    }

    private int[] generateEnvironment(Output initialOutput) {
        int[] environment = new int[pm.getServers().size() * pm.getTotalNumberOfFunctionsAux()];
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    int pointer = x * pm.getTotalNumberOfFunctionsAux() + s * pm.getServices().get(s).getFunctions().size() + v;
                    if (initialOutput.getpXSV()[x][s][v])
                        environment[pointer] = 1;
                    else
                        environment[pointer] = 0;
                }
        return environment;
    }

    private void learn(float[] input, int[] environment, double minCost, int iteration) {
        int[] localEnvironment = environment.clone();
        int timeStep = 0;
        int action = -1;
        while (true) {
            INDArray inputIndArray = Nd4j.create(input);
            int[] actionMask = generateActionMask(localEnvironment, action);
            action = deepQ.getAction(inputIndArray, actionMask, 1);
            modifyEnvironment(false, localEnvironment, action);
            int[] nextActionMask = generateActionMask(localEnvironment, action);
            double reward = computeReward();
            timeStep++;
            input[input.length - 1] = timeStep;
            if (reward >= (1 - minCost) * THRESHOLD) {
                deepQ.observeReward(inputIndArray, null, reward, nextActionMask);
                break;
            } else
                deepQ.observeReward(inputIndArray, Nd4j.create(input), reward, nextActionMask);
        }
        log.info("iteration " + iteration + " -> " + timeStep + " steps");
    }

    private double reason(float[] input, int[] environment, double minCost, double epsilon) {
        int[] localEnvironment = environment.clone();
        int timeStep = 0;
        double reward;
        int action = -1;
        while (true) {
            INDArray inputIndArray = Nd4j.create(input);
            int[] actionMask = generateActionMask(localEnvironment, action);
            action = deepQ.getAction(inputIndArray, actionMask, epsilon);
            modifyEnvironment(true, localEnvironment, action);
            int[] nextActionMask = generateActionMask(localEnvironment, action);
            reward = computeReward();
            timeStep++;
            input[input.length - 1] = timeStep;
            if (reward >= (1 - minCost) * THRESHOLD)
                break;
            else {
                deepQ.observeReward(inputIndArray, Nd4j.create(input), reward, nextActionMask);
                if (timeStep > 15)
                    break;
            }
        }
        computeFunctionsServers();
        log.info("reasoning in -> " + timeStep + " steps");
        return 1 - reward;
    }

    private int[] generateActionMask(int[] environment, int pastAction) {
        int[] actionMask = new int[environment.length];
        for (int v = 0; v < pm.getServiceLengthAux(); v++) {
            int activations = 0;
            for (int x = 0; x < pm.getServers().size(); x++)
                if (environment[x * pm.getServiceLengthAux() + v] == 1)
                    activations++;
            for (int x = 0; x < pm.getServers().size(); x++) {
                if (activations == pm.getServices().size())
                    if (environment[x * pm.getServiceLengthAux() + v] == 0)
                        actionMask[x * pm.getServiceLengthAux() + v] = 1;
                if (activations > pm.getServices().size())
                    actionMask[x * pm.getServiceLengthAux() + v] = 1;
            }
        }
        if (pastAction != -1)
            actionMask[pastAction] = 0;
        return actionMask;
    }

    private void modifyEnvironment(boolean isReasoning, int[] environment, int action) {
        if (environment[action] == 1)
            environment[action] = 0;
        else environment[action] = 1;
        Map<Integer, List<Path>> servicesAdmissiblePaths = getServicesAdmissiblePaths(environment);
        chooseServersPerDemand(servicesAdmissiblePaths, environment);
        calculateServerUtilization(environment);
        calculateLinkUtilization(environment);
        if (isReasoning) {
            computePaths();
            calculateReroutingTraffic();
        }
    }

    private Map<Integer, List<Path>> getServicesAdmissiblePaths(int[] environment) {
        Map<Integer, List<Path>> servicesAdmissiblePaths = new HashMap<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            servicesAdmissiblePaths.put(s, computeAdmissiblePaths(s, environment));
        return servicesAdmissiblePaths;
    }

    private List<Path> computeAdmissiblePaths(int s, int[] environment) {
        List<Path> admissiblePaths = new ArrayList<>();
        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                boolean allFunctionsExist = true;
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    boolean activatedInPath = false;
                    outerLoop:
                    for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++) {
                        Node node = pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n);
                        for (int x = 0; x < pm.getServers().size(); x++) {
                            if (pm.getServers().get(x).getNodeParent().equals(node))
                                if (environment[x * pm.getServices().get(s).getFunctions().size() + v] == 1) {
                                    activatedInPath = true;
                                    break outerLoop;
                                }
                        }
                    }
                    if (!activatedInPath) {
                        allFunctionsExist = false;
                        break;
                    }
                }
                if (allFunctionsExist)
                    admissiblePaths.add(pm.getPaths().get(p));
            }
        return admissiblePaths;
    }

    private void chooseServersPerDemand(Map<Integer, List<Path>> tSP, int[] environment) {
        Random rnd = new Random();
        xsvd = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()][pm.getDemandsPerTrafficFlowAux()];
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                Path path = tSP.get(s).get(rnd.nextInt(tSP.get(s).size()));
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    outerLoop:
                    for (int n = 0; n < path.getNodePath().size(); n++)
                        for (int x = 0; x < pm.getServers().size(); x++)
                            if (pm.getServers().get(x).getNodeParent().equals(path.getNodePath().get(n)))
                                if (environment[x * pm.getServices().get(s).getFunctions().size() + v] == 1) {
                                    xsvd[x][s][v][d] = true;
                                    break outerLoop;
                                }
                }
            }
    }

    private void calculateServerUtilization(int[] environment) {
        ux = new double[pm.getServers().size()];
        for (int x = 0; x < pm.getServers().size(); x++) {
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    double demands = 0;
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                        if (xsvd[x][s][v][d]) {
                            demands += pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d);
                        }
                    }
                    if (environment[x * pm.getServices().get(s).getFunctions().size() + v] == 1)
                        ux[x] += ((demands * pm.getServices().get(s).getFunctions().get(v).getLoad())
                                + (pm.getServices().get(s).getFunctions().get(v).getLoad() * pm.getAux()[0]))
                                / pm.getServers().get(x).getCapacity();
                }
        }
    }

    private void calculateLinkUtilization(int[] environment) {
        ul = new double[pm.getLinks().size()];
        // TODO
    }

    private double computeReward() {
        double cost, totalCost = 0;
        for (Double serverUtilization : ux) {
            cost = 0;
            for (int f = 0; f < Auxiliary.costFunctions.getValues().size(); f++) {
                double value = Auxiliary.costFunctions.getValues().get(f)[0] * serverUtilization
                        + Auxiliary.costFunctions.getValues().get(f)[1];
                if (value > cost)
                    cost = value;
            }
            totalCost += cost;
        }
        return 1 - (totalCost / pm.getServers().size());
    }

    private void computeFunctionsServers() {
        xsv = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()];
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (xsvd[x][s][v][d])
                            xsv[x][s][v] = true;
    }

    private void computePaths() {
        sp = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()];
        spd = new boolean[pm.getServices().size()][pm.getPathsPerTrafficFlowAux()][pm.getDemandsPerTrafficFlowAux()];
        // TODO
    }

    private void calculateReroutingTraffic() {
        // TODO
    }

    public double[] getUx() {
        return ux;
    }

    public double[] getUl() {
        return ul;
    }

    public boolean[][][][] getXsvd() {
        return xsvd;
    }

    public boolean[][][] getXsv() {
        return xsv;
    }

    public boolean[][] getSp() {
        return sp;
    }

    public boolean[][][] getSpd() {
        return spd;
    }

    public boolean[][][] getSvp() {
        return svp;
    }


}
