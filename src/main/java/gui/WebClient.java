package gui;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import utils.LinkJson;
import utils.ServerJson;
import gurobi.GRBException;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import model.Output;
import network.Server;
import org.graphstream.graph.Edge;
import results.Results;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WebClient {

    public static void updateResultsToWebApp(Output output, Results results) throws GRBException {
        if (results != null) {
            List<ServerJson> serverJsons = getNodeStringsWithResults(output.serversMap(), output.functionsStringMap());
            List<LinkJson> linkJsons = getLinkStringsWithResults(output.linksMap());
            try {
                postJsonNodes(serverJsons);
                postJsonLinks(linkJsons);
                postResults(results);
            } catch (URISyntaxException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpClient
                .newBuilder()
                .proxy(ProxySelector.getDefault())
                .build()
                .send(request, HttpResponse.BodyHandler.asString());
    }

    private static void postJsonNodes(List<ServerJson> serverJsons) throws URISyntaxException, IOException, InterruptedException {
        Type listType = new TypeToken<ArrayList<ServerJson>>() {
        }.getType();
        String stringJsonNodes = new Gson().toJson(serverJsons, listType);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/node"))
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyProcessor.fromString(stringJsonNodes))
                .build();
        sendRequest(request);
    }

    private static void postJsonLinks(List<LinkJson> linkJsons) throws URISyntaxException, IOException, InterruptedException {
        Type listType = new TypeToken<ArrayList<LinkJson>>() {
        }.getType();
        String stringJsonLinks = new Gson().toJson(linkJsons, listType);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/link"))
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyProcessor.fromString(stringJsonLinks))
                .build();
        sendRequest(request);
    }

    private static void postResults(Results results) throws URISyntaxException, IOException, InterruptedException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        String stringResults = gson.toJson(results);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/results"))
                .POST(HttpRequest.BodyProcessor.fromString(stringResults))
                .build();
        sendRequest(request);
    }

    public static void postMessage(String message) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/message"))
                    .POST(HttpRequest.BodyProcessor.fromString(message))
                    .build();
            sendRequest(request);
        } catch (URISyntaxException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private static List<ServerJson> getNodeStringsWithResults(Map<Server, Double> servers, Map<Server, String> functions) {
        List<ServerJson> serverJsons = new ArrayList<>();
        Iterator entries = servers.entrySet().iterator();
        StringBuilder functionsString;
        DecimalFormat df = new DecimalFormat("#.##");
        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            Double value = (Double) thisEntry.getValue();
            Server server = (Server) thisEntry.getKey();
            functionsString = new StringBuilder();
            if (value != 0 && functions.get(server).length() < 40)
                functionsString.append(String.valueOf(df.format(value))).append("\n").append(functions.get(server));
            else if (value != 0 && functions.get(server).length() >= 40)
                functionsString.append(String.valueOf(df.format(value)));
            serverJsons.add(new ServerJson(server.getId(), server.getNodeParent().getAttribute("x")
                    , server.getNodeParent().getAttribute("y")
                    , "#" + getColor(value), functionsString.toString(), true));
        }
        return serverJsons;
    }

    private static List<LinkJson> getLinkStringsWithResults(Map<Edge, Double> links) {
        List<LinkJson> linkJsons = new ArrayList<>();
        Iterator entries = links.entrySet().iterator();
        DecimalFormat df = new DecimalFormat("#.##");

        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            Double value = (Double) thisEntry.getValue();
            Edge edge = (Edge) thisEntry.getKey();
            String label = "";
            if (value != 0)
                label = df.format(value);
            linkJsons.add(new LinkJson(edge.getId(), edge.getSourceNode().getId()
                    , edge.getTargetNode().getId(), label, "#" + getColor(value)));
        }

        return linkJsons;
    }

    private static String getColor(Double utilization) {
        String[] colors = {"cccccc", "00cc00", "33cc00", "66cc00", "99cc00", "cccc00", "cc9900", "cc6600", "cc3300", "cc0000"};
        double[] gaps = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

        if (utilization <= gaps[0])
            return colors[0];

        for (int i = 1; i < gaps.length; i++)
            if (utilization > gaps[i - 1] && utilization <= gaps[i])
                return colors[i];

        return null;
    }
}
