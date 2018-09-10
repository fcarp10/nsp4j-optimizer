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

class DeepQ {

    private static final Logger log = LoggerFactory.getLogger(DeepQ.class);
    private MultiLayerNetwork multiLayerNetwork, targetMultiLayerNetwork;
    private List<Experience> experiences;
    private int startSize, batchSize, freq, counter, inputLength, memoryCapacity, lastAction;
    private float discount;
    private Random rnd;

    DeepQ(MultiLayerConfiguration conf, int memoryCapacity, float discount, int batchSize, int freq, int startSize, int inputLength) {
        this.multiLayerNetwork = new MultiLayerNetwork(conf);
        this.multiLayerNetwork.init();
        this.targetMultiLayerNetwork = new MultiLayerNetwork(conf);
        this.targetMultiLayerNetwork.init();
        this.targetMultiLayerNetwork.setParams(multiLayerNetwork.params());
        this.experiences = new ArrayList<>();
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

    int getAction(INDArray input, int[] actionMask, double epsilon) {
        boolean isValid = false;
        INDArray indArrayOutput = multiLayerNetwork.output(input);
        log.debug("DeepQ output: " + indArrayOutput);
        if (epsilon > rnd.nextDouble()) {
            int action = -1;
            while (!isValid) {
                action = rnd.nextInt(indArrayOutput.size(1));
                if (actionMask[action] == 1 && action != this.lastAction)
                    isValid = true;
            }
            this.lastAction = action;
        } else
            this.lastAction = findMaxAction(indArrayOutput, actionMask);
        log.debug("Agent action: " + this.lastAction);
        return this.lastAction;
    }

    private int findMaxAction(INDArray outputs, int actionMask[]) {
        float maxValue = Float.NEGATIVE_INFINITY;
        int actionMax = -1;
        for (int i = 0; i < outputs.size(1); i++) {
            if (actionMask[i] != 1) continue;
            if (outputs.getFloat(i) > maxValue) {
                maxValue = outputs.getFloat(i);
                actionMax = i;
            }
        }
        return actionMax;
    }

    private float findMaxValue(INDArray outputs, int actionMask[]) {
        float maxValue = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < outputs.size(1); i++) {
            if (actionMask[i] != 1) continue;
            if (outputs.getFloat(i) > maxValue)
                maxValue = outputs.getFloat(i);
        }
        return maxValue;
    }

    void observeReward(INDArray inputIndArray, INDArray nextInputIndArray, double reward, int[] actionMask) {
        // TO BE CHANGED, SHOULD REMOVE THE ONE WITH LOWEST REWARD
        if (experiences.size() >= memoryCapacity)
            experiences.remove(rnd.nextInt(experiences.size()));
        experiences.add(new Experience(inputIndArray, nextInputIndArray, lastAction, (float) reward, actionMask));
        if (startSize < experiences.size())
            trainNetwork();
        counter++;
        if (counter == freq) {
            counter = 0;
            targetMultiLayerNetwork.setParams(multiLayerNetwork.params());
        }
    }

    private void trainNetwork() {
        Experience experiences[] = getBatch();
        INDArray combinedLastInputs = combineInputs(experiences);
        INDArray combinedNextInputs = combineNextInputs(experiences);
        INDArray currentOutput = multiLayerNetwork.output(combinedLastInputs);
        INDArray targetOutput = targetMultiLayerNetwork.output(combinedNextInputs);
        for (int i = 0; i < experiences.length; i++) {
            float futureReward = 0;
            if (experiences[i].getNextInputIndArray() != null)
                futureReward = findMaxValue(targetOutput.getRow(i), experiences[i].getActionMask());
            float targetReward = experiences[i].getReward() + discount * futureReward;
            int actionScalar[] = {i, experiences[i].getAction()};
            currentOutput.putScalar(actionScalar, targetReward);
        }
        multiLayerNetwork.fit(combinedLastInputs, currentOutput);
    }

    private Experience[] getBatch() {
        int size = experiences.size() < batchSize ? experiences.size() : batchSize;
        Experience[] batch = new Experience[size];
        for (int i = 0; i < size; i++)
            batch[i] = experiences.get(this.rnd.nextInt(experiences.size()));
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
