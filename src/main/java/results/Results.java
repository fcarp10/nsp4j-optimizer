package results;


import utils.Auxiliary;

import java.util.List;

public class Results {

    private transient List<String> functions;
    private transient List<String> functionsPerDemand;
    private transient List<String> paths;
    private transient List<String> pathsPerDemands;
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
    private transient List<String> reRoutedTraffic;

    public Results(List<Double> lu, List<Double> xu, List<Integer> numOfFunctionsPerServer,
                   double totalTraffic, double trafficLinks, double avgPathLength, double cost
            , int numOfMigrations, int numOfReplicas, List<String> functions, List<String> functionsPerDemand
            , List<String> paths, List<String> pathsPerDemands
            , List<String> reRoutedTraffic) {

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
        this.functions = functions;
        this.functionsPerDemand = functionsPerDemand;
        this.paths = paths;
        this.pathsPerDemands = pathsPerDemands;
        this.reRoutedTraffic = reRoutedTraffic;
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

    public List<String> getFunctions() {
        return functions;
    }

    public List<String> getFunctionsPerDemand() {
        return functionsPerDemand;
    }

    public List<String> getPaths() {
        return paths;
    }

    public List<String> getPathsPerDemands() {
        return pathsPerDemands;
    }

    public List<String> getReRoutedTraffic() {
        return reRoutedTraffic;
    }
}
