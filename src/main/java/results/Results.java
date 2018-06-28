package results;


import network.Server;
import org.graphstream.graph.Edge;
import utils.Auxiliary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Results {

    private transient Map<Edge, Double> linksMap;
    private transient Map<Server, Double> serversMap;
    private transient Map<Server, List<Integer>> functionsMap;
    private transient Map<Server, String> functionsStringMap;
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
    private List<String> paths;

    public Results(Map<Edge, Double> linksMap, Map<Server, Double> serversMap
            , Map<Server, List<Integer>> functionsMap, Map<Server, String> functionsStringMap, double totalTraffic
            , double trafficLinks, double avgPathLength, double cost
            , int numOfMigrations, int numOfReplicas, List<String> paths) {
        this.linksMap = linksMap;
        this.serversMap = serversMap;
        this.functionsMap = functionsMap;
        this.functionsStringMap = functionsStringMap;
        List<Double> lu = new ArrayList<>(linksMap.values());
        List<Double> xu = new ArrayList<>(serversMap.values());
        List<List<Integer>> functionsPerServer = new ArrayList<>(functionsMap.values());
        List<Integer> numOfFunctionsPerServer = Auxiliary.listsSizes(functionsPerServer);
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
        this.paths = paths;
    }

    public Map<Edge, Double> getLinksMap() {
        return linksMap;
    }

    public Map<Server, Double> getServersMap() {
        return serversMap;
    }

    public Map<Server, List<Integer>> getFunctionsMap() {
        return functionsMap;
    }

    public Map<Server, String> getFunctionsStringMap() {
        return functionsStringMap;
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

    public List<String> getPaths() {
        return paths;
    }
}
