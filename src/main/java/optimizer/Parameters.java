package optimizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import optimizer.elements.*;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import optimizer.utils.ConfigFiles;
import optimizer.utils.GraphManager;

import java.math.BigInteger;
import java.util.*;

import static optimizer.Definitions.*;

public class Parameters {

   private static final Logger log = LoggerFactory.getLogger(Parameters.class);
   // global parameters
   private Map<String, Object> global;
   // service definitions
   @JsonProperty("service_chains")
   private List<Service> serviceChains;
   // function definitions
   @JsonProperty("function_types")
   private List<Function> functionTypes;
   // traffic flow definitions
   @JsonProperty("traffic_flows")
   private List<TrafficFlow> trafficFlows;
   // local parameters
   private Graph graph;
   private List<Node> nodes;
   private List<Edge> edges;
   private List<Server> servers;
   private List<Service> services;
   private List<Path> paths;
   private int pathsTrafficFlow;
   private int demandsTrafficFlow;
   private int serviceLength;
   private double totalTraffic;
   private int totalNumFunctions;
   private List<Long> seeds;
   private int seedCounter;
   private String graphName;
   private Random rnd;

   public Parameters() {
      nodes = new ArrayList<>();
      edges = new ArrayList<>();
      servers = new ArrayList<>();
      services = new ArrayList<>();
      paths = new ArrayList<>();
      trafficFlows = new ArrayList<>();
      functionTypes = new ArrayList<>();
      serviceChains = new ArrayList<>();
      global = new HashMap<String, Object>();
   }

   /**
    * Calculate calculateDistance between two points in latitude and longitude
    * taking into account height difference. If you are not interested in height
    * difference pass 0.0. Uses Haversine method as its base.
    * <p>
    * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters el2
    * End altitude in meters
    *
    * @returns Distance in Meters
    */
   public static double calculateDistance(double lat1, double lat2, double lon1, double lon2) {
      final int earthRadius = 6371;
      double latDistance = Math.toRadians(lat2 - lat1);
      double lonDistance = Math.toRadians(lon2 - lon1);
      double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
            * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
      double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      double distance = earthRadius * c * 1000;
      distance = Math.pow(distance, 2);
      return Math.sqrt(distance);
   }

   public boolean initialize(String topologyFile, String pathsFile, boolean directedEdges) {
      readSeeds();
      rnd = new Random(getSeed());
      new GraphManager();
      boolean allNodesToCloud = false;
      if (global.containsKey(ALL_NODES_TO_CLOUD))
         allNodesToCloud = (boolean) global.get(ALL_NODES_TO_CLOUD);
      // Always compute ISS unless "compute_iss" is set to false in the yml file
      if (!global.containsKey(COMPUTE_ISS)){
         global.put(COMPUTE_ISS, true);
      }
      graph = GraphManager.importTopology(topologyFile, directedEdges, allNodesToCloud);
      paths = GraphManager.importPaths(graph, pathsFile);
      try {
         nodes.addAll(graph.getNodeSet());
         edges.addAll(graph.getEdgeSet());
         checkNodeType();
         setLinkParameters();
         generateServers();
         generateTrafficFlows();
         createSetOfServices();
         calculateAuxiliaryValues();
         return true;
      } catch (Exception e) {
         log.warn("error generating parameters: " + e.toString());
         return false;
      }
   }

   private void checkNodeType() {
      for (Node node : nodes)
         if (!node.hasAttribute(NODE_TYPE))
            node.addAttribute(NODE_TYPE, NODE_TYPE_UNKNOWN);
   }

   private void setLinkParameters() {

      String longitudeLabel, latitudeLabel;
      if (edges.get(0).getSourceNode().getAttribute(LONGITUDE_LABEL_1) != null) {
         longitudeLabel = LONGITUDE_LABEL_1;
         latitudeLabel = LATITUDE_LABEL_1;
      } else {
         longitudeLabel = LONGITUDE_LABEL_2;
         latitudeLabel = LATITUDE_LABEL_2;
      }

      for (Edge edge : edges) {
         if ((int) edge.getSourceNode().getAttribute(NODE_TYPE) == NODE_TYPE_CLOUD
               || (int) edge.getTargetNode().getAttribute(NODE_TYPE) == NODE_TYPE_CLOUD)
            edge.setAttribute(LINK_CLOUD, true);
         if (edge.getAttribute(LINK_CAPACITY) == null) {
            edge.addAttribute(LINK_CAPACITY, (int) global.get(LINK_CAPACITY_DEFAULT));
         }
         if (edge.getAttribute(LINK_DELAY) == null) {
            double nLon = edge.getSourceNode().getAttribute(longitudeLabel);
            double nLat = edge.getSourceNode().getAttribute(latitudeLabel);
            double mLon = edge.getTargetNode().getAttribute(longitudeLabel);
            double mLat = edge.getTargetNode().getAttribute(latitudeLabel);
            double distance = calculateDistance(nLat, mLat, nLon, mLon) / 1000; // convert m to km
            edge.addAttribute(LINK_DISTANCE, distance);
            double delay = distance / 200000; // in sec
            edge.addAttribute(LINK_DELAY, delay);
         }
      }
   }

   private void generateServers() {
      for (Node node : nodes) {
         if (node.getAttribute(NODE_NUM_SERVERS) == null)
            node.addAttribute(NODE_NUM_SERVERS, (int) global.get(NODE_NUM_SERVERS));
         for (int s = 0; s < (int) node.getAttribute(NODE_NUM_SERVERS); s++) {
            if (node.getAttribute(SERVER_CAPACITY) == null)
               node.addAttribute(SERVER_CAPACITY, (int) global.get(SERVER_CAPACITY));

            servers.add(new Server(node.getId() + "_" + s, node, node.getAttribute(SERVER_CAPACITY)));
         }
      }
   }

   private void generateTrafficFlows() {
      TrafficFlow dtf = trafficFlows.get(0);
      if (dtf.getSrc() == null && dtf.getDst() == null) { // traffic flows are not specified
         for (Node src : nodes)
            for (Node dst : nodes) {
               if (src == dst)
                  continue;
               if ((int) src.getAttribute(NODE_TYPE) == NODE_TYPE_CLOUD
                     || (int) dst.getAttribute(NODE_TYPE) == NODE_TYPE_CLOUD)
                  continue;
               TrafficFlow trafficFlow = new TrafficFlow(src.getId(), dst.getId(), dtf.getServices(),
                     dtf.getServiceLength());
               trafficFlow.generateRandomDemandsFromSpecificValues(rnd, dtf.getMinDem(), dtf.getMaxDem(),
                     dtf.getMinBw(), dtf.getMaxBw());
               for (Path p : paths)
                  if (p.getNodePath().get(0).getId().equals(src.getId())
                        && p.getNodePath().get(p.size() - 1).getId().equals(dst.getId()))
                     trafficFlow.setAdmissiblePath(p);
               trafficFlows.add(trafficFlow);
            }
         trafficFlows.remove(0); // remove default traffic flow

      } else // traffic flows are specified
         for (TrafficFlow trafficFlow : trafficFlows) {
            if (trafficFlow.getDemandsSpecific() == null)
               trafficFlow.generateRandomDemands(rnd); // if traffic demands are not specified
            else { // if they are specified, the just initialize
               trafficFlow.generateDemands();
            }
            for (Path p : paths)
               if (p.getNodePath().get(0).getId().equals(trafficFlow.getSrc())
                     && p.getNodePath().get(p.size() - 1).getId().equals(trafficFlow.getDst()))
                  trafficFlow.setAdmissiblePath(p);
         }
   }

   private void createSetOfServices() {
      for (TrafficFlow trafficFlow : trafficFlows) {
         int[] servicesArray = null;
         if (trafficFlow.getServices() != null)
            servicesArray = trafficFlow.getServices();
         else {
            ArrayList<Integer> list = (ArrayList<Integer>) global.get(SERVICES);
            servicesArray = new int[list.size()];
            for (int i = 0; i < list.size(); i++)
               servicesArray[i] = list.get(i);
         }
         int rndService = rnd.nextInt(servicesArray.length);
         int serviceId = servicesArray[rndService];
         Service service = createServiceChain(serviceId);
         List<Function> functions = new ArrayList<>();
         int[] serviceLengthArray = new int[] { 0 };
         if (trafficFlow.getServiceLength() != null)
            serviceLengthArray = trafficFlow.getServiceLength();
         else if (global.containsKey(SERVICE_LENGTH)) {
            ArrayList<Integer> list = (ArrayList<Integer>) global.get(SERVICE_LENGTH);
            serviceLengthArray = new int[list.size()];
            for (int i = 0; i < list.size(); i++)
               serviceLengthArray[i] = list.get(i);
         }
         int serviceLength = serviceLengthArray[rndService];
         int[] chain;
         if (serviceLength > 0) {
            chain = new int[serviceLength];
            for (int i = 0; i < serviceLength; i++)
               chain[i] = service.getChain()[rnd.nextInt(service.getChain().length)];
         } else {
            chain = service.getChain();
         }
         for (Integer type : chain)
            functions.add(createFunction(type));
         StringBuilder id = new StringBuilder();
         for (Integer myInt : chain)
            id.append(myInt);
         services.add(new Service(id.toString(), service.getMaxDelay(), service.getMaxPropagationDelay(), functions,
               trafficFlow, service.getAttributes()));
      }
   }

   private Service createServiceChain(int id) {
      Service service = null;
      for (Service s : serviceChains)
         if (id == Integer.valueOf(s.getId())) {
            service = s;
            break;
         }
      return service;
   }

   private Function createFunction(int type) {
      Function function = new Function(type);
      for (Function fType : functionTypes)
         if (type == fType.getType()) {
            Iterator<String> it = fType.getAttributes().keySet().iterator();
            while (it.hasNext()) {
               String key = (String) it.next();
               Object values = fType.getAttributes().get(key);
               Object value;
               if (values instanceof ArrayList) {
                  List<Double> minMaxValues = (List<Double>) values;
                  Double minValue = minMaxValues.get(0);
                  Double maxValue = minMaxValues.get(1);
                  value = minValue + rnd.nextDouble() * (maxValue - minValue);
               } else
                  value = values;
               function.setAttribute(key, value);
            }
            break;
         }
      return function;
   }

   private void calculateAuxiliaryValues() {
      pathsTrafficFlow = 0;
      for (TrafficFlow trafficFlow : trafficFlows)
         if (trafficFlow.getPaths().size() > pathsTrafficFlow)
            pathsTrafficFlow = trafficFlow.getPaths().size();

      demandsTrafficFlow = 0;
      for (TrafficFlow trafficFlow : trafficFlows)
         if (trafficFlow.getDemands().size() > demandsTrafficFlow)
            demandsTrafficFlow = trafficFlow.getDemands().size();

      serviceLength = 0;
      for (Service service : services)
         if (service.getFunctions().size() > serviceLength)
            serviceLength = service.getFunctions().size();

      for (TrafficFlow trafficFlow : trafficFlows)
         for (int trafficDemand : trafficFlow.getDemands())
            totalTraffic += trafficDemand;

      totalNumFunctions = 0;
      for (Service service : services)
         totalNumFunctions += service.getFunctions().size();

   }

   private void readSeeds() {
      new ConfigFiles();
      Scanner scanner = ConfigFiles.scanPlainTextFileInResources("/aux_files/seeds.txt");
      seeds = new ArrayList<>();

      while (scanner.hasNextLine()) {
         String line = scanner.nextLine();
         if (!line.startsWith("#")) {
            line = line.replaceAll("\\s+", "");
            long seed = new BigInteger(line, 2).longValue();
            seeds.add(seed);
         }
      }
   }

   public Long getSeed() {
      seedCounter++;
      return seeds.get(seedCounter);
   }

   public Object getGlobal(String key) {
      return global.get(key);
   }

   public String getGraphName() {
      return graphName;
   }

   public void setGraphName(String graphName) {
      this.graphName = graphName;
   }

   public List<Service> getServices() {
      return services;
   }

   public List<Function> getFunctionTypes() {
      return functionTypes;
   }

   public void setServices(List<Service> services) {
      this.services = services;
   }

   public void setFunctionTypes(List<Function> functionTypes) {
      this.functionTypes = functionTypes;
   }

   public void setServiceChains(List<Service> serviceChains) {
      this.serviceChains = serviceChains;
   }

   public List<TrafficFlow> getTrafficFlows() {
      return trafficFlows;
   }

   public void setTrafficFlows(List<TrafficFlow> trafficFlows) {
      this.trafficFlows = trafficFlows;
   }

   public List<Path> getPaths() {
      return paths;
   }

   public List<Server> getServers() {
      return servers;
   }

   public List<Node> getNodes() {
      return nodes;
   }

   public List<Edge> getLinks() {
      return edges;
   }

   public int getPathsTrafficFlow() {
      return pathsTrafficFlow;
   }

   public int getDemandsTrafficFlow() {
      return demandsTrafficFlow;
   }

   public int getServiceLength() {
      return serviceLength;
   }

   public double getTotalTraffic() {
      return totalTraffic;
   }

   public int getTotalNumFunctions() {
      return totalNumFunctions;
   }

   public Map<String, Object> getGlobal() {
      return global;
   }
}
