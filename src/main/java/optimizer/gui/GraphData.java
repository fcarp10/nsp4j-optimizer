package optimizer.gui;

public class GraphData {

   private String year;
   private double value;

   public GraphData() {
   }

   public GraphData(String year, double value) {
      this.year = year;
      this.value = value;
   }

   public String getYear() {
      return year;
   }

   public double getValue() {
      return value;
   }

   public void setValue(double value) {
      this.value = value;
   }
}
