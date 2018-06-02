package model;

import gurobi.GRB;
import gurobi.GRBException;
import results.ResultsFormat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelResults {

    private ModelParameters mp;
    private int numOfMigrations;
    private int numOfReplicas;
    private static Map<String, Double> utilizationPerLink;

    public ModelResults(ModelParameters mp) {
        this.mp = mp;
    }

    public void calculateNumberOfMigrations(ModelResults initialPlacement) throws GRBException {
        numOfMigrations = 0;
        for (int x = 0; x < mp.ip.getServers().size(); x++)
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                    if (initialPlacement.mp.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1 && mp.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 0)
                        numOfMigrations++;
    }

    public void calculateNumberOfReplications() throws GRBException {
        numOfReplicas = 0;
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++) {
                int numberOfReplicas = 0;
                for (int x = 0; x < mp.ip.getServers().size(); x++)
                    if (mp.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                        numberOfReplicas++;
                numOfReplicas += numberOfReplicas - 1;
            }
    }

    public void printResults(double cost) throws GRBException {

        List<Double> luResults = prepareLinkUtilizationToPrint();
        List<Double> suResults = prepareServerUtilizationToPrint();

        ResultsFormat.printGeneralResults(luResults, suResults, mp.ip.getAuxTotalTraffic(), getTrafficOnLinks(luResults)
                , getAvgPathLength(), cost, -1, getFnv(), numOfMigrations, numOfReplicas);
        ResultsFormat.printHistogramLinkUtilization(luResults);

        ResultsFormat.printLinkUtilization(getMapLinks());
        ResultsFormat.printServerUtilization(getMapServers());

        ResultsFormat.printRoutingSP(prepareRoutingSPtoPrint());
        ResultsFormat.printRoutingSPR(prepareRoutingSPRtoPrint());

        ResultsFormat.printFunctionXSV(prepareFunctionXSVtoPrint());
        ResultsFormat.printFunctionsXSVR(prepareFunctionXSVRtoPrint());

        ResultsFormat.printReliability(prepareReliabilityToPrint());
    }

    private List<Double> prepareLinkUtilizationToPrint() throws GRBException {
        List<Double> linkUtilizationResults = new ArrayList<>();
        for (int l = 0; l < mp.ip.getLinks().size(); l++)
            linkUtilizationResults.add(Math.round(mp.lu[l].get(GRB.DoubleAttr.X) * 10000.0) / 10000.0);
        return linkUtilizationResults;
    }

    private List<Double> prepareServerUtilizationToPrint() throws GRBException {
        List<Double> serverUtilizationResults = new ArrayList<>();
        for (int x = 0; x < mp.ip.getServers().size(); x++)
            serverUtilizationResults.add(Math.round(mp.xu[x].get(GRB.DoubleAttr.X) * 10000.0) / 10000.0);
        return serverUtilizationResults;
    }

    private List<String> prepareRoutingSPtoPrint() throws GRBException {
        List<String> chosenPaths = new ArrayList<>();
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (mp.rSP[s][p].get(GRB.DoubleAttr.X) == 1)
                    chosenPaths.add("s" + s + " --> " + mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
        return chosenPaths;
    }

    private List<String> prepareRoutingSPRtoPrint() throws GRBException {
        List<String> chosenPaths = new ArrayList<>();

        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    if (mp.rSPD[s][p][d].get(GRB.DoubleAttr.X) == 1)
                        chosenPaths.add("s" + s + "-r" + d + " --> " + mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
        return chosenPaths;
    }

    private List<String> prepareFunctionXSVtoPrint() throws GRBException {
        List<String> chosenNodes = new ArrayList<>();
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < mp.ip.getServers().size(); x++)
                    if (mp.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                        chosenNodes.add("s" + s + "-v" + v + " --> " + mp.ip.getServers().get(x).getId());
        return chosenNodes;
    }

    private List<String> prepareFunctionXSVRtoPrint() throws GRBException {
        List<String> chosenNodes = new ArrayList<>();
        for (int x = 0; x < mp.ip.getServers().size(); x++)
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                    for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (mp.fXSVD[x][s][v][d].get(GRB.DoubleAttr.X) == 1)
                            chosenNodes.add("s" + s + "-v" + v + "-r" + d + " --> " + mp.ip.getServers().get(x).getId());
        return chosenNodes;
    }

    private String prepareReliabilityToPrint() throws GRBException {
        DecimalFormat df = new DecimalFormat("0.000");
        StringBuilder reliabilities = new StringBuilder();
        double reliabilityPerServiceChain;
        double totalReliability = 1;
        for (int s = 0; s < mp.ip.getServices().size(); s++) {
            reliabilityPerServiceChain = 1;
            for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++) {
                if (!mp.ip.getServices().get(s).getFunctions().get(v).isReplicable()) {
                    for (int x = 0; x < mp.ip.getServers().size(); x++)
                        if (mp.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                            reliabilityPerServiceChain *= mp.ip.getServers().get(x).getReliability();
                } else {
                    double reliabilityOfReplica = 1;
                    for (int x = 0; x < mp.ip.getServers().size(); x++)
                        if (mp.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                            reliabilityOfReplica *= 1 - mp.ip.getServers().get(x).getReliability();
                    reliabilityOfReplica = 1 - reliabilityOfReplica;
                    reliabilityPerServiceChain *= reliabilityOfReplica;
                }
            }
            reliabilities.append(String.valueOf(df.format(reliabilityPerServiceChain))).append(",");
            totalReliability *= reliabilityPerServiceChain;
        }
        reliabilities.append(" Total: ").append(String.valueOf(df.format(totalReliability))).append("\n");
        return reliabilities.toString();
    }

    private Map<String, Double> getMapLinks() throws GRBException {
        Map<String, Double> mapLinks = new HashMap<>();
        for (int l = 0; l < mp.ip.getLinks().size(); l++)
            mapLinks.put(mp.ip.getLinks().get(l).getSourceNode().getId() + "-" + mp.ip.getLinks().get(l).getTargetNode().getId(), mp.lu[l].get(GRB.DoubleAttr.X));
        return mapLinks;
    }

    private Map<String, Double> getMapServers() throws GRBException {
        Map<String, Double> mapServers = new HashMap<>();
        for (int s = 0; s < mp.ip.getServers().size(); s++)
            mapServers.put(mp.ip.getServers().get(s).getId(), mp.xu[s].get(GRB.DoubleAttr.X));
        return mapServers;
    }

    private double getAvgPathLength() throws GRBException {
        double avgPathLength = 0;
        int usedPaths = 0;
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                if (mp.rSP[s][p].get(GRB.DoubleAttr.X) == 1.0) {
                    avgPathLength += mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getEdgePath().size();
                    usedPaths++;
                }
            }
        avgPathLength = avgPathLength / usedPaths;

        return avgPathLength;
    }

    private List<Integer> getFnv() throws GRBException {
        List<Integer> fnv = new ArrayList<>();
        int counter;
        for (int x = 0; x < mp.ip.getServers().size(); x++) {
            counter = 0;
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                    if (mp.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                        counter++;
            fnv.add(counter);
        }
        return fnv;
    }

    private double getTrafficOnLinks(List<Double> utilizationResults) {
        double trafficOnLinks = 0;
        for (int l = 0; l < utilizationResults.size(); l++)
            trafficOnLinks += (utilizationResults.get(l) - utilizationPerLink.get(mp.ip.getLinks().get(l).getSourceNode().getId()
                    + "-" + mp.ip.getLinks().get(l).getTargetNode().getId())) * (double) mp.ip.getLinks().get(l).getAttribute("capacity");
        return trafficOnLinks;
    }
}
