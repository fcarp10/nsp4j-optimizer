package results;


import filemanager.Parameters;

import java.util.Arrays;
import java.util.List;

public class Results {

    private transient Parameters pm;
    // Elementary variables
    private transient List<String> rSP;
    private transient List<String> rSPD;
    private transient List<String> pXSV;
    private transient List<String> pXSVD;
    private transient List<String> uX;
    private transient List<String> uL;
    // Additional variables
    private transient List<String> sSVP;
    private transient List<String> dSP;
    // Extra
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

    Results(Parameters pm, List<Double> uL, List<Double> uX, List<Integer> numOfFunctionsPerServer
            , double totalTraffic, double trafficLinks, double avgPathLength, double cost, int numOfMigrations, int numOfReplicas
            , boolean pXSV[][][], boolean pXSVD[][][][], boolean rSP[][], boolean rSPD[][][], boolean sSVP[][][], double dSP[][]) {
        this.pm = pm;
        this.avgLu = Auxiliary.avg(uL);
        this.minLu = Auxiliary.min(uL);
        this.maxLu = Auxiliary.max(uL);
        this.vrcLu = Auxiliary.vrc(uL, avgLu);
        this.avgXu = Auxiliary.avg(uX);
        this.minXu = Auxiliary.min(uX);
        this.maxXu = Auxiliary.max(uX);
        this.vrcXu = Auxiliary.vrc(uX, avgXu);
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
        setrSP(rSP);
        setrSPD(rSPD);
        setpXSV(pXSV);
        setpXSVD(pXSVD);
        setuX(uX);
        setuL(uL);
        setsSVP(sSVP);
        setdSP(dSP);
    }

    private void setrSP(boolean rSPinput[][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (rSPinput[s][p])
                    rSP.add("(" + (s + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "): ["
                            + pm.getServices().get(s).getId() + "]"
                            + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
    }

    private void setrSPD(boolean rSPDinput[][][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    if (rSPDinput[s][p][d])
                        rSPD.add("(" + (s + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "," + (d + Auxiliary.OFFSET) + "): ["
                                + pm.getServices().get(s).getId() + "]"
                                + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath() + "["
                                + pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) + "]");
    }

    private void setpXSV(boolean pXSVinput[][][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++)
                    if (pXSVinput[x][s][v])
                        pXSV.add("(" + (x + Auxiliary.OFFSET) + "," + (s + Auxiliary.OFFSET) + "," + (v + Auxiliary.OFFSET) + "): ["
                                + pm.getServers().get(x).getId() + "]["
                                + pm.getServices().get(s).getId() + "]["
                                + pm.getServices().get(s).getFunctions().get(v).getType() + "]");
    }

    private void setpXSVD(boolean pXSVDinput[][][][]) {
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        if (pXSVDinput[x][s][v][d])
                            pXSVD.add("(" + (x + Auxiliary.OFFSET) + "," + (s + Auxiliary.OFFSET) + "," + (v + Auxiliary.OFFSET) + "," + (d + Auxiliary.OFFSET) + "): ["
                                    + pm.getServers().get(x).getId() + "]["
                                    + pm.getServices().get(s).getId() + "]["
                                    + pm.getServices().get(s).getFunctions().get(v).getType() + "]["
                                    + pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) + "]");
    }

    private void setuX(List<Double> uXinput) {
        for (int x = 0; x < pm.getServers().size(); x++)
            uX.add("(" + (x + Auxiliary.OFFSET) + "): ["
                    + pm.getServers().get(x).getId() + "]["
                    + uXinput.get(x) + "]");
    }

    private void setuL(List<Double> uLinput) {
        for (int l = 0; l < pm.getLinks().size(); l++)
            uL.add("(" + (l + Auxiliary.OFFSET) + "): ["
                    + pm.getLinks().get(l).getId() + "]["
                    + uLinput.get(l) + "]");
    }

    private void setsSVP(boolean sSVPinput[][][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int p = 0; p < pm.getPaths().size(); p++)
                    if (sSVPinput[s][v][p])
                        sSVP.add("(" + (s + Auxiliary.OFFSET) + "," + (v + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "): "
                                + pm.getPaths().get(p).getNodePath());
    }

    private void setdSP(double dSPinput[][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getPaths().size(); p++)
                if (dSPinput[s][p] > 0)
                    dSP.add("(" + (s + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "): "
                            + pm.getPaths().get(p).getNodePath() + "[" + Arrays.deepToString(dSPinput) + "]");
    }

    public List<String> getpXSV() {
        return pXSV;
    }

    public List<String> getpXSVD() {
        return pXSVD;
    }

    public List<String> getrSP() {
        return rSP;
    }

    public List<String> getrSPD() {
        return rSPD;
    }

    public List<String> getuX() {
        return uX;
    }

    public List<String> getuL() {
        return uL;
    }

    public List<String> getsSVP() {
        return sSVP;
    }

    public List<String> getdSP() {
        return dSP;
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
