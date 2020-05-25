package optimizer.algorithms.heuristics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import static optimizer.Definitions.*;
import org.eclipse.collections.impl.list.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import manager.Parameters;
import optimizer.algorithms.NetworkManager;
import optimizer.algorithms.VariablesAlg;
import optimizer.results.Auxiliary;

public class HeuristicAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(HeuristicAlgorithm.class);

    protected Random rnd;
    private NetworkManager networkManager;
    protected Parameters pm;
    protected VariablesAlg vars;
    protected Map<String, Integer> placementIncumbent;
    protected Map<String, Integer> pathsIncumbent;

    public HeuristicAlgorithm(Parameters pm, VariablesAlg vars, NetworkManager networkManager) {
        rnd = new Random();
        this.networkManager = networkManager;
        this.pm = pm;
        this.vars = vars;
        this.pathsIncumbent = new HashMap<>();
        this.placementIncumbent = new HashMap<>();
    }

    public void allocateServices(String algorithm) {
        // first place demands from initial placement
        for (int s = 0; s < pm.getServices().size(); s++) {
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                allocateDemandFromService(algorithm, s, d);
            networkManager.addSyncTraffic(s);
        }
    }

    public void allocateServicesHeuristic(String algorithm) {
        // first place demands from initial placement
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                if (checkIfDemandWasInInitialPlacement(s, d))
                    allocateDemandHeuristics(algorithm, s, d);
        // then the rest
        for (int s = 0; s < pm.getServices().size(); s++) {
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                if (!checkIfDemandWasInInitialPlacement(s, d))
                    allocateDemandHeuristics(algorithm, s, d);
            networkManager.removeUnusedFunctions(s);
            networkManager.addSyncTraffic(s);
        }
    }

    private void allocateDemandFromService(String alg, int s, int d) {
        // get paths with enough path link resources
        List<Integer> availablePaths = networkManager.getAvailablePaths(s, d);
        // get paths with enough servers resources
        Map<Integer, List<List<Integer>>> pathsMapServers = networkManager.findAdmissiblePathsServersMap(availablePaths,
                s, d);
        List<Integer> paths = new ArrayList<>(pathsMapServers.keySet());
        int pChosen = choosePath(alg, s, d, paths);
        List<List<Integer>> availableServers = pathsMapServers.get(pChosen);
        List<Integer> chosenServers = chooseServersForAllFunctions(alg, s, d, pChosen, availableServers);
        networkManager.addDemandToFunctionsToSpecificServers(s, d, chosenServers);
        networkManager.addDemandToPath(s, pChosen, d);
    }

    private int allocateDemandHeuristics(String alg, int s, int d) {
        List<Integer> availablePaths = networkManager.getAvailablePaths(s, d);
        int pChosen = -1;
        for (int p = 0; p < availablePaths.size(); p++) {
            int pTmp = choosePath(alg, s, d, availablePaths);
            List<Integer> functionServerMapping = allocateDemandInPathHeuristics(alg, s, d, pTmp);
            if (functionServerMapping.size() == pm.getServices().get(s).getFunctions().size()) {
                pChosen = pTmp;
                networkManager.addDemandToPath(s, pChosen, d);
                break;
            } else {
                for (int v = 0; v < functionServerMapping.size(); v++)
                    networkManager.removeDemandToFunctionToServer(s, functionServerMapping.get(v), v, d);
                availablePaths.remove(p);
                p--;
            }
        }
        if (pChosen == -1) {
            // TO-DO blocking
            Auxiliary.printLog(log, ERROR, "no admissible path available for [s][d] = [" + s + "][" + d + "]");
            System.exit(-1);
        }
        return pChosen;
    }

    private List<Integer> allocateDemandInPathHeuristics(String alg, int s, int d, int p) {
        // allocate functions on that path
        List<Integer> functionServerMapping = new ArrayList<>();
        for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            // find available servers for function v
            List<Integer> availableServers = networkManager.findServersForSpecificFunction(s, d, p, v, true, true);
            if (availableServers.isEmpty())
                break;
            // choose one server
            int xChosen = chooseServerForSpecificFunction(alg, s, d, p, v, availableServers);
            // add demand to server
            networkManager.addDemandToFunctionToServer(s, xChosen, v, d);
            functionServerMapping.add(xChosen);
        }
        return functionServerMapping;
    }

    public void optimizePlacement() {
        setPathsIncumbent();
        setPlacementIncumbent();
        double bestKnownObjVal = vars.objVal;
        Auxiliary.printLog(log, INFO, "initial incumbent [" + bestKnownObjVal + "]");
        List<Integer> services = Interval.zeroTo(pm.getServices().size() - 1).toList();
        Collections.shuffle(services);
        for (int sIndex = 0; sIndex < pm.getServices().size(); sIndex++) {
            int s = services.get(sIndex);
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                List<Integer> availablePaths = networkManager.getAvailablePaths(s, d);
                boolean removePreviousAllocation = true;
                for (Integer p : availablePaths) {
                    if (removePreviousAllocation) {
                        networkManager.removeDemandFromAllFunctionsToServer(s, d); // remove previous allocation
                        removeDemandFromOldPath(s, d);
                    }
                    List<Integer> functionServerMapping = allocateDemandInPathHeuristics(HEU, s, d, p);
                    if (functionServerMapping.size() == pm.getServices().get(s).getFunctions().size()) {
                        networkManager.addDemandToPath(s, p, d);
                        removePreviousAllocation = true;
                    } else {
                        for (int v = 0; v < functionServerMapping.size(); v++)
                            networkManager.removeDemandToFunctionToServer(s, functionServerMapping.get(v), v, d);
                        removePreviousAllocation = false;
                        continue;
                    }
                    // then, try to optimize the locations
                    double currentBestKnownObjVal = reallocateFunctionsInPath(s, d, p, bestKnownObjVal);
                    if (currentBestKnownObjVal < bestKnownObjVal) {
                        bestKnownObjVal = currentBestKnownObjVal;
                        pathsIncumbent.put(String.valueOf(s) + String.valueOf(d), p);
                    }
                }
                networkManager.removeDemandFromAllFunctionsToServer(s, d);
                removeDemandFromOldPath(s, d);
                int pBest = pathsIncumbent.get(String.valueOf(s) + String.valueOf(d));
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    int xChosen = placementIncumbent
                            .get(String.valueOf(s) + String.valueOf(d) + String.valueOf(pBest) + String.valueOf(v));
                    networkManager.addDemandToFunctionToServer(s, xChosen, v, d);
                }
                networkManager.addDemandToPath(s, pBest, d);
            }
            networkManager.removeUnusedFunctions(s);
            networkManager.removeSyncTraffic(s);
            networkManager.addSyncTraffic(s);
        }
    }

    private double reallocateFunctionsInPath(int s, int d, int p, double bestObjVal) {
        double localBestObjVal = bestObjVal;
        for (int j = 0; j < pm.getServices().get(s).getFunctions().size()
                * pm.getServices().get(s).getFunctions().size(); j++) {
            List<Integer> functions = Interval.zeroTo(pm.getServices().get(s).getFunctions().size() - 1).toList();
            Collections.shuffle(functions);
            for (int vIndex = 0; vIndex < functions.size(); vIndex++) {
                int v = functions.get(vIndex);
                List<Integer> availableServers = networkManager.findServersForSpecificFunction(s, d, p, v, true, true);
                Collections.shuffle(availableServers);
                for (int i = 0; i < availableServers.size(); i++) {
                    int xOld = networkManager.getUsedServerForFunction(s, d, v);
                    int xNew = availableServers.get(i);
                    if (xOld == xNew)
                        continue;
                    reallocateSpecificFunction(s, d, v, xOld, xNew);
                    if (vars.getObjVal() < localBestObjVal) {
                        Auxiliary.printLog(log, INFO, "new incumbent [" + vars.getObjVal() + "]");
                        localBestObjVal = vars.getObjVal();
                        placementIncumbent.put(
                                String.valueOf(s) + String.valueOf(d) + String.valueOf(p) + String.valueOf(v), xNew);
                    }
                }
            }

        }
        if (localBestObjVal < bestObjVal)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                reallocateFunctionToBesKnownPlacement(s, d, v, p);
        return localBestObjVal;
    }

    private void reallocateFunctionToBesKnownPlacement(int s, int d, int v, int p) {
        int xOld = networkManager.getUsedServerForFunction(s, d, v);
        int xBest = placementIncumbent
                .get(String.valueOf(s) + String.valueOf(d) + String.valueOf(p) + String.valueOf(v));
        if (xOld != xBest)
            reallocateSpecificFunction(s, d, v, xOld, xBest);
    }

    private void reallocateSpecificFunction(int s, int d, int v, int xOld, int xNew) {
        if (xOld != -1)
            networkManager.removeDemandToFunctionToServer(s, xOld, v, d);
        networkManager.addDemandToFunctionToServer(s, xNew, v, d);
        networkManager.removeUnusedFunctions(s);
        networkManager.removeSyncTraffic(s);
        networkManager.addSyncTraffic(s);
        vars.generateRestOfVariablesForResults();
    }

    private Integer choosePath(String algorithm, int s, int d, List<Integer> paths) {
        if (algorithm.equals(FF) || algorithm.equals(FFP_RFX))
            return paths.get(0);
        else if (algorithm.equals(RF) || algorithm.equals(RFP_FFX))
            return paths.get(rnd.nextInt(paths.size()));
        else if (algorithm.equals(HEU)) {
            int pChosen = getAlreadyUsedPathForDemandFromInitialPlacement(s, d, paths);
            if (pChosen != -1)
                return pChosen;
            pChosen = getAlreadyUsedPathFromInitialPlacement(s, paths);
            if (pChosen != -1)
                return pChosen;
            pChosen = getAlreadyUsedPathForService(s, paths);
            if (pChosen != -1)
                return pChosen;
            return getPathWithLowerServiceDelay(s, d, paths);
        }
        return -1;
    }

    public List<Integer> chooseServersForAllFunctions(String algorithm, int s, int d, int p,
            List<List<Integer>> listAvailableServersPerFunction) {
        List<Integer> specificServers = new ArrayList<>();
        int lastPathNodeUsed = 0;
        for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            List<Integer> availableServers = listAvailableServersPerFunction.get(v);
            availableServers = removePreviousServersFromNodeIndex(availableServers, lastPathNodeUsed, s, p);
            int xChosen = chooseServerForFunction(algorithm, availableServers, s, v, d);
            if (xChosen == -1) {
                Auxiliary.printLog(log, ERROR,
                        "function could not be allocated [s][d][p][v] = [" + s + "][" + d + "][" + p + "][" + v + "]");
                System.exit(-1);
            }
            specificServers.add(xChosen);
            lastPathNodeUsed = networkManager.getNodePathIndexFromServer(s, p, xChosen);
        }
        return specificServers;
    }

    public int chooseServerForSpecificFunction(String algorithm, int s, int d, int p, int v,
            List<Integer> availableServers) {
        if (v > 0) {
            int previousServer = networkManager.getUsedServerForFunction(s, d, v - 1);
            int serverFromPreviousFunction = networkManager.getNodePathIndexFromServer(s, p, previousServer);
            availableServers = removePreviousServersFromNodeIndex(availableServers, serverFromPreviousFunction, s, p);
        }
        if (v < pm.getServices().get(s).getFunctions().size() - 1) {
            int nextServer = networkManager.getUsedServerForFunction(s, d, v + 1);
            if (nextServer != -1) {
                int serverFromNextFunction = networkManager.getNodePathIndexFromServer(s, p, nextServer);
                availableServers = removeNextServersFromNodeIndex(availableServers, serverFromNextFunction, s, p);
            }
        }
        int xChosen = chooseServerForFunction(algorithm, availableServers, s, v, d);
        if (xChosen == -1) {
            Auxiliary.printLog(log, ERROR,
                    "function could not be allocated [s][d][p][v] = [" + s + "][" + d + "][" + p + "][" + v + "]");
            System.exit(-1);
        }
        return xChosen;
    }

    public List<Integer> removePreviousServersFromNodeIndex(List<Integer> servers, int nodeIndex, int s, int p) {
        int serverIndex = 0;
        for (int x = 0; x < servers.size(); x++)
            if (pm.getServers().get(servers.get(x)).getParent()
                    .equals(pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(nodeIndex)))
                serverIndex = x;
        if (serverIndex > 0)
            return servers.subList(serverIndex, servers.size());
        return servers;
    }

    public List<Integer> removeNextServersFromNodeIndex(List<Integer> servers, int nodeIndex, int s, int p) {
        int serverIndex = 0;
        for (int x = 0; x < servers.size(); x++)
            if (pm.getServers().get(servers.get(x)).getParent()
                    .equals(pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(nodeIndex)))
                serverIndex = x;
        if (serverIndex > 0)
            return servers.subList(0, serverIndex);
        return servers;
    }

    private Integer chooseServerForFunction(String algorithm, List<Integer> availableServers, int s, int v, int d) {
        if (algorithm.equals(FF) || algorithm.equals(RFP_FFX))
            return availableServers.get(0);
        else if (algorithm.equals(RF) || algorithm.equals(FFP_RFX))
            return availableServers.get(rnd.nextInt(availableServers.size()));
        else if (algorithm.equals(HEU))
            return chooseServerForFunctionHeuristics(availableServers, s, v, d);
        return -1;
    }

    private Integer chooseServerForFunctionHeuristics(List<Integer> availableServers, int s, int v, int d) {
        int xCloud = returnCloudServer(availableServers);
        int xChosen = -1;
        // reduce migrations by choosing servers from initial placement
        if ((xChosen = getAlreadyUsedServerforDemandFromInitialPlacement(s, v, d, availableServers)) != -1)
            return xChosen;
        // reduce replications by choosing servers from initial placement
        if ((xChosen = getAlreadyUsedServerFromInitialPlacement(s, v, availableServers)) != -1)
            if (availableServers.indexOf(xChosen) < availableServers.indexOf(xCloud))
                return xChosen;
        // reduce replications by choosing a server already used for the function
        if ((xChosen = getAlreadyUsedServerForService(s, v, availableServers)) != -1)
            if (availableServers.indexOf(xChosen) < availableServers.indexOf(xCloud))
                return xChosen;
        // choose the first available server
        return availableServers.get(0);
    }

    private void setPathsIncumbent() {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                    if (vars.zSPD[s][p][d])
                        pathsIncumbent.put(String.valueOf(s) + String.valueOf(d), p);
    }

    private void setPlacementIncumbent() {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        for (int x = 0; x < pm.getServers().size(); x++)
                            if (vars.fXSVD[x][s][v][d])
                                placementIncumbent.put(
                                        String.valueOf(s) + String.valueOf(d) + String.valueOf(p) + String.valueOf(v),
                                        x);
    }

    public void removeDemandFromOldPath(int s, int d) {
        int pOld = -1;
        for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (vars.zSPD[s][p][d])
                pOld = p;
        if (pOld != -1)
            networkManager.removeDemandFromPath(s, pOld, d); // remove demand from path
    }

    private int getAlreadyUsedPathForService(int s, List<Integer> paths) {
        for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (vars.zSP[s][p] && paths.contains(p))
                return p;
        return -1;
    }

    private int getAlreadyUsedPathForDemandFromInitialPlacement(int s, int d, List<Integer> paths) {
        for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (vars.zSPDinitial[s][p][d] && paths.contains(p))
                return p;
        return -1;
    }

    private int getAlreadyUsedPathFromInitialPlacement(int s, List<Integer> paths) {
        for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (vars.zSPinitial[s][p] && paths.contains(p))
                return p;
        return -1;
    }

    private int getAlreadyUsedServerForService(int s, int v, List<Integer> servers) {
        for (int x = 0; x < pm.getServers().size(); x++)
            if (vars.fXSV[x][s][v] && servers.contains(x))
                return x;
        return -1;
    }

    private int getAlreadyUsedServerforDemandFromInitialPlacement(int s, int v, int d, List<Integer> servers) {
        for (int x = 0; x < pm.getServers().size(); x++)
            if (vars.fXSVDinitial[x][s][v][d] && servers.contains(x))
                return x;
        return -1;
    }

    private int getAlreadyUsedServerFromInitialPlacement(int s, int v, List<Integer> servers) {
        for (int x = 0; x < pm.getServers().size(); x++)
            if (vars.fXSVinitial[x][s][v] && servers.contains(x))
                return x;
        return -1;
    }

    private int getPathWithLowerServiceDelay(int s, int d, List<Integer> paths) {
        int pathWithLowestDelay = 0;
        double lowestDelay = Double.MAX_VALUE;
        for (int p = 0; p < paths.size(); p++) {
            double pathDelay = vars.getCurrentServiceDelay(s, d, paths.get(p));
            if (pathDelay < lowestDelay) {
                lowestDelay = pathDelay;
                pathWithLowestDelay = paths.get(p);
            }
        }
        return pathWithLowestDelay;
    }

    private boolean checkIfDemandWasInInitialPlacement(int s, int d) {
        for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (vars.zSPDinitial[s][p][d])
                return true;
        return false;
    }

    private int returnCloudServer(List<Integer> availableServers) {
        int xCloud = -1;
        for (int x = 0; x < availableServers.size(); x++)
            if (pm.getServers().get(availableServers.get(x)).getParent().getAttribute(NODE_CLOUD) != null)
                xCloud = availableServers.get(x);
        return xCloud;
    }
}