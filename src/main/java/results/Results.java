package results;


import filemanager.Parameters;
import utils.Auxiliary;

import java.util.ArrayList;
import java.util.List;

public class Results {

    private transient Parameters pm;
    private transient List<String> functionsServers;
    private transient List<String> functionsDemandsServers;
    private transient List<String> servicesPaths;
    private transient List<String> servicesDemandsPaths;
    private transient List<String> serverUtilization;
    private transient List<String> linkUtilization;
    private transient List<String> reroutedTraffic;
    private double avgLu;
    private double minLu;
    private double maxLu;
    private double vrcLu;
    private double avgXu;
    private double minXu;
    private double maxXu;
    private double vrcXu;
    private double avgFu;
    private double minFu;
    private double maxFu;
    private double vrcFu;
    private double totalTraffic;
    private double trafficLinks;
    private double avgPathLength;
    private double cost;
    private int numOfMigrations;
    private int numOfReplicas;

    public Results(Parameters pm, List<Double> lu, List<Double> xu, List<Integer> numOfFunctionsPerServer
            , double totalTraffic, double trafficLinks, double avgPathLength, double cost, int numOfMigrations, int numOfReplicas
            , boolean fXSV[][][], boolean fXSVD[][][][], boolean tSP[][], boolean tSPD[][][], double mPSV[][][]) {
        this.pm = pm;
        this.avgLu = Auxiliary.avg(lu);
        this.minLu = Auxiliary.min(lu);
        this.maxLu = Auxiliary.max(lu);
        this.vrcLu = Auxiliary.vrc(lu, avgLu);
        this.avgXu = Auxiliary.avg(xu);
        this.minXu = Auxiliary.min(xu);
        this.maxXu = Auxiliary.max(xu);
        this.vrcXu = Auxiliary.vrc(xu, avgXu);
        this.avgFu = Auxiliary.avgF(numOfFunctionsPerServer);
        this.minFu = Auxiliary.minF(numOfFunctionsPerServer);
        this.maxFu = Auxiliary.maxF(numOfFunctionsPerServer);
        this.vrcFu = Auxiliary.vrcF(numOfFunctionsPerServer, avgFu);
        this.totalTraffic = totalTraffic;
        this.trafficLinks = trafficLinks;
        this.avgPathLength = avgPathLength;
        this.cost = cost;
        this.numOfMigrations = numOfMigrations;
        this.numOfReplicas = numOfReplicas;
        this.functionsServers = createFunctionsServersResults(fXSV);
        this.functionsDemandsServers = createFunctionsDemandsServersResults(fXSVD);
        this.servicesPaths = createServicesPathsResults(tSP);
        this.servicesDemandsPaths = createServicesDemandsPathsResults(tSPD);
        this.serverUtilization = createServerUtilizationResults(xu);
        this.linkUtilization = createLinkUtilizationResults(lu);
        this.reroutedTraffic = createReroutedTrafficResults(mPSV);
    }

    private List<String> createFunctionsServersResults(boolean fXSV[][][]) {
        List<String> usedServers = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++)
                    if (fXSV[x][s][v])
                        usedServers.add("s[" + s + "]-v[" + v + "]: " + pm.getServers().get(x).getId());
        return usedServers;
    }

    private List<String> createFunctionsDemandsServersResults(boolean fXSVD[][][][]) {
        List<String> usedServersPerDemand = new ArrayList<>();
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (fXSVD[x][s][v][d])
                            usedServersPerDemand.add("s[" + s + "]-v[" + v + "]-d[" + d + "]("
                                    + pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) + "): "
                                    + pm.getServers().get(x).getId());
        return usedServersPerDemand;
    }

    private List<String> createServicesPathsResults(boolean tSP[][]) {
        List<String> usedPaths = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (tSP[s][p])
                    usedPaths.add("s[" + s + "]: " + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
        return usedPaths;
    }

    private List<String> createServicesDemandsPathsResults(boolean tSPD[][][]) {
        List<String> usedPathsPerDemand = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    if (tSPD[s][p][d])
                        usedPathsPerDemand.add("s[" + s + "]-d[" + d + "]("
                                + pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) + "): "
                                + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
        return usedPathsPerDemand;
    }

    private List<String> createServerUtilizationResults(List<Double> xu) {
        List<String> serverUtilization = new ArrayList<>();
        for (int x = 0; x < pm.getServers().size(); x++)
            serverUtilization.add("x[" + x + "](" + pm.getServers().get(x).getId() + "): " + xu.get(x));
        return serverUtilization;
    }

    private List<String> createLinkUtilizationResults(List<Double> lu) {
        List<String> linkUtilization = new ArrayList<>();
        for (int l = 0; l < pm.getLinks().size(); l++)
            linkUtilization.add("l[" + l + "](" + pm.getLinks().get(l).getId() + "): " + lu.get(l));
        return linkUtilization;
    }

    private List<String> createReroutedTrafficResults(double mPSV[][][]) {
        List<String> reroutedTraffic = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int p = 0; p < pm.getPaths().size(); p++)
                    if (mPSV[p][s][v] > 0.0)
                        reroutedTraffic.add("s[" + s + "]-v[" + v + "]-p[" + pm.getPaths().get(p).getNodePath() + "]: " + mPSV[p][s][v]);
        return reroutedTraffic;
    }

    public double getAvgLu() {
        return avgLu;
    }

    public double getMinLu() {
        return minLu;
    }

    public double getMaxLu() {
        return maxLu;
    }

    public double getVrcLu() {
        return vrcLu;
    }

    public double getAvgXu() {
        return avgXu;
    }

    public double getMinXu() {
        return minXu;
    }

    public double getMaxXu() {
        return maxXu;
    }

    public double getVrcXu() {
        return vrcXu;
    }

    public double getAvgFu() {
        return avgFu;
    }

    public double getMinFu() {
        return minFu;
    }

    public double getMaxFu() {
        return maxFu;
    }

    public double getVrcFu() {
        return vrcFu;
    }

    public double getTotalTraffic() {
        return totalTraffic;
    }

    public double getTrafficLinks() {
        return trafficLinks;
    }

    public double getAvgPathLength() {
        return avgPathLength;
    }

    public double getCost() {
        return cost;
    }

    public int getNumOfMigrations() {
        return numOfMigrations;
    }

    public int getNumOfReplicas() {
        return numOfReplicas;
    }

    public List<String> getFunctionsServers() {
        return functionsServers;
    }

    public List<String> getFunctionsDemandsServers() {
        return functionsDemandsServers;
    }

    public List<String> getServicesPaths() {
        return servicesPaths;
    }

    public List<String> getServicesDemandsPaths() {
        return servicesDemandsPaths;
    }

    public List<String> getServerUtilization() {
        return serverUtilization;
    }

    public List<String> getLinkUtilization() {
        return linkUtilization;
    }

    public List<String> getReroutedTraffic() {
        return reroutedTraffic;
    }
}
