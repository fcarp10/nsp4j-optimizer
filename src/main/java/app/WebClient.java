package app;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import elements.json.LinkJson;
import elements.json.ServerJson;
import network.Server;
import org.graphstream.graph.Edge;
import results.Auxiliary;
import results.Output;
import results.Results;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.*;

class WebClient {

    static void updateResultsToWebApp(Output output, Results results) {
        if (results != null) {
            List<ServerJson> serverJsonList = generateServerStrings(output);
            List<LinkJson> linkJsonList = generateLinkStrings(output);
            try {
                postJsonServers(serverJsonList);
                postJsonLinks(linkJsonList);
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
                .send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void postJsonServers(List<ServerJson> serverJsons) throws URISyntaxException, IOException, InterruptedException {
        Type listType = new TypeToken<ArrayList<ServerJson>>() {
        }.getType();
        String stringJsonNodes = new Gson().toJson(serverJsons, listType);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/server"))
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(stringJsonNodes))
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
                .POST(HttpRequest.BodyPublishers.ofString(stringJsonLinks))
                .build();
        sendRequest(request);
    }

    private static void postResults(Results results) throws URISyntaxException, IOException, InterruptedException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        String stringResults = gson.toJson(results);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/results"))
                .POST(HttpRequest.BodyPublishers.ofString(stringResults))
                .build();
        sendRequest(request);
    }

    static void postMessage(String message) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/message"))
                    .POST(HttpRequest.BodyPublishers.ofString(message))
                    .build();
            sendRequest(request);
        } catch (URISyntaxException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private static List<ServerJson> generateServerStrings(Output output) {
        Map<Server, String> functions = generateFunctionsPerServerStringMap(output);
        List<ServerJson> serverJsonList = new ArrayList<>();
        Iterator entries = output.serverUtilizationMap().entrySet().iterator();
        DecimalFormat df = new DecimalFormat("#.##");
        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            Double utilization = (Double) thisEntry.getValue();
            Server server = (Server) thisEntry.getKey();
            StringBuilder u = new StringBuilder();
            if (utilization != 0) {
                u.append(String.valueOf(df.format(utilization)));
                if (functions.get(server).length() < 40)
                    u.append("\n").append(functions.get(server));
            }
            serverJsonList.add(new ServerJson(server.getId(), server.getNodeParent().getAttribute("pX")
                    , server.getNodeParent().getAttribute("y")
                    , "#" + getColor(utilization), u.toString()));
        }
        return serverJsonList;
    }

    private static List<LinkJson> generateLinkStrings(Output output) {
        List<LinkJson> linkJsonList = new ArrayList<>();
        Iterator entries = output.linkUtilizationMap().entrySet().iterator();
        DecimalFormat df = new DecimalFormat("#.##");
        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            Double value = (Double) thisEntry.getValue();
            Edge edge = (Edge) thisEntry.getKey();
            String label = "";
            if (value != 0)
                label = df.format(value);
            linkJsonList.add(new LinkJson(edge.getId(), edge.getSourceNode().getId()
                    , edge.getTargetNode().getId(), label, "#" + getColor(value)));
        }
        return linkJsonList;
    }

    private static Map<Server, String> generateFunctionsPerServerStringMap(Output output) {
        Map<Server, String> functionsStringMap = new HashMap<>();
        for (int x = 0; x < output.getPm().getServers().size(); x++) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int s = 0; s < output.getPm().getServices().size(); s++)
                for (int v = 0; v < output.getPm().getServices().get(s).getFunctions().size(); v++)
                    if (output.getpXSV()[x][s][v])
                        stringBuilder.append("f(").append(x + Auxiliary.OFFSET).append(",").append(s + Auxiliary.OFFSET).append(",").append(v + Auxiliary.OFFSET).append(")\n");
            functionsStringMap.put(output.getPm().getServers().get(x), stringBuilder.toString());
        }
        return functionsStringMap;
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
