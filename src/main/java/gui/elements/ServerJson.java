package gui.elements;

public class ServerJson extends NodeJson {

   public ServerJson(String id, int x, int y, String favecolor, String label) {
      int xS, yS;
      int index = Integer.valueOf(id.split("_")[1]);
      if (index % 2 == 0)
         xS = x - 11;
      else
         xS = x + 11;
      if (index <= 1)
         yS = y + 26;
      else
         yS = y + (26 * 2);
      position = new Position(xS, yS);
      data = new Data(id, favecolor, label, "rectangle", 20, 25);
   }
}
