package optimizer.utils;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static optimizer.Definitions.NODE_CLOUD;

public class KShortestPathGenerator {

   private static Logger log = LoggerFactory.getLogger(KShortestPathGenerator.class);
   private WritePlainTextFile writePlainTextFile;
   private int maxLength;
   private Graph graph;

   public KShortestPathGenerator(String path, String fileName, Graph graph, int maxLength) {
      writePlainTextFile = new WritePlainTextFile(path, fileName, ".txt");
      this.graph = graph;
      this.maxLength = maxLength;
   }

   public void run(int numOfKPaths) {
      List<Node> nodes = new ArrayList<>(graph.getNodeSet());
      for (Node src : nodes)
         for (Node dst : nodes)
            runFromNtoM(src, dst, numOfKPaths);
   }

   public void runTraversingIntermediateNode() {
      List<Node> nodes = new ArrayList<>();
      List<Node> intermediateNodes = new ArrayList<>();

      for (Node node : graph.getNodeSet()) {
         if (node.getAttribute(NODE_CLOUD) == null)
            nodes.add(node);
         else
            intermediateNodes.add(node);
      }

      for (Node src : nodes)
         for (Node dst : nodes)
            if (!src.equals(dst)) {
               List<Path> paths = generatePaths(src, dst);
               for (Node n : intermediateNodes)
                  printKPathsTraversingNodeN(paths, 1, n);
            }
   }

   public void runFromAndToSpecificNode(String specificNodeString, int numOfKPaths) {
      List<Node> nodes = new ArrayList<>(graph.getNodeSet());
      Node specificNode = null;
      for (Node n : nodes)
         if (n.getId().equals(specificNodeString))
            specificNode = n;

      if (specificNode != null)
         for (Node n : nodes)
            if (!n.equals(specificNode)) {
               List<Path> paths = generatePaths(n, specificNode);
               printKPaths(paths, numOfKPaths);
               paths = generatePaths(specificNode, n);
               printKPaths(paths, numOfKPaths);
            }
   }

   public void runFromNtoM(Node src, Node dst, int numOfKPaths) {
      if (!src.equals(dst)) {
         List<Path> paths = generatePaths(src, dst);
         printKPaths(paths, numOfKPaths);
      }
   }

   void printKPaths(List<Path> paths, int numOfPaths) {
      if (paths.size() == 0) {
         log.error("Not enough paths found, increase max_length or decrease number of k paths");
         System.exit(-1);
      }
      if (paths.size() < numOfPaths)
         log.warn("Not enough paths found, increase max_length or decrease number of k paths");
      for (int p = 0; p < numOfPaths; p++)
         if (p < paths.size())
            writePlainTextFile.write(paths.get(p) + System.getProperty("line.separator"));
   }

   void printKPathsTraversingNodeN(List<Path> paths, int numOfPaths, Node nodeN) {
      int printedPaths = 0;
      for (int p = 0; p < paths.size(); p++)
         if (printedPaths < numOfPaths) {
            if (paths.get(p).getNodePath().contains(nodeN)) {
               if (p < paths.size()) {
                  writePlainTextFile.write(paths.get(p) + System.getProperty("line.separator"));
                  printedPaths++;
               }
            }
         } else
            break;
      if (printedPaths < numOfPaths) {
         log.error("Not enough paths found, increase max_length or decrease number of k paths");
         System.exit(-1);
      }
   }

   private List<Path> generatePaths(Node src, Node dst) {
      log.info(src.getId() + " > " + dst.getId());
      PathCollection pathCollection = new PathCollection();

      pathCollection.generateSetOfPaths(src.getId(), dst.getId());
      pathCollection.orderPathsBySize();

      return pathCollection.getPaths();
   }

   private class PathCollection {

      private List<Path> paths = new ArrayList<>();
      private List<String> onPath = new ArrayList<>();
      private Stack<String> pathNodes = new Stack<>();

      void generateSetOfPaths(String srcNode, String dstNode) {
         pathNodes.push(srcNode);
         onPath.add(srcNode);
         if (!srcNode.equals(dstNode)) {
            for (Iterator<Node> it = graph.getNode(srcNode).getNeighborNodeIterator(); it.hasNext();) {
               Node currentNode = it.next();
               String currentNodeString = currentNode.getId();
               if (!onPath.contains(currentNodeString))
                  if (onPath.size() < maxLength)
                     generateSetOfPaths(currentNodeString, dstNode);
            }
         } else
            paths.add(generatePath(pathNodes));
         pathNodes.pop();
         onPath.remove(srcNode);
      }

      private Path generatePath(Stack<String> pathNodes) {
         ArrayList<Node> nodes = new ArrayList<>();
         ArrayList<Edge> edges = new ArrayList<>();
         for (int i = 0; i < pathNodes.size() - 1; i++) {
            Node node = graph.getNode(pathNodes.get(i));
            nodes.add(node);
            for (Edge edge : node.getEachEdge()) {
               if (edge.getOpposite(node).getId().equals(pathNodes.get(i + 1))) {
                  edges.add(edge);
                  break;
               }
            }
         }
         Path path = new Path();
         for (int n = 0; n < nodes.size(); n++)
            path.push(nodes.get(n), edges.get(n));
         return path;
      }

      void orderPathsBySize() {
         paths.sort(Comparator.comparingInt(Path::size));
      }

      public List<Path> getPaths() {
         return paths;
      }
   }
}
