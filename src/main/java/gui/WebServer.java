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
import results.Output;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static spark.Spark.get;
import static spark.Spark.post;

public class WebServer {

    private static List<NodeJson> nodeList;
    private static Map<String, ServerJson> serverJsonMap;
    private static Map<String, LinkJson> linkJsonMap;
    private static List<String> messages;
    private static Output output;
    private static Output initialOutput;

    public WebServer() {
        nodeList = new ArrayList<>();
        serverJsonMap = new HashMap<>();
        linkJsonMap = new HashMap<>();
        messages = new ArrayList<>();
        interfaces();
    }

    public void initialize(Parameters parameters) {
        nodeList = new ArrayList<>();
        serverJsonMap = new HashMap<>();
        linkJsonMap = new HashMap<>();
        for (Node node : parameters.getNodes())
            nodeList.add(new NodeJson(node.getId(), node.getAttribute("x"), node.getAttribute("y"), "Gray", node.getId()));
        for (Server server : parameters.getServers())
            serverJsonMap.put(server.getId(), new ServerJson(server.getId(), server.getNodeParent().getAttribute("x"), server.getNodeParent().getAttribute("y"), "Gray", server.getId()));
        for (Edge edge : parameters.getLinks())
            linkJsonMap.put(edge.getId(), new LinkJson(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.getId(), "Gray"));
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
            output = new Gson().fromJson(request.body(), Output.class);
            return 201;
        });

        post("/message", (request, response) -> {
            messages.add(request.body());
            return 201;
        });

        post("/load", (request, response) -> {
            Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
            Manager.loadTopology(scenario.getInputFileName());
            return 201;
        });

        post("/run", (request, response) -> {
            Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
            Runnable runnable = () -> initialOutput = Manager.start(scenario, initialOutput);
            executorService.submit(runnable);
            return 201;
        });

        post("/paths", (request, response) -> {
            Scenario scenario = new Gson().fromJson(request.body(), Scenario.class);
            Runnable runnable = () -> Manager.generatePaths(scenario);
            executorService.submit(runnable);
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
            return new Gson().toJson(output);
        });

        get("/message", (request, response) -> {
            if (messages.size() > 3)
                messages.remove(0);
            return messages;
        });
    }
}
