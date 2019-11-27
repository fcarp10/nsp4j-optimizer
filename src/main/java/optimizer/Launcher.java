package optimizer;

import optimizer.gui.ResultsGUI;
import optimizer.gui.WebServer;
import optimizer.results.Auxiliary;

import static spark.Spark.*;

public class Launcher {

   public static void main(String[] args) {
      port(Parameters.PORT);
      staticFiles.location("/public");
      init();
      new Auxiliary();
      new ResultsGUI();
      WebServer.interfaces();
      ResultsGUI.log("INFO - backend is ready");
   }
}