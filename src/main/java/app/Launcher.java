package app;

import results.Auxiliary;

import static spark.Spark.*;

public class Launcher {

    public static void main(String[] args) {
        //System.load("/opt/gurobi810/linux64/lib/libgurobi81.so");
        port(8080);
        staticFiles.location("/public");
        init();
        new Auxiliary();
        new WebServer();
    }
}