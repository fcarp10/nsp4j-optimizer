package launcher;

import gui.WebApp;

import static spark.Spark.*;

public class App {

    public static String configFile;

    public static void main(String[] args) {
        configFile = "config.yml";
        if (args.length > 0)
            configFile = args[0];
        port(8080);
        staticFiles.location("/public");
        init();
        new WebApp();
    }
}