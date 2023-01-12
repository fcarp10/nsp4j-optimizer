package optimizer.gui;

import static optimizer.Definitions.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import optimizer.Parameters;
import optimizer.elements.Server;
import optimizer.results.Results;

public class ResultsGUI {

   private static List<NodeJson> nodeList;
   private static Map<String, ServerJson> serverJsonMap;
   private static Map<String, LinkJson> linkJsonMap;
   private static Results results;
   private static LinkedList<String> messages;
   private static String longitudeLabel, latitudeLabel;

   public ResultsGUI() {
      nodeList = new ArrayList<>();
      serverJsonMap = new HashMap<>();
      linkJsonMap = new HashMap<>();
      messages = new LinkedList<>();
   }

   public static void initialize(Parameters pm) {
      nodeList = new ArrayList<>();
      serverJsonMap = new HashMap<>();
      linkJsonMap = new HashMap<>();
      if (pm.getNodes().get(0).getAttribute(LONGITUDE_LABEL_1) != null) {
         longitudeLabel = LONGITUDE_LABEL_1;
         latitudeLabel = LATITUDE_LABEL_1;
      } else {
         longitudeLabel = LONGITUDE_LABEL_2;
         latitudeLabel = LATITUDE_LABEL_2;
      }
      for (Node n : pm.getNodes()) {
         if ((int) n.getAttribute(NODE_TYPE) == NODE_TYPE_CLOUD && n.getAttribute(longitudeLabel + "_gui") != null)
            nodeList.add(new NodeJson(n.getId(), n.getAttribute(longitudeLabel + "_gui"),
                  n.getAttribute(latitudeLabel + "_gui"), NODE_COLOR, n.getId(), NODE_SHAPE));
         else
            nodeList.add(new NodeJson(n.getId(), n.getAttribute(longitudeLabel), n.getAttribute(latitudeLabel),
                  NODE_COLOR, n.getId(), NODE_SHAPE));
      }
      for (Server s : pm.getServers()) {
         if ((int) s.getParent().getAttribute(NODE_TYPE) == NODE_TYPE_CLOUD
               && s.getParent().getAttribute(longitudeLabel + "_gui") != null)
            serverJsonMap.put(s.getId(), new ServerJson(s.getId(), s.getParent().getAttribute(longitudeLabel + "_gui"),
                  s.getParent().getAttribute(latitudeLabel + "_gui"), SERVER_COLOR, s.getId()));
         else
            serverJsonMap.put(s.getId(), new ServerJson(s.getId(), s.getParent().getAttribute(longitudeLabel),
                  s.getParent().getAttribute(latitudeLabel), SERVER_COLOR, s.getId()));
      }
      for (Edge e : pm.getLinks()) {
         String color = LINK_COLOR;
         if ((int) e.getSourceNode().getAttribute(NODE_TYPE) == NODE_TYPE_CLOUD
               || (int) e.getTargetNode().getAttribute(NODE_TYPE) == NODE_TYPE_CLOUD)
            color = LINK_CLOUD_COLOR;
         linkJsonMap.put(e.getId(),
               new LinkJson(e.getId(), e.getSourceNode().getId(), e.getTargetNode().getId(), "", color));
      }
   }

   public static void updateResults(Results results) {
      if (results != null) {
         updateServers(getServerJsonResults(results));
         updateLinks(getLinkJsonResults(results));
         ResultsGUI.results = results;
      }
   }

   private static void updateServers(List<ServerJson> serverJsonResults) {
      for (ServerJson serverJson : serverJsonResults)
         serverJsonMap.put(serverJson.getData().getId(), serverJson);
   }

   private static void updateLinks(List<LinkJson> linkJsonResults) {
      for (LinkJson linkJson : linkJsonResults)
         linkJsonMap.replace(linkJson.getData().getId(), linkJson);
   }

   public static void log(String message) {
      if (messages.isEmpty())
         messages.add(message);
      else {
         if (!messages.peekLast().equals(message))
            messages.add(message);
      }
   }

   static List<ServerJson> getServerJsonResults(Results results) {
      Map<Server, String> functions = generateFunctionsPerServerStringMap(results);
      List<ServerJson> serverJsonList = new ArrayList<>();
      Iterator<Entry<Server, Double>> entries = results.serverUtilizationMap().entrySet().iterator();
      DecimalFormat df = new DecimalFormat("#.##");
      while (entries.hasNext()) {
         Map.Entry<Server, Double> thisEntry = (Map.Entry<Server, Double>) entries.next();
         Double utilization = (Double) thisEntry.getValue();
         Server server = (Server) thisEntry.getKey();
         StringBuilder u = new StringBuilder();
         if (utilization != 0)
            u.append(df.format(utilization));
         serverJsonList.add(new ServerJson(server.getId(), server.getParent().getAttribute(longitudeLabel),
               server.getParent().getAttribute(latitudeLabel), getColor(utilization), u.toString()));
      }
      return serverJsonList;
   }

   static List<LinkJson> getLinkJsonResults(Results results) {
      List<LinkJson> linkJsonList = new ArrayList<>();
      Iterator<Entry<Edge, Double>> entries = results.linkUtilizationMap().entrySet().iterator();
      DecimalFormat df = new DecimalFormat("#.##");
      while (entries.hasNext()) {
         Map.Entry<Edge, Double> thisEntry = (Map.Entry<Edge, Double>) entries.next();
         Double value = (Double) thisEntry.getValue();
         Edge edge = (Edge) thisEntry.getKey();
         String label = "";
         if (value != 0)
            label = df.format(value);
         linkJsonList.add(new LinkJson(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId(), label,
               getColor(value)));
      }
      return linkJsonList;
   }

   private static Map<Server, String> generateFunctionsPerServerStringMap(Results results) {
      boolean[][][] pXSV = (boolean[][][]) results.getRawVariables().get("fXSV");
      Map<Server, String> functionsStringMap = new HashMap<>();
      for (int x = 0; x < results.getPm().getServers().size(); x++) {
         StringBuilder stringBuilder = new StringBuilder();
         for (int s = 0; s < results.getPm().getServices().size(); s++)
            for (int v = 0; v < results.getPm().getServices().get(s).getFunctions().size(); v++)
               if (pXSV[x][s][v])
                  stringBuilder.append("s").append(results.getPm().getServices().get(s).getId()).append("v").append(v)
                        .append("\n");
         functionsStringMap.put(results.getPm().getServers().get(x), stringBuilder.toString());
      }
      return functionsStringMap;
   }

   private static String getColor(Double utilization) {
      String[] colors = { "Gray", "LightGray", "MediumSeaGreen", "ForestGreen", "Gold", "GoldenRod", "Orange", "Tomato",
            "OrangeRed", "Indigo" };
      double[] gaps = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };

      if (utilization <= gaps[0])
         return colors[0];

      for (int i = 1; i < gaps.length; i++)
         if (utilization > gaps[i - 1] && utilization <= gaps[i])
            return colors[i];

      return null;
   }

   public static Results getResults() {
      return results;
   }

   public static LinkedList<String> getMessages() {
      return messages;
   }

   public static List<NodeJson> getNodeList() {
      return nodeList;
   }

   public static Map<String, ServerJson> getServerJsonMap() {
      return serverJsonMap;
   }

   public static Map<String, LinkJson> getLinkJsonMap() {
      return linkJsonMap;
   }
}
