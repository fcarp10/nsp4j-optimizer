package optimizer.algorithms.learning;


import org.nd4j.linalg.api.ndarray.INDArray;

class Experience {

    private INDArray inputIndArray, nextInputIndArray;
    private int action;
    private float reward;
    private int[] nextActionMask;

    Experience(INDArray inputIndArray, INDArray nextInputIndArray, int action, float reward, int[] nextActionMask){
        this.inputIndArray = inputIndArray;
        this.nextInputIndArray = nextInputIndArray;
        this.action = action;
        this.reward = reward;
        this.nextActionMask = nextActionMask;
    }

    INDArray getInputIndArray() {
        return inputIndArray;
    }

    INDArray getNextInputIndArray() {
        return nextInputIndArray;
    }

    int getAction() {
        return action;
    }

    float getReward() {
        return reward;
    }

    int[] getNextActionMask() {
        return nextActionMask;
    }

}
