package learning;

import org.nd4j.linalg.api.ndarray.INDArray;

public class Experience {

    private INDArray inputIndArray, nextInputIndArray;
    private boolean[] environment;
    private int action;
    private float reward;

    public Experience(INDArray inputIndArray, INDArray nextInputIndArray, boolean[] environment, int action, float reward) {
        this.inputIndArray = inputIndArray;
        this.nextInputIndArray = nextInputIndArray;
        this.environment = environment;
        this.action = action;
        this.reward = reward;
    }

    public INDArray getInputIndArray() {
        return inputIndArray;
    }

    public INDArray getNextInputIndArray() {
        return nextInputIndArray;
    }

    public boolean[] getEnvironment() {
        return environment;
    }

    public int getAction() {
        return action;
    }

    public float getReward() {
        return reward;
    }
}
