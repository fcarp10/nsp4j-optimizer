package gui;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gui.elements.LinkJson;
import gui.elements.NodeJson;
import gui.elements.Scenario;
import gui.elements.ServerJson;
import gurobi.GRBModel;
import manager.Manager;
import manager.Parameters;
import manager.elements.Server;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import output.Results;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static output.Definitions.*;
import static spark.Spark.get;
import static spark.Spark.post;

public class WebServer {

   private static List<NodeJson> nodeList;
   private static Map<String, ServerJson> serverJsonMap;
   private static Map<String, LinkJson> linkJsonMap;
   private static Results results;
   private static GRBModel initialModel;
   private static LinkedList messages;

   public WebServer() {
      nodeList = new ArrayList<>();
      serverJsonMap = new HashMap<>();
      linkJsonMap = new HashMap<>();
      messages = new LinkedList();
      interfaces();
   }

   public static void initialize(Parameters parameters) {
      nodeList = new ArrayList<>();
      serverJsonMap = new HashMap<>();
      linkJsonMap = new HashMap<>();
      for (Node n : parameters.getNodes()) {
         String xAttr = "x", yAttr = "y";
         if (n.getAttribute(NODE_CLOUD) != null) {
            xAttr = "x_gui";
            yAttr = "y_gui";
         }
         nodeList.add(new NodeJson(n.getId(), n.getAttribute(xAttr), n.getAttribute(yAttr)
                 , NODE_COLOR, n.getId(), NODE_SHAPE));
      }
      for (Server s : parameters.getServers()) {
         String xAttr = "x", yAttr = "y";
         if (s.getParent().getAttribute(NODE_CLOUD) != null) {
            xAttr = "x_gui";
            yAttr = "y_gui";
         }
         serverJsonMap.put(s.getId(), new ServerJson(s.getId(), s.getParent().getAttribute(xAttr)
                 , s.getParent().getAttribute(yAttr), SERVER_COLOR, s.getId()));
      }
      for (Edge e : parameters.getLinks()) {
         String color = LINK_COLOR;
         if (e.getSourceNode().getAttribute(NODE_CLOUD) != null || e.getTargetNode().getAttribute(NODE_CLOUD) != null)
            color = LINK_CLOUD_COLOR;
         linkJsonMap.put(e.getId(), new LinkJson(e.getId(), e.getSourceNode().getId(), e.getTargetNode().getId()
                 , "", color));
      }
   }

   private static void interfaces() {

      ExecutorService executorService = Executors.newSingleThreadExecutor();

      post("/server", (request, response) -> {
         response.type("application/json");
         Type listType = new TypeToken<ArrayList<ServerJson>>() {
         }.getType();
         List<ServerJson> rServerJsons = new Gson().fromJson(request.body(), listType);
         for (ServerJson serverJson : rServerJsons)
            serverJsonMap.put(serverJson.getData().getId(), serverJson);
         response.status(201);
         return 201;
      });

      post("/link", (request, response) -> {
         response.type("application/json");
         Type listType = new TypeToken<ArrayList<LinkJson>>() {
         }.getType();
         List<LinkJson> rLinkJsons = new Gson().fromJson(request.body(), listType);
         for (LinkJson linkJson : rLinkJsons)
            linkJsonMap.replace(linkJson.getData().getId(), linkJson);
         response.status(201);
         return 201;
      });

      post("/results", (request, response) -> {
         response.type("application/json");
         results = new Gson().fromJson(request.body(), Results.class);
         return 201;
      });

      post("/message", (request, response) -> {
         String message = request.body();
         if (messages.isEmpty())
            messages.add(message);
         else {
            if (!messages.peekLast().equals(message))
               messages.add(message);
         }
         return 201;
      });

      post("/load", (request, response) -> {
         Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
         Manager.loadTopology(scenario.getInputFileName());
         return 201;
      });

      post("/paths", (request, response) -> {
         Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
         Runnable runnable = () -> Manager.generatePaths(scenario);
         executorService.submit(runnable);
         return 201;
      });

      post("/run", (request, response) -> {
         Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
         Runnable runnable = () -> initialModel = Manager.start(scenario, initialModel);
         executorService.submit(runnable);
         return 201;
      });

      get("/stop", (request, response) -> {
         Manager.stop();
         return 201;
      });

      get("/favicon.ico", (request, response) -> "");

      get("/node", (request, response) -> {
         response.type("application/json");
         return new Gson().toJson(nodeList);
      });

      get("/server", (request, response) -> {
         response.type("application/json");
         return new Gson().toJson(serverJsonMap.values());
      });

      get("/link", (request, response) -> {
         response.type("application/json");
         return new Gson().toJson(linkJsonMap.values());
      });

      get("/results", (request, response) -> {
         response.type("application/json");
         return new Gson().toJson(results);
      });

      get("/message", (request, response) -> {
         if (messages.peek() != null)
            return messages.remove();
         return "";
      });
   }
}
