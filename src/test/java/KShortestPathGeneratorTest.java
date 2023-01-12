import org.graphstream.graph.Graph;
import org.junit.Test;
import optimizer.utils.ConfigFiles;
import optimizer.utils.GraphManager;
import optimizer.utils.KShortestPathGenerator;

import java.io.File;
import java.net.URISyntaxException;

public class KShortestPathGeneratorTest {

   @Test
   public void inputParameters() throws URISyntaxException {

      final String graphName = "example";
      final String extensionGraph = ".dgs";
      final boolean directedEdges = true;
      final boolean allNodesToCloud = false;
      int MAX_LENGTH = 10;

      String path = new File(ConfigFiles.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
            .getParent() + "/";

      Graph graph = GraphManager.importTopology(path + graphName + extensionGraph, directedEdges, allNodesToCloud);
      KShortestPathGenerator generator = new KShortestPathGenerator(path, graphName, graph, MAX_LENGTH);

      // - generate k-shortest paths from N to M
      generator.runAll(3); // num k-paths

      // - generate paths traversing intermediate node
      // generator.runTraversingIntermediateNode();

      // - generate paths from specific source nodes
      // String[] srcNodes = new String[] { "1" };
      // generator.runFromNode(srcNodes, 5);

      // - generate paths to specific destination nodes
      // String[] dstNodes = new String[] { "1", "2", "3", "4" };
      // generator.runFromNode(dstNodes, 5);

   }
}
