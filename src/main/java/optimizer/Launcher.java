package optimizer;

import optimizer.gui.ResultsGUI;
import optimizer.gui.WebServer;

import static spark.Spark.*;

public class Launcher {

   public static void main(String[] args) {
      port(Definitions.PORT);
      staticFiles.location("/public");
      init();
      new ResultsGUI();
      WebServer.interfaces();
      ResultsGUI.log("INFO - backend is ready");
   }
}