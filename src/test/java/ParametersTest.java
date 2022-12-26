import optimizer.Parameters;
import optimizer.elements.Function;
import optimizer.elements.Service;
import org.junit.Test;
import optimizer.utils.ConfigFiles;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static optimizer.Definitions.*;

public class ParametersTest {

   @Test
   public void parameters() throws URISyntaxException {

      final String graphName = "example";
      final String extensionGraph = ".dgs";
      final boolean directedEdges = true;
      String path = new File(ConfigFiles.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
            .getParent() + "/";

      Parameters pm = ConfigFiles.readParameters(path + graphName + ".yml");
      pm.initialize(path + graphName + extensionGraph, path + graphName + ".txt", directedEdges);
      assertNotNull(pm.getGraphName());
      assertNotNull(pm.getServers());
      for (Service s : pm.getServices()) {
         assertNotNull(s.getAttribute(SERVICE_MIN_PATHS));
         assertNotNull(s.getAttribute(SERVICE_MAX_PATHS));
         assertTrue(s.getMaxPropagationDelay() > 0);
         for (Function f : s.getFunctions()) {
            assertNotNull(f.getAttribute(FUNCTION_REPLICABLE));
            assertNotNull(f.getAttribute(FUNCTION_LOAD_RATIO));
            assertNotNull(f.getAttribute(FUNCTION_OVERHEAD_RATIO));
            assertNotNull(f.getAttribute(FUNCTION_PROCESS_TRAFFIC_DELAY));
            assertNotNull(f.getAttribute(FUNCTION_MAX_DELAY));
            assertNotNull(f.getAttribute(FUNCTION_MIN_PROCESS_DELAY));
            assertNotNull(f.getAttribute(FUNCTION_PROCESS_DELAY));
         }
         assertTrue(s.getTrafficFlow().getDemands().size() > 0);
         assertTrue(s.getTrafficFlow().getPaths().size() > 0);
      }
   }
}
