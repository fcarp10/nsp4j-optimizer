package optimizer.scenarios;

import static optimizer.Definitions.*;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gurobi.GRBException;
import gurobi.GRBModel;
import optimizer.Manager;
import optimizer.Parameters;
import optimizer.algorithms.LauncherAlg;
import optimizer.algorithms.VariablesAlg;
import optimizer.gui.Scenario;
import optimizer.lp.LauncherLP;
import optimizer.results.Auxiliary;
import optimizer.results.ResultsManager;
import static optimizer.results.Auxiliary.printLog;

public class Journal {

    private static final Logger log = LoggerFactory.getLogger(Journal.class);

    private static Parameters pm;

    public static String readParameters(String graphNameForm, boolean considerSubsetOfDemands,
            ArrayList<Integer> services, ArrayList<Integer> serviceLength, int serverCapacity) {
        String path = Auxiliary.getResourcesPath(graphNameForm + ".yml");
        String graphName = Manager.readYamlFile(path, graphNameForm);
        modifyYamlParameters(services, serviceLength, serverCapacity);
        Manager.readTopologyFiles(path, graphName);
        Manager.determineUsedDemands(pm, considerSubsetOfDemands);
        return graphName;
    }

    private static void modifyYamlParameters(ArrayList<Integer> services, ArrayList<Integer> serviceLength,
            int serverCapacity) {
        if (services.get(0) > 0)
            pm.getGlobal().put(SERVICES, services);
        if (serviceLength.get(0) > 0)
            pm.getGlobal().put(SERVICE_LENGTH, serviceLength);
        if (serverCapacity > 0)
            pm.getGlobal().put(SERVER_CAPACITY, serverCapacity);
    }

    public static void run(Parameters parameters, Scenario sce) {
        pm = parameters;
        ResultsManager rm;
        String outputFileName;
        try {
            switch (sce.getName()) {
                case JOURNAL_LP_INIT:
                    rm = new ResultsManager(pm.getGraphName());
                    String graphNameShort = Manager.readParameters(sce.getInputFileName());
                    GRBModel initModel = rm.loadInitialPlacement(
                            Auxiliary.getResourcesPath(graphNameShort + "_init-lp.mst") + graphNameShort + "_init-lp",
                            pm,
                            sce);
                    GRBModel initSol = rm.loadModel(
                            Auxiliary.getResourcesPath(pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc() + ".mst")
                                    + pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc(),
                            pm, sce, false);
                    outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc();
                    LauncherLP.run(pm, sce, rm, initModel, initSol, outputFileName, true);
                    break;

                case JOURNAL_ALL_SFC_LENGTH:
                    runCustomSFCLength(sce, JOURNAL_ALL);
                    break;

                case JOURNAL_ALL_SERVER_CAP:
                    runCustomServerCap(sce, JOURNAL_ALL);
                    break;

                case JOURNAL_HEU_SFC_LENGTH:
                    runCustomSFCLength(sce, JOURNAL_HEU);
                    break;

                case JOURNAL_HEU_SERVER_CAP:
                    runCustomServerCap(sce, JOURNAL_HEU);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            printLog(log, ERROR, "something went wrong");
        }
    }

    private static void runCustomSFCLength(Scenario sce, String customString) throws GRBException {
        ArrayList<Integer> services = new ArrayList<>();
        ArrayList<Integer> serviceLength = new ArrayList<>();
        for (int s = 1; s <= 10; s++) {
            services = new ArrayList<>();
            services.add(1);
            serviceLength = new ArrayList<>();
            serviceLength.add(s);
            if (customString.equals(JOURNAL_ALL))
                runJournalAll(sce, services, serviceLength, 0);
            if (customString.equals(JOURNAL_HEU))
                runJournalHeu(sce, services, serviceLength, 0);
        }
    }

    private static void runCustomServerCap(Scenario sce, String customString) throws GRBException {
        ArrayList<Integer> services = new ArrayList<>();
        ArrayList<Integer> serviceLength = new ArrayList<>();
        for (int s = 1; s <= 10; s++) {
            services.add(1);
            serviceLength.add(s);
        }

        int[] serverCaps = new int[] { 250, 500, 750, 1000, 1250, 1500, 1750, 2000, 2250, 2500, 2750, 3000 };
        for (int s = 0; s < serverCaps.length; s++) {
            if (customString.equals(JOURNAL_ALL))
                runJournalAll(sce, services, serviceLength, serverCaps[s]);
            if (customString.equals(JOURNAL_HEU))
                runJournalHeu(sce, services, serviceLength, serverCaps[s]);
        }
    }

    private static void runJournalAll(Scenario sce, ArrayList<Integer> services, ArrayList<Integer> service_lengths,
            int serverCap) throws GRBException {
        String resultsFolderExtension = "";
        if (serverCap != 0)
            resultsFolderExtension = "_" + String.valueOf(serverCap);
        else
            resultsFolderExtension = "_" + service_lengths.get(0);
        ResultsManager rm = new ResultsManager(sce.getInputFileName() + resultsFolderExtension);
        boolean toMST = false;

        // 1 - obsv1 [LP]
        GRBModel obsv1LP = runJournalLP(sce, MGR_REP_CLOUD, OBSV_1, NULL, rm, null, toMST, services, service_lengths,
                serverCap);
        VariablesAlg obsv1Alg = new VariablesAlg(pm, obsv1LP);
        // 2 - pred2 [LP]
        GRBModel pred2LP = runJournalLP(sce, MGR_REP_CLOUD, PRED_2, NULL, rm, null, toMST, services, service_lengths,
                serverCap);
        VariablesAlg pred2Alg = new VariablesAlg(pm, pred2LP);
        // 3 - over2 [LP]
        GRBModel over2LP = runJournalLP(sce, MGR_REP_CLOUD, OVER_2, NULL, rm, null, toMST, services, service_lengths,
                serverCap);
        VariablesAlg over2Alg = new VariablesAlg(pm, over2LP);

        // 1 - obsv1 -- > obsv2 [LP]
        runJournalLP(sce, MGR, OBSV_2, OBSV_1, rm, obsv1LP, toMST, services, service_lengths, serverCap);
        runJournalLP(sce, REP, OBSV_2, OBSV_1, rm, obsv1LP, toMST, services, service_lengths, serverCap);
        runJournalLP(sce, CLOUD, OBSV_2, OBSV_1, rm, obsv1LP, toMST, services, service_lengths, serverCap);
        runJournalLP(sce, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1LP, toMST, services, service_lengths, serverCap);
        // 2 - pred2 -- > obsv2 [LP]
        runJournalLP(sce, MGR, OBSV_2, PRED_2, rm, pred2LP, toMST, services, service_lengths, serverCap);
        runJournalLP(sce, REP, OBSV_2, PRED_2, rm, pred2LP, toMST, services, service_lengths, serverCap);
        runJournalLP(sce, CLOUD, OBSV_2, PRED_2, rm, pred2LP, toMST, services, service_lengths, serverCap);
        runJournalLP(sce, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2LP, toMST, services, service_lengths, serverCap);
        // 3 - over2 -- > obsv2 [LP]
        runJournalLP(sce, MGR, OBSV_2, OVER_2, rm, over2LP, toMST, services, service_lengths, serverCap);
        runJournalLP(sce, REP, OBSV_2, OVER_2, rm, over2LP, toMST, services, service_lengths, serverCap);
        runJournalLP(sce, CLOUD, OBSV_2, OVER_2, rm, over2LP, toMST, services, service_lengths, serverCap);
        runJournalLP(sce, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2LP, toMST, services, service_lengths, serverCap);

        // 1 - obsv1 -- > obsv2 [FF]
        runJournalHeu(sce, FF, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1Alg, toMST, services, service_lengths, serverCap);
        // 2 - pred2 -- > obsv2 [FF]
        runJournalHeu(sce, FF, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2Alg, toMST, services, service_lengths, serverCap);
        // 3 - over2 -- > obsv2 [FF]
        runJournalHeu(sce, FF, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2Alg, toMST, services, service_lengths, serverCap);

        // 1 - obsv1 -- > obsv2 [RF]
        for (int i = 0; i < 10; i++)
            runJournalHeu(sce, RF, MGR_REP_CLOUD, OBSV_2, OBSV_1 + "_" + i, rm, obsv1Alg, toMST, services,
                    service_lengths,
                    serverCap);
        // 2 - pred2 -- > obsv2 [RF]
        for (int i = 0; i < 10; i++)
            runJournalHeu(sce, RF, MGR_REP_CLOUD, OBSV_2, PRED_2 + "_" + i, rm, pred2Alg, toMST, services,
                    service_lengths,
                    serverCap);
        // 3 - over2 -- > obsv2 [RF]
        for (int i = 0; i < 10; i++)
            runJournalHeu(sce, RF, MGR_REP_CLOUD, OBSV_2, OVER_2 + "_" + i, rm, over2Alg, toMST, services,
                    service_lengths,
                    serverCap);

        // 1 - obsv1 -- > obsv2 [GRD]
        runJournalHeu(sce, GRD, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1Alg, toMST, services, service_lengths,
                serverCap);
        // 2 - pred2 -- > obsv2 [GRD]
        runJournalHeu(sce, GRD, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2Alg, toMST, services, service_lengths,
                serverCap);
        // 3 - over2 -- > obsv2 [GRD]
        runJournalHeu(sce, GRD, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2Alg, toMST, services, service_lengths,
                serverCap);
    }

    private static void runJournalHeu(Scenario sce, ArrayList<Integer> services, ArrayList<Integer> service_lengths,
            int serverCap) {
        String resultsFolderExtension = "";
        if (serverCap != 0)
            resultsFolderExtension = "_" + String.valueOf(serverCap);
        else
            resultsFolderExtension = "_" + service_lengths.get(0);
        ResultsManager rm = new ResultsManager(sce.getInputFileName() + resultsFolderExtension);
        boolean toMST = false;

        // 1 - obsv1 [GRD]
        VariablesAlg obsv1GRD = runJournalHeu(sce, JOURNAL_GRD_FIRST, MGR_REP_CLOUD, OBSV_1, NULL, rm, null, toMST, services,
                service_lengths, serverCap);
        // 2 - pred2 [GRD]
        VariablesAlg pred2GRD = runJournalHeu(sce, JOURNAL_GRD_FIRST, MGR_REP_CLOUD, PRED_2, NULL, rm, null, toMST, services,
                service_lengths, serverCap);
        // 3 - over2 [GRD]
        VariablesAlg over2GRD = runJournalHeu(sce, JOURNAL_GRD_FIRST, MGR_REP_CLOUD, OVER_2, NULL, rm, null, toMST, services,
                service_lengths, serverCap);

        // 1 - obsv1 -- > obsv2 [FF]
        runJournalHeu(sce, FF, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1GRD, toMST, services, service_lengths, serverCap);
        // 2 - pred2 -- > obsv2 [FF]
        runJournalHeu(sce, FF, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2GRD, toMST, services, service_lengths, serverCap);
        // 3 - over2 -- > obsv2 [FF]
        runJournalHeu(sce, FF, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2GRD, toMST, services, service_lengths, serverCap);

        // 1 - obsv1 -- > obsv2 [RF]
        for (int i = 0; i < 10; i++)
            runJournalHeu(sce, RF, MGR_REP_CLOUD, OBSV_2, OBSV_1 + "_" + i, rm, obsv1GRD, toMST, services,
                    service_lengths,
                    serverCap);
        // 2 - pred2 -- > obsv2 [RF]
        for (int i = 0; i < 10; i++)
            runJournalHeu(sce, RF, MGR_REP_CLOUD, OBSV_2, PRED_2 + "_" + i, rm, pred2GRD, toMST, services,
                    service_lengths,
                    serverCap);
        // 3 - over2 -- > obsv2 [RF]
        for (int i = 0; i < 10; i++)
            runJournalHeu(sce, RF, MGR_REP_CLOUD, OBSV_2, OVER_2 + "_" + i, rm, over2GRD, toMST, services,
                    service_lengths,
                    serverCap);

        // 1 - obsv1 -- > obsv2 [GRD]
        runJournalHeu(sce, GRD, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1GRD, toMST, services, service_lengths,
                serverCap);
        // 2 - pred2 -- > obsv2 [GRD]
        runJournalHeu(sce, GRD, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2GRD, toMST, services, service_lengths,
                serverCap);
        // 3 - over2 -- > obsv2 [GRD]
        runJournalHeu(sce, GRD, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2GRD, toMST, services, service_lengths,
                serverCap);
    }

    private static GRBModel runJournalLP(Scenario sce, String objFunc, String inputFileExtension,
            String outputFileExtension, ResultsManager resultsManager, GRBModel initPlacementModel, boolean exportMST,
            ArrayList<Integer> services, ArrayList<Integer> serviceLength, int serverCap) throws GRBException {
        readParameters(sce.getInputFileName() + "_" + inputFileExtension, false, services, serviceLength, serverCap);
        sce.setObjFunc(objFunc);
        sce.setConstraint(PATHS_SERVERS_CLOUD, true);
        String outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_" + outputFileExtension;
        return LauncherLP.run(pm, sce, resultsManager, initPlacementModel, null, outputFileName, exportMST);
    }

    private static VariablesAlg runJournalHeu(Scenario sce, String alg, String objFunc, String inputFileExtension,
            String outputFileExtension, ResultsManager resultsManager, VariablesAlg initPlacementVars,
            boolean exportMST,
            ArrayList<Integer> services, ArrayList<Integer> serviceLength, int serverCap) {
        readParameters(sce.getInputFileName() + "_" + inputFileExtension, false, services, serviceLength, serverCap);
        sce.setName(alg);
        sce.setObjFunc(objFunc);
        String outputFileName = pm.getGraphName() + "_" + alg + "_" + sce.getObjFunc() + "_" + outputFileExtension;
        return LauncherAlg.run(pm, sce, resultsManager, initPlacementVars, outputFileName, exportMST);
    }

}
