
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

public class FunctionAllocator {

    private Agent agent;
    private List<Integer> serversCapacity;
    private List<Integer> functionsDemands;
    private final double THRESHOLD = 0.9;
    private INDArray lastIndArray;
    private boolean[] lastOutput;

    public FunctionAllocator(List<Integer> functionsDemands, List<Integer> serversCapacity) {

        this.functionsDemands = functionsDemands;
        this.serversCapacity = serversCapacity;
        int inputLength = 2 + functionsDemands.size() + serversCapacity.size();
        int outputLength = serversCapacity.size() * functionsDemands.size();
        lastIndArray = null;
        lastOutput = null;

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

        agent = new Agent(conf, 100000, .99f, 1024, 500, 1024, inputLength, outputLength);

    }

    public boolean learn(List<Integer> input) {

        INDArray inputIndArray = createINDArray(input);
        double maxReward = calculateMaxReward();
        boolean[] output = agent.getOutput(inputIndArray, 1, functionsDemands.size());
        double reward = calculateReward(output);
        boolean optimalSolution = false;
//        if (reward >= maxReward * THRESHOLD) {
//            agent.observeReward(lastIndArray, lastOutput, null, null, reward);
//            optimalSolution = true;
//        } else
//            agent.observeReward(lastIndArray, lastOutput, inputIndArray, output, reward);
        this.lastIndArray = inputIndArray;
        this.lastOutput = output;
        return optimalSolution;
    }

    public boolean[] reason(List<Integer> input, double epsilon) {
        boolean[] output = agent.getOutput(createINDArray(input), epsilon, functionsDemands.size());

        return output;
    }

    private INDArray createINDArray(List<Integer> input) {
        float convertedInput[] = new float[input.size()];
        for (int i = 0; i < input.size(); i++)
            convertedInput[i] = input.get(i);
        return Nd4j.create(convertedInput);
    }

    private double calculateReward(boolean[] output) {
        double reward = 0;
        List<Double> tmpUtilization = new ArrayList<>();
        for (Integer ignored : serversCapacity)
            tmpUtilization.add(0.0);

        for (int x = 0; x < serversCapacity.size(); x++)
            for (int v = 0; v < functionsDemands.size(); v++)
                if (output[(x * functionsDemands.size()) + v])
                    tmpUtilization.set(x, tmpUtilization.get(x) + (double) functionsDemands.get(v) / (double) serversCapacity.get(x));

        for (Double aTmpUtilization : tmpUtilization)
            reward += Math.pow(aTmpUtilization, 2);

        reward = serversCapacity.size() - reward;

        return reward;
    }

    private double calculateMaxReward() {
        double maxReward = 0;
        List<Double> tmpUtilization = new ArrayList<>();
        for (Integer ignored : serversCapacity)
            tmpUtilization.add(0.0);

        for (Integer functionDemand : functionsDemands) {
            int x = findIndexMinValue(tmpUtilization);
            tmpUtilization.set(x, tmpUtilization.get(x) + (double) functionDemand / (double) serversCapacity.get(x));
        }

        for (Double aTmpUtilization : tmpUtilization)
            maxReward += Math.pow(aTmpUtilization, 2);

        maxReward = serversCapacity.size() - maxReward;

        return maxReward;
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
