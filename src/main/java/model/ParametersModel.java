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
    GRBVar[][] rSP;
    GRBVar[][][] rSPD;
    GRBVar[][][] fXSV;
    GRBVar[][][][] fXSVD;
    GRBVar[] lk;
    GRBVar[] xk;
    GRBVar[] lu;
    GRBVar[] xu;
    GRBVar[][] mkVS;
    GRBVar[][] rkVS;

    public ParametersModel(InputParameters inputParameters) {
        this.ip = inputParameters;
        this.readInputParameters();
    }

    public void initializeVariables() throws GRBException {

            grbEnv = new GRBEnv("mip.log");
            grbEnv.set(GRB.IntParam.LogToConsole, 0);
            grbModel = new GRBModel(grbEnv);
            grbModel.getEnv().set(GRB.DoubleParam.MIPGap, ip.getGap());

            rSP = new GRBVar[ip.getServices().size()][ip.getAuxPathsPerTrafficFlow()];
            for (int s = 0; s < ip.getServices().size(); s++)
                for (int p = 0; p < ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    rSP[s][p] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "rSP[" + s + "][" + p + "]");

            rSPD = new GRBVar[ip.getServices().size()][ip.getAuxPathsPerTrafficFlow()][ip.getAuxDemandsPerTrafficFlow()];
            for (int s = 0; s < ip.getServices().size(); s++)
                for (int p = 0; p < ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                    for (int d = 0; d < ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        rSPD[s][p][d] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "rSPD[" + s + "][" + p + "][" + d + "]");

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

            lu = new GRBVar[ip.getLinks().size()];
            for (int l = 0; l < ip.getLinks().size(); l++)
                lu[l] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "lu[" + l + "]");

            xu = new GRBVar[ip.getServers().size()];
            for (int x = 0; x < ip.getServers().size(); x++)
                xu[x] = grbModel.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "xu[" + x + "]");

            lk = new GRBVar[ip.getLinks().size()];
            for (int l = 0; l < ip.getLinks().size(); l++)
                lk[l] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "lk[" + l + "]");

            xk = new GRBVar[ip.getServers().size()];
            for (int x = 0; x < ip.getServers().size(); x++)
                xk[x] = grbModel.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "xk[" + x + "]");

            mkVS = new GRBVar[ip.getServices().size()][ip.getAuxServiceLength()];
            for (int s = 0; s < ip.getServices().size(); s++)
                for (int v = 0; v < ip.getServices().get(s).getFunctions().size(); v++)
                    mkVS[s][v] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "mkVS[" + s + "][" + v + "]");

            rkVS = new GRBVar[ip.getServices().size()][ip.getAuxServiceLength()];
            for (int s = 0; s < ip.getServices().size(); s++)
                for (int v = 0; v < ip.getServices().get(s).getFunctions().size(); v++)
                    rkVS[s][v] = grbModel.addVar(0.0, 1.0, 0.0, GRB.BINARY, "rkVS[" + s + "][" + v + "]");

            grbModel.update();
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
