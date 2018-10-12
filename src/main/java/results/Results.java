package results;


import filemanager.Parameters;

import java.util.ArrayList;
import java.util.List;

public class Results {

    private transient Parameters pm;
    private transient List<String> sp;
    private transient List<String> spd;
    private transient List<String> xsv;
    private transient List<String> xsvd;
    private transient List<String> ux;
    private transient List<String> ul;
    private transient List<String> svp;
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

    Results(Parameters pm, List<Double> lu, List<Double> xu, List<Integer> numOfFunctionsPerServer
            , double totalTraffic, double trafficLinks, double avgPathLength, double cost, int numOfMigrations, int numOfReplicas
            , boolean xsv[][][], boolean xsvd[][][][], boolean sp[][], boolean spd[][][], boolean svp[][][]) {
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
        this.sp = generateSPResults(sp);
        this.spd = generateSPDResults(spd);
        this.xsv = generateXSVResults(xsv);
        this.xsvd = generateXSVDResults(xsvd);
        this.ux = generateUXResults(xu);
        this.ul = generateULResults(lu);
        this.svp = generateSvpResults(svp);
    }

    private List<String> generateSPResults(boolean tSP[][]) {
        List<String> spStrings = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (tSP[s][p])
                    spStrings.add("(" + (s + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "): ["
                            + pm.getServices().get(s).getId() + "]"
                            + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
        return spStrings;
    }

    private List<String> generateSPDResults(boolean tSPD[][][]) {
        List<String> spdStrings = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    if (tSPD[s][p][d])
                        spdStrings.add("(" + (s + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "," + (d + Auxiliary.OFFSET) + "): ["
                                + pm.getServices().get(s).getId() + "]"
                                + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath() + "["
                                + pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) + "]");
        return spdStrings;
    }

    private List<String> generateXSVResults(boolean fXSV[][][]) {
        List<String> xsvStrings = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++)
                    if (fXSV[x][s][v])
                        xsvStrings.add("(" + (x + Auxiliary.OFFSET) + "," + (s + Auxiliary.OFFSET) + "," + (v + Auxiliary.OFFSET) + "): ["
                                + pm.getServers().get(x).getId() + "]["
                                + pm.getServices().get(s).getId() + "]["
                                + pm.getServices().get(s).getFunctions().get(v).getType() + "]");
        return xsvStrings;
    }

    private List<String> generateXSVDResults(boolean fXSVD[][][][]) {
        List<String> xsvdStrings = new ArrayList<>();
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (fXSVD[x][s][v][d])
                            xsvdStrings.add("(" + (x + Auxiliary.OFFSET) + "," + (s + Auxiliary.OFFSET) + "," + (v + Auxiliary.OFFSET) + "," + (d + Auxiliary.OFFSET) + "): ["
                                    + pm.getServers().get(x).getId() + "]["
                                    + pm.getServices().get(s).getId() + "]["
                                    + pm.getServices().get(s).getFunctions().get(v).getType() + "]["
                                    + pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) + "]");
        return xsvdStrings;
    }

    private List<String> generateUXResults(List<Double> xu) {
        List<String> uxStrings = new ArrayList<>();
        for (int x = 0; x < pm.getServers().size(); x++)
            uxStrings.add("(" + (x + Auxiliary.OFFSET) + "): ["
                    + pm.getServers().get(x).getId() + "]["
                    + xu.get(x) + "]");
        return uxStrings;
    }

    private List<String> generateULResults(List<Double> lu) {
        List<String> ulStrings = new ArrayList<>();
        for (int l = 0; l < pm.getLinks().size(); l++)
            ulStrings.add("(" + (l + Auxiliary.OFFSET) + "): ["
                    + pm.getLinks().get(l).getId() + "]["
                    + lu.get(l) + "]");
        return ulStrings;
    }

    private List<String> generateSvpResults(boolean svp[][][]) {
        List<String> svpStrings = new ArrayList<>();
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int p = 0; p < pm.getPaths().size(); p++)
                    if (svp[s][v][p])
                        svpStrings.add("(" + (s + Auxiliary.OFFSET) + "," + (v + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "): "
                                + pm.getPaths().get(p).getNodePath());
        return svpStrings;
    }

    public List<String> getXsv() {
        return xsv;
    }

    public List<String> getXsvd() {
        return xsvd;
    }

    public List<String> getSp() {
        return sp;
    }

    public List<String> getSpd() {
        return spd;
    }

    public List<String> getUx() {
        return ux;
    }

    public List<String> getUl() {
        return ul;
    }

    public List<String> getSvp() {
        return svp;
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
}
