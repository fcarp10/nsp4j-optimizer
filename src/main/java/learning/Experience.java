package learning;

import org.nd4j.linalg.api.ndarray.INDArray;

public class Experience {

    private INDArray inputIndArray, nextInputIndArray;
    private int[] environment;
    private int action;
    private float reward;

    Experience(INDArray inputIndArray, INDArray nextInputIndArray, int[] environment, int action, float reward) {
        this.inputIndArray = inputIndArray;
        this.nextInputIndArray = nextInputIndArray;
        this.environment = environment;
        this.action = action;
        this.reward = reward;
    }

    INDArray getInputIndArray() {
        return inputIndArray;
    }

    INDArray getNextInputIndArray() {
        return nextInputIndArray;
    }

    public int[] getEnvironment() {
        return environment;
    }

    public int getAction() {
        return action;
    }

    float getReward() {
        return reward;
    }
}
