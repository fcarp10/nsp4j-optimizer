package model;

import gurobi.GRB;
import gurobi.GRBException;

public class ModelResults {

    private ModelParameters mp;
    private int[][][] functionXSV;
    private double[][][] resourcesXSV;
    private int numOfMigrations;
    private int numOfReplications;

    public ModelResults(ModelParameters mp) throws GRBException {

        this.mp = mp;
        this.functionXSV = new int[mp.ip.getServersPerNode()][mp.ip.getServices().size()][mp.ip.getAuxServiceLength()];
        this.resourcesXSV = new double[mp.ip.getServersPerNode()][mp.ip.getServices().size()][mp.ip.getAuxServiceLength()];

        for (int x = 0; x < mp.ip.getServers().size(); x++)
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                    this.functionXSV[x][s][v] = (int) mp.fXSV[x][s][v].get(GRB.DoubleAttr.X);

        double functionUtilization;
        for (int x = 0; x < mp.ip.getServers().size(); x++)
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++) {
                    functionUtilization = 0;
                    for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (mp.fXSVD[x][s][v][d].get(GRB.DoubleAttr.X) == 1)
                            functionUtilization += (mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                    * mp.ip.getServices().get(s).getFunctions().get(v).getLoad());
                    this.resourcesXSV[x][s][v] = functionUtilization;
                }
    }

    public void calculateNumberOfMigrations(ModelResults initialPlacement) {
        numOfMigrations = 0;
        for (int x = 0; x < mp.ip.getServers().size(); x++)
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                    if (initialPlacement.getFunctionXSV()[x][s][v] == 1 && this.functionXSV[x][s][v] == 0)
                        numOfMigrations++;
    }

    public void calculateNumberOfReplications() {
        numOfReplications = 0;
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++) {
                int numberOfReplicas = 0;
                for (int x = 0; x < mp.ip.getServers().size(); x++)
                    if (this.functionXSV[x][s][v] == 1)
                        numberOfReplicas++;
                numOfReplications += numberOfReplicas - 1;
            }
    }

    public void printSolution() throws GRBException {

    }

    public void printResults(double cost, ModelResults modelResults) throws GRBException {

    }

    public int[][][] getFunctionXSV() {
        return functionXSV;
    }
}
