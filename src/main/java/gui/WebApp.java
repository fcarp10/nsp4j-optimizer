package gui;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import filemanager.GraphManager;
import model.Launcher;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import results.Results;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.post;

public class WebApp {

    private static Map<String, JsonServer> jsonNodes;
    private static Map<String, JsonLink> jsonLinks;
    private static Results results;
    private static String message;

    public WebApp() {
        jsonNodes = new HashMap<>();
        jsonLinks = new HashMap<>();
        interfaces();
    }

    public void initializeResults() {
        jsonNodes = new HashMap<>();
        jsonLinks = new HashMap<>();
        List<Node> nodes = new ArrayList<>(GraphManager.getGraph().getNodeSet());
        for (Node node : nodes)
            jsonNodes.put(node.getId(), new JsonServer(node.getId(), node.getAttribute("x"), node.getAttribute("y"), "#BDBDBD", node.getId()));

        List<Edge> edges = new ArrayList<>(GraphManager.getGraph().getEdgeSet());
        for (Edge edge : edges)
            jsonLinks.put(edge.getId(), new JsonLink(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.getId(), "#000"));
    }

    private static void interfaces() {
        post("/node", (request, response) -> {
            response.type("application/json");
            Type listType = new TypeToken<ArrayList<JsonServer>>() {
            }.getType();
            List<JsonServer> rJsonServers = new Gson().fromJson(request.body(), listType);
            for (JsonServer jsonServer : rJsonServers)
                jsonNodes.put(jsonServer.getData().getId(), jsonServer);
            response.status(201);
            return 201;
        });

        post("/link", (request, response) -> {
            response.type("application/json");
            Type listType = new TypeToken<ArrayList<JsonLink>>() {
            }.getType();
            List<JsonLink> rJsonLinks = new Gson().fromJson(request.body(), listType);
            for (JsonLink jsonLink : rJsonLinks)
                jsonLinks.replace(jsonLink.getData().getId(), jsonLink);
            response.status(201);
            return 201;
        });

        post("/results", (request, response) -> {
            response.type("application/json");
            results = new Gson().fromJson(request.body(), Results.class);
            return 201;
        });

        post("/message", (request, response) -> {
            message = request.body();
            return 201;
        });

        post("/run", (request, response) -> {
            String runMessage = request.body();
            String[] message = runMessage.split("-");
            new Launcher();
            Launcher.startOptimization(message[0], message[1]);
            return "Running...";
        });

        get("/node", (request, response) -> {
            response.type("application/json");
            return new Gson().toJson(jsonNodes.values());
        });

        get("/link", (request, response) -> {
            response.type("application/json");
            return new Gson().toJson(jsonLinks.values());
        });

        get("/results", (request, response) -> {
            response.type("application/json");
            return new Gson().toJson(results);
        });

        get("/message", (request, response) -> message);
    }
}
