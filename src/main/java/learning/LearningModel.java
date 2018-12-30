package learning;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.Function;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import output.Auxiliary;
import output.Definitions;

import java.util.*;

import static output.Auxiliary.printLog;
import static output.Definitions.INFO;

public class LearningModel {

   private static final Logger log = LoggerFactory.getLogger(LearningModel.class);
   private Parameters pm;
   private DeepQ deepQ;
   private double objVal;

   // Elementary variables
   private boolean[][] rSP;
   private boolean[][][] rSPD;
   private boolean[][][] pXSV;
   private boolean[][][][] pXSVD;
   private double[] uL;
   private double[] uX;
   private double[] dS;

   public LearningModel(Parameters pm) {
      this.pm = pm;
   }

   private void initializeModel(int inputLength, int outputLength) {
      MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
              .seed(123)
              .iterations(1)
              .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
              .learningRate(0.1)
              .updater(Updater.NESTEROVS)
              .list()
              .layer(0, new DenseLayer.Builder()
                      .nIn(inputLength)
                      .nOut((int) pm.getAux("rl_num_hidden_layers"))
                      .weightInit(WeightInit.XAVIER)
                      .activation(Activation.RELU)
                      .build())
              .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                      .nIn((int) pm.getAux("rl_num_hidden_layers"))
                      .nOut(outputLength)
                      .weightInit(WeightInit.XAVIER)
                      .activation(Activation.IDENTITY)
                      .build())
              .pretrain(false)
              .backprop(true)
              .build();
      deepQ = new DeepQ(conf, 100000, .99f, 1024, 100, 1024, inputLength);
   }

   public void run(GRBModel initialPlacement, double objValTarget) throws GRBException {
      float[] input = generateInput(initialPlacement);
      int[] environment = generateEnvironment(initialPlacement);
      initializeModel(input.length, environment.length);
      for (int i = 0; i < (int) pm.getAux("rl_training_iterations"); i++)
         learn(input, environment, objValTarget, i, (double) pm.getAux("epsilon"));
      this.objVal = reason(input, environment, objValTarget, 0);
      printLog(log, INFO, "finished [" + Auxiliary.roundDouble(objVal, 2) + "]");
   }

   private float[] generateInput(GRBModel initialPlacement) throws GRBException {
      List<float[]> inputList = new ArrayList<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               if (initialPlacement.getVarByName(Definitions.rSPD + "[" + s + "][" + p + "][" + d + "]").get(GRB.DoubleAttr.X) == 1.0) {
                  float[] individualInput = new float[2 + pm.getServiceLength()];
                  individualInput[0] = pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
                  individualInput[1] = p;
                  for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (initialPlacement.getVarByName(Definitions.pXSVD + "[" + x + "][" + s + "][" + v + "][" + d + "]").get(GRB.DoubleAttr.X) == 1.0)
                           individualInput[2 + v] = x;
                  inputList.add(individualInput);
               }
            }
      float[] inputArray = new float[inputList.size() * (2 + pm.getServiceLength()) + 1];
      for (int i = 0; i < inputList.size(); i++)
         if (inputList.get(i).length >= 0)
            System.arraycopy(inputList.get(i), 0, inputArray, i * 4, inputList.get(i).length);
      return inputArray;
   }

   private int[] generateEnvironment(GRBModel initialPlacement) throws GRBException {
      int[] environment = new int[pm.getServers().size() * pm.getTotalNumFunctions()];
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               int pointer = x * pm.getTotalNumFunctions() + s * pm.getServices().get(s).getFunctions().size() + v;
               if (initialPlacement.getVarByName(Definitions.pXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 1.0)
                  environment[pointer] = 1;
               else
                  environment[pointer] = 0;
            }
      return environment;
   }

   private void learn(float[] input, int[] environment, double objValTarget, int iteration, double epsilon) {
      int[] localEnvironment = environment.clone();
      int timeStep = 0;
      int action = -1;
      while (true) {
         INDArray inputIndArray = Nd4j.create(input);
         int[] actionMask = generateActionMask(localEnvironment, action);
         action = deepQ.getAction(inputIndArray, actionMask, epsilon);
         modifyEnvironment(false, localEnvironment, action);
         int[] nextActionMask = generateActionMask(localEnvironment, action);
         double reward = computeReward();
         timeStep++;
         input[input.length - 1] = timeStep;
         if (reward >= (1 - objValTarget) * (double) pm.getAux("threshold")) {
            deepQ.observeReward(inputIndArray, null, reward, nextActionMask);
            break;
         } else {
            deepQ.observeReward(inputIndArray, Nd4j.create(input), reward, nextActionMask);
            if (timeStep == (int) pm.getAux("rl_learning_steps"))
               break;
         }
      }
      log.info("iteration " + iteration + " -> " + timeStep + " steps");
   }

   private double reason(float[] input, int[] environment, double objValTarget, double epsilon) {
      int[] localEnvironment = environment.clone();
      int timeStep = 0;
      double reward;
      int action = -1;
      while (true) {
         INDArray inputIndArray = Nd4j.create(input);
         int[] actionMask = generateActionMask(localEnvironment, action);
         action = deepQ.getAction(inputIndArray, actionMask, epsilon);
         modifyEnvironment(true, localEnvironment, action);
         int[] nextActionMask = generateActionMask(localEnvironment, action);
         reward = computeReward();
         timeStep++;
         input[input.length - 1] = timeStep;
         if (reward >= (1 - objValTarget) * (double) pm.getAux("threshold"))
            break;
         else {
            deepQ.observeReward(inputIndArray, Nd4j.create(input), reward, nextActionMask);
            if (timeStep == (int) pm.getAux("rl_learning_steps"))
               break;
         }
      }
      computeFunctionsServers();
      log.info("reasoning in -> " + timeStep + " steps");
      return 1 - reward;
   }

   private int[] generateActionMask(int[] environment, int pastAction) {
      int[] actionMask = new int[environment.length];
      for (int v = 0; v < pm.getServiceLength(); v++) {
         int activations = 0;
         for (int x = 0; x < pm.getServers().size(); x++)
            if (environment[x * pm.getServiceLength() + v] == 1)
               activations++;
         for (int x = 0; x < pm.getServers().size(); x++) {
            if (activations == pm.getServices().size())
               if (environment[x * pm.getServiceLength() + v] == 0)
                  actionMask[x * pm.getServiceLength() + v] = 1;
            if (activations > pm.getServices().size())
               actionMask[x * pm.getServiceLength() + v] = 1;
         }
      }
      if (pastAction != -1)
         actionMask[pastAction] = 0;
      return actionMask;
   }

   private void modifyEnvironment(boolean isReasoning, int[] environment, int action) {
      if (environment[action] == 1)
         environment[action] = 0;
      else environment[action] = 1;
      Map<Integer, List<Path>> servicesAdmissiblePaths = getServicesAdmissiblePaths(environment);
      chooseServersPerDemand(servicesAdmissiblePaths, environment);
      calculateServerUtilization(environment);
      calculateLinkUtilization(environment);
      if (isReasoning) {
         computePaths();
         calculateReroutingTraffic();
         computeServiceDelay();
      }
   }

   private Map<Integer, List<Path>> getServicesAdmissiblePaths(int[] environment) {
      Map<Integer, List<Path>> servicesAdmissiblePaths = new HashMap<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         servicesAdmissiblePaths.put(s, computeAdmissiblePaths(s, environment));
      return servicesAdmissiblePaths;
   }

   private List<Path> computeAdmissiblePaths(int s, int[] environment) {
      List<Path> admissiblePaths = new ArrayList<>();
      for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
            boolean allFunctionsExist = true;
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               boolean activatedInPath = false;
               outerLoop:
               for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().size(); n++) {
                  Node node = pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(n);
                  for (int x = 0; x < pm.getServers().size(); x++) {
                     if (pm.getServers().get(x).getParent().equals(node))
                        if (environment[x * pm.getServices().get(s).getFunctions().size() + v] == 1) {
                           activatedInPath = true;
                           break outerLoop;
                        }
                  }
               }
               if (!activatedInPath) {
                  allFunctionsExist = false;
                  break;
               }
            }
            if (allFunctionsExist)
               admissiblePaths.add(pm.getPaths().get(p));
         }
      return admissiblePaths;
   }

   private void chooseServersPerDemand(Map<Integer, List<Path>> tSP, int[] environment) {
      Random rnd = new Random();
      pXSVD = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()][pm.getDemandsTrafficFlow()];
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
            Path path = tSP.get(s).get(rnd.nextInt(tSP.get(s).size()));
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               outerLoop:
               for (int n = 0; n < path.getNodePath().size(); n++)
                  for (int x = 0; x < pm.getServers().size(); x++)
                     if (pm.getServers().get(x).getParent().equals(path.getNodePath().get(n)))
                        if (environment[x * pm.getServices().get(s).getFunctions().size() + v] == 1) {
                           pXSVD[x][s][v][d] = true;
                           break outerLoop;
                        }
            }
         }
   }

   private void calculateServerUtilization(int[] environment) {
      uX = new double[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++) {
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               double demands = 0;
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                  if (pXSVD[x][s][v][d]) {
                     demands += pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
                  }
               }
               if (environment[x * pm.getServices().get(s).getFunctions().size() + v] == 1) {
                  Function function = pm.getServices().get(s).getFunctions().get(v);
                  uX[x] += ((demands * (double) function.getAttribute("load"))
                          + ((double) function.getAttribute("load")
                          * (int) function.getAttribute("overhead")))
                          / pm.getServers().get(x).getCapacity();
               }

            }
      }
   }

   private void calculateLinkUtilization(int[] environment) {
      uL = new double[pm.getLinks().size()];
      // TODO
   }

   private double computeReward() {
      double cost, totalCost = 0;
      for (Double serverUtilization : uX) {
         cost = 0;
         for (int f = 0; f < Auxiliary.costFunctions.getValues().size(); f++) {
            double value = Auxiliary.costFunctions.getValues().get(f)[0] * serverUtilization
                    + Auxiliary.costFunctions.getValues().get(f)[1];
            if (value > cost)
               cost = value;
         }
         totalCost += cost;
      }
      return 1 - (totalCost / pm.getServers().size());
   }

   private void computeFunctionsServers() {
      pXSV = new boolean[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (pXSVD[x][s][v][d])
                     pXSV[x][s][v] = true;
   }

   private void computePaths() {
      rSP = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()];
      rSPD = new boolean[pm.getServices().size()][pm.getPathsTrafficFlow()][pm.getDemandsTrafficFlow()];
      // TODO
   }

   private void computeServiceDelay() {
      dS = new double[pm.getServices().size()];
      // TODO
   }

   private void calculateReroutingTraffic() {
      // TODO
   }

   public double[] getuX() {
      return uX;
   }

   public double[] getuL() {
      return uL;
   }

   public boolean[][][][] getpXSVD() {
      return pXSVD;
   }

   public boolean[][][] getpXSV() {
      return pXSV;
   }

   public boolean[][] getrSP() {
      return rSP;
   }

   public boolean[][][] getrSPD() {
      return rSPD;
   }

   public double[] getdS() {
      return dS;
   }

   public double getObjVal() {
      return objVal;
   }
}
