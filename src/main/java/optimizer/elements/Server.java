package optimizer.elements;

import org.graphstream.graph.Node;

public class Server {

   private Node parent;
   private String id;
   private int capacity;

   public Server(String id, Node parent, int capacity) {
      this.id = id;
      this.parent = parent;
      this.capacity = capacity;
   }

   public Node getParent() {
      return parent;
   }

   public String getId() {
      return id;
   }

   public int getCapacity() {
      return capacity;
   }

   public void setCapacity(int capacity) {
      this.capacity = capacity;
   }
}
