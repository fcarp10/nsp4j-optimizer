package learning;

import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBException;
import model.Output;

import java.util.Random;

public class Placement {

    private Parameters pm;
    private Output initialOutput;
    private int trainingIterations;
    private Model model;
    private double maxReward;

    public Placement(Parameters pm, Output initialOutput, double maxReward) {
        this.pm = pm;
        this.initialOutput = initialOutput;
        this.trainingIterations = pm.getAux()[0];
        this.model = new Model(pm.getServers().size() * pm.getServices().size() * pm.getServiceLengthAux());
        this.maxReward = maxReward;
    }

    public void run() throws GRBException {
        for (int i = 0; i < trainingIterations; i++)
            model.learn(generateInput(), generateEnvironment(), maxReward);
    }

    private int[] generateEnvironment() throws GRBException {
        int[] environment = new int[pm.getServers().size() * pm.getServices().size() * pm.getServiceLengthAux()];
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    if (initialOutput.getVariables().fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                        environment[x * pm.getServers().size() + s * pm.getServices().size() + v * pm.getServices().get(s).getFunctions().size()] = 1;
                    else
                        environment[x * pm.getServers().size() + s * pm.getServices().size() + v * pm.getServices().get(s).getFunctions().size()] = 0;
        return environment;
    }

    private int[] generateInput() {
        Random random = new Random();
        int[] input = new int[pm.getTrafficFlows().size() * 2 * pm.getDemandsPerTrafficFlowAux() + pm.getTrafficFlows().size()];
        for (int t = 0; t < pm.getTrafficFlows().size(); t++) {
            for (int d = 0; d < pm.getTrafficFlows().get(t).getTrafficDemands().size(); d++) {
                for (int s = 0; s < pm.getServers().size(); s++) {
                    if (pm.getServers().get(s).getNodeParent().equals(pm.getTrafficFlows().get(t).getSrc()))
                        input[t * pm.getTrafficFlows().size()] = s;
                    if (pm.getServers().get(s).getNodeParent().equals(pm.getTrafficFlows().get(t).getDst()))
                        input[t * pm.getTrafficFlows().size() + 1] = s;
                }
                input[t * pm.getTrafficFlows().size() + 2] = pm.getTrafficFlows().get(t).getTrafficDemands().get(t);
                input[t * pm.getTrafficFlows().size() + 3] = random.nextInt(pm.getTrafficFlows().get(t).getAdmissiblePaths().size() - 1);
            }
        }
        return input;
    }
}
