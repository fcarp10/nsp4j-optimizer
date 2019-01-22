package learning;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.Function;
import manager.elements.Service;
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
      float[] environment = generateEnvironment(initialPlacement);
      initializeModel(environment.length, environment.length - 1);
      for (int i = 0; i < (int) pm.getAux("rl_training_iterations"); i++)
         run(environment, objValTarget, i, (double) pm.getAux("epsilon"), false);
      run(environment, objValTarget, -1, 0, true);
      this.objVal = computeCost();
      printLog(log, INFO, "finished [" + Auxiliary.roundDouble(objVal, 2) + "]");
   }

   private float[] generateEnvironment(GRBModel initialPlacement) throws GRBException {
      float[] environment = new float[pm.getServers().size() * pm.getTotalNumFunctions() + 1];
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

   private void run(float[] environment, double objValTarget, int iteration, double epsilon, boolean isReasoning) {
      float[] localEnvironment = environment.clone();
      int timeStep = 0;
      int action = -1;
      while (true) {
         INDArray inputIndArray = Nd4j.create(localEnvironment);
         int[] actionMask = generateActionMask(localEnvironment, action);
         action = deepQ.getAction(inputIndArray, actionMask, epsilon);
         modifyEnvironment(localEnvironment, action);
         int[] nextActionMask = generateActionMask(localEnvironment, action);
         computeEnvironment(isReasoning, localEnvironment);
         double cost = computeCost();
         double reward = computeReward(cost, objValTarget);
         timeStep++;
         localEnvironment[localEnvironment.length - 1] = timeStep;
         if (cost >= objValTarget * (double) pm.getAux("threshold")) {
            deepQ.observeReward(inputIndArray, null, reward, nextActionMask);
            break;
         } else {
            deepQ.observeReward(inputIndArray, Nd4j.create(localEnvironment), reward, nextActionMask);
            if (timeStep == (int) pm.getAux("rl_learning_steps"))
               break;
         }
      }
      if (iteration > -1)
         log.info("iteration " + iteration + " -> " + timeStep + " steps");
      else {
         computeFunctionsServers();
         log.info("reasoning in -> " + timeStep + " steps");
      }
   }

   private int[] generateActionMask(float[] environment, int pastAction) {
      int[] actionMask = new int[environment.length - 1];
      // to avoid doing the same action twice
      if (pastAction != -1)
         actionMask[pastAction] = 0;
      // calculate the admissible paths for all services
      Map<Integer, List<Path>> servicesAdmissiblePaths = getServicesAdmissiblePaths(environment);
      // check which servers can be used based on the admissible paths
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++) {
            for (int v = 0; v < pm.getServiceLength(); v++) {
               outerloop:
               if (environment[x * pm.getServiceLength() + v] == 0)
                  // check if it is possible to activate the server
                  for (int p = 0; p < servicesAdmissiblePaths.get(s).size(); p++)
                     for (int n = 0; n < servicesAdmissiblePaths.get(s).get(p).getNodePath().size(); n++)
                        if (pm.getServers().get(x).getParent().equals(servicesAdmissiblePaths.get(s).get(p).getNodePath().get(n))) {
                           actionMask[x * pm.getServiceLength() + v] = 1;
                           break outerloop;
                        }
            }
         }
      // find the maximum demand for the function with highest load
      double maxDemandLoad = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServiceLength(); v++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               double demandLoad = pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                       * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load");
               if (demandLoad > maxDemandLoad)
                  maxDemandLoad = demandLoad;
            }
      // check which servers can be used based on server capacity
      for (int x = 0; x < pm.getServers().size(); x++) {
         double totalTraffic = 0;
         for (int s = 0; s < pm.getServices().size(); s++) {
            int traffic = 0;
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               traffic += pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
            for (int v = 0; v < pm.getServiceLength(); v++) {
               if (environment[x * pm.getServiceLength() + v] == 1)
                  totalTraffic += (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load") * traffic;
            }
         }
         if (totalTraffic + maxDemandLoad >= pm.getServers().get(x).getCapacity())
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServiceLength(); v++)
                  actionMask[x * pm.getServiceLength() + v] = 0;

      }
      return actionMask;
   }

   private void modifyEnvironment(float[] environment, int action) {
      outerloop:
      for (int s = 0; s < pm.getServices().size(); s++) {
         Service service = pm.getServices().get(s);
         for (int v = 0; v < pm.getServiceLength(); v++)
            for (int x = 0; x < pm.getServers().size(); x++) {
               if (action != x * pm.getServiceLength() + v) continue;
               if (service.getFunctions().size() <= v) continue;
               if (!(boolean) service.getFunctions().get(v).getAttribute("replicable"))
                  for (int y = 0; y < pm.getServers().size(); y++)
                     environment[y * pm.getServiceLength() + v] = 0;
               if (environment[action] == 1)
                  environment[action] = 0;
               else environment[action] = 1;
               break outerloop;
            }
      }
   }

   private void computeEnvironment(boolean isReasoning, float[] environment) {
      Map<Integer, List<Path>> servicesAdmissiblePaths = getServicesAdmissiblePaths(environment);
      activateServersPerDemand(servicesAdmissiblePaths, environment);
      calculateServerUtilization(environment);
      calculateLinkUtilization(environment);
      if (isReasoning) {
         computePaths();
         computeServiceDelay();
      }
   }

   private Map<Integer, List<Path>> getServicesAdmissiblePaths(float[] environment) {
      Map<Integer, List<Path>> servicesAdmissiblePaths = new HashMap<>();
      for (int s = 0; s < pm.getServices().size(); s++)
         servicesAdmissiblePaths.put(s, computeAdmissiblePaths(s, environment));
      return servicesAdmissiblePaths;
   }

   private List<Path> computeAdmissiblePaths(int s, float[] environment) {
      List<Path> admissiblePaths = new ArrayList<>();
      for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
            boolean allFunctionsExist = true;
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               boolean activatedInPath = false;
               outerLoop:
               for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().size(); n++) {
                  Node node = pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(n);
                  for (int x = 0; x < pm.getServers().size(); x++)
                     if (pm.getServers().get(x).getParent().equals(node)) {
                        int pointer = pm.getServices().size() * pm.getServices().get(s).getFunctions().size()
                                * x + s * pm.getServices().get(s).getFunctions().size() + v;
                        if (environment[pointer] == 1) {
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
               admissiblePaths.add(pm.getServices().get(s).getTrafficFlow().getPaths().get(p));
         }
      return admissiblePaths;
   }

   private void activateServersPerDemand(Map<Integer, List<Path>> tSP, float[] environment) {
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

   private void calculateServerUtilization(float[] environment) {
      uX = new double[pm.getServers().size()];
      for (int x = 0; x < pm.getServers().size(); x++) {
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               double demands = 0;
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  if (pXSVD[x][s][v][d])
                     demands += pm.getServices().get(s).getTrafficFlow().getDemands().get(d);
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

   private void calculateLinkUtilization(float[] environment) {
      uL = new double[pm.getLinks().size()];
      // TODO
   }

   private double computeCost() {
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
      return Auxiliary.roundDouble(totalCost / pm.getServers().size(), 4);
   }

   private double computeReward(double cost, double objValTarget) {
      double reward;
      if (cost >= objValTarget)
         reward = 100;
      else reward = -1;
      return reward;
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
