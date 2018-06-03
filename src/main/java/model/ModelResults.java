package model;

import gurobi.GRB;
import gurobi.GRBException;
import network.Server;
import org.graphstream.graph.Edge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelResults {

    private ModelParameters mp;
    private int numOfMigrations;
    private int numOfReplicas;

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

        Map<Edge, Double> linksMap = linksMap();
        Map<Server, Double> serverMap = serversMap();
        List<Double> linksUtilization = new ArrayList<>(linksMap.values());
        List<Double> serversUtilization = new ArrayList<>(serverMap.values());

//        ResultsFiles.printGeneralResults(linksUtilization, serversUtilization, mp.ip.getAuxTotalTraffic(), trafficOnLinks()
//                , avgPathLength(), cost, -1, functionsPerServer(), numOfMigrations, numOfReplicas);

//        ClientResults.updateResultsToWebApp(serverMap, linksMap, functionsMap(), ResultsFiles.getGeneralResults());

    }

    private Map<Edge, Double> linksMap() throws GRBException {
        Map<Edge, Double> linkMapResults = new HashMap<>();
        for (int l = 0; l < mp.ip.getLinks().size(); l++)
            linkMapResults.put(mp.ip.getLinks().get(l), Math.round(mp.lu[l].get(GRB.DoubleAttr.X) * 10000.0) / 10000.0);
        return linkMapResults;
    }

    private Map<Server, Double> serversMap() throws GRBException {
        Map<Server, Double> serverMapResults = new HashMap<>();
        for (int x = 0; x < mp.ip.getServers().size(); x++)
            serverMapResults.put(mp.ip.getServers().get(x), Math.round(mp.xu[x].get(GRB.DoubleAttr.X) * 10000.0) / 10000.0);
        return serverMapResults;
    }

    public Map<Server, String> functionsMap() throws GRBException {
        Map<Server, String> functions = new HashMap<>();
        for (int x = 0; x < mp.ip.getServers().size(); x++) {
            StringBuilder stringVnf = new StringBuilder();
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                    if (mp.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1.0)
                        stringVnf.append("s").append(String.valueOf(s)).append("v").append(String.valueOf(v)).append("\n");
            functions.put(mp.ip.getServers().get(x), stringVnf.toString());
        }
        return functions;
    }

    private List<String> usedPaths() throws GRBException {
        List<String> usedPaths = new ArrayList<>();
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (mp.rSP[s][p].get(GRB.DoubleAttr.X) == 1)
                    usedPaths.add("s" + s + " --> " + mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
        return usedPaths;
    }

    private List<String> usedPathsPerDemand() throws GRBException {
        List<String> usedPathsPerDemand = new ArrayList<>();
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    if (mp.rSPD[s][p][d].get(GRB.DoubleAttr.X) == 1)
                        usedPathsPerDemand.add("s" + s + "-r" + d + " --> " + mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
        return usedPathsPerDemand;
    }

    private List<String> usedServers() throws GRBException {
        List<String> usedServers = new ArrayList<>();
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < mp.ip.getServers().size(); x++)
                    if (mp.fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                        usedServers.add("s" + s + "-v" + v + " --> " + mp.ip.getServers().get(x).getId());
        return usedServers;
    }

    private List<String> usedServersPerDemand() throws GRBException {
        List<String> usedServersPerDemand = new ArrayList<>();
        for (int x = 0; x < mp.ip.getServers().size(); x++)
            for (int s = 0; s < mp.ip.getServices().size(); s++)
                for (int v = 0; v < mp.ip.getServices().get(s).getFunctions().size(); v++)
                    for (int d = 0; d < mp.ip.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (mp.fXSVD[x][s][v][d].get(GRB.DoubleAttr.X) == 1)
                            usedServersPerDemand.add("s" + s + "-v" + v + "-r" + d + " --> " + mp.ip.getServers().get(x).getId());
        return usedServersPerDemand;
    }

    private double avgPathLength() throws GRBException {
        double avgPathLength = 0;
        int usedPaths = 0;
        for (int s = 0; s < mp.ip.getServices().size(); s++)
            for (int p = 0; p < mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (mp.rSP[s][p].get(GRB.DoubleAttr.X) == 1.0) {
                    avgPathLength += mp.ip.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getEdgePath().size();
                    usedPaths++;
                }
        avgPathLength = avgPathLength / usedPaths;
        return avgPathLength;
    }

    private List<Integer> functionsPerServer() throws GRBException {
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

    private double trafficOnLinks() throws GRBException {
        double trafficOnLinks = 0;
        for (int l = 0; l < mp.ip.getLinks().size(); l++)
            trafficOnLinks += mp.lu[l].get(GRB.DoubleAttr.X) * (double) mp.ip.getLinks().get(l).getAttribute("capacity");
        return trafficOnLinks;
    }
}
