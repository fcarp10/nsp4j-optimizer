package results;


import elements.Scenario;
import filemanager.Parameters;

import java.util.ArrayList;
import java.util.List;

public class Results {

    // Scenario
    private transient Parameters pm;
    private transient Scenario scenario;
    // Summary results
    private double[] luSummary;
    private double[] xuSummary;
    private double[] fuSummary;
    private double[] sdSummary;
    private double avgPathLength;
    private double totalTraffic;
    private double trafficLinks;
    private int migrationsNum;
    private int replicationsNum;
    private double cost;
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

    Results(Parameters pm, Scenario scenario) {
        this.pm = pm;
        this.scenario = scenario;
        luSummary = new double[4];
        xuSummary = new double[4];
        fuSummary = new double[4];
        sdSummary = new double[4];
        rSP = new ArrayList<>();
        rSPD = new ArrayList<>();
        pXSV = new ArrayList<>();
        pXSVD = new ArrayList<>();
        uX = new ArrayList<>();
        uL = new ArrayList<>();
        sSVP = new ArrayList<>();
        dSP = new ArrayList<>();
    }

    void setrSP(boolean rSPinput[][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (rSPinput[s][p])
                    rSP.add("(" + (s + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "): ["
                            + pm.getServices().get(s).getId() + "]"
                            + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath());
    }

    void setrSPD(boolean rSPDinput[][][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                    if (rSPDinput[s][p][d])
                        rSPD.add("(" + (s + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "," + (d + Auxiliary.OFFSET) + "): ["
                                + pm.getServices().get(s).getId() + "]"
                                + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath() + "["
                                + pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d) + "]");
    }

    void setpXSV(boolean pXSVinput[][][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++)
                    if (pXSVinput[x][s][v])
                        pXSV.add("(" + (x + Auxiliary.OFFSET) + "," + (s + Auxiliary.OFFSET) + "," + (v + Auxiliary.OFFSET) + "): ["
                                + pm.getServers().get(x).getId() + "]["
                                + pm.getServices().get(s).getId() + "]["
                                + pm.getServices().get(s).getFunctions().get(v).getType() + "]");
    }

    void setpXSVD(boolean pXSVDinput[][][][]) {
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

    void setuX(List<Double> uXinput) {
        for (int x = 0; x < pm.getServers().size(); x++)
            uX.add("(" + (x + Auxiliary.OFFSET) + "): ["
                    + pm.getServers().get(x).getId() + "]["
                    + uXinput.get(x) + "]");
    }

    void setuL(List<Double> uLinput) {
        for (int l = 0; l < pm.getLinks().size(); l++)
            uL.add("(" + (l + Auxiliary.OFFSET) + "): ["
                    + pm.getLinks().get(l).getId() + "]["
                    + uLinput.get(l) + "]");
    }

    void setsSVP(boolean sSVPinput[][][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int p = 0; p < pm.getPaths().size(); p++)
                    if (sSVPinput[s][v][p])
                        sSVP.add("(" + (s + Auxiliary.OFFSET) + "," + (v + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "): "
                                + pm.getPaths().get(p).getNodePath());
    }

    void setdSP(double dSPinput[][]) {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                if (dSPinput[s][p] > 0)
                    dSP.add("(" + (s + Auxiliary.OFFSET) + "," + (p + Auxiliary.OFFSET) + "): "
                            + pm.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath()
                            + "[" + Auxiliary.roundDouble(dSPinput[s][p], 2) + "]");
    }

    void setLinkResults(List<Double> uL) {
        luSummary[0] = Auxiliary.avg(uL);
        luSummary[1] = Auxiliary.min(uL);
        luSummary[2] = Auxiliary.max(uL);
        luSummary[3] = Auxiliary.vrc(uL, luSummary[0]);
    }

    void setServerResults(List<Double> uX) {
        xuSummary[0] = Auxiliary.avg(uX);
        xuSummary[1] = Auxiliary.min(uX);
        xuSummary[2] = Auxiliary.max(uX);
        xuSummary[3] = Auxiliary.vrc(uX, xuSummary[0]);
    }

    void setFunctionResults(List<Integer> uF) {
        fuSummary[0] = Auxiliary.avgF(uF);
        fuSummary[1] = Auxiliary.minF(uF);
        fuSummary[2] = Auxiliary.maxF(uF);
        fuSummary[3] = Auxiliary.vrcF(uF, fuSummary[0]);
    }

    void setServiceDelayResults(List<Double> sd) {
        sdSummary[0] = Auxiliary.avg(sd);
        sdSummary[1] = Auxiliary.min(sd);
        sdSummary[2] = Auxiliary.max(sd);
        sdSummary[3] = Auxiliary.vrc(sd, sdSummary[0]);
    }

    public Scenario getScenario() {
        return scenario;
    }

    public double[] getLuSummary() {
        return luSummary;
    }

    public double[] getXuSummary() {
        return xuSummary;
    }

    public double[] getFuSummary() {
        return fuSummary;
    }

    public double[] getSdSummary() {
        return sdSummary;
    }

    public double getAvgPathLength() {
        return avgPathLength;
    }

    public double getTotalTraffic() {
        return totalTraffic;
    }

    public double getTrafficLinks() {
        return trafficLinks;
    }

    public int getMigrationsNum() {
        return migrationsNum;
    }

    public int getReplicationsNum() {
        return replicationsNum;
    }

    public double getCost() {
        return cost;
    }

    public List<String> getrSP() {
        return rSP;
    }

    public List<String> getrSPD() {
        return rSPD;
    }

    public List<String> getpXSV() {
        return pXSV;
    }

    public List<String> getpXSVD() {
        return pXSVD;
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

    void setTotalTraffic(double totalTraffic) {
        this.totalTraffic = totalTraffic;
    }

    void setTrafficOnLinks(double trafficLinks) {
        this.trafficLinks = trafficLinks;
    }

    void setAvgPathLength(double avgPathLength) {
        this.avgPathLength = avgPathLength;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    void setMigrationsNum(int migrationsNum) {
        this.migrationsNum = migrationsNum;
    }

    void setReplicationsNum(int replicationsNum) {
        this.replicationsNum = replicationsNum;
    }
}
