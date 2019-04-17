package manager;

import gui.WebClient;
import gui.WebServer;
import output.Auxiliary;

import static spark.Spark.*;

public class Launcher {

   public static final int PORT = 8080;

   public static void main(String[] args) {
      port(PORT);
      staticFiles.location("/public");
      init();
      new Auxiliary();
      new WebServer();
      WebClient.postMessage("Info: please, load the topology");
   }
}