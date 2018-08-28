package gui;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import utils.LinkJson;
import utils.Scenario;
import utils.ServerJson;
import filemanager.GraphManager;
import app.App;
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

public class WebServer {

    private static Map<String, ServerJson> jsonNodes;
    private static Map<String, LinkJson> jsonLinks;
    private static Results results;
    private static String message;

    public WebServer() {
        jsonNodes = new HashMap<>();
        jsonLinks = new HashMap<>();
        interfaces();
    }

    public void initializeResults() {
        jsonNodes = new HashMap<>();
        jsonLinks = new HashMap<>();
        List<Node> nodes = new ArrayList<>(GraphManager.getGraph().getNodeSet());
        for (Node node : nodes)
            jsonNodes.put(node.getId(), new ServerJson(node.getId(), node.getAttribute("x"), node.getAttribute("y"), "#BDBDBD", node.getId()));

        List<Edge> edges = new ArrayList<>(GraphManager.getGraph().getEdgeSet());
        for (Edge edge : edges)
            jsonLinks.put(edge.getId(), new LinkJson(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.getId(), "#000"));
    }

    private static void interfaces() {
        post("/node", (request, response) -> {
            response.type("application/json");
            Type listType = new TypeToken<ArrayList<ServerJson>>() {
            }.getType();
            List<ServerJson> rServerJsons = new Gson().fromJson(request.body(), listType);
            for (ServerJson serverJson : rServerJsons)
                jsonNodes.put(serverJson.getData().getId(), serverJson);
            response.status(201);
            return 201;
        });

        post("/link", (request, response) -> {
            response.type("application/json");
            Type listType = new TypeToken<ArrayList<LinkJson>>() {
            }.getType();
            List<LinkJson> rLinkJsons = new Gson().fromJson(request.body(), listType);
            for (LinkJson linkJson : rLinkJsons)
                jsonLinks.replace(linkJson.getData().getId(), linkJson);
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
            Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
            App.start(scenario);
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
