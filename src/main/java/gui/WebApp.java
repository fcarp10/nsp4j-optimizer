package gui;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import filemanager.GraphManager;
import filemanager.InputParameters;
import model.ModelLauncher;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

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
    private static String output;

    public WebApp(InputParameters inputParameters) {
        jsonNodes = new HashMap<>();
        List<Node> nodes = new ArrayList<>(GraphManager.getGraph().getNodeSet());
        for (Node node : nodes)
            jsonNodes.put(node.getId(), new JsonServer(node.getId(), node.getAttribute("x"), node.getAttribute("y"), "#BDBDBD", node.getId()));

        List<Edge> edges = new ArrayList<>(GraphManager.getGraph().getEdgeSet());
        jsonLinks = new HashMap<>();
        for (Edge edge : edges)
            jsonLinks.put(edge.getId(), new JsonLink(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.getId(), "#000"));


        interfaces(inputParameters);
    }

    private static void interfaces(InputParameters inputParameters) {
        post("/node", (request, response) -> {
            response.type("application/json");
            Type listType = new TypeToken<ArrayList<JsonServer>>() {
            }.getType();
            List<JsonServer> rJsonServers = new Gson().fromJson(request.body(), listType);
            for (JsonServer jsonServer : rJsonServers)
                jsonNodes.put(jsonServer.getData().getId(), jsonServer);
            response.status(201);
            return "";
        });

        post("/link", (request, response) -> {
            response.type("application/json");
            Type listType = new TypeToken<ArrayList<JsonLink>>() {
            }.getType();
            List<JsonLink> rJsonLinks = new Gson().fromJson(request.body(), listType);
            for (JsonLink jsonLink : rJsonLinks)
                jsonLinks.replace(jsonLink.getData().getId(), jsonLink);
            response.status(201);
            return "";
        });

        get("/node", (request, response) -> {
            response.type("application/json");
            return new Gson().toJson(jsonNodes.values());
        });

        get("/link", (request, response) -> {
            response.type("application/json");
            return new Gson().toJson(jsonLinks.values());
        });

        get("/link-opt", (request, response) -> {
            ModelLauncher.startLinkOptimization(inputParameters);
            return 200;
        });

        post("/output", (request, response) -> {
            output += "\n" + request.body();
            return 201;
        });

        get("/output", (request, response) -> {

            String rOutput = output;
            output = "";
            return rOutput;
        });
    }
}
