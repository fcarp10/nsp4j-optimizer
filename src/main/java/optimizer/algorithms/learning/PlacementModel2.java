package optimizer.algorithms.learning;

import static optimizer.Definitions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import manager.Parameters;
import optimizer.algorithms.Heuristic;
import optimizer.algorithms.VariablesAlg;
import optimizer.results.Auxiliary;

public class PlacementModel2 {
   protected Parameters pm;
   protected VariablesAlg vars;
   protected boolean[][][] initialPlacement;
   protected String objFunc;
   private DeepQ deepQ;
   private MultiLayerConfiguration conf;
   private final int offsetInput;
   private Heuristic heu;
   private Map<String, Double> epsilons;
   private Map<String, Integer> optimumPaths;
   private int outputLength;
   private int inputLength;

   private static final Logger log = LoggerFactory.getLogger(PlacementModel2.class);

   public PlacementModel2(String conf, Parameters pm, VariablesAlg variablesAlg, boolean[][][] initialPlacement,
         String objFunc, Heuristic heu) {
      this.pm = pm;
      this.vars = variablesAlg;
      this.initialPlacement = initialPlacement;
      this.objFunc = objFunc;
      this.heu = heu;
      offsetInput = 5;
      inputLength = pm.getServers().size() + offsetInput;
      outputLength = pm.getServers().size() + 1;
      epsilons = new HashMap<>();
      optimumPaths = new HashMap<>();
      if (conf == null)
         initializeModel(inputLength, outputLength);
      else
         initializeModel(conf, inputLength);
   }

   private void initializeModel(int inputLength, int outputLength) {
      conf = new NeuralNetConfiguration.Builder().seed(123)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).list()
            .layer(0,
                  new DenseLayer.Builder().nIn(inputLength).nOut(NUM_HIDDEN_LAYERS).weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU).build())
            .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(NUM_HIDDEN_LAYERS).nOut(outputLength)
                  .weightInit(WeightInit.XAVIER).activation(Activation.IDENTITY).build())
            .build();
      deepQ = new DeepQ(conf, MEMORY_CAPACITY, DISCOUNT_FACTOR, BATCH_SIZE, FREQUENCY, START_SIZE, inputLength);
   }

   private void initializeModel(String confString, int inputLength) {
      MultiLayerConfiguration conf = MultiLayerConfiguration.fromJson(confString);
      this.conf = conf;
      deepQ = new DeepQ(conf, MEMORY_CAPACITY, DISCOUNT_FACTOR, BATCH_SIZE, FREQUENCY, START_SIZE, inputLength);
   }

   public void run() {

      initializeEpsilons();
      setCurrentOptimumPaths();
      Auxiliary.printLog(log, INFO, "DRL running...");
      float bestObjVal = (float) vars.objVal;
      for (int i = 0; i < pm.getServices().size() * pm.getDemandsTrafficFlow() * pm.getPathsTrafficFlow(); i++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               // get paths with enough path link resources
               List<Integer> availablePaths = heu.getAvailablePaths(s, d);
               for (Integer p : availablePaths) {
                  // before placing in a new path remove previous ones
                  heu.removeDemandFromAllFunctionsToServer(s, d);
                  // and place them on servers in the path
                  List<List<Integer>> availableServersPerFunction = heu.findServersForFunctionsInPath(s, d, p);
                  if (availableServersPerFunction == null)
                     continue;
                  List<Integer> chosenServers = heu.chooseServersForFunctionAllocation(s, d, p,
                        availableServersPerFunction);
                  heu.addDemandToFunctionsToSpecificServers(s, d, chosenServers);
                  // route traffic to path p
                  rerouteSpecificDemand(s, d, p);
                  // then, try to optimize the locations
                  for (int j = 0; j < 10; j++) {
                     availableServersPerFunction = heu.findServersForFunctionsInPath(s, d, p);
                     float currentBestObjVal = functionPlacement(s, d, p, availableServersPerFunction, bestObjVal);
                     if (currentBestObjVal < bestObjVal) {
                        bestObjVal = currentBestObjVal;
                        Auxiliary.printLog(log, INFO, "new incumbent solution found [" + bestObjVal + "]");
                        setCurrentOptimumPaths();
                        optimizePlacementUsingCurrentOptimumPaths(bestObjVal);
                     }
                  }
               }
            }
   }

   private void optimizePlacementUsingCurrentOptimumPaths(float bestObjVal) {
      for (int i = 0; i < pm.getServices().size() * pm.getDemandsTrafficFlow() * pm.getPathsTrafficFlow(); i++) {
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               String optPathKey = String.valueOf(s) + String.valueOf(d);
               // before placing in the best path remove previous ones
               heu.removeDemandFromAllFunctionsToServer(s, d);
               // and place them on servers in the best path
               List<List<Integer>> availableServersPerFunction = heu.findServersForFunctionsInPath(s, d,
                     optimumPaths.get(optPathKey));
               List<Integer> chosenServers = heu.chooseServersForFunctionAllocation(s, d, optimumPaths.get(optPathKey),
                     availableServersPerFunction);
               heu.addDemandToFunctionsToSpecificServers(s, d, chosenServers);
               rerouteSpecificDemand(s, d, optimumPaths.get(optPathKey));
               // then, try to optimize the locations
               availableServersPerFunction = heu.findServersForFunctionsInPath(s, d, optimumPaths.get(optPathKey));
               functionPlacement(s, d, optimumPaths.get(optPathKey), availableServersPerFunction, bestObjVal);
            }
      }
   }

   private float functionPlacement(int s, int d, int p, List<List<Integer>> availableServersPerFunction,
         float bestObjVal) {
      float localBestObjVal = bestObjVal;
      for (int j = 0; j < pm.getServices().get(s).getFunctions().size()
            * pm.getServices().get(s).getFunctions().size(); j++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            String epsilonKey = String.valueOf(s) + String.valueOf(d) + String.valueOf(p) + String.valueOf(v);
            float[] environment = createEnvironment(s, d, v, p);
            float[] nextEnvironment;
            List<Integer> availableServers = availableServersPerFunction.get(v);
            int possibleActions = availableServers.size() + 1;
            for (int i = 0; i < possibleActions * possibleActions; i++) {
               INDArray inputIndArray = Nd4j.create(environment);
               int[] actionMask = generateActionMask(environment, s, availableServers);
               int action = deepQ.getAction(inputIndArray, actionMask, epsilons.get(epsilonKey));
               nextEnvironment = modifyEnvironment(environment, action, s, d, v);
               heu.removeUnusedFunctions(s);
               heu.removeSyncTraffic(s);
               heu.addSyncTraffic(s);
               vars.generateRestOfVariablesForResults(initialPlacement, objFunc);
               float currentObjVal = (float) vars.getObjVal();
               float reward = computeReward(currentObjVal, localBestObjVal);
               int[] nextActionMask = generateActionMask(nextEnvironment, s, availableServers);
               deepQ.observeReward(Nd4j.create(environment), Nd4j.create(nextEnvironment), reward, nextActionMask);
               environment = nextEnvironment;
               log.info("[s][d][p][v] - [" + s + "][" + d + "][" + p + "][" + v + "] placement iteration " + i + ": ["
                     + vars.objVal + "][" + reward + "][" + action + "]");
               if (currentObjVal < localBestObjVal) { // best case => reduce randomness to 0 and exit
                  localBestObjVal = currentObjVal;
                  epsilons.put(epsilonKey, 0.0);
                  break;
               } else if (currentObjVal == localBestObjVal) { // same solution => reduce randomness or exit
                  if (epsilons.get(epsilonKey) > 0)
                     epsilons.put(epsilonKey,
                           Auxiliary.roundDouble(epsilons.get(epsilonKey) - (double) pm.getAux(EPSILON_STEPPER), 1));
                  else
                     break;
               } else {
                  if (epsilons.get(epsilonKey) < 1) // worse solution, increase randomness
                     epsilons.put(epsilonKey,
                           Auxiliary.roundDouble(epsilons.get(epsilonKey) + (double) pm.getAux(EPSILON_STEPPER), 1));
               }
            }
         }
      return localBestObjVal;
   }

   private float[] createEnvironment(int s, int d, int v, int p) {
      float[] environment = new float[inputLength];

      int srcNode = -1, dstNode = -1;
      for (int x = 0; x < pm.getServers().size(); x++) {
         environment[x] = vars.fXSVD[x][s][v][d] ? 1 : 0;
         if (pm.getServers().get(x).getParent().getId().equals(pm.getServices().get(s).getTrafficFlow().getSrc()))
            srcNode = x;
         if (pm.getServers().get(x).getParent().getId().equals(pm.getServices().get(s).getTrafficFlow().getDst()))
            dstNode = x;
      }

      environment[environment.length - 5] = srcNode;
      environment[environment.length - 4] = dstNode;
      environment[environment.length - 3] = pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
      environment[environment.length - 2] = p;
      environment[environment.length - 1] = v;
      return environment;
   }

   private int[] generateActionMask(float[] environment, int s, List<Integer> availableServers) {

      int[] actionMask = new int[outputLength];
      int index = 0;

      for (int x = 0; x < pm.getServers().size(); x++) {
         if (availableServers.contains(x))
            actionMask[index] = 1;
         index++;
      }

      actionMask[actionMask.length - 1] = 1; // last action to not perform any action
      return actionMask;
   }

   float[] modifyEnvironment(float[] environment, int action, int s, int d, int v) {

      float[] nextEnvironment = environment.clone();

      if (action == outputLength - 1)
         return nextEnvironment;

      int xOld = -1, xNew = -1;
      for (int x = 0; x < pm.getServers().size(); x++)
         if (x == action) {
            if (nextEnvironment[x] == 0)
               nextEnvironment[x] = 1;
            else
               xOld = x;
            xNew = x;
         } else {
            if (nextEnvironment[x] == 1) {
               nextEnvironment[x] = 0;
               xOld = x;
            }
         }

      // modify variables based on the taken action
      if (xOld != -1)
         heu.removeDemandToFunctionToServer(s, xOld, v, d);
      heu.addDemandToFunctionToServer(s, xNew, v, d);
      return nextEnvironment;
   }

   private float computeReward(float currentObjVal, float bestObjVal) {
      if (currentObjVal < bestObjVal)
         return 10;
      else if (currentObjVal == bestObjVal)
         return 0;
      else
         return -10;
   }

   public void rerouteSpecificDemand(int s, int d, int pBest) {
      int pOld = -1;
      for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
         if (vars.zSPD[s][p][d])
            pOld = p;
      heu.removeDemandFromPath(s, pOld, d); // remove demand from path
      heu.addDemandToPath(s, pBest, d); // add demand to path
   }

   private void setCurrentOptimumPaths() {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               if (vars.zSPD[s][p][d])
                  optimumPaths.put(String.valueOf(s) + String.valueOf(d), p);
   }

   private void initializeEpsilons() {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  epsilons.put(String.valueOf(s) + String.valueOf(d) + String.valueOf(p) + String.valueOf(v), 1.0);
   }

   public MultiLayerConfiguration getConf() {
      return conf;
   }
}
