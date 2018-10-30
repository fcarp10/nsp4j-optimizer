***********
Constraints
***********

setLinkUtilizationExpr
======================

.. code-block:: java

    public void setLinkUtilizationExpr(boolean isMigration) throws GRBException {
        for (int l = 0; l < parameters.getLinks().size(); l++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {
                    if (!parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).contains(parameters.getLinks().get(l)))
                        continue;
                    for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm((double) parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                / (int) parameters.getLinks().get(l).getAttribute("capacity"), variables.tSPD[s][p][d]);
                }

            model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uL[l], "setLinkUtilizationExpr");
            setLinearCostFunctions(expr, variables.ukL[l]);
        }
    }

.. math::
  :nowrap:

    \begin{equation}
    \forall \ell \in L: u_{\ell}  =  \frac{1}{C_\ell} \sum_{s \in  S} \sum_{\lambda \in  \Lambda_s} \sum_{p \in P_s} \lambda \cdot  t_{p}^{\lambda,s}  \cdot T_{p}^\ell  \leq 1
    \end{equation}

The first constraint we look at in the code is *setLinkUtilizationExpr()*, which correlates to equation (6) in the paper, meant to check if a link is utilized in consideration of the paths that might traverse the link, the bandwidth of the traffic demand :math:`\lambda` and the maximum capacity of the link.

The method itself is performed as followed:

The first loop

.. code-block:: java

        for (int l = 0; l < parameters.getLinks().size(); l++) {

makes sure that all links :math:`l`, element of the set of link, are to be considered when executing the following operations.

            Starting a new expression with

.. code-block:: java

            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {

the loops then express the summatories over all service chains :math:`s` , element of the set of service chains :math:`S` and all paths :math:`p` , element of the set of admissible paths :math:`P_s` for the service chain :math:`s`.

            The subsequent operation

.. code-block:: java

                    if (!parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).contains(parameters.getLinks().get(l)))
                        continue;

makes sure that the operation will only continue if the current service chain s and the currently used path p contain the link :math:`l` we are looking at. If that is not the case the operation will end here. 
            In the mathematical model this is portrayed by the parameter :math:`T_{p}^\ell`, that will enter the equation as multiplier by :math:`1` , if the link :math:`l` is used by path p and service chain :math:`s` , or by :math:`0`, if it is not. In case of a multiplication with :math:`0` , the whole equation will equal :math:`0` and the observed link will not be utilized.

On the other hand, if the parameter :math:`T_{p}^\ell` equals :math:`1`, the following will be executed:

.. code-block:: java

                    for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm((double) parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                / (int) parameters.getLinks().get(l).getAttribute("capacity"), variables.tSPD[s][p][d]);
                }

Taking the sum over all traffic demands :math:`\lambda` , that are element of a set of traffic demands :math:`\lambda_s` for a service :math:`s`, the demand :math:`\lambda` will be divided by the link capacity :math:`C_l` and multiplied with the variable :math:`t_{p}^{\lambda,s}`.

The total operation could be interpreted by the equation (6) on the paper.

With

.. code-block:: java

            model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uL[l], "setLinkUtilizationExpr");
            setLinearCostFunctions(expr, variables.ukL[l]);

defining the equation as the link utilization :math:`u_l`, returning the results to *setLinkUtilizationExpr()* and sending them to the method setLinearCostFunctions for further computing.


setServerUtilizationExpr
========================
.. code-block:: java

	private void setServerUtilizationExpr() throws GRBException {                                                       
	    for (int x = 0; x < parameters.getServers().size(); x++) {                                                      
	        GRBLinExpr expr = new GRBLinExpr();
	        for (int s = 0; s < parameters.getServices().size(); s++)
	            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {                       
	                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) { 
	                    expr.addTerm((parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
	                                    * parameters.getServices().get(s).getFunctions().get(v).getLoad())
	                                    / parameters.getServers().get(x).getCapacity()
	                            , variables.fXSVD[x][s][v][d]);
	                }
	            }
	        model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uX[x], "setServerUtilizationExpr");
	        setLinearCostFunctions(expr, variables.ukX[x]);
	    }
	}

.. math::
  :nowrap:

    \begin{equation}
    \forall x \in X: u_{x}  = \sum_{s \in S} \sum_{v \in V_s}  u_{x}^{v,s}  + \bigg[E_r \cdot u_{x}^{v,s} + \frac{f_{x}^{v,s}}{C_x  E_r} \bigg] \leq 1
	\end{equation}

.. math::
  :nowrap:

    \begin{equation}
	\forall s \in  S, \forall v \in V_s, \forall x \in X:  u_{x}^{v,s} = \sum_{\lambda \in \Lambda_s}  \frac{\lambda \cdot f_{x,\lambda}^{v,s} \cdot L_v}{C_x}
	\end{equation}

Similar to the previous constraint *setServerUtilizationExpr()* is an operation, that is supposed to check the utilization of a server within the service in consideration of the bandwidth of the traffic demands, the load ratio of the VNF functions and the maximum capacity of the server.
This method, corresponding to the equations (7) + (8), is running as followed:

Similar to the previous method, the loop

.. code-block:: java

        for (int x = 0; x < parameters.getServers().size(); x++)

ensures that the following operations will be valid and executed for all servers x, element of the set of servers X in the network.

Following loops

.. code-block:: java

            for (int s = 0; s < parameters.getServices().size(); s++)
	            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {                       
	                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) { 


all translate to summatories over all service chains :math:`s`, element of the set of service chains :math:`S`, over all VNF functions :math:`v`, element of the ordered set of VNFs :math:`V_s` in service chain :math:`s`, and over all traffic demands :math:`\lambda`, that are element of the set of demands :math:`\Lambda_s` of service chain :math:`s`.

            The subsequent commands

.. code-block:: java

                        expr.addTerm((parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
	                                    * parameters.getServices().get(s).getFunctions().get(v).getLoad())
	                                    / parameters.getServers().get(x).getCapacity()
	                            , variables.fXSVD[x][s][v][d]);

are to be interpreted as a demand :math:`\lambda`, determined by the previous loop, will be multiplied with a load ratio :math:`L_v`, influenced by the current VNF function :math:`v`. The resulting product will be divided by the maximum server capacity :math:`C_x` and multiplied with the parameter :math:`f_{x,\lambda}^{v,s}`.

A possible way to summarize this operation would be 

.. math::
  :nowrap:

    \begin{equation}
    \forall x \in X: u_{x}  = \sum_{s \in S} \sum_{v \in V_s} \sum_{\lambda \in \Lambda_s} \frac{\lambda \cdot f_{x,\lambda}^{v,s} \cdot L_v}{C_x}
    \end{equation}

With the following lines

.. code-block:: java

            model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uX[x], "setServerUtilizationExpr");
	        setLinearCostFunctions(expr, variables.ukX[x]);

defining the previous equation as the server utilization :math:`u_x`, it is then returning the results to *setServerUtilizationExpr()* and sending them to the method *setLinearCostFunctions* for further computing.



COMMENT: overhead still missing in the code?



setLinearCostFunctions
======================

.. code-block:: java

    public void setLinearCostFunctions(GRBLinExpr expr, GRBVar grbVar) throws GRBException {
        for (int l = 0; l < variables.linearCostFunctions.getValues().size(); l++) {
            GRBLinExpr expr2 = new GRBLinExpr();
            expr2.multAdd(variables.linearCostFunctions.getValues().get(l)[0], expr);
            expr2.addConstant(variables.linearCostFunctions.getValues().get(l)[1]);
            model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, grbVar, "setLinearCostFunctions");
        }
    }

.. math::
  :nowrap:

    \begin{equation}
	\forall n \in N, \forall y \in Y: k_x \geq y \big( =u_{x} \big)
	\end{equation}

.. math::
  :nowrap:

    \begin{equation}
	\forall \ell \in L, \forall y \in Y: k_{\ell} \geq y \big( u_{\ell} \big)
	\end{equation}

This method *setLinearCostFunctions* is, as the title said, defining the linear cost functions for both server and link utilization. The input parameters here are taken from the previous methods *setServerUtilizationExpr()* and *setLinkUtilizationExpr()*. Correlating to the equations (4) and (5) from the paper, the method is executed as follows:

The loop

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
	\forall \ell \in L: k_{\ast} \geq y \big( u_{\ast} \big)
	y \big( u_{\ast} \big) = a \cdot u_{\ast} + b
    u_{\ast} \in { u_l , u_x }
    k_{\ast} \in { k_l , k_x }
    \end{equation}

All results will then be returned to *setLinearCostFunctions*.


countNumberOfUsedServers
========================

.. code-block:: java

    public void countNumberOfUsedServers() throws GRBException {
        for (int x = 0; x < parameters.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
                    expr.addTerm(1.0 / parameters.getTotalNumberOfFunctionsAux(), variables.fXSV[x][s][v]);
                    expr2.addTerm(1.0, variables.fXSV[x][s][v]);
                }
            model.getGrbModel().addConstr(variables.fX[x], GRB.GREATER_EQUAL, expr, "countNumberOfUsedServers");
            model.getGrbModel().addConstr(variables.fX[x], GRB.LESS_EQUAL, expr2, "countNumberOfUsedServers");
        }
    }

.. math::
  :nowrap:

    \begin{equation}
	\forall x \in X: \frac{1}{|V|} \sum_{s \in S} \sum_{v \in V_s} f_{x}^{v,s} \leq  f_x 
	\end{equation}


This next method *countNumberOfUsedServers* basically counts all servers that are used for all the VNF functions for all service chains in relation to the total number of servers. Equivalent to equation (2) from the paper, this method is running as followed:

The for-loop

.. code-block:: java

        for (int x = 0; x < parameters.getServers().size(); x++) {

makes sure, that for all servers :math:`x`, element of the the set of servers :math:`X` in the network will be regarded in the following operation.

All subsequent loops

.. code-block:: java

            GRBLinExpr expr2 = new GRBLinExpr();
            for (int s = 0; s < parameters.getServices().size(); s++)
                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)

are to be translated as summatories over all service chains :math:`s`, element of the set of service chains :math:`S` and over all VNF functions :math:`v`, element of a ordered set of functions :math:`V_s`  for the service chain :math:`s`, for the following expression

.. code-block:: java

                    expr.addTerm(1.0 / parameters.getTotalNumberOfFunctionsAux(), variables.fXSV[x][s][v]);

which describes a division of :math:`1` by the total number of VNF functions, multiplied with the variable :math:`f_{x}^{v,s}`.

Following up

.. code-block:: java

            model.getGrbModel().addConstr(variables.fX[x], GRB.GREATER_EQUAL, expr, "countNumberOfUsedServers");

sets a new variable :math:`f_x` as greater equal to the term defined in the previous expression. 
This result will then be returned again as *countNumberOfUsedServers*.

A resulting equation describing this operation could be

.. math::
  :nowrap:

    \begin{equation}
	\forall x \in X:  \sum_{s \in S} \sum_{v \in V_s} \frac{1}{|V|} f_{x}^{v,s} \leq  f_x
	\end{equation}


onePathPerDemand
================

.. code-block:: java

	private void onePathPerDemand() throws GRBException {
	    for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
	            GRBLinExpr expr = new GRBLinExpr();

.. math::
  :nowrap:

    \begin{equation} \label{onePathPerDemand}
	\forall s \in  S, \forall \lambda \in  \Lambda_s: \sum_{p \in P_s} t_{p}^{\lambda,s} = 1
	\end{equation}

This constraint we look at will limit the number of paths used for each traffic demand to 1 and is executed by the method onePathPerDemand. It runs as follows:

The first two *for loops* ensure that for all service chains :math:`s`, element of a set of service chains :math:`S`, and for all traffic demands :math:`\lambda`, an element of a set of demands :math:`\Lambda_s`  for the service chain :math:`s`, the following operations will be valid.

            The following code forces to 1 the summatory of all the traffic demands :math:`\lambda` of service *s* using the path *p* in order to ensure that each traffic demand only uses one path.

.. code-block:: java

                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
	                expr.addTerm(1.0, variables.tSPD[s][p][d]);
	            model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "onePathPerDemand");
	        }
    	}


activatePathForService
======================

.. math::
  :nowrap:

    \begin{equation} \label{activatePathForService}
	\forall s \in  S, \forall \lambda \in  \Lambda_s, \forall p \in P_s : t_{p}^{\lambda, s} \leq t_{p}^{s} \leq \sum_{\lambda' \in \Lambda_s} t_{p}^{\lambda', s}
	\end{equation}


The method *activePathForService* is meant to ensure that when a traffic demand :math:`\lambda` is using a path :math:`p`, said path will be activated for the corresponding service :math:`s`. Following the equation, this method is executed as follows:

The first block 

.. code-block:: java
	
	private void activatePathForService() throws GRBException {
	    for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
	            for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
	                model.getGrbModel().addConstr(variables.tSPD[s][p][d], GRB.LESS_EQUAL, variables.tSP[s][p], "activatePathForService");

ensures that for all service chains :math:`s`, an element of a set of service chains :math:`S`, for all paths :math:`p`, element of a set of admissable paths :math:`P_s`  for a service :math:`s`, and for all demands :math:`\lambda`, element of a set of traffic demands :math:`\Lambda_s`  for a service :math:`s`, a variable :math:`t_{p}^{\lambda', s}` is less equal to a variable :math:`t_{p}^{s}`. 

The results are then returned to activePathForService.

This correlation can be portrayed in a formula as such

.. math::
  :nowrap:

    \begin{equation}
	\forall s \in  S, \forall \lambda \in  \Lambda_s, \forall p \in P_s : t_{p}^{\lambda, s} \leq t_{p}^{s}
	\end{equation}


The second block 

.. code-block:: java

        for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++) {


starts ensuring that for all services :math:`s`, element of a set of service chains :math:`S`, and for all paths :math:`p`, element of a set of admissible paths :math:`P_s`  for a service :math:`s`, the following operations are valid.

            Then it express a summatory function over all demands :math:`\lambda`, that are an element of a set of traffic demands :math:`\Lambda_s` for a certain service :math:`s`, for a function :math:`t_{p}^{\lambda, s}`. This summatory function is then defined as greater equal than a variable :math:`t_{p}^{s}`, also defined as mentioned earlier, and then likewise returned to *activePathForService*.

.. code-block:: java

                GRBLinExpr expr = new GRBLinExpr();
	            for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
	                expr.addTerm(1.0, variables.tSPD[s][p][d]);
	            model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.tSP[s][p], "activatePathForService");
	        }
	}



This block can also be expressed as


.. math::
  :nowrap:

    \begin{equation}
	\forall s \in  S, \forall p \in P_s :  t_{p}^{s} \leq \sum_{\lambda' \in \Lambda_s} t_{p}^{\lambda', s}
	\end{equation}


To summarize both blocks of commands into one formula, we can simply interpret them as an inequation, with :math:`t_{p}^{s}` acting like the connecting link, resulting on the shown manager formula stated above.


pathConstrainedByFunctions
==========================


.. code-block:: java

	private void pathsConstrainedByFunctions() throws GRBException {

	    for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
	            GRBLinExpr expr = new GRBLinExpr();
	            for (int x = 0; x < parameters.getServers().size(); x++)
	                expr.addTerm(1.0, variables.fXSV[x][s][v]);
	            if (parameters.getServices().get(s).getFunctions().get(v).isReplicable()) {
	                GRBLinExpr expr2 = new GRBLinExpr();
	                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
	                    expr2.addTerm(1.0, variables.tSP[s][p]);
	                model.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, "pathsConstrainedByFunctions");
	            } else
	                model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "pathsConstrainedByFunctions");
	        }
	}

.. math::
  :nowrap:

    \begin{equation} \label{pathsConstrainedByFunctions}
	\forall s \in S, \forall v \in V_s:  \sum_{x \in X} f_x^{v,s} \leq F_v^{s} \sum_{p \in P_s} t_{p}^s + 1 - F_v^{s}
	\end{equation}

This next constraint pathConstrainedByFunctions is defined to check the replicability of a VNF, determined by a parameter :math:`F_v^{s}`. Corresponding to equation (11) from the paper it is set to run as follows:

First 

.. code-block:: java

        for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {


makes sure that all following operations are valid and to be executed for all services :math:`s`, an element of a set service chains :math:`S`, and for all functions :math:`v`, that are element of a set of ordered functions :math:`V_s`  for a service :math:`s`.


.. code-block:: java

                for (int x = 0; x < parameters.getServers().size(); x++)
	                expr.addTerm(1.0, variables.fXSV[x][s][v]);

will then give us a summatory function over all servers :math:`x`, that are element of the set of servers :math:`X` in the network, for a variable :math:`f_x^{v,s}`.

This first half of the method describes this formula:

.. math::
  :nowrap:

    \begin{equation}
	\forall s \in S, \forall v \in V_s:  \sum_{x \in X} f_x^{v,s} 
	\end{equation}


In the next lines of code this if-loop is initiated

.. code-block:: java

                if (parameters.getServices().get(s).getFunctions().get(v).isReplicable()) {
	                GRBLinExpr expr2 = new GRBLinExpr();
	                for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
	                    expr2.addTerm(1.0, variables.tSP[s][p]);
	                model.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, "pathsConstrainedByFunctions");
	            } else
	                model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "pathsConstrainedByFunctions");

For all replicable functions :math:`v` of the service :math:`s` a new expression is defined as a summatory function over all paths :math:`p`, that are element of a set of admissible paths :math:`P_s`  for the service :math:`s`, for a variable :math:`t_{p}^s`.

This new expression is then set as equal to the first expression, mentioned above. So if the loop is true, this formula will be taking effect:

.. math::
  :nowrap:

    \begin{equation}
	\forall s \in S, \forall v \in V_s:  \sum_{x \in X} f_x^{v,s} = \sum_{p \in P_s} t_{p}^s
	\end{equation}

If the loop is false however, meaning that the function is not replicable, the first expression will just be equal to :math:`1`, which would translate to:

.. math::
  :nowrap:

    	\begin{equation}
	\forall s \in S, \forall v \in V_s:  \sum_{x \in X} f_x^{v,s} = 1
	\end{equation}

Both results would be returned to *pathConstrainedByFunctions*, regardless if the function is replicable or not.

At this point it is noteworthy, that we can summarize the if-loop into one formula by introducing a variable :math:`F_v^{s}`, that can take the values :math:`1` for a replicable function of a service :math:`s` or :math:`0` for a non replicable function. Doing this we have to make sure that in both cases the original values of the two equations is not changed.

            A form this equation might take would be this one introduced in the paper as equation (11).

            In this the variable :math:`F_v^{s}`  acts as a stand-in for the if-loop, with :math:`F = 1` canceling out :math:`(1- F_v^{s})` ensuring that only the summatory function will be considered, and with :math:`F = 0` canceling out the summatory function so that the left half is only equal to :math:`1`.



functionPlacement
=================


.. code-block:: java

	private void functionPlacement() throws GRBException {

	    for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
	            for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
	                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {
	                    GRBLinExpr expr = new GRBLinExpr();
	                    for (int n = 0; n < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++)
	                        for (int x = 0; x < parameters.getServers().size(); x++)
	                            if (parameters.getServers().get(x).getNodeParent().equals(parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n)))
	                                expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
	                    model.getGrbModel().addConstr(variables.tSPD[s][p][d], GRB.LESS_EQUAL, expr, "functionPlacement");
	                }
	}

.. math::
  :nowrap:

    \begin{equation}  \label{functionPlacement}
	\forall s \in  S, \forall p \in P_s, \forall \lambda \in \Lambda_s, \forall v \in  V_s: t_{p}^{\lambda, s} \leq \sum_{x \in X_p} f_{x,\lambda}^{v,s} 
	\end{equation}


The VNF allocation is controlled by this next constrained defined in functionPlacement. Related to equation (12) in the paper, it assigns all VNFs for a service :math:`s` in the active paths :math:`p` and is executed as followed: 

             First of all the code lines

.. code-block:: java

        for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
	            for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
	                for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++) {

ensure that for all services :math:`s`, that are an element of a set of service chains :math:`S`, for all paths :math:`p`, an element of a set of  admissible paths :math:`P_s`  for a service :math:`s`, for all demands :math:`\lambda`, an element of a set of traffic demands :math:`\lambda_s`  for the service :math:`s`, and for all functions :math:`v`, that are an element of a set of ordered VNF functions :math:`V_s`  for a service :math:`s`, the following operations are valid and executed.

            Following up

.. code-block:: java

                        GRBLinExpr expr = new GRBLinExpr();
	                    for (int n = 0; n < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++)
	                        for (int x = 0; x < parameters.getServers().size(); x++)
	                            if (parameters.getServers().get(x).getNodeParent().equals(parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n)))
	                                expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
	                    model.getGrbModel().addConstr(variables.tSPD[s][p][d], GRB.LESS_EQUAL, expr, "functionPlacement");

then introduces a summatory function over all nodes :math:`n`, that are element of the set of nodes :math:`N_p^s` that are traversed by the path :math:`p` for a service :math:`s`, and over all the servers :math:`x`, that are element of a set of servers :math:`X_p` that are also traversed by :math:`p`, for a function :math:`f_{x,\lambda}^{v,s}`, if the current node equals the parent node. 

A variable :math:`t_{p}^{\lambda, s}` is then set to be less equal to this function :math:`f_{x,\lambda}^{v,s}` and the result is then returned to functionPlacement. 



oneFunctionPerDemand
====================


.. code-block:: java

	private void oneFunctionPerDemand() throws GRBException {

	    for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
	            for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
	                GRBLinExpr expr = new GRBLinExpr();
	                for (int x = 0; x < parameters.getServers().size(); x++)
	                    expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
	                model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "oneFunctionPerDemand");
	            }
	}

.. math::
  :nowrap:

    \begin{equation} \label{oneFunctionPerDemand}
	\forall s \in S, \forall v \in  V_s, \forall \lambda \in \Lambda_s: \sum_{x \in  X} f_{x,\lambda}^{v,s} = 1
	\end{equation}


This method oneFunctionPerDemand is, similar to constraint (13) from the paper, ensuring that each traffic demand λ has to traverse a specific VNF :math:`v` in only one server. All of this is realized as followed:

            First of all the block

.. code-block:: java

        for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
	            for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {


makes sure that the following operations are executed for all services :math:`s`, an element of a set of service chains :math:`S`, for all functions :math:`v`, element of a set of ordered VNF functions :math:`V_s`  for a service :math:`s`, and for all demands :math:`\lambda`, that are an element of a set of traffic demands :math:`\Lambda_s`  for a service :math:`s`.

            Thereafter

.. code-block:: java

                    GRBLinExpr expr = new GRBLinExpr();
	                for (int x = 0; x < parameters.getServers().size(); x++)
	                    expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
	                model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "oneFunctionPerDemand");

will introduce a summatory function over all servers :math:`x`, that are manager.elements of a set of servers :math:`X`, for a function :math:`f_{x,\lambda}^{v,s}`.
This function :math:`f_{x,\lambda}^{v,s}`  is then set to be equal 1 and the results are returned to *oneFunctionPerDemand*.


mappingFunctionsWithDemands
===========================



.. code-block:: java

	private void mappingFunctionsWithDemands() throws GRBException {

	    for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
	            for (int x = 0; x < parameters.getServers().size(); x++)
	                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
	                    model.getGrbModel().addConstr(variables.fXSVD[x][s][v][d], GRB.LESS_EQUAL, variables.fXSV[x][s][v], "mappingFunctionsWithDemands");

	    for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
	            for (int x = 0; x < parameters.getServers().size(); x++) {
	                GRBLinExpr expr = new GRBLinExpr();
	                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
	                    expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
	                model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.fXSV[x][s][v], "mappingFunctionsWithDemands");
	            }
	}

.. math::
  :nowrap:

    	\begin{equation} \label{mappingFunctionsWithDemands}
	\forall s \in  S, \forall v \in  V_s, \forall x \in X, \forall \lambda \in \Lambda_s: f_{x,\lambda}^{v,s} \leq f_x^{v,s} \leq \sum_{\lambda' \in  \Lambda_s} f_{x,\lambda'}^{v,s} 
	\end{equation}

This next constraint expressed by the method mappingFunctionsWithDemands, ensures that a VNF :math:`v` is only placed in a server :math:`x` if said server is used by at least one traffic demand. Corresponding to equation (14) in the paper, this method is executed as follows:

            The first block of code

.. code-block:: java

        for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
	            for (int x = 0; x < parameters.getServers().size(); x++)
	                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
	                    model.getGrbModel().addConstr(variables.fXSVD[x][s][v][d], GRB.LESS_EQUAL, variables.fXSV[x][s][v], "mappingFunctionsWithDemands");

ensures that for all servers :math:`s`, an element of a set of service chains :math:`S`, for all functions :math:`v`, an element of an ordered set of VNF functions :math:`V_s`  for a service :math:`s`, for all servers :math:`x`, that are element of a set of servers :math:`X`, and for all demands :math:`\lambda`, that are manager.elements of a set of traffic demands :math:`\Lambda_s`  for a service :math:`s`, the following inequation is valid. Said inequation is defined as a *variable0* :math:`f_{x,\lambda}^{v,s}`, which is set to be lesser equal to :math:`f_x^{v,s}`, and returned to *mappingFunctionsWithDemands*.
	
This first half can be interpreted as follows:

.. math::
  :nowrap:

    	\begin{equation}
	\forall s \in  S, \forall v \in  V_s, \forall x \in X, \forall \lambda \in \Lambda_s: f_{x,\lambda}^{v,s} \leq f_x^{v,s} 
	\end{equation}


The second block

.. code-block:: java

        for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
	            for (int x = 0; x < parameters.getServers().size(); x++) {
	                GRBLinExpr expr = new GRBLinExpr();
	                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
	                    expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
	                model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.fXSV[x][s][v], "mappingFunctionsWithDemands");

first makes sure that for all servers :math:`s`, that are element of a set of service chains :math:`S`, for all functions :math:`v`, that are element of an ordered set of VNF functions :math:`V_s`  for a service :math:`s`, and for all server :math:`x`, that are element of a set of servers :math:`X`, the following operations are realized.

Following up

.. code-block:: java

                    GRBLinExpr expr = new GRBLinExpr();
	                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
	                    expr.addTerm(1.0, variables.fXSVD[x][s][v][d]);
	                model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, variables.fXSV[x][s][v], "mappingFunctionsWithDemands");

Express a summatory function over all demands :math:`\lambda`, that are an element of a set of traffic demands :math:`\Lambda_s` for a service :math:`s`, for a variable :math:`f_{x,\lambda}^{v,s}` that is then set to be greater equal than a variable :math:`f_x^{v,s}`  and the results are also sent back to *mappingFunctionsWithDemands*.

A possible mathematical translation for this block could be

.. math::
  :nowrap:

    	\begin{equation}
	\forall s \in  S, \forall v \in  V_s, \forall x \in X, \forall \lambda \in \Lambda_s: f_x^{v,s} \leq \sum_{\lambda' \in  \Lambda_s} f_{x,\lambda'}^{v,s} 
	\end{equation}

Combining both inequations from the first and the second half of the method will result in the initial shown equation.


functionSequenceOrder
=====================



.. code-block:: java
	
	private void functionSequenceOrder() throws GRBException {

	    for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
	            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
	                for (int v = 1; v < parameters.getServices().get(s).getFunctions().size(); v++) {
	                    for (int n = 0; n < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++) {
	                        GRBLinExpr expr = new GRBLinExpr();
	                        GRBLinExpr expr2 = new GRBLinExpr();
	                        Node nodeN = parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n);
	                        for (int m = 0; m <= n; m++) {
	                            Node nodeM = parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(m);
	                            for (int x = 0; x < parameters.getServers().size(); x++)
	                                if (parameters.getServers().get(x).getNodeParent().equals(nodeM))
	                                    expr.addTerm(1.0, variables.fXSVD[x][s][v - 1][d]);
	                        }
	                        for (int x = 0; x < parameters.getServers().size(); x++)
	                            if (parameters.getServers().get(x).getNodeParent().equals(nodeN))
	                                expr.addTerm(-1.0, variables.fXSVD[x][s][v][d]);

	                        expr2.addConstant(-1);
	                        expr2.addTerm(1.0, variables.tSPD[s][p][d]);
	                        model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "functionSequenceOrder");
	                    }
	                }
	        }
	}

.. math::
  :nowrap:

    \begin{equation}
    	\forall s \in S, \forall \lambda \in \Lambda_s, \forall p \in P_s, \forall v \in {V_s}, \forall n \in N_p: \\
    	\Bigg( \sum_{n' = 0}^{n} \sum_{x' \in X_{n'}} f_{x', \lambda}^{(v-1),s} \Bigg) + \Bigg( \sum_{x \in X_n} - f_{x, \lambda}^{v,s} \Bigg) \geq t_{p}^{\lambda,s}  - 1 \quad if \quad v>0
    \end{equation}

Arguably the most complex constraint, the method functionSequenceOrder, equal to equation (15) in the paper, ensures that a traffic demand :math:`\lambda` is only to traverse VNFs in a set order. This constraint is implemented in the code as follows:

The first few loops

.. code-block:: java

        for (int s = 0; s < parameters.getServices().size(); s++)
	        for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
	            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
	                for (int v = 1; v < parameters.getServices().get(s).getFunctions().size(); v++) {
	                    for (int n = 0; n < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().size(); n++) {

make sure that all following operations are valid and executed for all services :math:`s`, that are element of a set of service chains :math:`S`, for all demands :math:`\lambda`, that are element of a set of traffic demands :math:`\Lambda_s` for a service :math:`s`, for all paths :math:`p`, that are element of a set of admissible paths :math:`P_s` for a service :math:`s`, for all functions :math:`v`, that are element of an ordered set of VNF functions :math:`V_s`  for a service :math:`s`, starting with a function :math:`v_1`, excluding the start function :math:`v_0`,  and for all nodes :math:`n`, that are element of an ordered set of nodes :math:`N_p^s`  that are traversed by a path :math:`p` for a service :math:`s`.

            Following up

.. code-block:: java

                            GRBLinExpr expr = new GRBLinExpr();
	                        GRBLinExpr expr2 = new GRBLinExpr();
	                        Node nodeN = parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(n);

define two new expressions and a node named nodeN that is set to be the currently regarded node :math:`n`, traversed by a path :math:`p` for a service :math:`s`. 

.. code-block:: java

                                Node nodeM = parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().get(p).getNodePath().get(m);
	                            for (int x = 0; x < parameters.getServers().size(); x++)
	                                if (parameters.getServers().get(x).getNodeParent().equals(nodeM))
	                                    expr.addTerm(1.0, variables.fXSVD[x][s][v - 1][d]);

then instigates a summatory function over all nodes :math:`m`, that are part of the set :math:`N_p^s`  and lesser in value than the node :math:`n`, and over all servers :math:`x`, that are element of a set of servers :math:`X_m`, consisting of the servers allocated in node :math:`m`, for a function :math:`f_{x', \lambda}^{(v-1),s}`, if the current node/node parent is equal to the nodeM. 
nodeM is defined herby as a current node :math:`m`, that is traversed by a path :math:`p` for a service :math:`s`.

            The lines

.. code-block:: java

                            for (int x = 0; x < parameters.getServers().size(); x++)
	                            if (parameters.getServers().get(x).getNodeParent().equals(nodeN))
	                                expr.addTerm(-1.0, variables.fXSVD[x][s][v][d]);

then add a term that equals a summatory function over all servers :math:`x`, that are an element of a set of servers :math:`X_n`, consisting of all servers in the node :math:`n`, for a variable :math:`f_{x, \lambda}^{v,s}`, multiplied by minus 1, if the current node/node parent is equal to the previously defined nodeN.

Interpreted as a mathematical term this first expression may take this form:

.. math::
  :nowrap:

    \begin{equation}
    \forall s \in S, \forall \lambda \in \Lambda_s, \forall p \in P_s, \forall v \in {V_s}, \forall n \in N_p: \\
    \Bigg( \sum_{n' = 0}^{n} \sum_{x' \in X_{n'}} f_{x', \lambda}^{(v-1),s} \Bigg) + \Bigg( \sum_{x \in X_n} - f_{x, \lambda}^{v,s} \Bigg)
    \end{equation}

Continuing in the code

.. code-block:: java

                            expr2.addConstant(-1);
	                        expr2.addTerm(1.0, variables.tSPD[s][p][d]);
	                        model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "functionSequenceOrder");


expression *expr2* will be added the constant (-1) and the variable :math:`t_{p}^{\lambda,s}`.
This expression is then set as greater equal to the previous expression expr and the results will be returned to *functionSequenceOrder*.

Resulting on the first equation.


noParallelPaths
===============

.. math::
  :nowrap:

    \begin{equation}
        	\forall s \in S: \sum_{p \in P_s} t_{p}^{s} = 1
    \end{equation}

The first specific constraint noParallelPaths ensures, as the title said, that the paths used by one service chain to forward traffic demands are restricted to one. Corresponding to the equation, it runs as follows:

First it makes sure that for all services :math:`s`, that are manager.elements of a set of service chains :math:`S`, the following operations will be valid and executed.

Then implements a summatory function over all paths :math:`p`, that are an element of a set of admissible paths :math:`P_s` for a service :math:`s`, for a variable :math:`t_p^s`.

The summatory function is then set to be equal one and returned to *noParallelPaths*.



.. code-block:: java

    public void noParallelPaths() throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < parameters.getServices().get(s).getTrafficFlow().getAdmissiblePaths().size(); p++)
                expr.addTerm(1.0, variables.tSP[s][p]);
            model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, "noParallelPaths");
        }
    }



setVariablesFromInitialPlacementAsConstraints
=============================================

.. math::
  :nowrap:

    \begin{equation}
    \forall s \in S, \forall v \in {V_s}, \forall x \in X: F_{x}^{v,s} \leq f_{x}^{v,s}
    \end{equation}

The second specific constraint *setVariablesFromInitialPlacementAsConstrains* fixes the initial placement of functions in the network. Correlating to the equation, this constraint is implemented as follows:

The code ensures that for all servers :math:`x`, that are element of a set of servers :math:`X`, for all services :math:`s`, that are element of a set of services :math:`S` and for all functions :math:`v`, that are element of a set of VNF functions :math:`V_s`  for a service s, a variable :math:`f_x^(v,s)`  will be assigned as equal to 1, if the initial output :math:`F_x^(v,s)`  was equal to 1.

Should that not be the case, :math:`f_x^(v,s)`  will behave like a binary variable, taking either 1 or 0 as a value.

This means that we have two equations

.. math::
  :nowrap:

      \begin{equation}
	\forall s \in S, \forall v \in {V_s}, \forall x \in X: F_{x}^{v,s} = f_{x}^{v,s}
    \end{equation}

if the initial output is equal to 1 and

.. math::
  :nowrap:

      \begin{equation}
	\forall s \in S, \forall v \in {V_s}, \forall x \in X: F_{x}^{v,s} \leq f_{x}^{v,s}
    \end{equation}

for all other cases.

To simplify we will only take the second equation into consideration since the first one is also implied in the second, and therefore our method is to be seen as equal to the equation at the beggining of the section.

The output of this method will be returned back to *setVariablesFromInitialPlacementAsConstraints*.



.. code-block:: java

    public void setVariablesFromInitialPlacementAsConstraints(Output initialOutput) throws GRBException {
        for (int x = 0; x < initialOutput.getVariables().fXSV.length; x++)
            for (int s = 0; s < initialOutput.getVariables().fXSV[x].length; s++)
                for (int v = 0; v < initialOutput.getVariables().fXSV[x][s].length; v++)
                    if (initialOutput.getVariables().fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                        model.getGrbModel().addConstr(variables.fXSV[x][s][v], GRB.EQUAL, 1, "setVariablesFromInitialPlacementAsConstraints");
    }

reRoutingMigration
==================

.. math::
  :nowrap:

    \begin{equation}
	\forall s \in S, \forall v \in {V_s},  \forall x \in X,  \forall x' \in X: m_{x,x'}^{v,s} = f_{x'}^{v,s} \sum_{\lambda \in  \Lambda_s} \lambda \cdot L_v  \cdot F_{x}^{v,s} \quad if \quad x \neq x'
    \end{equation}

.. math::
  :nowrap:

    \begin{equation}
 	\forall s \in S, \forall v \in {V_s},  \forall x \in X,  \forall x' \in X:   \frac{m_{x,x'}^{v,s}}{M} \leq  \sum_{p \in P_s} m_{p}^{v,s} \leq m_{x,x'}^{v,s}
    \end{equation}

.. math::
  :nowrap:

    \begin{equation}
 	\forall s \in  S, \forall v \in {V_s}: \sum_{p \in P_s} m_{p}^{v,s} = 1
    \end{equation}

.. code-block:: java

    public void reRoutingMigration(Output initialOutput) throws GRBException {
        for (int s = 0; s < parameters.getServices().size(); s++)
            for (int v = 0; v < parameters.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < parameters.getServers().size(); x++)
                    if (initialOutput.getVariables().fXSV[x][s][v].get(GRB.DoubleAttr.X) == 1)
                        for (int y = 0; y < parameters.getServers().size(); y++)
                            if (x != y) {
                                GRBLinExpr expr = new GRBLinExpr();
                                for (int d = 0; d < parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                                    expr.addTerm(parameters.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                            * parameters.getServices().get(s).getFunctions().get(v).getLoad(), variables.fXSV[y][s][v]);
                                model.getGrbModel().addConstr(variables.mXYSV[x][y][s][v], GRB.EQUAL, expr, "reRoutingMigration");
                            }
    }

