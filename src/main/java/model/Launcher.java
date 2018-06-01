package model;

import filemanager.ConfigFiles;
import filemanager.InputParameters;
import gui.WebApp;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    public static boolean debugging;

    public static void main(String[] args) throws GRBException {

        if (debugging)
            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");

        log.info("Initializing NFV framework");
        InputParameters inputParameters = ConfigFiles.readInputParameters("/config.yml");
        inputParameters.initializeParameters();
        new Thread(() -> WebApp.main(new String[]{})).start();
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startLinkOptimization(inputParameters);
    }

    private static void startLinkOptimization(InputParameters ip) throws GRBException {

        for (int numOfReplicas = ip.getMinReplicas(); numOfReplicas < ip.getMaxReplicas() + 1; numOfReplicas++) {
            ModelParameters modelParameters = new ModelParameters(ip);
            modelParameters.initializeVariables();
            Model model = new Model(modelParameters);
            GRBLinExpr expr = new GRBLinExpr();
            expr.add(model.getExprLinkCosts());
            model.setObjectiveFunction(expr);
            new ModelConstraints(modelParameters, numOfReplicas);
            model.run(numOfReplicas);
        }
    }
}
