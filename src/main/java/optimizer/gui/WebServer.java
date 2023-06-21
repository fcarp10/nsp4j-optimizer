package optimizer.gui;

import com.google.gson.Gson;
import optimizer.Manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.delete;

import static optimizer.Definitions.*;

public class WebServer {

   private static final Logger log = LoggerFactory.getLogger(WebServer.class);

   public static void interfaces() {

      ExecutorService executorService = Executors.newSingleThreadExecutor();

      post("/load", (request, response) -> {
         Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
         Manager.readParameters(scenario.getInputFileName());
         return 201;
      });

      post("/upload", (request, response) -> {
         String resourcesPath = Manager.class.getClassLoader().getResource(SCENARIOS_PATH).getPath();
         request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));
         try {
            Part filePart = request.raw().getPart("file");
            String uploadedFileName = filePart.getSubmittedFileName();
            InputStream stream = filePart.getInputStream();
            Files.copy(stream, Paths.get(resourcesPath).resolve(uploadedFileName), StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException | ServletException e) {
            log.warn("error occurred while uploading file: " + e.toString());
            return 503;
          } 
         return 201;
      });

      delete("/delete/:file", (request, response) -> {
         String resourcesPath = Manager.class.getClassLoader().getResource(SCENARIOS_PATH).getPath();
         File file = Paths.get(resourcesPath).resolve(request.params(":file")).toFile();
         if (file.exists()) {
            file.delete();
            return 200;
         } else {
            log.warn("File " + request.params(":file") + " doesn't exist");
            return 503;
         }
      });

      post("/run", (request, response) -> {
         Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
         Runnable runnable = () -> Manager.main(scenario);
         executorService.submit(runnable);
         return 201;
      });

      get("/stop", (request, response) -> {
         Manager.terminate();
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
