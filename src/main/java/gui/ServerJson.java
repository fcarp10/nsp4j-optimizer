package gui;

import static output.Parameters.MAX_NUM_SERVERS;
import static output.Parameters.SERVER_SHAPE;

class ServerJson extends NodeJson {

   ServerJson(String id, double x, double y, String favecolor, String label) {
      double xS, yS = y + 11;
      int index = Integer.parseInt(id.split("_")[1]);
      if (index % 2 == 0)
         xS = x - 11;
      else
         xS = x + 11;
      for (int i = 1; i < MAX_NUM_SERVERS; i++) {
         if (index - i <= 1) {
            if (index % 2 == 0)
               yS = yS + (13 * (index + 1));
            else
               yS = yS + (13 * index);
            break;
         }
      }
      position = new Position(xS, yS);
      data = new Data(id, favecolor, label, SERVER_SHAPE, 20, 25);
   }
}
