package optimizer.algorithms.learning;

import manager.Parameters;
import optimizer.algorithms.Heuristic;
import optimizer.algorithms.VariablesAlg;
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

import java.util.ArrayList;
import java.util.List;

import static optimizer.Definitions.*;

public class RoutingModel {
   protected Parameters pm;
   protected VariablesAlg vars;
   protected boolean[][][] initialPlacement;
   protected String objFunc;
   private DeepQ deepQ;
   private MultiLayerConfiguration conf;
   private final int offsetInput;
   protected int environmentSize;
   private float bestObjVal;
   private Heuristic heu;
   private PlacementModel placementModel;

   private static final Logger log = LoggerFactory.getLogger(RoutingModel.class);


   public RoutingModel(MultiLayerConfiguration conf, Parameters pm, VariablesAlg variablesAlg, boolean[][][] initialPlacement, String objFunc, Heuristic heu, PlacementModel placementModel) {
      this.pm = pm;
      this.vars = variablesAlg;
      this.initialPlacement = initialPlacement;
      this.objFunc = objFunc;
      this.heu = heu;
      this.placementModel = placementModel;
      environmentSize = calculateEnvironmentLength();
      offsetInput = 2;
      int inputLength = environmentSize + offsetInput;
      int outputLength = environmentSize;
      if (conf == null) initializeModel(inputLength, outputLength);
      else initializeModel(conf, inputLength);
   }

   private void initializeModel(int inputLength, int outputLength) {
      conf = new NeuralNetConfiguration.Builder()
              .seed(123)
              .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
              .list()
              .layer(0, new DenseLayer.Builder()
                      .nIn(inputLength)
                      .nOut(NUM_HIDDEN_LAYERS)
                      .weightInit(WeightInit.XAVIER)
                      .activation(Activation.RELU)
                      .build())
              .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                      .nIn(NUM_HIDDEN_LAYERS)
                      .nOut(outputLength)
                      .weightInit(WeightInit.XAVIER)
                      .activation(Activation.IDENTITY)
                      .build())
              .build();
      deepQ = new DeepQ(conf, MEMORY_CAPACITY, DISCOUNT_FACTOR, BATCH_SIZE, FREQUENCY, START_SIZE, inputLength);
   }

   private void initializeModel(MultiLayerConfiguration conf, int inputLength) {
      this.conf = conf;
      deepQ = new DeepQ(conf, MEMORY_CAPACITY, DISCOUNT_FACTOR, BATCH_SIZE, FREQUENCY, START_SIZE, inputLength);
   }

   public void run() {

      bestObjVal = (float) vars.getObjVal();
      float[] environment = createEnvironment();
      float[] nextEnvironment;

      log.info("initial solution: [" + bestObjVal + "]");

      for (int i = 0; i < (int) pm.getAux(ROUTING_ITERATIONS); i++) {

         // switch one traffic demand to a different path
         INDArray inputIndArray = Nd4j.create(environment);
         int[] actionMask = generateActionMask(environment);

         int action = deepQ.getAction(inputIndArray, actionMask);

         // generate next environment of on the new chosen path
         nextEnvironment = modifyEnvironment(environment, action, i);

         // calculate new objective value
         vars.generateRestOfVariablesForResults(initialPlacement, objFunc);

         // update new objective value to the next environment
         nextEnvironment[nextEnvironment.length - 2] = (float) vars.objVal;

         // calculate the reward and create a new experience
         float reward = computeReward();
         int[] nextActionMask = generateActionMask(nextEnvironment);
         deepQ.observeReward(Nd4j.create(environment), Nd4j.create(nextEnvironment), reward, nextActionMask);

         environment = nextEnvironment;

         if (vars.objVal < bestObjVal)
            bestObjVal = (float) vars.objVal;

         log.info("routing iteration " + i + ": [" + vars.objVal + "][" + reward + "]");
      }
   }

   private int calculateEnvironmentLength() {
      int inputSize = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            inputSize += pm.getServices().get(s).getTrafficFlow().getPaths().size();
      return inputSize;
   }

   private float[] createEnvironment() {
      float[] environment = new float[environmentSize + offsetInput];
      List<Float> environmentList = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               float value = vars.zSPD[s][p][d] ? 1 : 0;
               environmentList.add(value);
            }

      for (int i = 0; i < environmentList.size(); i++) environment[i] = environmentList.get(i);

      environment[environment.length - 2] = (float) vars.objVal;
      environment[environment.length - 1] = 0f;
      return environment;
   }

   private int[] generateActionMask(float[] environment) {
      int[] actionMask = new int[environment.length - offsetInput];

      int index = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               if (checkPathForRerouting(s, d, p) && environment[index] == 0)
                  actionMask[index] = 1;
               index++;
            }
      boolean allFalse = true;
      for (int i = 0; i < actionMask.length; i++) {
         if (actionMask[i] == 1)
            allFalse = false;
      }

      if (allFalse)
         log.error("");
      return actionMask;
   }

   float[] modifyEnvironment(float[] environment, int action, int timeStep) {
      float[] nextEnvironment = environment.clone();

      int initialServiceDemandIndex = 0, index = 0, sChosen = 0, dChosen = 0;
      outerLoop:
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               if (action == index) {
                  sChosen = s;
                  dChosen = d;
                  break outerLoop;
               }
               index++;
            }
            initialServiceDemandIndex += pm.getServices().get(s).getTrafficFlow().getPaths().size();
         }

      int pOld = 0, pNew = 0;
      for (int p = 0; p < pm.getPathsTrafficFlow(); p++) {
         if (initialServiceDemandIndex + p != action) {
            if (nextEnvironment[initialServiceDemandIndex + p] == 1) {
               nextEnvironment[initialServiceDemandIndex + p] = 0;
               pOld = p;
            }
         } else {
            nextEnvironment[initialServiceDemandIndex + p] = 1;
            pNew = p;
         }
      }

      nextEnvironment[nextEnvironment.length - 1] = timeStep;

      // modify variables based on the taken action
      rerouteSpecificDemand(sChosen, dChosen, pOld, pNew, timeStep);

      return nextEnvironment;
   }

   private float computeReward() {
      float newObjVal = (float) vars.getObjVal();
      if (newObjVal < bestObjVal)
         return 100;
      else if (newObjVal == bestObjVal)
         return 0;
      else
         return -100;
   }

   public boolean checkPathForRerouting(int s, int d, int p) {
      List<List<Integer>> availableServersPerFunction = heu.findServersForFunctionsInPath(s, d, p);
      return availableServersPerFunction != null;
   }

   public void rerouteSpecificDemand(int s, int d, int pOld, int pNew, int timeStep) {

      boolean successfulPlacement = placementModel.run(s, d, pNew, timeStep); // run drl for function placement
      if (successfulPlacement) {
         heu.removeDemandFromPath(s, pOld, d); // remove demand from path
         heu.addDemandToPath(s, pNew, d); // add demand to path
         heu.removeUnusedFunctions(s);
         heu.removeSyncTraffic(s);
         heu.addSyncTraffic(s);
      }
   }

   MultiLayerConfiguration getConf() {
      return conf;
   }
}
