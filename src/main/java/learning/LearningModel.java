package learning;

import filemanager.Parameters;
import lp.Output;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LearningModel {

    private Parameters pm;
    private int trainingIterations;
    private DeepQ deepQ;
    private double maxReward;

    public LearningModel(Parameters pm, double maxReward) {
        this.pm = pm;
        this.trainingIterations = pm.getAux()[0];
        this.deepQ = new DeepQ(pm.getServers().size() * pm.getServices().size() * pm.getServiceLengthAux());
        this.maxReward = maxReward;
    }

    public void run(Output initialOutput) {
        for (int i = 0; i < trainingIterations; i++)
            deepQ.learn(generateInput(), generateEnvironment(initialOutput), maxReward);
    }

    private int[] generateEnvironment(Output initialOutput) {
        int[] environment = new int[pm.getServers().size() * pm.getTotalNumberOfFunctionsAux() + 1];
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    int pointer = x * pm.getTotalNumberOfFunctionsAux() + s * pm.getServices().get(s).getFunctions().size() + v;
                    if (initialOutput.getfXSV()[x][s][v])
                        environment[pointer] = 1;
                    else
                        environment[pointer] = 0;
                }
        return environment;
    }

    private float[] generateInput() {
        Random random = new Random();
        List<float[]> inputList = new ArrayList<>();
        for (int t = 0; t < pm.getTrafficFlows().size(); t++) {
            for (int d = 0; d < pm.getTrafficFlows().get(t).getTrafficDemands().size(); d++) {
                float[] individualInput = new float[4];
                for (int n = 0; n < pm.getNodes().size(); n++) {
                    if (pm.getNodes().get(n).getId().equals(pm.getTrafficFlows().get(t).getSrc()))
                        individualInput[0] = n;
                    if (pm.getNodes().get(n).getId().equals(pm.getTrafficFlows().get(t).getDst()))
                        individualInput[1] = n;
                }
                individualInput[2] = pm.getTrafficFlows().get(t).getTrafficDemands().get(t);
                individualInput[3] = random.nextInt(pm.getTrafficFlows().get(t).getAdmissiblePaths().size());
                inputList.add(individualInput);
            }
        }
        float[] inputArray = new float[inputList.size() * 4];
        for (int i = 0; i < inputList.size(); i++)
            if (inputList.get(i).length >= 0)
                System.arraycopy(inputList.get(i), 0, inputArray, i * 4, inputList.get(i).length);
        return inputArray;
    }
}
