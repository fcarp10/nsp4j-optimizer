import filemanager.ConfigFiles;
import filemanager.InputParameters;
import gui.WebApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.staticFiles;

public class Launcher {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {

        InputParameters inputParameters = ConfigFiles.readInputParameters("/config.yml");
        inputParameters.initializeParameters();
        //port(8080);
        staticFiles.location("/public");
        new WebApp(inputParameters);
    }
}

