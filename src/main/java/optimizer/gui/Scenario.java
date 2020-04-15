package optimizer.gui;

import java.util.HashMap;
import java.util.Map;

public class Scenario {

   private String inputFileName;
   private String objFunc;
   private boolean maximization;
   private String algorithm;
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

   public String getObjFunc() {
      return objFunc;
   }

   public void setObjFunc(String objFunc) {
      this.objFunc = objFunc;
   }

   public String getAlgorithm() {
      return algorithm;
   }

   public void setAlgorithm(String algorithm) {
      this.algorithm = algorithm;
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

}
