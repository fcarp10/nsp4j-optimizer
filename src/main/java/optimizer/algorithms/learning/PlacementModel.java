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

public class PlacementModel {
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

   private static final Logger log = LoggerFactory.getLogger(PlacementModel.class);


   public PlacementModel(MultiLayerConfiguration conf, Parameters pm, VariablesAlg variablesAlg, boolean[][][] initialPlacement, String objFunc, Heuristic heu) {
      this.pm = pm;
      this.vars = variablesAlg;
      this.initialPlacement = initialPlacement;
      this.objFunc = objFunc;
      this.heu = heu;
      environmentSize = pm.getServiceLength() * pm.getServers().size();
      offsetInput = 5;
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

   public boolean run(int s, int d, int p, int timeStep) {

      bestObjVal = (float) vars.getObjVal();
      float[] environment = createEnvironment(s, d, p);
      float[] nextEnvironment;

      List<List<Integer>> availableServersPerFunction = heu.findServersForFunctionsInPath(s, d, p);
      if (availableServersPerFunction == null) {
         log.info("no available servers for function placement");
         return false;
      } else {
         for (int i = 0; i < (int) pm.getAux(PLACEMENT_ITERATIONS); i++) {
            // switch one traffic demand to a different path
            INDArray inputIndArray = Nd4j.create(environment);
            int[] actionMask = generateActionMask(environment, s, availableServersPerFunction);
            int action = deepQ.getAction(inputIndArray, actionMask);

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
            if (vars.objVal < bestObjVal)
               bestObjVal = (float) vars.objVal;
            log.info("placement iteration " + timeStep + "." + i + ": [" + vars.objVal + "][" + reward + "]");
         }
         return true;
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

      for (int i = 0; i < environmentList.size(); i++) environment[i] = environmentList.get(i);

      environment[environment.length - 5] = s;
      environment[environment.length - 4] = d;
      environment[environment.length - 3] = p;
      environment[environment.length - 2] = (float) vars.objVal;
      environment[environment.length - 1] = 0f;
      return environment;
   }


   private int[] generateActionMask(float[] environment, int s, List<List<Integer>> availableServersPerFunction) {

      int[] actionMask = new int[environment.length - offsetInput];
      int index = 0;

      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
         for (int x = 0; x < pm.getServers().size(); x++) {
            if (availableServersPerFunction.get(v).contains(x))
               actionMask[index] = 1;
            index++;
         }

      return actionMask;
   }

   float[] modifyEnvironment(float[] environment, int action, int timeStep, int s, int d) {

      float[] nextEnvironment = environment.clone();
      int initialFunctionServerIndex = 0, index = 0, vChosen = 0;

      outerLoop:
      for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
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

      nextEnvironment[nextEnvironment.length - 1] = timeStep;

      // modify variables based on the taken action
      heu.removeDemandToFunctionToServer(s, xOld, vChosen, d);
      heu.addDemandToFunctionToServer(s, xNew, vChosen, d);

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

   MultiLayerConfiguration getConf() {
      return conf;
   }
}
