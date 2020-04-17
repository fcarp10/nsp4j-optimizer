package optimizer.algorithms.learning;


import manager.Parameters;
import optimizer.algorithms.VariablesAlg;

import java.util.Random;

import static optimizer.Definitions.EPSILON;
import static optimizer.Definitions.ITERATIONS;

public class DeepRL {

   protected Parameters pm;
   protected VariablesAlg variablesAlg;
   protected Random rnd;
   protected ModelLearning modelLearning;
   protected int inputSize;
   protected boolean[][][] initialPlacement;
   protected String objFunc;

   public DeepRL(Parameters pm, VariablesAlg variablesAlg, boolean[][][] initialPlacement, String objFunc) {
      this.pm = pm;
      this.variablesAlg = variablesAlg;
      this.initialPlacement = initialPlacement;
      this.objFunc = objFunc;
      rnd = new Random();
      inputSize = calculateInputSize();
      modelLearning = new ModelLearning(null, inputSize);
   }

   public void run() {

      float[] environment = new float[inputSize];
      float[] nextEnvironment = new float[inputSize];

      for (int i = 0; i < ITERATIONS; i++) {

         modelLearning.observeReward(environment, nextEnvironment, variablesAlg.getObjVal());
         environment = nextEnvironment;

         int action = modelLearning.takeAction(environment, EPSILON);


         // modify variables based on the taken action
         variablesAlg.generateRestOfVariablesForResults(initialPlacement, objFunc); // calculate new objective value

         nextEnvironment = modelLearning.modifyEnvironment(environment, action, i);

      }
   }

   private int calculateInputSize() {
      int inputSize = 0;
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
            inputSize += pm.getServices().get(s).getTrafficFlow().getPaths().size();
      return inputSize;
   }

}
