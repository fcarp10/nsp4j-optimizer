package learning;

import filemanager.Parameters;
import lp.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LearningModel {

    private static final Logger log = LoggerFactory.getLogger(LearningModel.class);
    private Parameters pm;
    private int trainingIterations;
    private DeepQ deepQ;
    private double minCost;

    public LearningModel(Parameters pm, double minCost) {
        this.pm = pm;
        this.trainingIterations = pm.getAux()[1];
        this.deepQ = new DeepQ(pm);
        this.minCost = minCost;
    }

    public void run(Output initialPlacement) {

        float[] input = generateInput(initialPlacement);
        int[] environment = generateEnvironment(initialPlacement);
        int steps;
        for (int i = 0; i < trainingIterations; i++) {
            steps = deepQ.learn(input, environment, minCost);
            log.info("iteration " + i + " -> " + steps + " steps");
        }
        for (int i = 0; i < 10; i++) {
            steps = deepQ.reason(input, environment, minCost, 0);
            log.info("Reasoning in " + steps + " steps");
        }
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

    private float[] generateInput(Output initialOutput) {
        List<float[]> inputList = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
                    if (initialOutput.gettSPD()[s][p][d]) {
                        float[] individualInput = new float[2 + pm.getServiceLengthAux()];
                        individualInput[0] = pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d);
                        individualInput[1] = p;
                        for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                            for (int x = 0; x < pm.getServers().size(); x++)
                                if (initialOutput.getfXSVD()[x][s][v][d])
                                    individualInput[2 + v] = x;
                        inputList.add(individualInput);
                    }
                }
        float[] inputArray = new float[inputList.size() * (2 + pm.getServiceLengthAux())];
        for (int i = 0; i < inputList.size(); i++)
            if (inputList.get(i).length >= 0)
                System.arraycopy(inputList.get(i), 0, inputArray, i * 4, inputList.get(i).length);
        return inputArray;
    }
}
