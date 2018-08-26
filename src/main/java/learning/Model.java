
package learning;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.ArrayList;
import java.util.List;

public class Model {

    private Agent agent;
    private final double THRESHOLD = 0.9;
    private INDArray lastIndArray;
    private boolean[] lastOutput;

    public Model(int inputLength) {
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

    public boolean learn(int[] input, int[] environment, double maxReward) {

        INDArray inputIndArray = Nd4j.create(input);
        boolean[] output = agent.getAction(inputIndArray, 1);
        double reward = calculateReward(environment);
        boolean optimalSolution = false;
        if (reward >= maxReward * THRESHOLD) {
//            agent.observeReward(lastIndArray, lastOutput, null, null, reward);
//            optimalSolution = true;
        }
//        else
//            agent.observeReward(lastIndArray, lastOutput, inputIndArray, output, reward);
        this.lastIndArray = inputIndArray;
        this.lastOutput = output;
        return optimalSolution;
    }

    public boolean[] reason(int[] input, double epsilon) {
        boolean[] output = agent.getAction(Nd4j.create(input), epsilon);

        return output;
    }

    private double calculateReward(int[] environment) {
        double reward = 0;

        List<Double> tmpUtilization = new ArrayList<>();
//        for (Integer ignored : numOfServers)
//            tmpUtilization.add(0.0);
//
//        for (int x = 0; x < serversCapacity.size(); x++)
//            for (int v = 0; v < functionsDemands.size(); v++)
//                if (output[(x * functionsDemands.size()) + v])
//                    tmpUtilization.set(x, tmpUtilization.get(x) + (double) functionsDemands.get(v) / (double) serversCapacity.get(x));
//
//        for (Double aTmpUtilization : tmpUtilization)
//            reward += Math.pow(aTmpUtilization, 2);
//
//        reward = serversCapacity.size() - reward;

        return reward;
    }


    private int findIndexMinValue(List<Double> list) {
        double minValue = Integer.MAX_VALUE;
        int indexMinValue = -1;
        for (int i = 0; i < list.size(); i++)
            if (list.get(i) < minValue) {
                indexMinValue = i;
                minValue = list.get(i);
            }
        return indexMinValue;
    }
}
