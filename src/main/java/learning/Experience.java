package learning;

import org.nd4j.linalg.api.ndarray.INDArray;

public class Experience {

    private INDArray inputIndArray, nextInputIndArray;
    private int action;
    private float reward;

    Experience(INDArray inputIndArray, INDArray nextInputIndArray, int action, float reward) {
        this.inputIndArray = inputIndArray;
        this.nextInputIndArray = nextInputIndArray;
        this.action = action;
        this.reward = reward;
    }

    INDArray getInputIndArray() {
        return inputIndArray;
    }

    INDArray getNextInputIndArray() {
        return nextInputIndArray;
    }

    public int getAction() {
        return action;
    }

    float getReward() {
        return reward;
    }
}
