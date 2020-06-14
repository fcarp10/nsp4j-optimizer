package optimizer;

import optimizer.gui.ResultsGUI;
import optimizer.gui.WebServer;

import static spark.Spark.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

   private static final Logger log = LoggerFactory.getLogger(Launcher.class);

   public static void main(String[] args) {
      log.info("max heap size (MB): " + java.lang.Runtime.getRuntime().maxMemory() / 1000000);
      port(Definitions.PORT);
      staticFiles.location("/public");
      init();
      new ResultsGUI();
      WebServer.interfaces();
      ResultsGUI.log("INFO - backend is ready");
   }
}