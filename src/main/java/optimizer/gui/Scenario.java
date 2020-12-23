package optimizer.gui;

import java.util.HashMap;
import java.util.Map;

public class Scenario {

   private String inputFileName;
   private String objFunc;
   private boolean maximization;
   private String name;
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

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Map<String, Boolean> getConstraints() {
      return constraints;
   }

   public void setConstraint(String key, Boolean value) {
      this.constraints.put(key, value);
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
