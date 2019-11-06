package gui.elements;

import static output.Definitions.SERVER_SHAPE;

public class ServerJson extends NodeJson {

   public ServerJson(String id, double x, double y, String favecolor, String label) {
      double xS, yS;
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
      data = new Data(id, favecolor, label, SERVER_SHAPE, 20, 25);
   }
}
