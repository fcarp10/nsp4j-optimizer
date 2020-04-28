package optimizer.algorithms.learning;

import static optimizer.Definitions.*;

import java.util.ArrayList;
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

public class PlacementModel {
   protected Parameters pm;
   protected VariablesAlg vars;
   protected boolean[][][] initialPlacement;
   protected String objFunc;
   private DeepQ deepQ;
   private MultiLayerConfiguration conf;
   private final int offsetInput;
   protected int environmentSize;
   private float bestGlobalObjVal;
   private Map<String, Double> epsilons;
   private Heuristic heu;

   private static final Logger log = LoggerFactory.getLogger(PlacementModel.class);

   public PlacementModel(String conf, Parameters pm, VariablesAlg variablesAlg, boolean[][][] initialPlacement,
         String objFunc, Heuristic heu) {
      this.pm = pm;
      this.vars = variablesAlg;
      this.initialPlacement = initialPlacement;
      this.objFunc = objFunc;
      this.heu = heu;
      epsilons = new HashMap<>();
      environmentSize = pm.getServiceLength() * pm.getServers().size();
      offsetInput = 5;
      int inputLength = environmentSize + offsetInput;
      int outputLength = environmentSize;
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
      deepQ = new DeepQ(conf, MEMORY_CAPACITY, DISCOUNT_FACTOR_PLACEMENT, BATCH_SIZE, FREQUENCY, START_SIZE,
            inputLength);
   }

   private void initializeModel(String confString, int inputLength) {
      MultiLayerConfiguration conf = MultiLayerConfiguration.fromJson(confString);
      this.conf = conf;
      deepQ = new DeepQ(conf, MEMORY_CAPACITY, DISCOUNT_FACTOR_PLACEMENT, BATCH_SIZE, FREQUENCY, START_SIZE,
            inputLength);
   }

   public boolean run(int s, int d, int p, float bestGlobalObjVal) {

      this.bestGlobalObjVal = bestGlobalObjVal;
      float[] environment = createEnvironment(s, d, p);
      float[] nextEnvironment;
      String epsilonKey = String.valueOf(s) + String.valueOf(d) + String.valueOf(p);
      if (!epsilons.containsKey(epsilonKey))
         epsilons.put(epsilonKey, 1.0);

      List<List<Integer>> availableServersPerFunction = heu.findServersForFunctionsInPath(s, d, p);
      if (availableServersPerFunction != null) {
         int i = 0;
         int repetitionsWithSameValue = 0;

         while (true) {
            INDArray inputIndArray = Nd4j.create(environment);
            int[] actionMask = generateActionMask(environment, s, availableServersPerFunction);
            int action = deepQ.getAction(inputIndArray, actionMask, epsilons.get(epsilonKey));
            // generate next environment of on the new chosen path
            nextEnvironment = modifyEnvironment(environment, action, i, s, d);
            // calculate new objective value
            vars.generateRestOfVariablesForResults(initialPlacement, objFunc);
            // update new objective value to the next environment
            nextEnvironment[nextEnvironment.length - 2] = (float) vars.objVal;
            // calculate the reward and create a new experience
            float reward = computeReward();
            int[] nextActionMask = generateActionMask(nextEnvironment, s, availableServersPerFunction);
            deepQ.observeReward(Nd4j.create(environment), Nd4j.create(nextEnvironment), reward, nextActionMask);
            environment = nextEnvironment;
            i++;
            log.info("placement iteration " + i + ": [" + vars.objVal + "][" + reward + "][" + action + "]");
            if (epsilons.get(epsilonKey) > 0) {
               if ((float) vars.getObjVal() < bestGlobalObjVal) {
                  epsilons.put(epsilonKey, Auxiliary.roundDouble(epsilons.get(epsilonKey) - (double) pm.getAux(PLACEMENT_EPSILON_DECREMENT), 1));
               } else if ((float) vars.getObjVal() >= bestGlobalObjVal) {
                  repetitionsWithSameValue++;
                  if (repetitionsWithSameValue == (int) pm.getAux(PLACEMENT_MAX_REPETITIONS))
                     break;
               }
            } else
               break;
         }
         log.info("placement finished for [s][d][p]: [" + s + "][" + d + "][" + p + "]");
         return true;
      } else {
         log.info("no available servers for function placement");
         return false;
      }
   }

   private float[] createEnvironment(int s, int d, int p) {
      float[] environment = new float[environmentSize + offsetInput];
      List<Float> environmentList = new ArrayList<>();

      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
         for (int x = 0; x < pm.getServers().size(); x++) {
            float value = vars.fXSVD[x][s][v][d] ? 1 : 0;
            environmentList.add(value);
         }

      for (int i = 0; i < environmentList.size(); i++)
         environment[i] = environmentList.get(i);

      environment[environment.length - 5] = s;
      environment[environment.length - 4] = d;
      environment[environment.length - 3] = p;
      environment[environment.length - 2] = (float) vars.objVal;
      environment[environment.length - 1] = 0;
      return environment;
   }

   private int[] generateActionMask(float[] environment, int s, List<List<Integer>> availableServersPerFunction) {

      int[] actionMask = new int[environment.length + 1 - offsetInput];
      int index = 0;

      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
         for (int x = 0; x < pm.getServers().size(); x++) {
            if (availableServersPerFunction.get(v).contains(x))
               actionMask[index] = 1;
            index++;
         }

      actionMask[actionMask.length - 1] = 1; // last action to not perform any action
      return actionMask;
   }

   float[] modifyEnvironment(float[] environment, int action, int timeStep, int s, int d) {

      float[] nextEnvironment = environment.clone();
      int initialFunctionServerIndex = 0, index = 0, vChosen = 0;
      int notActionIndex = (environment.length - 1) + 1 - offsetInput;

      nextEnvironment[nextEnvironment.length - 1] = timeStep;

      if (action == notActionIndex)
         return nextEnvironment;

      outerLoop: for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
         for (int x = 0; x < pm.getServers().size(); x++) {
            if (action == index) {
               vChosen = v;
               break outerLoop;
            }
            index++;
         }
         initialFunctionServerIndex += pm.getServers().size();
      }

      int xOld = -1, xNew = -1;
      for (int x = 0; x < pm.getServers().size(); x++)
         if (initialFunctionServerIndex + x == action) {
            if (nextEnvironment[initialFunctionServerIndex + x] == 0)
               nextEnvironment[initialFunctionServerIndex + x] = 1;
            else
               xOld = x;
            xNew = x;
         } else {
            if (nextEnvironment[initialFunctionServerIndex + x] == 1) {
               nextEnvironment[initialFunctionServerIndex + x] = 0;
               xOld = x;
            }
         }

      // modify variables based on the taken action
      heu.removeDemandToFunctionToServer(s, xOld, vChosen, d);
      heu.addDemandToFunctionToServer(s, xNew, vChosen, d);

      return nextEnvironment;
   }

   private float computeReward() {
      float newObjVal = (float) vars.getObjVal();
      if (newObjVal < bestGlobalObjVal)
         return 100;
      else if (newObjVal == bestGlobalObjVal)
         return 0;
      else
         return -1;
   }

   public MultiLayerConfiguration getConf() {
      return conf;
   }
}
