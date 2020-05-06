package optimizer.algorithms.heuristics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import static optimizer.Definitions.*;
import org.graphstream.graph.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import manager.Parameters;
import manager.elements.TrafficFlow;
import optimizer.algorithms.NetworkManager;
import optimizer.algorithms.VariablesAlg;
import optimizer.results.Auxiliary;

public class HeuristicAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(HeuristicAlgorithm.class);

    protected Random rnd;
    private String objFunc;
    private NetworkManager networkManager;
    protected Parameters pm;
    protected VariablesAlg vars;
    protected boolean[][][] initialPlacement;
    protected Map<String, Integer> placementIncumbent;
    protected Map<String, Integer> pathsIncumbent;

    public HeuristicAlgorithm(Parameters pm, VariablesAlg vars, boolean[][][] initialPlacement,
            NetworkManager networkManager, String objFunc) {
        rnd = new Random();
        this.objFunc = objFunc;
        this.networkManager = networkManager;
        this.pm = pm;
        this.vars = vars;
        this.initialPlacement = initialPlacement;
        this.pathsIncumbent = new HashMap<>();
        this.placementIncumbent = new HashMap<>();
    }

    public void allocateAllServices(String algorithm) {
        networkManager.assignFunctionsToServersFromInitialPlacement(); // add functions from initial placement
        for (int s = 0; s < pm.getServices().size(); s++) {
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                // get paths with enough path link resources
                List<Integer> availablePaths = networkManager.getAvailablePaths(s, d);
                // get paths with enough servers resources
                Map<Integer, List<List<Integer>>> pathsMappingServers = networkManager
                        .findAdmissiblePaths(availablePaths, s, d);
                List<Integer> paths = new ArrayList<>(pathsMappingServers.keySet());
                int pChosen = choosePath(algorithm, paths, pm.getServices().get(s).getTrafficFlow());
                List<List<Integer>> listAvailableServersPerFunction = pathsMappingServers.get(pChosen);
                List<Integer> chosenServers = chooseServersForFunctionAllocation(algorithm, s, d, pChosen,
                        listAvailableServersPerFunction);
                networkManager.addDemandToFunctionsToSpecificServers(s, d, chosenServers);
                networkManager.addDemandToPath(s, pChosen, d);
            }
            networkManager.removeUnusedFunctions(s); // remove unused servers from initial placement
            networkManager.addSyncTraffic(s); // add synchronization traffic
        }
        vars.generateRestOfVariablesForResults(initialPlacement, objFunc);
    }

    public void optimizePlacement(String algorithm) {
        setPathsIncumbent();
        setPlacementIncumbent();
        double bestKnownObjVal = vars.objVal;
        for (int i = 0; i < pm.getServices().size() * pm.getDemandsTrafficFlow() * pm.getPathsTrafficFlow(); i++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                    // get paths with enough path link resources
                    List<Integer> availablePaths = networkManager.getAvailablePaths(s, d);
                    for (Integer p : availablePaths) {
                        // before placing in a new path remove previous ones
                        networkManager.removeDemandFromAllFunctionsToServer(s, d);
                        // and place them on servers in the path
                        List<List<Integer>> availableServersPerFunction = networkManager
                                .findServersForFunctionsInPath(s, d, p);
                        if (availableServersPerFunction == null)
                            continue;
                        List<Integer> chosenServers = chooseServersForFunctionAllocation(algorithm, s, d, p,
                                availableServersPerFunction);
                        networkManager.addDemandToFunctionsToSpecificServers(s, d, chosenServers);
                        // route traffic to path p
                        rerouteSpecificDemand(s, d, p);
                        // then, try to optimize the locations
                        for (int j = 0; j < 10; j++) {
                            double currentBestKnownObjVal = reallocateFunctions(s, d, p, bestKnownObjVal);
                            if (currentBestKnownObjVal < bestKnownObjVal) {
                                bestKnownObjVal = currentBestKnownObjVal;
                                setPathsIncumbent();
                            }
                        }
                    }
                }
        placementUsingIncumbentPathsAndFunctions();
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

    private double reallocateFunctions(int s, int d, int p, double bestObjVal) {
        double localBestObjVal = bestObjVal;
        for (int j = 0; j < pm.getServices().get(s).getFunctions().size()
                * pm.getServices().get(s).getFunctions().size(); j++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                List<Integer> availableServers = networkManager.findServersForFunctionsInPath(s, d, p).get(v);
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
        return localBestObjVal;
    }

    private void reallocateSpecificFunction(int s, int d, int v, int xOld, int xNew) {
        if (xOld != -1)
            networkManager.removeDemandToFunctionToServer(s, xOld, v, d);
        networkManager.addDemandToFunctionToServer(s, xNew, v, d);
        networkManager.removeUnusedFunctions(s);
        networkManager.removeSyncTraffic(s);
        networkManager.addSyncTraffic(s);
        vars.generateRestOfVariablesForResults(initialPlacement, objFunc);
    }

    private Integer choosePath(String algorithm, List<Integer> paths, TrafficFlow tf) {
        int chosenPath = -1;
        if (algorithm.equals(FIRST_FIT) || algorithm.equals(FFP_RFX))
            chosenPath = chooseFirstFitPath(paths, tf);
        else if (algorithm.equals(RANDOM_FIT) || algorithm.equals(RFP_FFX))
            chosenPath = paths.get(rnd.nextInt(paths.size()));
        return chosenPath;
    }

    public List<Integer> chooseServersForFunctionAllocation(String algorithm, int s, int d, int pChosen,
            List<List<Integer>> listAvailableServersPerFunction) {
        List<Integer> specificServers = new ArrayList<>();
        int lastPathNodeUsed = 0;
        for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            List<Integer> availableServers = listAvailableServersPerFunction.get(v);
            int xChosen = chooseServerForFunction(algorithm, availableServers, lastPathNodeUsed, s, pChosen, v, d);
            specificServers.add(xChosen);
            lastPathNodeUsed = networkManager.getNodePathIndexFromServer(s, pChosen, xChosen);
        }
        return specificServers;
    }

    private Integer chooseServerForFunction(String algorithm, List<Integer> availableServers, int lastPathNodeUsed,
            int s, int p, int v, int d) {
        int chosenServer = -1;
        if (algorithm.equals(FIRST_FIT) || algorithm.equals(RFP_FFX))
            chosenServer = chooseFirstFitServerForFunction(availableServers, lastPathNodeUsed, s, p, v, d);
        else if (algorithm.equals(RANDOM_FIT) || algorithm.equals(FFP_RFX)) {
            while (true) {
                chosenServer = availableServers.get(rnd.nextInt(availableServers.size()));
                int nodeIndex = networkManager.getNodePathIndexFromServer(s, p, chosenServer);
                if (nodeIndex >= lastPathNodeUsed)
                    break;
            }
        }
        if (chosenServer == -1) {
            Auxiliary.printLog(log, ERROR,
                    "function could not be allocated [s][d][p][v] = [" + s + "][" + d + "][" + p + "][" + v + "]");
            System.exit(-1);
        }
        return chosenServer;
    }

    private Integer chooseFirstFitPath(List<Integer> paths, TrafficFlow tf) {
        boolean cloudPathsFirst = false;
        if (objFunc.equals(OPEX_SERVERS_OBJ)) // order set of admissible paths depending on the objective function
            cloudPathsFirst = true;
        paths = orderPaths(paths, cloudPathsFirst, tf);
        return paths.get(0); // and take the first path
    }

    private Integer chooseFirstFitServerForFunction(List<Integer> availableServers, int lastPathNodeUsed, int s, int p,
            int v, int d) {
        int chosenServer = -1;
        boolean cloudServersFirst = false;
        if (objFunc.equals(OPEX_SERVERS_OBJ)) // order set of available servers depending on the objective function
            cloudServersFirst = true;
        availableServers = selectServers(availableServers, cloudServersFirst);
        availableServers = networkManager.removePreviousServersFromNodeIndex(availableServers, lastPathNodeUsed, s, p);

        for (Integer xAvailable : availableServers)
            if (networkManager.checkIfFreeServerResources(s, xAvailable, v, d, 1)) {
                chosenServer = xAvailable;
                break;
            }
        return chosenServer;
    }

    private List<Integer> orderPaths(List<Integer> paths, boolean connectingCloudFirst, TrafficFlow tf) {
        List<Integer> pathsConnectingEdge = new ArrayList<>();
        List<Integer> pathsConnectingCloud = new ArrayList<>();
        for (Integer p : paths) {
            boolean connectsCloud = false;
            for (Node node : tf.getPaths().get(p).getNodePath())
                if (node.hasAttribute(NODE_CLOUD))
                    connectsCloud = true;
            if (connectsCloud)
                pathsConnectingCloud.add(p);
            else
                pathsConnectingEdge.add(p);

        }
        List<Integer> orderedPaths = new ArrayList<>();
        if (connectingCloudFirst) {
            orderedPaths.addAll(pathsConnectingCloud);
            orderedPaths.addAll(pathsConnectingEdge);
        } else {
            orderedPaths.addAll(pathsConnectingEdge);
            orderedPaths.addAll(pathsConnectingCloud);
        }
        return orderedPaths;
    }

    private List<Integer> selectServers(List<Integer> servers, boolean cloudFirst) {
        List<Integer> chosenServers = new ArrayList<>();
        if (cloudFirst) {
            for (Integer x : servers)
                if (pm.getServers().get(x).getParent().hasAttribute(NODE_CLOUD))
                    chosenServers.add(x);
            int serverIndex = -1;
            for (int i = 0; i < servers.size(); i++) {
                if (!servers.get(i).equals(chosenServers.get(chosenServers.size() - 1)))
                    continue;
                serverIndex = i + 1;
            }
            for (int i = serverIndex; i < servers.size(); i++)
                chosenServers.add(servers.get(i));
        } else
            chosenServers = servers;
        return chosenServers;
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
}