
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
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import utils.Auxiliary;

import java.util.ArrayList;
import java.util.List;

public class DeepQ {

    private Agent agent;
    private final double THRESHOLD = 0.9;
    private INDArray lastInputIndArray;
    private boolean[] lastOutput;

    public DeepQ(int inputLength) {
        int outputLength = 1;
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
        agent = new Agent(conf, 100000, .99f, 1024, 500, 1024, inputLength);
    }

    void learn(float[] input, int[] environment, double maxReward, Parameters parameters) {
        int[] localEnvironment = environment.clone();
        INDArray inputIndArray = Nd4j.create(input);
        boolean optimal = false;
        while (!optimal) {
            int action = agent.getAction(inputIndArray, 1);
            localEnvironment = modifyEnvironment(localEnvironment, action);
            double reward = calculateReward(localEnvironment, input, parameters);
            if (reward >= maxReward * THRESHOLD) {
//            agent.observeReward(lastInputIndArray, lastOutput, null, null, reward);
                optimal = true;
            } else
                agent.observeReward(inputIndArray, action, reward);

            this.lastInputIndArray = inputIndArray;
//        this.lastOutput = output;
        }
    }

    private int[] modifyEnvironment(int[] environment, int action) {
        if (environment[action] == 1)
            environment[action] = 0;
        else environment[action] = 1;
        return environment;
    }

    public void reason(int[] input, double epsilon) {
        int output = agent.getAction(Nd4j.create(input), epsilon);
    }

    private double calculateReward(int[] environment, float[] input, Parameters pm) {
        double reward = 0;
        List<Double> tmpUtilization = new ArrayList<>();
        double utilization;

        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                List<Integer> availablePaths = new ArrayList<>();
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                        boolean activatedInPath = false;
                        for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++) {
                            Node node = pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n);
                            for (int x = 0; x < pm.getServers().size(); x++) {
                                if (pm.getServers().get(x).getNodeParent().equals(node)) {
                                    if (environment[x * v] == 1)
                                        activatedInPath = true;
                                }
                            }
                        }
                        if (!activatedInPath)
                            break;
                    }
                    availablePaths.add(p);
                }
            }


        for (int x = 0; x < pm.getServers().size(); x++) {
            utilization = 0.0;
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {


                }


            tmpUtilization.add(utilization);
        }

        for (Double serverUtilization : tmpUtilization) {
            for (int f = 0; f < Auxiliary.linearCostFunctions.getValues().size(); f++) {
                double cost = Auxiliary.linearCostFunctions.getValues().get(f)[0] * serverUtilization + Auxiliary.linearCostFunctions.getValues().get(f)[1];
                reward += 1 - cost;
            }
        }
        return reward;
    }
}
