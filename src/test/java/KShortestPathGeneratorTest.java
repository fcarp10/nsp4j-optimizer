import app.App;
import filemanager.GraphManager;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import utils.KShortestPathGenerator;

public class KShortestPathGeneratorTest {

    @Test
    public void inputParameters() {
        final String TOPOLOGY = "test_scenario1";
        String path = FilenameUtils.getPath(App.class.getClassLoader().getResource(TOPOLOGY + ".yml").getFile());
        if (System.getProperty("os.name").equals("Mac OS X")) path = "/" + path;
        new GraphManager();
        GraphManager.importTopology(path, TOPOLOGY);
        KShortestPathGenerator kShortestPathGenerator = new KShortestPathGenerator(10, 5, path, TOPOLOGY);
        kShortestPathGenerator.run();
    }
}
