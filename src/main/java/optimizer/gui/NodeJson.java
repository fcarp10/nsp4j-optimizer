package optimizer.gui;


public class NodeJson {

   Position position;
   Data data;
   private String classes = "multiline-manual";

   NodeJson() {
   }

   public NodeJson(String id, double x, double y, String favecolor, String label, String shape) {
      position = new Position(x, y);
      data = new Data(id, favecolor, label, shape, 25, 25);
   }

   public Position getPosition() {
      return position;
   }

   public void setPosition(Position position) {
      this.position = position;
   }

   public Data getData() {
      return data;
   }

   public void setData(Data data) {
      this.data = data;
   }

   public String getClasses() {
      return classes;
   }

   public void setClasses(String classes) {
      this.classes = classes;
   }

   public class Position {
      private double x;
      private double y;

      Position(double x, double y) {
         this.x = x;
         this.y = y;
      }

      public double getX() {
         return x;
      }

      public void setX(double x) {
         this.x = x;
      }

      public double getY() {
         return y;
      }

      public void setY(double y) {
         this.y = y;
      }
   }

   public class Data {
      private String id;
      private String faveColor;
      private String label;
      private String faveShape;
      private int width;
      private int height;

      public Data(String id, String faveColor, String label, String faveShape, int width, int height) {
         this.id = id;
         this.faveColor = faveColor;
         this.label = label;
         this.faveShape = faveShape;
         this.width = width;
         this.height = height;
      }

      public String getId() {
         return id;
      }

      public void setId(String id) {
         this.id = id;
      }

      public String getFaveColor() {
         return faveColor;
      }

      public void setFaveColor(String faveColor) {
         this.faveColor = faveColor;
      }

      public String getLabel() {
         return label;
      }

      public void setLabel(String label) {
         this.label = label;
      }

      public String getFaveShape() {
         return faveShape;
      }

      public void setFaveShape(String faveShape) {
         this.faveShape = faveShape;
      }

      public int getWidth() {
         return width;
      }

      public int getHeight() {
         return height;
      }
   }

}
