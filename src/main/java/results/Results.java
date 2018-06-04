package results;


import network.Server;
import org.graphstream.graph.Edge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Results {

    private transient Map<Edge, Double> linksMap;
    private transient Map<Server, Double> serversMap;
    private transient Map<Server, String> functionsMap;
    private double avgLu;
    private double minLu;
    private double maxLu;
    private double vrcLu;
    private double avgXu;
    private double minXu;
    private double maxXu;
    private double vrcXu;
    private double totalTraffic;
    private double trafficLinks;
    private double avgPathLength;
    private double cost;
    private int numOfMigrations;
    private int numOfReplicas;

    public Results(Map<Edge, Double> linksMap, Map<Server, Double> serversMap
            , Map<Server, String> functionsMap, double totalTraffic
            , double trafficLinks, double avgPathLength, double cost
            , int numOfMigrations, int numOfReplicas){
        this.linksMap = linksMap;
        this.serversMap = serversMap;
        this.functionsMap = functionsMap;
        List<Double> lu = new ArrayList<>(linksMap.values());
        List<Double> xu = new ArrayList<>(serversMap.values());
        this.avgLu = Auxiliary.avg(lu);
        this.minLu = Auxiliary.min(lu);
        this.maxLu = Auxiliary.max(lu);
        this.vrcLu = Auxiliary.vrc(lu, avgLu);
        this.avgXu = Auxiliary.avg(xu);
        this.minXu = Auxiliary.min(xu);
        this.maxXu = Auxiliary.max(xu);
        this.vrcXu = Auxiliary.vrc(xu, avgXu);
        this.totalTraffic = totalTraffic;
        this.trafficLinks = trafficLinks;
        this.avgPathLength = avgPathLength;
        this.cost = cost;
        this.numOfMigrations = numOfMigrations;
        this.numOfReplicas = numOfReplicas;
    }

    public Map<Edge, Double> getLinksMap() {
        return linksMap;
    }

    public Map<Server, Double> getServersMap() {
        return serversMap;
    }

    public Map<Server, String> getFunctionsMap() {
        return functionsMap;
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
}
