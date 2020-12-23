package optimizer.gui;

import com.google.gson.Gson;
import optimizer.Manager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static spark.Spark.get;
import static spark.Spark.post;

public class WebServer {

   public static void interfaces() {

      ExecutorService executorService = Executors.newSingleThreadExecutor();

      post("/load", (request, response) -> {
         Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
         Manager.readInputParameters(scenario.getInputFileName(), false);
         return 201;
      });

      post("/run", (request, response) -> {
         Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
         Runnable runnable = () -> Manager.main(scenario);
         executorService.submit(runnable);
         return 201;
      });

      get("/stop", (request, response) -> {
         Manager.stop();
         return 201;
      });

      get("/node", (request, response) -> {
         response.type("application/json");
         return new Gson().toJson(ResultsGUI.getNodeList());
      });

      get("/server", (request, response) -> {
         response.type("application/json");
         return new Gson().toJson(ResultsGUI.getServerJsonMap().values());
      });

      get("/link", (request, response) -> {
         response.type("application/json");
         return new Gson().toJson(ResultsGUI.getLinkJsonMap().values());
      });

      get("/results", (request, response) -> {
         response.type("application/json");
         return new Gson().toJson(ResultsGUI.getResults());
      });

      get("/message", (request, response) -> {
         if (ResultsGUI.getMessages().peek() != null)
            return ResultsGUI.getMessages().remove();
         return "";
      });
   }
}
