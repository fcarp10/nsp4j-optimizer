package learning;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Agent {

    private static final Logger log = LoggerFactory.getLogger(Agent.class);
    private MultiLayerNetwork multiLayerNetwork, targetMultiLayerNetwork;
    private List<Experience> memoryAction;
    private int startSize, batchSize, freq, counter, inputLength, memoryCapacity, lastAction;
    private float discount;
    private Random rnd;

    Agent(MultiLayerConfiguration conf, int memoryCapacity, float discount, int batchSize, int freq, int startSize, int inputLength) {

        this.multiLayerNetwork = new MultiLayerNetwork(conf);
        this.multiLayerNetwork.init();
        this.targetMultiLayerNetwork = new MultiLayerNetwork(conf);
        this.targetMultiLayerNetwork.init();
        this.targetMultiLayerNetwork.setParams(multiLayerNetwork.params());
        this.memoryAction = new ArrayList<>();
        this.memoryCapacity = memoryCapacity;
        this.discount = discount;
        this.batchSize = batchSize;
        this.freq = freq;
        this.counter = 0;
        this.startSize = startSize;
        this.inputLength = inputLength;
        this.rnd = new Random();
        this.lastAction = -1;
    }

    int getAction(INDArray input, double epsilon) {
        boolean isValid = false;
        int lastAction = -1;
        INDArray indArrayOutput = multiLayerNetwork.output(input);
        log.debug("DeepQ output: " + indArrayOutput);
        if (epsilon > rnd.nextDouble()) {
            while (!isValid) {
                lastAction = rnd.nextInt(indArrayOutput.size(1));
                isValid = actionMask(lastAction);
            }
            this.lastAction = lastAction;
        } else
            lastAction = findMaxAction(indArrayOutput);
        log.debug("Agent action: " + lastAction);
        return lastAction;
    }

    private boolean actionMask(int action) {
        return action != this.lastAction;
    }

    private int findMaxAction(INDArray outputs) {
        int actionMax = 0;
        float maxValue = outputs.getFloat(0);
        for (int i = 1; i < outputs.size(1); i++) {
            float value = outputs.getFloat(i);
            if (value > maxValue) {
                maxValue = value;
                actionMax = i;
            }
        }
        return actionMax;
    }

    private float findMaxValue(INDArray outputs) {
        float maxValue = 0;
        for (int i = 1; i < outputs.size(1); i++) {
            float value = outputs.getFloat(i);
            if (value > maxValue)
                maxValue = value;
        }
        return maxValue;
    }

    void observeReward(INDArray inputIndArray, INDArray nextInputIndArray, double reward) {
        // TO BE CHANGED, SHOULD REMOVE THE ONE WITH LOWEST REWARD
        if (memoryAction.size() >= memoryCapacity)
            memoryAction.remove(rnd.nextInt(memoryAction.size()));
        memoryAction.add(new Experience(inputIndArray, nextInputIndArray, lastAction, (float) reward));
        if (startSize < memoryAction.size())
            trainNetwork();
        counter++;
        if (counter == freq) {
            counter = 0;
            targetMultiLayerNetwork.setParams(multiLayerNetwork.params());
        }
    }

    private void trainNetwork() {
        Experience actionArray[] = getBatch();
        INDArray combinedLastInputs = combineInputs(actionArray);
        INDArray combinedNextInputs = combineNextInputs(actionArray);
        INDArray currentOutput = multiLayerNetwork.output(combinedLastInputs);
        INDArray targetOutput = targetMultiLayerNetwork.output(combinedNextInputs);

        for (int i = 0; i < actionArray.length; i++) {
            float futureReward = 0;
            if (actionArray[i].getNextInputIndArray() != null)
                futureReward = findMaxValue(targetOutput.getRow(i));
            float targetReward = actionArray[i].getReward() + discount * futureReward;
            int actionScalar[] = {i, actionArray[i].getAction()};
            currentOutput.putScalar(actionScalar, targetReward);
        }
        multiLayerNetwork.fit(combinedLastInputs, currentOutput);
    }

    private Experience[] getBatch() {
        int size = memoryAction.size() < batchSize ? memoryAction.size() : batchSize;
        Experience[] batch = new Experience[size];
        for (int i = 0; i < size; i++)
            batch[i] = memoryAction.get(this.rnd.nextInt(memoryAction.size()));
        return batch;
    }

    private INDArray combineInputs(Experience actionArray[]) {
        INDArray combinedLastInputs = Nd4j.create(actionArray.length, inputLength);
        for (int i = 0; i < actionArray.length; i++)
            combinedLastInputs.putRow(i, actionArray[i].getInputIndArray());
        return combinedLastInputs;
    }

    private INDArray combineNextInputs(Experience actionArray[]) {
        INDArray combinedNextInputs = Nd4j.create(actionArray.length, inputLength);
        for (int i = 0; i < actionArray.length; i++)
            if (actionArray[i].getNextInputIndArray() != null)
                combinedNextInputs.putRow(i, actionArray[i].getNextInputIndArray());
        return combinedNextInputs;
    }
}
