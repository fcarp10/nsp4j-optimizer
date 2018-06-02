package model;

import filemanager.InputParameters;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelLauncher {

    private static final Logger log = LoggerFactory.getLogger(ModelLauncher.class);

    public static void startLinkOptimization(InputParameters ip) throws GRBException {
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
