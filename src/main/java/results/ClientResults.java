package results;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gui.JsonLink;
import gui.JsonNode;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ClientResults {

    public void updateResultsToWebApp(){

    }

    private void sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient
                .newBuilder()
                .proxy(ProxySelector.getDefault())
                .build()
                .send(request, HttpResponse.BodyHandler.asString());
    }

    private void postJsonNodes(List<JsonNode> jsonNodes) throws URISyntaxException, IOException, InterruptedException {
        Type listType = new TypeToken<ArrayList<JsonNode>>() {}.getType();
        String stringJsonNodes = new Gson().toJson(jsonNodes, listType);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/node"))
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublisher.fromString(stringJsonNodes))
                .build();
        sendRequest(request);
    }

    private void postJsonLinks(List<JsonLink> jsonLinks) throws URISyntaxException, IOException, InterruptedException {
        Type listType = new TypeToken<ArrayList<JsonLink>>() {}.getType();
        String stringJsonLinks = new Gson().toJson(jsonLinks, listType);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://postman-echo.com/post"))
                .headers("Content-Type", "text/plain;charset=UTF-8")
                .POST(HttpRequest.BodyPublisher.fromString(stringJsonLinks))
                .build();
        sendRequest(request);
    }
    
    private List<JsonNode> getNodeStringsWithResults(Map<Node, Double> nodes, Map<Node, String> vnfs) {
        List<JsonNode> jsonNodes = new ArrayList<>();
        Iterator entries = nodes.entrySet().iterator();
        String su;
        DecimalFormat df = new DecimalFormat("#.##");
        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            Double value = (Double) thisEntry.getValue();
            Node node = (Node) thisEntry.getKey();
            su = "";
            if (value != 0 && vnfs.get(node).length() < 40)
                su = "\\nsu = " + String.valueOf(df.format(value)) + "\\n" + vnfs.get(node);
            else if (value != 0 && vnfs.get(node).length() >= 40) {
                su = "\\nsu = " + String.valueOf(df.format(value));
            }
            jsonNodes.add(new JsonNode(node.getId(), node.getAttribute("x"), node.getAttribute("y"), "#" + getColor(value), node.getId() + su));
        }
        return jsonNodes;
    }

    private List<JsonLink> getLinkStringsWithResults(Map<Edge, Double> links) {
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
            jsonLinks.add(new JsonLink(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId(), label, "#" + getColor(value)));
        }

        return jsonLinks;
    }

    private String getColor(Double utilization) {
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
