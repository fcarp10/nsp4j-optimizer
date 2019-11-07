package gui;

import gurobi.GRBModel;
import manager.Parameters;
import manager.elements.Server;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import output.Results;

import java.text.DecimalFormat;
import java.util.*;

import static output.Parameters.*;

public class ResultsGUI {

   private static List<NodeJson> nodeList;
   private static Map<String, ServerJson> serverJsonMap;
   private static Map<String, LinkJson> linkJsonMap;
   private static Results results;
   private static GRBModel initialModel;
   private static LinkedList<String> messages;

   public ResultsGUI() {
      nodeList = new ArrayList<>();
      serverJsonMap = new HashMap<>();
      linkJsonMap = new HashMap<>();
      messages = new LinkedList<>();
   }

   public static void initialize(Parameters parameters) {
      nodeList = new ArrayList<>();
      serverJsonMap = new HashMap<>();
      linkJsonMap = new HashMap<>();
      for (Node n : parameters.getNodes()) {
         String xAttr = "x", yAttr = "y";
         if (n.getAttribute(NODE_CLOUD) != null) {
            xAttr = "x_gui";
            yAttr = "y_gui";
         }
         nodeList.add(new NodeJson(n.getId(), n.getAttribute(xAttr), n.getAttribute(yAttr)
                 , NODE_COLOR, n.getId(), NODE_SHAPE));
      }
      for (Server s : parameters.getServers()) {
         String xAttr = "x", yAttr = "y";
         if (s.getParent().getAttribute(NODE_CLOUD) != null) {
            xAttr = "x_gui";
            yAttr = "y_gui";
         }
         serverJsonMap.put(s.getId(), new ServerJson(s.getId(), s.getParent().getAttribute(xAttr)
                 , s.getParent().getAttribute(yAttr), SERVER_COLOR, s.getId()));
      }
      for (Edge e : parameters.getLinks()) {
         String color = LINK_COLOR;
         if (e.getSourceNode().getAttribute(NODE_CLOUD) != null || e.getTargetNode().getAttribute(NODE_CLOUD) != null)
            color = LINK_CLOUD_COLOR;
         linkJsonMap.put(e.getId(), new LinkJson(e.getId(), e.getSourceNode().getId(), e.getTargetNode().getId()
                 , "", color));
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
      Iterator entries = results.serverUtilizationMap().entrySet().iterator();
      DecimalFormat df = new DecimalFormat("#.##");
      while (entries.hasNext()) {
         Map.Entry thisEntry = (Map.Entry) entries.next();
         Double utilization = (Double) thisEntry.getValue();
         Server server = (Server) thisEntry.getKey();
         StringBuilder u = new StringBuilder();
         if (utilization != 0) {
            u.append(df.format(utilization));
            if (functions.get(server).length() < 20)
               u.append("\n").append(functions.get(server));
         }
         serverJsonList.add(new ServerJson(server.getId(), server.getParent().getAttribute("x")
                 , server.getParent().getAttribute("y")
                 , getColor(utilization), u.toString()));
      }
      return serverJsonList;
   }

   static List<LinkJson> getLinkJsonResults(Results results) {
      List<LinkJson> linkJsonList = new ArrayList<>();
      Iterator entries = results.linkUtilizationMap().entrySet().iterator();
      DecimalFormat df = new DecimalFormat("#.##");
      while (entries.hasNext()) {
         Map.Entry thisEntry = (Map.Entry) entries.next();
         Double value = (Double) thisEntry.getValue();
         Edge edge = (Edge) thisEntry.getKey();
         String label = "";
         if (value != 0)
            label = df.format(value);
         linkJsonList.add(new LinkJson(edge.getId(), edge.getSourceNode().getId()
                 , edge.getTargetNode().getId(), label, getColor(value)));
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
                  stringBuilder.append("s").append(results.getPm().getServices().get(s).getId())
                          .append("v").append(v)
                          .append("\n");
         functionsStringMap.put(results.getPm().getServers().get(x), stringBuilder.toString());
      }
      return functionsStringMap;
   }

   private static String getColor(Double utilization) {
      String[] colors = {"Gray", "LightGray", "MediumSeaGreen", "ForestGreen", "Gold", "GoldenRod", "Orange", "Tomato", "OrangeRed", "Indigo"};
      double[] gaps = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

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

   static GRBModel getInitialModel() {
      return initialModel;
   }

   static void setInitialModel(GRBModel initialModel) {
      ResultsGUI.initialModel = initialModel;
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
