package optimizer;

import gurobi.GRBException;
import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.TrafficFlow;
import optimizer.gui.ResultsGUI;
import optimizer.gui.Scenario;
import optimizer.algorithms.LauncherAlg;
import optimizer.algorithms.VariablesAlg;
import optimizer.lp.LauncherLP;
import optimizer.results.Auxiliary;
import optimizer.results.ResultsManager;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConfigFiles;

import java.util.ArrayList;
import java.util.Random;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class Manager {

   private static final Logger log = LoggerFactory.getLogger(Manager.class);
   private static boolean interrupted;
   private static Parameters pm;

   public static String readParameters(String graphNameForm, boolean considerSubsetOfDemands,
         ArrayList<Integer> services, ArrayList<Integer> serviceLength, int serverCapacity) {
      String path = Auxiliary.getResourcesPath(graphNameForm + ".yml");
      String graphName = readYamlFile(path, graphNameForm);
      modifyYamlParameters(services, serviceLength, serverCapacity);
      readTopologyFiles(path, graphName);
      determineUsedDemands(pm, considerSubsetOfDemands);
      return graphName;
   }

   public static String readParameters(String graphNameForm) {
      String path = Auxiliary.getResourcesPath(graphNameForm + ".yml");
      String graphName = readYamlFile(path, graphNameForm);
      readTopologyFiles(path, graphName);
      determineUsedDemands(pm, false);
      return graphName;
   }

   private static String readYamlFile(String path, String graphNameForm) {
      String[] graphName = graphNameForm.split("_");
      try {
         pm = ConfigFiles.readParameters(path + graphNameForm + ".yml");
      } catch (Exception e) {
         printLog(log, ERROR, "error loading .yml file");
      }
      return graphName[0];
   }

   private static void modifyYamlParameters(ArrayList<Integer> services, ArrayList<Integer> serviceLength,
         int serverCapacity) {
      if (services.get(0) > 0)
         pm.getAux().put(SERVICES, services);
      if (serviceLength.get(0) > 0)
         pm.getAux().put(SERVICE_LENGTH, serviceLength);
      if (serverCapacity > 0)
         pm.getAux().put(SERVER_CAPACITY, serverCapacity);
   }

   private static void readTopologyFiles(String path, String graphName) {
      String[] extensions = new String[] { ".dgs", ".gml" };
      boolean isLoaded = false;
      for (int i = 0; i < extensions.length; i++) {
         if (pm.initialize(path + graphName + extensions[i], path + graphName + ".txt",
               (boolean) pm.getAux(DIRECTED_EDGES), (boolean) pm.getAux(ALL_NODES_TO_CLOUD))) {
            isLoaded = true;
            break;
         }
         printLog(log, WARNING, graphName + extensions[i] + " file not found");
      }
      if (!isLoaded) {
         printLog(log, ERROR, "error loading graph or paths files");
      }
      checkTopologyScale(pm);
      ResultsGUI.initialize(pm);
      printLog(log, INFO, "topology loaded");
   }

   private static void checkTopologyScale(Parameters pm) {
      double scalingX = (double) pm.getAux(X_SCALING);
      double scalingY = (double) pm.getAux(Y_SCALING);
      if (scalingX != 1.0 || scalingY != 1.0) {
         String longitudeLabel, latitudeLabel;
         if (pm.getNodes().get(0).getAttribute(LONGITUDE_LABEL_1) != null) {
            longitudeLabel = LONGITUDE_LABEL_1;
            latitudeLabel = LATITUDE_LABEL_1;
         } else {
            longitudeLabel = LONGITUDE_LABEL_2;
            latitudeLabel = LATITUDE_LABEL_2;
         }
         for (Node node : pm.getNodes()) {
            if (node.getAttribute(NODE_CLOUD) != null && node.getAttribute(longitudeLabel + "_gui") != null) {
               node.setAttribute(longitudeLabel + "_gui",
                     (double) node.getAttribute(longitudeLabel + "_gui") * scalingX);
               node.setAttribute(latitudeLabel + "_gui", (double) node.getAttribute(latitudeLabel + "_gui") * scalingY);
            } else {
               node.setAttribute(longitudeLabel, (double) node.getAttribute(longitudeLabel) * scalingX);
               node.setAttribute(latitudeLabel, (double) node.getAttribute(latitudeLabel) * scalingY);
            }
         }
      }
   }

   private static void determineUsedDemands(Parameters pm, boolean considerSubset) {
      Random rnd = new Random(pm.getSeed());
      for (TrafficFlow trafficFlow : pm.getTrafficFlows()) {
         trafficFlow.getAux().clear();
         for (int d = 0; d < trafficFlow.getDemands().size(); d++)
            trafficFlow.getAux().add(true);
         if (considerSubset) {
            double initialTrafficLoad = (double) pm.getAux().get(INITIAL_TRAFFIC_LOAD);
            double value;
            for (int d = 0; d < trafficFlow.getDemands().size(); d++) {
               value = rnd.nextDouble();
               if (value > initialTrafficLoad)
                  trafficFlow.getAux().set(d, false);
            }
            boolean allFalse = true;
            for (int d = 0; d < trafficFlow.getDemands().size(); d++)
               if (trafficFlow.getAux().get(d))
                  allFalse = false;
            if (allFalse)
               trafficFlow.getAux().set(0, true);
         }
      }
   }

   public static void main(Scenario sce) {
      ResultsManager rm;
      String graphNameShort;
      try {
         switch (sce.getName()) {
         case LP:
            boolean exportMST = true;
            rm = new ResultsManager(pm.getGraphName());
            readParameters(sce.getInputFileName());
            String outputFileName = pm.getGraphName() + sce.getName() + sce.getObjFunc();
            LauncherLP.run(pm, sce, rm, null, null, outputFileName, exportMST);
            break;

         case FF:
            rm = new ResultsManager(pm.getGraphName());
            readParameters(sce.getInputFileName());
            outputFileName = pm.getGraphName() + "_" + FF + "_" + sce.getObjFunc();
            LauncherAlg.run(pm, sce, rm, null, outputFileName, false);
            break;

         case RF:
            rm = new ResultsManager(pm.getGraphName());
            readParameters(sce.getInputFileName());
            for (int i = 0; i < 10; i++) {
               outputFileName = pm.getGraphName() + "_" + RF + "_" + sce.getObjFunc() + "_" + i;
               LauncherAlg.run(pm, sce, rm, null, outputFileName, false);
            }
            break;

         case GRD:
            rm = new ResultsManager(pm.getGraphName());
            readParameters(sce.getInputFileName());
            outputFileName = pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc();
            LauncherAlg.run(pm, sce, rm, null, outputFileName, false);
            break;

         case CUSTOM_1:
            exportMST = true;
            rm = new ResultsManager(pm.getGraphName());
            graphNameShort = readParameters(sce.getInputFileName());
            GRBModel initModel = rm.loadInitialPlacement(
                  Auxiliary.getResourcesPath(graphNameShort + "_init-lp.mst") + graphNameShort + "_init-lp", pm,
                  sce);
            GRBModel initSol = rm.loadModel(
                  Auxiliary.getResourcesPath(pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc() + ".mst")
                        + pm.getGraphName() + "_" + GRD + "_" + sce.getObjFunc(),
                  pm, sce, false);
            outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc();
            LauncherLP.run(pm, sce, rm, initModel, initSol, outputFileName, exportMST);
            break;

         case CUSTOM_2_SFC_LENGTH:
            runCustomSFCLength(sce, CUSTOM_2);
            break;

         case CUSTOM_2_SERVER_CAP:
            runCustomServerCap(sce, CUSTOM_2);
            break;

         case CUSTOM_3_SFC_LENGTH:
            runCustomSFCLength(sce, CUSTOM_3);
            break;

         case CUSTOM_3_SERVER_CAP:
            runCustomServerCap(sce, CUSTOM_3);
            break;

         default:
            printLog(log, INFO, "no algorithm selected");
            break;
         }
         printLog(log, INFO, "backend is ready");
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
         if (customString.equals(CUSTOM_2))
            runCustom2(sce, services, serviceLength, 0);
         if (customString.equals(CUSTOM_3))
            runCustom3(sce, services, serviceLength, 0);
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
         if (customString.equals(CUSTOM_2))
            runCustom2(sce, services, serviceLength, serverCaps[s]);
         if (customString.equals(CUSTOM_3))
            runCustom3(sce, services, serviceLength, serverCaps[s]);
      }
   }

   private static void runCustom2(Scenario sce, ArrayList<Integer> services, ArrayList<Integer> service_lengths,
         int serverCap) throws GRBException {
      String resultsFolderExtension = "";
      if (serverCap != 0)
         resultsFolderExtension = "_" + String.valueOf(serverCap);
      else
         resultsFolderExtension = "_" + service_lengths.get(0);
      ResultsManager rm = new ResultsManager(sce.getInputFileName() + resultsFolderExtension);
      boolean toMST = false;

      // 1 - obsv1 [LP]
      GRBModel obsv1LP = runCustomLP(sce, MGR_REP_CLOUD, OBSV_1, NULL, rm, null, toMST, services, service_lengths,
            serverCap);
      VariablesAlg obsv1Alg = new VariablesAlg(pm, obsv1LP);
      // 2 - pred2 [LP]
      GRBModel pred2LP = runCustomLP(sce, MGR_REP_CLOUD, PRED_2, NULL, rm, null, toMST, services, service_lengths,
            serverCap);
      VariablesAlg pred2Alg = new VariablesAlg(pm, pred2LP);
      // 3 - over2 [LP]
      GRBModel over2LP = runCustomLP(sce, MGR_REP_CLOUD, OVER_2, NULL, rm, null, toMST, services, service_lengths,
            serverCap);
      VariablesAlg over2Alg = new VariablesAlg(pm, over2LP);

      // 1 - obsv1 -- > obsv2 [LP]
      runCustomLP(sce, MGR, OBSV_2, OBSV_1, rm, obsv1LP, toMST, services, service_lengths, serverCap);
      runCustomLP(sce, REP, OBSV_2, OBSV_1, rm, obsv1LP, toMST, services, service_lengths, serverCap);
      runCustomLP(sce, CLOUD, OBSV_2, OBSV_1, rm, obsv1LP, toMST, services, service_lengths, serverCap);
      runCustomLP(sce, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1LP, toMST, services, service_lengths, serverCap);
      // 2 - pred2 -- > obsv2 [LP]
      runCustomLP(sce, MGR, OBSV_2, PRED_2, rm, pred2LP, toMST, services, service_lengths, serverCap);
      runCustomLP(sce, REP, OBSV_2, PRED_2, rm, pred2LP, toMST, services, service_lengths, serverCap);
      runCustomLP(sce, CLOUD, OBSV_2, PRED_2, rm, pred2LP, toMST, services, service_lengths, serverCap);
      runCustomLP(sce, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2LP, toMST, services, service_lengths, serverCap);
      // 3 - over2 -- > obsv2 [LP]
      runCustomLP(sce, MGR, OBSV_2, OVER_2, rm, over2LP, toMST, services, service_lengths, serverCap);
      runCustomLP(sce, REP, OBSV_2, OVER_2, rm, over2LP, toMST, services, service_lengths, serverCap);
      runCustomLP(sce, CLOUD, OBSV_2, OVER_2, rm, over2LP, toMST, services, service_lengths, serverCap);
      runCustomLP(sce, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2LP, toMST, services, service_lengths, serverCap);

      // 1 - obsv1 -- > obsv2 [FF]
      runCustomAlg(sce, FF, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1Alg, toMST, services, service_lengths, serverCap);
      // 2 - pred2 -- > obsv2 [FF]
      runCustomAlg(sce, FF, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2Alg, toMST, services, service_lengths, serverCap);
      // 3 - over2 -- > obsv2 [FF]
      runCustomAlg(sce, FF, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2Alg, toMST, services, service_lengths, serverCap);

      // 1 - obsv1 -- > obsv2 [RF]
      for (int i = 0; i < 10; i++)
         runCustomAlg(sce, RF, MGR_REP_CLOUD, OBSV_2, OBSV_1 + "_" + i, rm, obsv1Alg, toMST, services, service_lengths,
               serverCap);
      // 2 - pred2 -- > obsv2 [RF]
      for (int i = 0; i < 10; i++)
         runCustomAlg(sce, RF, MGR_REP_CLOUD, OBSV_2, PRED_2 + "_" + i, rm, pred2Alg, toMST, services, service_lengths,
               serverCap);
      // 3 - over2 -- > obsv2 [RF]
      for (int i = 0; i < 10; i++)
         runCustomAlg(sce, RF, MGR_REP_CLOUD, OBSV_2, OVER_2 + "_" + i, rm, over2Alg, toMST, services, service_lengths,
               serverCap);

      // 1 - obsv1 -- > obsv2 [GRD]
      runCustomAlg(sce, GRD, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1Alg, toMST, services, service_lengths, serverCap);
      // 2 - pred2 -- > obsv2 [GRD]
      runCustomAlg(sce, GRD, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2Alg, toMST, services, service_lengths, serverCap);
      // 3 - over2 -- > obsv2 [GRD]
      runCustomAlg(sce, GRD, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2Alg, toMST, services, service_lengths, serverCap);
   }

   private static void runCustom3(Scenario sce, ArrayList<Integer> services, ArrayList<Integer> service_lengths,
         int serverCap) {
      String resultsFolderExtension = "";
      if (serverCap != 0)
         resultsFolderExtension = "_" + String.valueOf(serverCap);
      else
         resultsFolderExtension = "_" + service_lengths.get(0);
      ResultsManager rm = new ResultsManager(sce.getInputFileName() + resultsFolderExtension);
      boolean toMST = false;

      // 1 - obsv1 [GRD]
      VariablesAlg obsv1GRD = runCustomAlg(sce, GRD_FIRST, MGR_REP_CLOUD, OBSV_1, NULL, rm, null, toMST, services,
            service_lengths, serverCap);
      // 2 - pred2 [GRD]
      VariablesAlg pred2GRD = runCustomAlg(sce, GRD_FIRST, MGR_REP_CLOUD, PRED_2, NULL, rm, null, toMST, services,
            service_lengths, serverCap);
      // 3 - over2 [GRD]
      VariablesAlg over2GRD = runCustomAlg(sce, GRD_FIRST, MGR_REP_CLOUD, OVER_2, NULL, rm, null, toMST, services,
            service_lengths, serverCap);

      // 1 - obsv1 -- > obsv2 [FF]
      runCustomAlg(sce, FF, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1GRD, toMST, services, service_lengths, serverCap);
      // 2 - pred2 -- > obsv2 [FF]
      runCustomAlg(sce, FF, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2GRD, toMST, services, service_lengths, serverCap);
      // 3 - over2 -- > obsv2 [FF]
      runCustomAlg(sce, FF, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2GRD, toMST, services, service_lengths, serverCap);

      // 1 - obsv1 -- > obsv2 [RF]
      for (int i = 0; i < 10; i++)
         runCustomAlg(sce, RF, MGR_REP_CLOUD, OBSV_2, OBSV_1 + "_" + i, rm, obsv1GRD, toMST, services, service_lengths,
               serverCap);
      // 2 - pred2 -- > obsv2 [RF]
      for (int i = 0; i < 10; i++)
         runCustomAlg(sce, RF, MGR_REP_CLOUD, OBSV_2, PRED_2 + "_" + i, rm, pred2GRD, toMST, services, service_lengths,
               serverCap);
      // 3 - over2 -- > obsv2 [RF]
      for (int i = 0; i < 10; i++)
         runCustomAlg(sce, RF, MGR_REP_CLOUD, OBSV_2, OVER_2 + "_" + i, rm, over2GRD, toMST, services, service_lengths,
               serverCap);

      // 1 - obsv1 -- > obsv2 [GRD]
      runCustomAlg(sce, GRD, MGR_REP_CLOUD, OBSV_2, OBSV_1, rm, obsv1GRD, toMST, services, service_lengths, serverCap);
      // 2 - pred2 -- > obsv2 [GRD]
      runCustomAlg(sce, GRD, MGR_REP_CLOUD, OBSV_2, PRED_2, rm, pred2GRD, toMST, services, service_lengths, serverCap);
      // 3 - over2 -- > obsv2 [GRD]
      runCustomAlg(sce, GRD, MGR_REP_CLOUD, OBSV_2, OVER_2, rm, over2GRD, toMST, services, service_lengths, serverCap);
   }

   private static GRBModel runCustomLP(Scenario sce, String objFunc, String inputFileExtension,
         String outputFileExtension, ResultsManager resultsManager, GRBModel initPlacementModel, boolean exportMST,
         ArrayList<Integer> services, ArrayList<Integer> serviceLength, int serverCap) throws GRBException {
      readParameters(sce.getInputFileName() + "_" + inputFileExtension, false, services, serviceLength, serverCap);
      sce.setObjFunc(objFunc);
      sce.setConstraint(PATHS_SERVERS_CLOUD, true);
      String outputFileName = pm.getGraphName() + "_" + LP + "_" + sce.getObjFunc() + "_" + outputFileExtension;
      return LauncherLP.run(pm, sce, resultsManager, initPlacementModel, null, outputFileName, exportMST);
   }

   private static VariablesAlg runCustomAlg(Scenario sce, String alg, String objFunc, String inputFileExtension,
         String outputFileExtension, ResultsManager resultsManager, VariablesAlg initPlacementVars, boolean exportMST,
         ArrayList<Integer> services, ArrayList<Integer> serviceLength, int serverCap) {
      readParameters(sce.getInputFileName() + "_" + inputFileExtension, false, services, serviceLength, serverCap);
      sce.setName(alg);
      sce.setObjFunc(objFunc);
      String outputFileName = pm.getGraphName() + "_" + alg + "_" + sce.getObjFunc() + "_" + outputFileExtension;
      return LauncherAlg.run(pm, sce, resultsManager, initPlacementVars, outputFileName, exportMST);
   }

   public static void terminate() {
      interrupted = true;
   }

   public static void reset() {
      interrupted = false;
   }

   public static boolean isInterrupted() {
      return interrupted;
   }
}
