package app;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import elements.Scenario;
import elements.json.LinkJson;
import elements.json.NodeJson;
import elements.json.ServerJson;
import filemanager.GraphManager;
import network.Server;
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

class WebServer {

    private static List<NodeJson> nodeList;
    private static Map<String, ServerJson> serverJsonMap;
    private static Map<String, LinkJson> linkJsonMap;
    private static Results results;
    private static String message;

    WebServer() {
        nodeList = new ArrayList<>();
        serverJsonMap = new HashMap<>();
        linkJsonMap = new HashMap<>();
        interfaces();
    }

    void initialize(List<Server> servers) {
        nodeList = new ArrayList<>();
        serverJsonMap = new HashMap<>();
        linkJsonMap = new HashMap<>();
        List<Node> nodes = new ArrayList<>(GraphManager.getGraph().getNodeSet());
        for (Node node : nodes)
            nodeList.add(new NodeJson(node.getId(), node.getAttribute("x"), node.getAttribute("y"), "#cccccc", node.getId()));
        for (Server server : servers)
            serverJsonMap.put(server.getId(), new ServerJson(server.getId(), server.getNodeParent().getAttribute("x"), server.getNodeParent().getAttribute("y"), "#cccccc", server.getId()));
        List<Edge> edges = new ArrayList<>(GraphManager.getGraph().getEdgeSet());
        for (Edge edge : edges)
            linkJsonMap.put(edge.getId(), new LinkJson(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.getId(), "#000"));
    }

    private static void interfaces() {

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

        get("/message", (request, response) -> message);
    }
}
