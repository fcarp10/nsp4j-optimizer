package optimizer.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Service {

   private String id;
   private int[] chain;
   @JsonProperty("max_delay")
   private double maxDelay;
   @JsonProperty("max_propagation_delay")
   private double maxPropagationDelay;
   private transient List<Function> functions;
   private TrafficFlow trafficFlow;
   private Map<String, Object> attributes;

   public Service() {
      attributes = new HashMap<>();
   }

   public Service(String id, double maxDelay, double maxPropagationDelay, List<Function> functions,
         TrafficFlow trafficFlow, Map<String, Object> attributes) {
      this.id = id;
      this.maxDelay = maxDelay;
      this.maxPropagationDelay = maxPropagationDelay;
      this.functions = functions;
      this.trafficFlow = trafficFlow;
      this.attributes = attributes;
   }

   public Object getAttribute(String key) {
      return attributes.get(key);
   }

   public Map<String, Object> getAttributes() {
      return attributes;
   }

   public String getId() {
      return id;
   }

   public double getMaxDelay() {
      return maxDelay;
   }

   public double getMaxPropagationDelay() {
      return maxPropagationDelay;
   }

   public int[] getChain() {
      return chain;
   }

   public List<Function> getFunctions() {
      return functions;
   }

   public TrafficFlow getTrafficFlow() {
      return trafficFlow;
   }
}
