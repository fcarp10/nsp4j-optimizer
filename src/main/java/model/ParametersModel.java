package model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import elements.LinearCostFunctions;
import filemanager.InputParameters;
import gurobi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ParametersModel {

    private static final Logger log = LoggerFactory.getLogger(ParametersModel.class);

    InputParameters ip;
    GRBModel grbModel;
    GRBEnv grbEnv;
    LinearCostFunctions linearCostFunctions;
    GRBVar[][] tSP;
    GRBVar[][][] tSPD;
    GRBVar[][][] fXSV;
    GRBVar[][][][] fXSVD;
    GRBVar[] ukL;
    GRBVar[] ukX;
    GRBVar[] uL;
    GRBVar[] uX;
    GRBVar[][] mkVS;
    GRBVar[][] rkVS;

    public ParametersModel(InputParameters inputParameters) {
        this.ip = inputParameters;
        this.readInputParameters();
    }

    public void initializeVariables() {
        try {
            grbEnv = new GRBEnv();
            grbEnv.set(GRB.IntParam.LogToConsole, 0);
            grbModel = new GRBModel(grbEnv);
            grbModel.getEnv().set(GRB.DoubleParam.MIPGap, ip.getGap());

            tSP = new GRBVar[ip.getServices().size()][ip.getAuxPathsPerTrafficFlow()];
            for (int s = 0; s < ip.getServices().size(); s++)
                for (int p = 0; p < ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    tSP[s][p] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "tSP[" + s + "][" + p + "]");

            tSPD = new GRBVar[ip.getServices().size()][ip.getAuxPathsPerTrafficFlow()][ip.getAuxDemandsPerTrafficFlow()];
            for (int s = 0; s < ip.getServices().size(); s++)
                for (int p = 0; p < ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int d = 0; d < ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        tSPD[s][p][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "tSPD[" + s + "][" + p + "][" + d + "]");

            fXSV = new GRBVar[ip.getServers().size()][ip.getServices().size()][ip.getAuxServiceLength()];
            for (int x = 0; x < ip.getServers().size(); x++)
                for (int s = 0; s < ip.getServices().size(); s++)
                    for (int v = 0; v < ip.getServices().get(s).getFunctions().size(); v++)
                        fXSV[x][s][v] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "fXSV[" + x + "][" + s + "][" + v + "]");

            fXSVD = new GRBVar[ip.getServers().size()][ip.getServices().size()][ip.getAuxServiceLength()][ip.getAuxDemandsPerTrafficFlow()];
            for (int x = 0; x < ip.getServers().size(); x++)
                for (int s = 0; s < ip.getServices().size(); s++)
                    for (int v = 0; v < ip.getServices().get(s).getFunctions().size(); v++)
                        for (int d = 0; d < ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                            fXSVD[x][s][v][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "fXSVD[" + x + "][" + s + "][" + v + "][" + d + "]");

            uL = new GRBVar[ip.getLinks().size()];
            for (int l = 0; l < ip.getLinks().size(); l++)
                uL[l] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "uL[" + l + "]");

            uX = new GRBVar[ip.getServers().size()];
            for (int x = 0; x < ip.getServers().size(); x++)
                uX[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "uX[" + x + "]");

            ukL = new GRBVar[ip.getLinks().size()];
            for (int l = 0; l < ip.getLinks().size(); l++)
                ukL[l] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "ukL[" + l + "]");

            ukX = new GRBVar[ip.getServers().size()];
            for (int x = 0; x < ip.getServers().size(); x++)
                ukX[x] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "ukX[" + x + "]");

            mkVS = new GRBVar[ip.getServices().size()][ip.getAuxServiceLength()];
            for (int s = 0; s < ip.getServices().size(); s++)
                for (int v = 0; v < ip.getServices().get(s).getFunctions().size(); v++)
                    mkVS[s][v] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "mkVS[" + s + "][" + v + "]");

            rkVS = new GRBVar[ip.getServices().size()][ip.getAuxServiceLength()];
            for (int s = 0; s < ip.getServices().size(); s++)
                for (int v = 0; v < ip.getServices().get(s).getFunctions().size(); v++)
                    rkVS[s][v] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "rkVS[" + s + "][" + v + "]");

            grbModel.update();
        } catch (Exception e) {
        }
    }

    private void readInputParameters() {

        TypeReference<LinearCostFunctions> typeReference = new TypeReference<LinearCostFunctions>() {
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
