package optimizer.utils;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.GraphParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import static optimizer.Definitions.NODE_CLOUD;

public class GraphManager {

   private static final Logger log = LoggerFactory.getLogger(GraphManager.class);

   public static Graph importTopology(String file, boolean directedEdges, boolean allNodesToCloud) {
      Graph graph = new DefaultGraph("graph");
      try {
         graph.read(file);
         if (!directedEdges) {
            Set<Edge> edges = new HashSet<>();
            edges.addAll(graph.getEdgeSet());
            for (Edge edge : edges) {
               String srcNodeString = edge.getSourceNode().getId();
               String dstNodeString = edge.getTargetNode().getId();
               graph.removeEdge(edge);
               graph.addEdge("e" + srcNodeString + dstNodeString + "-1", srcNodeString, dstNodeString, true);
               graph.addEdge("e" + dstNodeString + srcNodeString + "-2", dstNodeString, srcNodeString, true);
            }
         }
         if (allNodesToCloud) {
            Set<Node> nodes = new HashSet<>();
            nodes.addAll(graph.getNodeSet());
            for (Node cloudNode : nodes) {
               if (cloudNode.getAttribute(NODE_CLOUD) != null)
                  for (Node node : nodes) {
                     if (node.getAttribute(NODE_CLOUD) == null) {
                        graph.addEdge("e" + node.getId() + cloudNode.getId() + "-1", node.getId(), cloudNode.getId(),
                              true);
                        graph.addEdge("e" + cloudNode.getId() + node.getId() + "-2", cloudNode.getId(), node.getId(),
                              true);
                     }
                  }
            }
         }
      } catch (IOException e) {
         log.error(e.toString());
      } catch (GraphParseException e) {
         log.error("error reading topology file: " + e.toString());
      }
      return graph;
   }

   public static List<Path> importPaths(Graph graph, String file) {
      List<Path> paths = new ArrayList<>();
      FileInputStream stream = null;
      try {
         stream = new FileInputStream(file);
      } catch (FileNotFoundException e) {
         log.error("error reading the .txt path file: " + e.getMessage());
      }
      try {
         Scanner input = new Scanner(stream);
         while (input.hasNext()) {
            String sPath = input.nextLine();
            String[] pNodes = sPath.replaceAll("\\s+", "").replace("[", "").replace("]", "").split(",");
            Path path = new Path();
            for (int i = 0; i < pNodes.length - 1; i++) {
               Node node = graph.getNode(pNodes[i]);
               boolean foundTargetNode = false;
               for (Edge edge : node.getEachEdge()) {
                  if (edge.getTargetNode().getId().equals(pNodes[i + 1])) {
                     path.push(node, edge);
                     foundTargetNode = true;
                     break;
                  }
               }
               if (!foundTargetNode) {
                  log.error("error while creating paths, target node not found: " + pNodes[i + 1]);
                  System.exit(-1);
               }
            }
            paths.add(path);
         }
         input.close();
      } catch (Exception e) {
         log.error("format error .txt path file: " + e.toString());
      }
      return paths;
   }
}
