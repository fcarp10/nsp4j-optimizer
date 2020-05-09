package optimizer.algorithms.heuristics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import static optimizer.Definitions.*;
import org.eclipse.collections.impl.list.Interval;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Service;
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

    public void allocateAllServices(String algorithm) {
        // first place demands from initial placement
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                if (checkIfDemandWasInInitialPlacement(s, d))
                    allocateDemandFromService(algorithm, s, d);
        // then the rest
        for (int s = 0; s < pm.getServices().size(); s++) {
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                if (!checkIfDemandWasInInitialPlacement(s, d))
                    allocateDemandFromService(algorithm, s, d);
            networkManager.addSyncTraffic(s);
        }
    }

    private void allocateDemandFromService(String alg, int s, int d) {
        // get paths with enough path link resources
        List<Integer> availablePaths = networkManager.getAvailablePaths(s, d);
        // get paths with enough servers resources
        Map<Integer, List<List<Integer>>> pathsMapServers = networkManager.findAdmissiblePaths(availablePaths, s, d);
        List<Integer> paths = new ArrayList<>(pathsMapServers.keySet());
        int pChosen = choosePath(alg, s, d, paths);
        List<List<Integer>> availableServers = pathsMapServers.get(pChosen);
        List<Integer> chosenServers = chooseServersForFunctionAllocation(alg, s, d, pChosen, availableServers);
        networkManager.addDemandToFunctionsToSpecificServers(s, d, chosenServers);
        networkManager.addDemandToPath(s, pChosen, d);
    }

    public void optimizePlacement(String algorithm) {
        setPathsIncumbent();
        setPlacementIncumbent();
        double bestKnownObjVal = vars.objVal;
        List<Integer> services = Interval.zeroTo(pm.getServices().size() - 1).toList();
        Collections.shuffle(services);
        for (int sIndex = 0; sIndex < pm.getServices().size(); sIndex++) {
            int s = services.get(sIndex);
            List<Integer> demands = Interval.zeroTo(pm.getServices().get(s).getTrafficFlow().getDemands().size() - 1)
                    .toList();
            Collections.shuffle(demands);
            for (int dIndex = 0; dIndex < demands.size(); dIndex++) {
                int d = demands.get(dIndex);
                // get paths with enough path link resources
                List<Integer> availablePaths = networkManager.getAvailablePaths(s, d);
                for (Integer p : availablePaths) {
                    // before placing in a new path remove functions from previous path
                    networkManager.removeDemandFromAllFunctionsToServer(s, d);
                    // and place them on servers in the current path
                    List<List<Integer>> availableServersPerFunction = networkManager.findServersForFunctionsInPath(s, d,
                            p);
                    if (availableServersPerFunction == null)
                        continue;
                    List<Integer> chosenServers = chooseServersForFunctionAllocation(algorithm, s, d, p,
                            availableServersPerFunction);
                    networkManager.addDemandToFunctionsToSpecificServers(s, d, chosenServers);
                    // route traffic to path p
                    rerouteSpecificDemand(s, d, p);
                    // then, try to optimize the locations
                    double currentBestKnownObjVal = reallocateFunctionsInPath(s, d, p, bestKnownObjVal);
                    if (currentBestKnownObjVal < bestKnownObjVal) {
                        bestKnownObjVal = currentBestKnownObjVal;
                        setPathsIncumbent();
                    }
                }

            }
        }

        placementUsingIncumbentPathsAndFunctions();
    }

    private double reallocateFunctionsInPath(int s, int d, int p, double bestObjVal) {
        double localBestObjVal = bestObjVal;
        for (int j = 0; j < pm.getServices().get(s).getFunctions().size()
                * pm.getServices().get(s).getFunctions().size(); j++) {
            List<Integer> functions = Interval.zeroTo(pm.getServices().get(s).getFunctions().size() - 1).toList();
            Collections.shuffle(functions);
            for (int vIndex = 0; vIndex < functions.size(); vIndex++) {
                int v = functions.get(vIndex);
                List<List<Integer>> availableServersPerFunction = networkManager.findServersForFunctionsInPath(s, d, p);
                if (availableServersPerFunction == null)
                    continue;
                List<Integer> availableServers = availableServersPerFunction.get(v);
                if (availableServers.size() == 1)
                    continue;
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
                        setPlacementIncumbent();
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

    private void placementUsingIncumbentPathsAndFunctions() {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                String optPathKey = String.valueOf(s) + String.valueOf(d);
                // before placing in the best path remove previous ones
                networkManager.removeDemandFromAllFunctionsToServer(s, d);
                // and place them on servers in the best path
                int p = pathsIncumbent.get(optPathKey);
                List<Integer> chosenServers = new ArrayList<>();
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    chosenServers.add(placementIncumbent
                            .get(String.valueOf(s) + String.valueOf(d) + String.valueOf(p) + String.valueOf(v)));
                networkManager.addDemandToFunctionsToSpecificServers(s, d, chosenServers);
                rerouteSpecificDemand(s, d, p);
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    int xOld = networkManager.getUsedServerForFunction(s, d, v);
                    int xNew = chosenServers.get(v);
                    reallocateSpecificFunction(s, d, v, xOld, xNew);
                }
            }
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
        if (algorithm.equals(FIRST_FIT) || algorithm.equals(FFP_RFX))
            return paths.get(0);
        else if (algorithm.equals(RANDOM_FIT) || algorithm.equals(RFP_FFX))
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

    public List<Integer> chooseServersForFunctionAllocation(String algorithm, int s, int d, int p,
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

    private Integer chooseServerForFunction(String algorithm, List<Integer> availableServers, int s, int v, int d) {
        if (algorithm.equals(FIRST_FIT) || algorithm.equals(RFP_FFX))
            return availableServers.get(0);
        else if (algorithm.equals(RANDOM_FIT) || algorithm.equals(FFP_RFX))
            return availableServers.get(rnd.nextInt(availableServers.size()));
        else if (algorithm.equals(HEU)) {
            int sChosen = getAlreadyUsedServerforDemandFromInitialPlacement(s, v, d, availableServers);
            if (sChosen != -1)
                return sChosen;
            sChosen = getAlreadyUsedServerFromInitialPlacement(s, v, availableServers);
            if (sChosen != -1)
                return sChosen;
            sChosen = getAlreadyUsedServerForService(s, v, availableServers);
            if (sChosen != -1)
                return sChosen;
            return availableServers.get(0);
        }
        return -1;
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

    public void rerouteSpecificDemand(int s, int d, int pBest) {
        int pOld = -1;
        for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (vars.zSPD[s][p][d])
                pOld = p;
        networkManager.removeDemandFromPath(s, pOld, d); // remove demand from path
        networkManager.addDemandToPath(s, pBest, d); // add demand to path
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
            double pathDelay = getCurrentServiceDelay(s, d, paths.get(p));
            if (pathDelay < lowestDelay) {
                lowestDelay = pathDelay;
                pathWithLowestDelay = paths.get(p);
            }
        }
        return pathWithLowestDelay;
    }

    private double getCurrentServiceDelay(int s, int d, int p) {
        double serviceDelay = 0;
        Service service = pm.getServices().get(s);
        Path path = pm.getServices().get(s).getTrafficFlow().getPaths().get(p);
        // processing delay
        for (int n = 0; n < path.getNodePath().size(); n++)
            for (int x = 0; x < pm.getServers().size(); x++)
                if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
                    for (int v = 0; v < service.getFunctions().size(); v++)
                        if (vars.fXSVD[x][s][v][d]) {
                            Function function = service.getFunctions().get(v);
                            double ratio = (double) function.getAttribute(FUNCTION_LOAD_RATIO)
                                    * (double) function.getAttribute(FUNCTION_PROCESS_TRAFFIC_DELAY)
                                    / (int) function.getAttribute(FUNCTION_MAX_CAP_SERVER);
                            double processingDelay = 0;
                            for (int d1 = 0; d1 < service.getTrafficFlow().getDemands().size(); d1++)
                                if (service.getTrafficFlow().getAux().get(d1))
                                    if (vars.fXSVD[x][s][v][d1])
                                        processingDelay += ratio * service.getTrafficFlow().getDemands().get(d1);
                            processingDelay += (double) function.getAttribute(FUNCTION_MIN_PROCESS_DELAY);
                            processingDelay += (double) function.getAttribute(FUNCTION_PROCESS_DELAY)
                                    * vars.uX.get(pm.getServers().get(x).getId());
                            serviceDelay += processingDelay;
                        }
        // propagation delay
        double pathDelay = 0.0;
        for (Edge link : path.getEdgePath())
            pathDelay += (double) link.getAttribute(LINK_DELAY) * 1000; // in ms
        serviceDelay += pathDelay;
        return serviceDelay;
    }

    private boolean checkIfDemandWasInInitialPlacement(int s, int d) {
        for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            if (vars.zSPDinitial[s][p][d])
                return true;
        return false;
    }
}