package manager;

import gui.ResultsGUI;
import gui.WebServer;
import output.Auxiliary;

import static output.Parameters.PORT;
import static spark.Spark.*;

public class Launcher {

   public static void main(String[] args) {
      port(PORT);
      staticFiles.location("/public");
      init();
      new Auxiliary();
      new ResultsGUI();
      WebServer.interfaces();
      ResultsGUI.log("Info: please, load the topology");
   }
}