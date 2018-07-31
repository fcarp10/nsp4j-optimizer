package learning;

import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBException;
import model.Output;

public class NetworkState {

    private Parameters pm;
    private Output initialOutput;
    private int trainingIterations;

    public NetworkState(Parameters pm, Output initialOutput) {
        this.pm = pm;
        this.initialOutput = initialOutput;
        this.trainingIterations = pm.getAuxValues()[0];
    }

    private boolean[] generateEnvironment() throws GRBException {

        boolean[] environment = new boolean[pm.getServers().size() * pm.getServices().size() * pm.getServiceLengthAux()];

        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    environment[x * pm.getServers().size() + s * pm.getServices().size() + v * pm.getServices().get(s).getFunctions().size()] = initialOutput.getVariables().fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0;

        return environment;
    }

}
