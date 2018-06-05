package model;

import gurobi.GRB;
import gurobi.GRBException;
import network.Server;
import org.graphstream.graph.Edge;
import results.Auxiliary;
import results.Results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsModel {

    private ParametersModel pm;
    private int numOfMigrations;
    private int numOfReplicas;

    public ResultsModel(ParametersModel pm) {
        this.pm = pm;
    }

    public void calculateNumberOfMigrations(ResultsModel initialPlacement) throws GRBException {
        numOfMigrations = 0;
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            for (int s = 0; s < pm.ip.getServices().size(); s++)
                for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                    if (initialPlacement.pm.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1 && pm.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 0)
                        numOfMigrations++;
    }

    public void calculateNumberOfReplications() throws GRBException {
        numOfReplicas = 0;
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++) {
                int numberOfReplicas = 0;
                for (int x = 0; x < pm.ip.getServers().size(); x++)
                    if (pm.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                        numberOfReplicas++;
                numOfReplicas += numberOfReplicas - 1;
            }
    }

    public Results generate(double cost) throws GRBException {
        return new Results(linksMap(), serversMap(), functionsMap(), pm.ip.getAuxTotalTraffic()
                , trafficOnLinks(), avgPathLength(), Auxiliary.roundDouble(cost), numOfMigrations, numOfReplicas);
    }

    private Map<Edge, Double> linksMap() throws GRBException {
        Map<Edge, Double> linkMapResults = new HashMap<>();
        for (int l = 0; l < pm.ip.getLinks().size(); l++)
            linkMapResults.put(pm.ip.getLinks().get(l), Math.round(pm.lu[l].get(GRB.DoubleAttr.X) * 10000.0) / 10000.0);
        return linkMapResults;
    }

    private Map<Server, Double> serversMap() throws GRBException {
        Map<Server, Double> serverMapResults = new HashMap<>();
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            serverMapResults.put(pm.ip.getServers().get(x), Math.round(pm.xu[x].get(GRB.DoubleAttr.X) * 10000.0) / 10000.0);
        return serverMapResults;
    }

    public Map<Server, String> functionsMap() throws GRBException {
        Map<Server, String> functions = new HashMap<>();
        for (int x = 0; x < pm.ip.getServers().size(); x++) {
            StringBuilder stringVnf = new StringBuilder();
            for (int s = 0; s < pm.ip.getServices().size(); s++)
                for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                    if (pm.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                        stringVnf.append("s").append(String.valueOf(s)).append("v").append(String.valueOf(v)).append("\n");
            functions.put(pm.ip.getServers().get(x), stringVnf.toString());
        }
        return functions;
    }

    private List<String> usedPaths() throws GRBException {
        List<String> usedPaths = new ArrayList<>();
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (pm.rSP[s][p].get(GRB.DoubleAttr.X) == 1)
                    usedPaths.add("s" + s + " --> " + pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
        return usedPaths;
    }

    private List<String> usedPathsPerDemand() throws GRBException {
        List<String> usedPathsPerDemand = new ArrayList<>();
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    if (pm.rSPD[s][p][d].get(GRB.DoubleAttr.X) == 1)
                        usedPathsPerDemand.add("s" + s + "-r" + d + " --> " + pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
        return usedPathsPerDemand;
    }

    private List<String> usedServers() throws GRBException {
        List<String> usedServers = new ArrayList<>();
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.ip.getServers().size(); x++)
                    if (pm.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                        usedServers.add("s" + s + "-v" + v + " --> " + pm.ip.getServers().get(x).getId());
        return usedServers;
    }

    private List<String> usedServersPerDemand() throws GRBException {
        List<String> usedServersPerDemand = new ArrayList<>();
        for (int x = 0; x < pm.ip.getServers().size(); x++)
            for (int s = 0; s < pm.ip.getServices().size(); s++)
                for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                    for (int d = 0; d < pm.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (pm.fXSVD[x][s][v][d].get(GRB.DoubleAttr.X) == 1)
                            usedServersPerDemand.add("s" + s + "-v" + v + "-r" + d + " --> " + pm.ip.getServers().get(x).getId());
        return usedServersPerDemand;
    }

    private double avgPathLength() throws GRBException {
        double avgPathLength = 0;
        int usedPaths = 0;
        for (int s = 0; s < pm.ip.getServices().size(); s++)
            for (int p = 0; p < pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (pm.rSP[s][p].get(GRB.DoubleAttr.X) == 1.0) {
                    avgPathLength += pm.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getEdgePath().size();
                    usedPaths++;
                }
        avgPathLength = avgPathLength / usedPaths;
        return avgPathLength;
    }

    private List<Integer> functionsPerServer() throws GRBException {
        List<Integer> fnv = new ArrayList<>();
        int counter;
        for (int x = 0; x < pm.ip.getServers().size(); x++) {
            counter = 0;
            for (int s = 0; s < pm.ip.getServices().size(); s++)
                for (int v = 0; v < pm.ip.getServices().get(s).getFunctions().size(); v++)
                    if (pm.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                        counter++;
            fnv.add(counter);
        }
        return fnv;
    }

    private double trafficOnLinks() throws GRBException {
        double trafficOnLinks = 0;
        for (int l = 0; l < pm.ip.getLinks().size(); l++)
            trafficOnLinks += pm.lu[l].get(GRB.DoubleAttr.X) * (double) pm.ip.getLinks().get(l).getAttribute("capacity");
        return trafficOnLinks;
    }

    public ParametersModel getPm() {
        return pm;
    }
}
