package optimizer.elements;

import java.util.HashMap;
import java.util.Map;

public class Function {

   private int type;
   private Map<String, Object> attributes;

   public Function() {
      attributes = new HashMap<>();
   }

   public Function(int type) {
      attributes = new HashMap<>();
      this.type = type;
   }

   public Object getAttribute(String key) {
      return attributes.get(key);
   }

   public void setAttribute(String key, Object attr) {
      attributes.put(key, attr);
   }

   public int getType() {
      return type;
   }

   public Map<String, Object> getAttributes() {
      return attributes;
   }
}
