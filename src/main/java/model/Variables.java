package model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import utils.LinearCostFunctions;
import filemanager.Parameters;
import gurobi.GRB;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.IOException;
import java.io.InputStream;

public class Variables {

    public LinearCostFunctions linearCostFunctions;
    public GRBVar[][] tSP;
    public GRBVar[][][] tSPD;
    public GRBVar[] fX;
    public GRBVar[][][] fXSV;
    public GRBVar[][][][] fXSVD;
    public GRBVar[] ukL;
    public GRBVar[] ukX;
    public GRBVar[] uL;
    public GRBVar[] uX;
    public GRBVar[][][] mPSV;

    public Variables() {
        this.readLinearCostFunctions();
    }

    public void initializeVariables(Parameters param, GRBModel grbModel) {
        try {
            tSP = new GRBVar[param.getServices().size()][param.getPathsPerTrafficFlowAux()];
            for (int s = 0; s < param.getServices().size(); s++)
                for (int p = 0; p < param.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    tSP[s][p] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "tSP[" + s + "][" + p + "]");

            tSPD = new GRBVar[param.getServices().size()][param.getPathsPerTrafficFlowAux()][param.getDemandsPerTrafficFlowAux()];
            for (int s = 0; s < param.getServices().size(); s++)
                for (int p = 0; p < param.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int d = 0; d < param.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        tSPD[s][p][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "tSPD[" + s + "][" + p + "][" + d + "]");

            fX = new GRBVar[param.getServers().size()];
            for (int x = 0; x < param.getServers().size(); x++)
                fX[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "fX[" + x + "]");

            fXSV = new GRBVar[param.getServers().size()][param.getServices().size()][param.getServiceLengthAux()];
            for (int x = 0; x < param.getServers().size(); x++)
                for (int s = 0; s < param.getServices().size(); s++)
                    for (int v = 0; v < param.getServices().get(s).getFunctions().size(); v++)
                        fXSV[x][s][v] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "fXSV[" + x + "][" + s + "][" + v + "]");

            fXSVD = new GRBVar[param.getServers().size()][param.getServices().size()][param.getServiceLengthAux()][param.getDemandsPerTrafficFlowAux()];
            for (int x = 0; x < param.getServers().size(); x++)
                for (int s = 0; s < param.getServices().size(); s++)
                    for (int v = 0; v < param.getServices().get(s).getFunctions().size(); v++)
                        for (int d = 0; d < param.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            fXSVD[x][s][v][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "fXSVD[" + x + "][" + s + "][" + v + "][" + d + "]");

            uL = new GRBVar[param.getLinks().size()];
            for (int l = 0; l < param.getLinks().size(); l++)
                uL[l] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "uL[" + l + "]");

            uX = new GRBVar[param.getServers().size()];
            for (int x = 0; x < param.getServers().size(); x++)
                uX[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "uX[" + x + "]");

            ukL = new GRBVar[param.getLinks().size()];
            for (int l = 0; l < param.getLinks().size(); l++)
                ukL[l] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "ukL[" + l + "]");

            ukX = new GRBVar[param.getServers().size()];
            for (int x = 0; x < param.getServers().size(); x++)
                ukX[x] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "ukX[" + x + "]");

            mPSV = new GRBVar[param.getPaths().size()][param.getServices().size()][param.getServiceLengthAux()];
            for (int s = 0; s < param.getServices().size(); s++)
                for (int v = 0; v < param.getServices().get(s).getFunctions().size(); v++)
                    for (int p = 0; p < param.getPaths().size(); p++)
                        mPSV[p][s][v] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "mPSV[" + p + "][" + s + "][" + v + "]");

            grbModel.update();
        } catch (Exception e) {
        }
    }

    private void readLinearCostFunctions() {

        TypeReference<LinearCostFunctions> typeReference = new TypeReference<>() {
        };
        InputStream inputStream = TypeReference.class.getResourceAsStream("/linear-cost-functions.yml");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            linearCostFunctions = mapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
