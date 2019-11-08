package optimizer.lp;

import java.util.LinkedList;

public class CostFunctions {

   private LinkedList<Double[]> values = new LinkedList<>();

   public CostFunctions() {
   }

   public LinkedList<Double[]> getValues() {
      return values;
   }

   public void setValues(LinkedList<Double[]> values) {
      this.values = values;
   }
}
