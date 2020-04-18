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
import java.util.Random;

import static optimizer.Definitions.*;
import static optimizer.results.Auxiliary.printLog;

public class ModelLearning {
   protected Parameters pm;
   protected VariablesAlg vars;
   protected Random rnd;
   protected boolean[][][] initialPlacement;
   protected String objFunc;
   protected int environmentSize;
   private DeepQ deepQ;
   private MultiLayerConfiguration conf;
   private final int offsetInput;
   private float bestObjVal;
   private Heuristic heuristic;

   private static final Logger log = LoggerFactory.getLogger(ModelLearning.class);


   public ModelLearning(MultiLayerConfiguration conf, Parameters pm, VariablesAlg variablesAlg, boolean[][][] initialPlacement, String objFunc, Heuristic heuristic) {
      this.pm = pm;
      this.vars = variablesAlg;
      this.initialPlacement = initialPlacement;
      this.objFunc = objFunc;
      rnd = new Random();
      this.heuristic = heuristic;
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

      for (int i = 0; i < ITERATIONS; i++) {

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
         float reward = observeReward(environment, nextEnvironment);

         environment = nextEnvironment;

         if (vars.objVal < bestObjVal)
            bestObjVal = (float) vars.objVal;

         printLog(log, INFO, "iteration " + i + ": [" + vars.objVal + "][" + reward + "]");

      }
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

      environment[environmentSize - 2] = (float) vars.objVal;
      environment[environmentSize - 1] = 0f;
      return environment;
   }

   private int calculateEnvironmentLength() {
      int inputSize = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            inputSize += pm.getServices().get(s).getTrafficFlow().getPaths().size();
      return inputSize;
   }

   private int[] generateActionMask(float[] environment) {
      int[] actionMask = new int[environment.length - offsetInput];

      int index = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               if (heuristic.checkPathForReallocation(s, d, p) && environment[index] == 0)
                  actionMask[index] = 1;
               index++;
            }

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
      heuristic.reallocateSpecificDemand(sChosen, dChosen, pOld, pNew);

      return nextEnvironment;
   }

   private float observeReward(float[] environment, float[] nextEnvironment) {
      float reward = computeReward();
      int[] nextActionMask = generateActionMask(nextEnvironment);
      if (nextEnvironment[nextEnvironment.length - 1] == 0)
         deepQ.observeReward(Nd4j.create(environment), null, reward, nextActionMask);
      else
         deepQ.observeReward(Nd4j.create(environment), Nd4j.create(nextEnvironment), reward, nextActionMask);
      return reward;
   }

   private float computeReward() {
      if (vars.getObjVal() < bestObjVal)
         return 100;
      else if (vars.getObjVal() == bestObjVal)
         return 0;
      else
         return -100;
   }

   MultiLayerConfiguration getConf() {
      return conf;
   }
}
