package results;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import gui.JsonLink;
import gui.JsonServer;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import network.Server;
import org.graphstream.graph.Edge;

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

public class Client {

    public static void updateResultsToWebApp(Results results) {
        if (results != null) {
            List<JsonServer> jsonServers = getNodeStringsWithResults(results.getServersMap(), results.getFunctionsStringMap());
            List<JsonLink> jsonLinks = getLinkStringsWithResults(results.getLinksMap());
            try {
                postJsonNodes(jsonServers);
                postJsonLinks(jsonLinks);
                postResults(results);
            } catch (URISyntaxException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient
                .newBuilder()
                .proxy(ProxySelector.getDefault())
                .build()
                .send(request, HttpResponse.BodyHandler.asString());
    }

    private static void postJsonNodes(List<JsonServer> jsonServers) throws URISyntaxException, IOException, InterruptedException {
        Type listType = new TypeToken<ArrayList<JsonServer>>() {
        }.getType();
        String stringJsonNodes = new Gson().toJson(jsonServers, listType);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/node"))
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublisher.fromString(stringJsonNodes))
                .build();
        sendRequest(request);
    }

    private static void postJsonLinks(List<JsonLink> jsonLinks) throws URISyntaxException, IOException, InterruptedException {
        Type listType = new TypeToken<ArrayList<JsonLink>>() {
        }.getType();
        String stringJsonLinks = new Gson().toJson(jsonLinks, listType);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/link"))
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublisher.fromString(stringJsonLinks))
                .build();
        sendRequest(request);
    }

    private static void postResults(Results results) throws URISyntaxException, IOException, InterruptedException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        String stringResults = gson.toJson(results);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/results"))
                .POST(HttpRequest.BodyPublisher.fromString(stringResults))
                .build();
        sendRequest(request);
    }

    public static void postMessage(String message) {
        HttpRequest request = null;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/message"))
                    .POST(HttpRequest.BodyPublisher.fromString(message))
                    .build();
            sendRequest(request);
        } catch (URISyntaxException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private static List<JsonServer> getNodeStringsWithResults(Map<Server, Double> servers, Map<Server, String> functions) {
        List<JsonServer> jsonServers = new ArrayList<>();
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
            jsonServers.add(new JsonServer(server.getId(), server.getNodeParent().getAttribute("x")
                    , server.getNodeParent().getAttribute("y")
                    , "#" + getColor(value), functionsString.toString(), true));
        }
        return jsonServers;
    }

    private static List<JsonLink> getLinkStringsWithResults(Map<Edge, Double> links) {
        List<JsonLink> jsonLinks = new ArrayList<>();
        Iterator entries = links.entrySet().iterator();
        DecimalFormat df = new DecimalFormat("#.##");

        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            Double value = (Double) thisEntry.getValue();
            Edge edge = (Edge) thisEntry.getKey();
            String label = "";
            if (value != 0)
                label = df.format(value);
            jsonLinks.add(new JsonLink(edge.getId(), edge.getSourceNode().getId()
                    , edge.getTargetNode().getId(), label, "#" + getColor(value)));
        }

        return jsonLinks;
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
