package gui;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gui.elements.LinkJson;
import gui.elements.NodeJson;
import gui.elements.ServerJson;
import manager.Manager;
import manager.Parameters;
import gui.elements.Scenario;
import manager.elements.Server;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import results.Results;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static results.Auxiliary.INFO;
import static spark.Spark.get;
import static spark.Spark.post;

public class WebServer {

    private static List<NodeJson> nodeList;
    private static Map<String, ServerJson> serverJsonMap;
    private static Map<String, LinkJson> linkJsonMap;
    private static Results results;
    private static String message;

    public WebServer() {
        nodeList = new ArrayList<>();
        serverJsonMap = new HashMap<>();
        linkJsonMap = new HashMap<>();
        interfaces();
    }

    public void initialize(Parameters parameters) {
        nodeList = new ArrayList<>();
        serverJsonMap = new HashMap<>();
        linkJsonMap = new HashMap<>();
        for (Node node : parameters.getNodes())
            nodeList.add(new NodeJson(node.getId(), node.getAttribute("x"), node.getAttribute("y"), "#cccccc", node.getId()));
        for (Server server : parameters.getServers())
            serverJsonMap.put(server.getId(), new ServerJson(server.getId(), server.getNodeParent().getAttribute("x"), server.getNodeParent().getAttribute("y"), "#cccccc", server.getId()));
        for (Edge edge : parameters.getLinks())
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
            Manager.start(scenario);
            return INFO + "running the model...";
        });

        post("/paths", (request, response) -> {
            Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
            Manager.generatePaths(scenario);
            return INFO + "generating paths...";
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
