Optimization models
===================

This chapter defines the specific constraints for each model with the different objective functions.

Objective functions constraints
-------------------------------

Constraints OFC1 / OFC2
^^^^^^^^^^^^^^^^^^^^^^^


.. math::
    :nowrap:

        \begin{equation}  \qquad
	       \forall y_i \in Y = \{ y_0,y_1,....  \}: k_* \geq y_i \big( u_{*} \big)
	    \end{equation}

The method *setLinearCostFunctions* is, as the title said, defining the linear cost functions for both server (:math:`* = x`) and link utilization (:math:`* = e`) . The input parameters here are taken from the previous methods *setServerUtilizationExpr()* and *setLinkUtilizationExpr()*.

.. code-block:: java

    private void linearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {
        for (int l = 0; l < Auxiliary.costFunctions.getValues().size(); l++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(Auxiliary.costFunctions.getValues().get(l)[0], expr);
            expr2.addConstant(Auxiliary.costFunctions.getValues().get(l)[1]);
            model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar, "costFunctions");
        }
    }

The method is executed as follows. The loop

.. code-block:: java

        for (int l = 0; l < variables.linearCostFunctions.getValues().size(); l++) {

ensures that the following operations will be valid for all variables here defined as :math:`l`, :math:`l` being an element of a set of the considered variables :math:`L`.

The code lines

.. code-block:: java

            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(variables.linearCostFunctions.getValues().get(l)[0], expr);
            expr2.addConstant(variables.linearCostFunctions.getValues().get(l)[1]);

define a new expression *expr2* in which the results from *setLinkUtilizationExpr()* or from *setServerUtilizationExpr()* will be multiplied with a variable. A constant is then added to the product of that multiplication.

.. code-block:: java

            model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar, "setLinearCostFunctions");

then sets this new expression as less equal to a variable defined as the linear cost functions.

This result is roughly to be translated as 

.. math::
  :nowrap:

    \begin{equation}
	\forall \ell \in L: k_{\ast} \geq y \big( u_{\ast} \big); \quad y \big( u_{\ast} \big) = a \cdot u_{\ast} + b
    \end{equation}

All results will then be returned to *setLinearCostFunctions*.


Constrain OFC3 / OFC4
^^^^^^^^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation}
            \forall x \in \mathbb{X}: u_{x}   \leq  u_{max}
        \end{equation}


.. math::
    :nowrap:

        \begin{equation}
            \forall e \in \mathbb{E}: u_{e}   \leq  u_{max}
        \end{equation}




.. code-block:: java

    private void maxUtilization() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         model.getGrbModel().addConstr(vars.uX[x], GRB.LESS_EQUAL, vars.uMax, "");
      for (int l = 0; l < pm.getLinks().size(); l++)
         model.getGrbModel().addConstr(vars.uL[l], GRB.LESS_EQUAL, vars.uMax, "");
    }



Constrain IPC1
^^^^^^^^^^^^^^

.. math::
    :nowrap:

      \begin{equation}  \label{IPC1} \qquad
	    \forall x \in  \mathbb{X}, \forall s \in  \mathbb{S},  \forall v \in  \mathbb{V}_s:
	     F_{I_x}^{v,s} \leq  f_{x}^{v,s}
     \end{equation}


After the first stage of the optimization procedure, an initial optimization result is available, the variables are denoted as :math:`F_{I_x}^{v,s}` . The specific constraint *InitialPlacementAsConstrains* transfers this initial placement of functions to the second optimization stage. Correlating to the equation, this constraint is implemented as follows:

The code ensures that for all servers :math:`x` , that are element of a set of servers :math:`X` , for all services :math:`s` , that are element of a set of services :math:`S` and for all functions :math:`v` , that are element of a set of functions :math:`V_s`  for a service s, a variable :math:`f_x^{v,s}`  will be assigned as equal to 1, if the initial output :math:`F_{I_x}^{v,s}`  was equal to 1. Should that not be the case, :math:`f_x^{v,s}`  will behave like a binary variable, taking either 1 or 0 as a value.

This means that we simple have the condition

.. math::
  :nowrap:

      \begin{equation}
	\forall s \in S, \forall v \in {V_s}, \forall x \in X: F_{I_x}^{v,s} = 1 \Longrightarrow  f_{x}^{v,s} = 1
    \end{equation}

if the initial variable is equal to 1. The output of this method will be returned back to *InitialPlacementAsConstraints*.



.. code-block:: java

    private void initialPlacementAsConstraints(GRBModel initialModel) throws GRBException {
        if (initialModel != null) {
            for (int x = 0; x < pm.getServers().size(); x++)
                for (int s = 0; s < pm.getServices().size(); s++)
                    for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                        if (initialModel.getVarByName(Auxiliary.pXSV + "[" + x + "][" + s + "][" + v + "]").get(GRB.DoubleAttr.X) == 1.0)
                            model.getGrbModel().addConstr(vars.pXSV[x][s][v], GRB.EQUAL, 1, "initialPlacementAsConstraints");
        }
    }


Objective functions
-------------------

All objective functions are defined in the file *../src/main/java/lp/OptimizationModel*

Optimization selector
^^^^^^^^^^^^^^^^^^^^^

.. code-block:: java

   public void setObjectiveFunction(GRBLinExpr expr, boolean isMaximization) throws GRBException {
      if (!isMaximization)
         grbModel.setObjective(expr, GRB.MINIMIZE);
      else
         grbModel.setObjective(expr, GRB.MAXIMIZE);
   }


This very first method setObjectiveFunction() in this class will take whatever expressions are returned to it and decide whether they will be minimized or maximized. Therefore it will check the boolean isMaximization for a true or false. If the boolean is false the method will take whatever expression is returned by the following methods in this class and minimize the function it is given. If the boolean is false it will maximize whatever the following methods in this class will return to it.

The graphical interface allows to select a combination of different objective functions, which are in detail OF1; a combination of OF4 + OF5; or OF2 + OF3. The weighting factors are given by the input parameters. The next function shows how the tool chooses between the different possible optimization models.

.. code-block:: java

    private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel model, Scenario scenario, String obj) throws GRBException {
      GRBLinExpr expr = new GRBLinExpr();
      String[] weights = scenario.getWeights().split("-");
      double linksWeight = Double.valueOf(weights[0]) / pm.getLinks().size();
      double serversWeight = Double.valueOf(weights[1]) / pm.getServers().size();
      switch (obj) {
         case NUM_OF_SERVERS_OBJ:
            expr.add(model.usedServersExpr());
            break;
         case COSTS_OBJ:
            expr.add(model.linkCostsExpr(linksWeight));
            expr.add(model.serverCostsExpr(serversWeight));
            break;
         case UTILIZATION_OBJ:
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            break;
         case NUM_DEDICATED_FUNCTIONS_OBJ:
            expr.add(model.numDedicatedFunctionsExpr());
            break;
         case MAX_UTILIZATION_OBJ:
            expr.add(model.linkUtilizationExpr(linksWeight));
            expr.add(model.serverUtilizationExpr(serversWeight));
            expr.add(model.maxUtilizationExpr(Double.valueOf(weights[2])));
            break;
      }
      return expr;
    }


Objective OF1
^^^^^^^^^^^^^

.. math::
     :nowrap:

        \begin{equation} \label{OF1} \qquad
	         \sum_{x \in \mathbb{X}} f_x
        \end{equation}


.. code-block:: java

   public GRBLinExpr usedServersExpr() {
      GRBLinExpr expr = new GRBLinExpr();
      for (int x = 0; x < parameters.getServers().size(); x++)
         expr.addTerm(1.0, variables.pX[x]);
      return expr;
   }

The method usedServersExpr() first initiates a new expression expr, before implementing a summatory function over all servers x, that are element of a set of servers X, here displayed in a for-loop, for all variables :math:`f_x` . The results of this summatory are then returned.

The next few following methods are structured in a similar way and as is are also almost identical in coding.


Objective OF2
^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{OF2}
            W_1  \cdot \sum_{e \in \mathbb{E}}  u_e
        \end{equation}



In this method, linkUtilizationExpr(), also takes into account a function weight :math:`W_1` as input parameter *weight* . A new expression expr is installed before implementing a summatory function over all links e (index variable l), that are element of a set of links :math:`E` , for this expression. Hereby expr is defined as the link weight multiplied by the utilization variable :math:`u_e` . The summatory results are then returned.

.. code-block:: java

   public GRBLinExpr linkUtilizationExpr(double weight) {
      GRBLinExpr expr = new GRBLinExpr();
      for (int l = 0; l < parameters.getLinks().size(); l++)
         expr.addTerm(weight, variables.uL[l]);
      return expr;
   }



Objective OF3
^^^^^^^^^^^^^


.. math::
    :nowrap:

        \begin{equation} \label{OF3}
            W_2  \cdot \sum_{x \in \mathbb{X}} u_x
        \end{equation}


serverUtilizationExpr(), similarly to the others in consideration of the weight  :math:`W_2` , first instigates a new expression expr. It then implements a summatory function over all servers x, that are element of a set of servers :math:`X` , for said expression. This expression is then defines as the utilization weight multiplied by a utilization variable :math:`u_x`  for all used servers. All results of this summatory are then returned.


.. code-block:: java

   public GRBLinExpr serverUtilizationExpr(double weight) {
      GRBLinExpr expr = new GRBLinExpr();
      for (int x = 0; x < parameters.getServers().size(); x++)
         expr.addTerm(weight, variables.uX[x]);
      return expr;
   }




Objective OF4
^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{OF4} \qquad
	         W_1  \cdot \sum_{e \in \mathbb{E}}  k_e
        \end{equation}


Similarly to the previous method linkCostsExpr(), taking the weight  :math:`W_1` in consideration, first sets a new expression expr before installing a summatory function over all links e, that are an element of links :math:`E`, for the expression expr. The expression is defined as the link weight multiplied by the utilization cost variable :math:`k_e` depending on the links. All results of the summatory are then returned.

.. code-block:: java

   public GRBLinExpr linkCostsExpr(double weight) {
      GRBLinExpr expr = new GRBLinExpr();
      for (int l = 0; l < parameters.getLinks().size(); l++)
         expr.addTerm(weight, variables.kL[l]);
      return expr;
   }


Objective OF5
^^^^^^^^^^^^^


.. math::
    :nowrap:

        \begin{equation} \label{OF5} \qquad
	         W_2  \cdot \sum_{x \in \mathbb{X}} k_x
        \end{equation}

serverCostsExpr(), again taking the weight  :math:`W_2` in consideration, firsts sets a new expression expr and implements a summatory function over all servers x, that are an element of a set of a servers :math:`X`, for the expression expr. The expression is then defined as the server weight multiplied by the utilization cost variable :math:`k_x` for all the servers. The results are then returned.

.. code-block:: java

   public GRBLinExpr serverCostsExpr(double weight) {
      GRBLinExpr expr = new GRBLinExpr();
      for (int x = 0; x < parameters.getServers().size(); x++)
         expr.addTerm(weight, variables.kX[x]);
      return expr;
   }

Objective OF6
^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{Umax-objective}
             W_3 \cdot u_{max}
        \end{equation}


.. code-block:: java

    public GRBLinExpr maxUtilizationExpr(double weight) {
        GRBLinExpr expr = new GRBLinExpr();
        expr.addTerm(weight, variables.uMax);
        return expr;
    }




Objective OF7
^^^^^^^^^^^^^

Linear  cost function for the number of VNF instances  :math:`\hat{ \eta}^{v,s}_x`.

.. math::
    :nowrap:

        \begin{equation} \label{lincostVNF-objective}   \qquad
	     \sum_{x \in \mathbb{X}}     \sum_{s \in \mathbb{S}}    \sum_{v \in \mathbb{\mathbb{V}}_s}   \hat{ \eta}^{v,s}_x
        \end{equation}



.. code-block:: java

     public GRBLinExpr numDedicatedFunctionsExpr() {
        GRBLinExpr expr = new GRBLinExpr();
        for (int x = 0; x < parameters.getServers().size(); x++)
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                    expr.addTerm(1.0, variables.nXSV[x][s][v]);
        return expr;
     }