package gui.elements;

import java.util.HashMap;
import java.util.Map;

public class Scenario {

   private String inputFileName;
   private String objectiveFunction;
   private boolean maximization;
   private String weights;
   private String model;
   private Map<String, Boolean> constraints;

   public Scenario() {
      constraints = new HashMap<>();
   }

   public String getInputFileName() {
      return inputFileName;
   }

   public void setInputFileName(String inputFileName) {
      this.inputFileName = inputFileName;
   }

   public String getObjectiveFunction() {
      return objectiveFunction;
   }

   public void setObjectiveFunction(String objectiveFunction) {
      this.objectiveFunction = objectiveFunction;
   }

   public String getModel() {
      return model;
   }

   public void setModel(String model) {
      this.model = model;
   }

   public Map<String, Boolean> getConstraints() {
      return constraints;
   }

   public void setConstraints(Map<String, Boolean> constraints) {
      this.constraints = constraints;
   }

   public boolean isMaximization() {
      return maximization;
   }

   public void setMaximization(boolean maximization) {
      this.maximization = maximization;
   }

   public String getWeights() {
      return weights;
   }

   public void setWeights(String weights) {
      this.weights = weights;
   }
}
