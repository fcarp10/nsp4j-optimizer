
package learning;

import filemanager.Parameters;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Auxiliary;

import java.util.*;

public class DeepQ {

    private static final Logger log = LoggerFactory.getLogger(DeepQ.class);
    private Agent agent;
    private final double THRESHOLD = 1.0;
    private Parameters pm;

    public DeepQ(Parameters pm) {
        this.pm = pm;
        int inputLength = pm.getServers().size() * pm.getServices().size() * pm.getServiceLengthAux();
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
        agent = new Agent(conf, 100000, .99f, 1024, 100, 1024, inputLength);
    }

    int learn(float[] input, int[] environment, double minCost) {
        int[] localEnvironment = environment.clone();
        INDArray inputIndArray = Nd4j.create(input);
        boolean optimal = false;
        int timeStep = 0;
        while (!optimal) {
            int action = agent.getAction(inputIndArray, 1);
            localEnvironment = modifyEnvironment(localEnvironment, action);
            double reward = computeReward(localEnvironment);
            timeStep++;
            input[input.length - 1] = timeStep;
            if (reward >= (1 - minCost) * THRESHOLD) {
                agent.observeReward(inputIndArray, null, localEnvironment, reward);
                optimal = true;
            } else
                agent.observeReward(inputIndArray, Nd4j.create(input), localEnvironment, reward);
        }
        return timeStep;
    }

    private int[] modifyEnvironment(int[] environment, int action) {
        if (environment[action] == 1)
            environment[action] = 0;
        else environment[action] = 1;
        return environment;
    }

    int reason(float[] input, int[] environment, double minCost, double epsilon) {
        int[] localEnvironment = environment.clone();
        INDArray inputIndArray = Nd4j.create(input);
        boolean optimal = false;
        int timeStep = 0;
        while (!optimal) {
            int action = agent.getAction(inputIndArray, epsilon);
            localEnvironment = modifyEnvironment(localEnvironment, action);
            double reward = computeReward(localEnvironment);
            timeStep++;
            if (reward >= (1 - minCost) * THRESHOLD)
                optimal = true;
            else {
                agent.observeReward(inputIndArray, Nd4j.create(input), localEnvironment, reward);
                if (timeStep > 15)
                    break;
            }
        }
        return timeStep;
    }

    private double computeReward(int[] environment) {

        Map<Integer, List<Path>> tSP = new HashMap<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            tSP.put(s, computeAdmissiblePaths(s, environment));
        boolean[][][][] fXSVD = chooseServers(tSP, environment);

        List<Double> uX = new ArrayList<>();
        for (int x = 0; x < pm.getServers().size(); x++) {
            uX.add(0.0);
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    double demands = 0;
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                        if (fXSVD[x][s][v][d]) {
                            demands += pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d);
                        }
                    }
                    if (environment[x * pm.getServices().get(s).getFunctions().size() + v] == 1)
                        uX.set(x, uX.get(x) + ((demands * pm.getServices().get(s).getFunctions().get(v).getLoad()) + (pm.getServices().get(s).getFunctions().get(v).getLoad() * pm.getAux()[0])) / pm.getServers().get(x).getCapacity());
                }
        }

        double cost, totalCost = 0;
        for (Double serverUtilization : uX) {
            cost = 0;
            for (int f = 0; f < Auxiliary.linearCostFunctions.getValues().size(); f++) {
                double value = Auxiliary.linearCostFunctions.getValues().get(f)[0] * serverUtilization + Auxiliary.linearCostFunctions.getValues().get(f)[1];
                if (value > cost)
                    cost = value;
            }
            totalCost += cost;
        }
        return 1 - (totalCost / pm.getServers().size());
    }

    private List<Path> computeAdmissiblePaths(int s, int[] environment) {
        List<Path> admissiblePaths = new ArrayList<>();
        for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    boolean activatedInPath = false;
                    outerLoop:
                    for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++) {
                        Node node = pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n);
                        for (int x = 0; x < pm.getServers().size(); x++) {
                            if (pm.getServers().get(x).getNodeParent().equals(node)) {
                                if (environment[x * pm.getServices().get(s).getFunctions().size() + v] == 1) {
                                    activatedInPath = true;
                                    break outerLoop;
                                }
                            }
                        }
                    }
                    if (!activatedInPath)
                        break;
                }
                admissiblePaths.add(pm.getPaths().get(p));
            }
        }
        return admissiblePaths;
    }

    private boolean[][][][] chooseServers(Map<Integer, List<Path>> tSP, int[] environment) {
        Random rnd = new Random();
        boolean[][][][] fXSVD = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLengthAux()][pm.getDemandsPerTrafficFlowAux()];
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                Path path = tSP.get(s).get(rnd.nextInt(tSP.get(s).size()));
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    outerLoop:
                    for (int n = 0; n < path.getNodePath().size(); n++)
                        for (int x = 0; x < pm.getServers().size(); x++)
                            if (pm.getServers().get(x).getNodeParent().equals(path.getNodePath().get(n)))
                                if (environment[x * pm.getServices().get(s).getFunctions().size() + v] == 1) {
                                    fXSVD[x][s][v][d] = true;
                                    break outerLoop;
                                }
                }
            }
        return fXSVD;
    }
}
