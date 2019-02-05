Constraints
===========


Routing and VNF constraints
---------------------------

Constrain RPC1
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{RPC1}
        \forall s \in \mathbb{S},  k = 1,..., \|\Lambda_s \| :  \sum_{p \in \mathbb{P}_s} z_{p}^{k,s} = 1
        \end{equation}

.. code-block:: java

	private void onePathPerDemand() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               expr.addTerm(1.0, vars.rSPD[s][p][d]);
            model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "onePathPerDemand");
         }
   }


This constraint we look at will limit the number of paths used for each traffic demand to 1 and is executed by the method onePathPerDemand. It runs as follows:

The first two *for loops* ensure that for all service chains :math:`s` , element of a set of service chains :math:`S` , and for all traffic demands out of a set of demands :math:`\Lambda_s`, the following operations will be valid.

The following code forces to 1 the summatory of all the traffic demands :math:`\lambda^s_k` of service *s* using the path *p* in order to ensure that each traffic demand only uses one path.

.. code-block:: java

                for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                    expr.addTerm(1.0, vars.rSPD[s][p][d]);
                model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "onePathPerDemand");


Constrain RPC2
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{rmax}   \text{RPC2:} \qquad
	        \forall s \in \mathbb{S}:  \quad     R^s_{MIN}  \leq \sum_{p\in \mathbb{P}_s} z_{p}^s \leq R^s_{MAX}.
        \end{equation}



.. code-block:: java

    private void numberOfActivePathsBoundByService() throws GRBException {
        for (int s=0; s < pm.getServices().size(); s++) {
            int rmin = (int) pm.getServices().get(s).getAttribute("minPaths");
            int rmax = (int) pm.getServices().get(s).getAttribute("maxPaths");
            GRBLinExpr expr = new GRBLinExpr();
            for (int p=0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
                expr.addTerm(1.0, vars.rSP[s][p]);
            }
            model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, rmax, "numberOfActivePathsBoundByService");
            model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, rmin, "numberOfActivePathsBoundByService");
        }
    }



Constrain RPC3
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{RPC3:} \qquad
	      \forall s \in \mathbb{S} : \sum_{p \in \mathbb{P}_s} z_p^s = 1
        \end{equation}


The first specific constraint *noParallelPaths* ensures, as the title said, that the paths used by one service chain to forward traffic demands are restricted to one. Corresponding to the equation, it runs as follows:

First it makes sure that for all services :math:`s` , that are manager.elements of a set of service chains :math:`S` , the following operations will be valid and executed.

Then implements a summatory function over all paths :math:`p`, that are an element of a set of admissible paths :math:`P_s` for a service :math:`s` , for a variable :math:`z_p^s`.

The summatory function is then set to be equal one and returned to *noParallelPaths*.



.. code-block:: java

    private void noParallelPaths() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                expr.addTerm(1.0, vars.rSP[s][p]);
            model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, "noParallelPaths");
        }
   }





Constrain RPI1
^^^^^^^^^^^^^^


.. math::
    :nowrap:

      \begin{equation}  \label{RPI1} \qquad
        \forall s \in \mathbb{S},  \forall p \in \mathbb{ P}_s  :  \quad   \frac{ \sum_{k=1 }^{\|\Lambda_s \|}  z_{p}^{k, s} } {M} \leq z_{p}^{s} \leq \sum_{k=1 }^{\|\Lambda_s \|}  z_{p}^{k, s}
        \end{equation}


The method *activePathForService* is meant to ensure that when a traffic demand :math:`\lambda^s_k` is using a path :math:`p` , said path will be activated for the corresponding service :math:`s`. Following the equation, this method is executed as follows:


.. code-block:: java

	private void activatePathForService() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
                GRBLinExpr expr = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();
                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                    expr.addTerm(1.0, vars.rSPD[s][p][d]);
                    expr2.addTerm(1.0 / (pm.getServices().get(s).getTrafficFlow().getDemands().size() * 10), vars.rSPD[s][p][d]);
                }
                model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.rSP[s][p], "activatePathForService");
                model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, vars.rSP[s][p], "activatePathForService");
            }
   }

The first two loops ensure that for all service chains :math:`s` , an element of a set of service chains :math:`S` and for all paths :math:`p` , element of a set of admissable paths :math:`P_s`  for a service :math:`s` all the following operations are to be executed.

Following up the first expression *expr* is defined to be a summatory of the variable :math:`z_{p}^{k, s}` over all demands :math:`\lambda^s_k`, element of a set of traffic demands :math:`\Lambda_s`  for a service :math:`s` . *expr* is then set to be lesser equal to a variable :math:`z_{p}^{s}` and the results are then returned to *activePathForService*.

This correlation can be portrayed in a formula as such

.. math::
    :nowrap:

      \begin{equation}
        z_{p}^{s} \leq \sum_{k=1 }^{\|\Lambda_s \|}  z_{p}^{k, s}
        \end{equation}


The second expression *expr2* on the other hand is defined as a summatory over all demands :math:`\lambda^s_k`, that are an element of a set of traffic demands :math:`\Lambda_s`  for a service :math:`s` , for a variable :math:`z_{p}^{k, s}` that is also divided by a big number *M*. In this case this *M* is the total number of demands multiplied by 10.
*expr2* is then declared as greater equal to a variable :math:`z_{p}^{s}` and the results are then returned to *activePathForService*.

Similar to *expr* this relation can be displayed as

.. math::
    :nowrap:

      \begin{equation}
        \frac{ \sum_{k=1 }^{\|\Lambda_s \|}  z_{p}^{k, s} } {M} \leq z_{p}^{s}
        \end{equation}


To summarize both blocks of commands into one formula, we can simply interpret them as an inequation, with :math:`z_{p}^{s}` acting like the connecting link, resulting on the shown manager formula stated above.



Constrain VAI1
^^^^^^^^^^^^^^


.. math::
    :nowrap:

        \begin{equation} \label{VAI1}
         \forall s \in \mathbb{S},  \forall v \in {\mathbb{V}_s}, \forall x \in \mathbb{X} :  \quad \frac{ \sum_{k=1 }^{\|\Lambda_s \|}      f_{x,k}^{v,s} }  {\|\Lambda_s \|} \leq f_x^{v,s} \leq   \sum_{k=1 }^{\|\Lambda_s \|}   f_{x,k}^{v,s}
        \end{equation}



.. code-block:: java

    private void mappingFunctionsWithDemands() throws GRBException {

        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                        expr.addTerm(1.0, vars.pXSVD[x][s][v][d]);
                        expr2.addTerm(1.0 / (pm.getServices().get(s).getTrafficFlow().getDemands().size() * 10), vars.pXSVD[x][s][v][d]);
                    }
                    model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.pXSV[x][s][v], "mappingFunctionsWithDemands");
                    model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, vars.pXSV[x][s][v], "mappingFunctionsWithDemands");
                }


   }


This next constraint expressed by the method mappingFunctionsWithDemands, ensures that a function :math:`v` is only placed in a server :math:`x` if said server is used by at least one traffic demand. This method is executed as follows:

Similar to the other constraints the first three loops ensure that for all servers :math:`s` , an element of a set of service chains :math:`S` , for all functions :math:`v` , an element of an ordered set of functions :math:`V_s`  for a service :math:`s` and for all servers :math:`x` , that are element of a set of servers :math:`X` the following inequations are valid.

The first expression *expr* is then set to be a summatory of a variable :math:`f_{x,k}^{v,s}` over all demands :math:`\lambda^s_k` , that are manager.elements of a set of traffic demands :math:`\Lambda_s`  for a service :math:`s` and is then defined to be greater equal than a variable :math:`f_x^{v,s}`.
The results are then returned as *mappingFunctionsWithDemands* and  can be interpreted as follows:

.. math::
    :nowrap:

        \begin{equation}
          \quad f_x^{v,s} \leq   \sum_{k=1 }^{\|\Lambda_s \|}   f_{x,k}^{v,s}
          \end{equation}


The second expression *expr2* is then defined as a summatory function over all demands :math:`\lambda^s_k` , that are an element of a set of traffic demands :math:`\Lambda_s` for a service :math:`s` , for a variable :math:`f_{x,k}^{v,s}` that is divided by a big number *M*. In this case *M* is defined as the total number of demands multiplied by 10.

A possible mathematical translation for this block could be

.. math::
    :nowrap:

        \begin{equation}
	      \frac{ \sum_{k=1 }^{\|\Lambda_s \|}
          f_{x,k}^{v,s} }  {\|\Lambda_s \|} \leq f_x^{v,s}
	      \end{equation}



Combining both inequations from the first and the second half of the method will result in the initial shown equation.



Constrain VAI2
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation}
           \forall s \in \mathbb{S}, \forall x \in \mathbb{X}:  \quad \frac{ \sum_{ v \in \mathbb{V}_s}  f_{x}^{v,s} }  {\| \mathbb{V}_s \|} \leq  f_x^{s}  \leq \sum_{ v \in \mathbb{V}_s}  f_{x}^{v,s}
        \end{equation}


.. code-block:: java

   private void constraintVAI2() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int x = 0; x < pm.getServers().size(); x++) {
                GRBLinExpr expr = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    expr.addTerm(1.0, vars.pXSV[x][s][v]);
                    expr2.addTerm(1.0 / pm.getServices().get(s).getFunctions().size(), vars.pXSV[x][s][v]);
                }
                model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, vars.pXS[x][s], "constraintVAI2");
                model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, vars.pXS[x][s], "constraintVAI2");
            }
    }






Constrain VAI3
^^^^^^^^^^^^^^

.. math::
    :nowrap:

      \begin{equation} \label{VAI3}
	     \forall x \in \mathbb{X} :     \quad    \frac{ \sum_{s \in \mathbb{S}} \sum_{v \in \mathbb{V}_s} f_x^{v,s}} {M} \leq f_x  \leq  \sum_{s \in \mathbb{S}}  \sum_{v \in  \mathbb{V}_s} f_x^{v,s}
     \end{equation}


.. code-block:: java

    private void countNumberOfUsedServers() throws GRBException {
        for (int x = 0; x < pm.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    expr.addTerm(1.0 / pm.getTotalNumberOfFunctionsAux(), variables.pXSV[x][s][v]);
                    expr2.addTerm(1.0, variables.pXSV[x][s][v]);
                }
            model.getGrbModel().addConstr(variables.pX[x], GRB.GREATER_EQUAL, expr, "countNumberOfUsedServers");
            model.getGrbModel().addConstr(variables.pX[x], GRB.LESS_EQUAL, expr2, "countNumberOfUsedServers");
        }
    }


This next method *countNumberOfUsedServers* basically counts all servers that are used for all the functions for all service chains in relation to the total number of servers.  This method is running as followed:

The for-loop

.. code-block:: java

        for (int x = 0; x < pm.getServers().size(); x++) {

makes sure, that for all servers :math:`x` , element of the the set of servers :math:`X` in the network will be regarded in the following operation and all subsequent loops

.. code-block:: java

            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)

are to be translated as summatories over all service chains :math:`s` , element of the set of service chains :math:`S` and over all functions :math:`v` , element of a ordered set of functions :math:`V_s`  for the service chain :math:`s`, for the following expressions

.. code-block:: java

                    expr.addTerm(1.0 / pm.getTotalNumberOfFunctionsAux(), variables.pXSV[x][s][v]);

which describes a division of :math:`1` by the total number of functions, multiplied with the variable :math:`f_{x}^{v,s}`; and

.. code-block:: java

             expr2.addTerm(1.0, variables.pXSV[x][s][v]);

which is simply the summatory over the variable :math:`f_{x}^{v,s}`.

Following up

.. code-block:: java

            model.getGrbModel().addConstr(variables.pX[x], GRB.GREATER_EQUAL, expr, "countNumberOfUsedServers");
            model.getGrbModel().addConstr(variables.pX[x], GRB.LESS_EQUAL, expr2, "countNumberOfUsedServers");

sets a new variable :math:`f_x` as greater equal to the term defined in the previous expression expr and as lesser equal to expr2.
The results will then be returned again as *countNumberOfUsedServers*.




VNF allocation constraints
--------------------------


Constrain VAC1
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{VAC1}
        \forall s \in \mathbb{S}, \forall v \in {\mathbb{V}_s}, \forall p \in \mathbb{ P}_s, k = 1,..., \|\Lambda_s \| :   \quad      z_{p}^{k, s} \leq  \sum_{i=1}^{ | \Pi^s_{p}|} \sum_{x \in \mathbb{ X}_{ n^{p,s}_i} } f_{x,k}^{v,s}  \text{ ,}
        \end{equation}



.. code-block:: java

   private void functionPlacement() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                  GRBLinExpr expr = new GRBLinExpr();
                  for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().size(); n++)
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (pm.getServers().get(x).getParent().equals(pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(n)))
                           expr.addTerm(1.0, vars.pXSVD[x][s][v][d]);
                  model.getGrbModel().addConstr(vars.rSPD[s][p][d], GRB.LESS_EQUAL, expr, "functionPlacement");
               }
   }



The function allocation is controlled by this next constrained defined in *functionPlacement*. It assigns all functions for a service :math:`s` in the active paths :math:`p` and is executed as followed:

                 First of all the code lines

.. code-block:: java

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {

ensure that for all services :math:`s` , that are an element of a set of service chains :math:`S` , for all paths :math:`p` , an element of a set of  admissible paths :math:`P_s`  for a service :math:`s` , for all demands out of a set of traffic demands :math:`\Lambda_s` , and for all functions :math:`v` , that are an element of a set of ordered functions :math:`V_s` , the following operations are valid and executed.

                Following up

.. code-block:: java

                  GRBLinExpr expr = new GRBLinExpr();
                  for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().size(); n++)
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (pm.getServers().get(x).getParent().equals(pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(n)))
                           expr.addTerm(1.0, vars.pXSVD[x][s][v][d]);
                  model.getGrbModel().addConstr(vars.rSPD[s][p][d], GRB.LESS_EQUAL, expr, "functionPlacement");

then introduces a summatory function over all nodes :math:`n` , that are element of the set of nodes :math:`\Pi_p^s` that are traversed by the path :math:`p` for a service :math:`s` , and over all the servers :math:`x` , that are element of a set of servers :math:`X_{n}` that are also traversed by :math:`p` , for a function :math:`f_{x,k}^{v,s}`, if the current node equals the parent node.

A variable :math:`z_{p}^{k, s}` is then set to be less equal to this function :math:`f_{x,k}^{v,s}` and the result is then returned to functionPlacement.






Constrain VAC2
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{VAC2}
        \forall s \in \mathbb{S}, \forall v \in {\mathbb{V}_s}, k = 1,..., \|\Lambda_s \| :   \quad         \sum_{x \in  \mathbb{ X}} f_{x,k}^{v,s} = 1
        \end{equation}



.. code-block:: java

	private void oneFunctionPerDemand() throws GRBException {

	    for (int s = 0; s < pm.getServices().size(); s++)
	        for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
	            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {
	                GRBLinExpr expr = new GRBLinExpr();
	                for (int x = 0; x < pm.getServers().size(); x++)
	                    expr.addTerm(1.0, variables.pXSVD[x][s][v][d]);
	                model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "oneFunctionPerDemand");
	            }
	}



This method oneFunctionPerDemand is ensuring that each traffic demand :math:`\lambda^s_k` has to traverse a specific function :math:`v` in only one server. All of this is realized as followed:

First of all the block

.. code-block:: java

        for (int s = 0; s < pm.getServices().size(); s++)
	        for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
	            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {


makes sure that the following operations are executed for all services :math:`s` , an element of a set of service chains :math:`S` , for all functions :math:`v` , element of a set of ordered functions :math:`V_s`  for a service :math:`s` , and for all demands :math:`\lambda^s_k`, that are an element of a set of traffic demands :math:`\Lambda_s`  for a service :math:`s`.

Thereafter

.. code-block:: java

                    GRBLinExpr expr = new GRBLinExpr();
	                for (int x = 0; x < pm.getServers().size(); x++)
	                    expr.addTerm(1.0, variables.pXSVD[x][s][v][d]);
	                model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "oneFunctionPerDemand");

will introduce a summatory function over all servers :math:`x` , that are elements of a set of servers :math:`X` , for a function :math:`f_{x,k}^{v,s}`.
This function :math:`f_{x,k}^{v,s}`  is then set to be equal 1 and the results are returned to *oneFunctionPerDemand*.




Constrain VAC3
^^^^^^^^^^^^^^


.. math::
    :nowrap:

        \begin{multline}   \label{VAC3:} \qquad
	    \forall s \in \mathbb{S},  \forall v \in\mathbb{V}_s,  k = 1,..., |\Lambda_s|,  \forall p \in \mathbb{P}_s,   1 \le m \le |\Pi^s_p |      :   \\
	    \Bigg( \sum_{i = 1}^{m} \sum_{x \in  \mathbb{ X}_{ n^{p,s}_{i}  } } f_{x, k}^{(v-1),s} \Bigg) -    \sum_{x \in  \mathbb{ X}_{ n^{p,s}_{m} }  } f_{x, k}^{v,s} \geq z_{p}^{k,s}  - 1  \text{ ,}    \quad 1 < v \leq   |\mathbb{V}_s| \text{ ,}
        \end{multline}


.. code-block:: java

   private void functionSequenceOrder() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int v = 1; v < pm.getServices().get(s).getFunctions().size(); v++) {
                  for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().size(); n++) {
                     GRBLinExpr expr = new GRBLinExpr();
                     GRBLinExpr expr2 = new GRBLinExpr();
                     Node nodeN = pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(n);
                     for (int m = 0; m <= n; m++) {
                        Node nodeM = pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(m);
                        for (int x = 0; x < pm.getServers().size(); x++)
                           if (pm.getServers().get(x).getParent().equals(nodeM))
                              expr.addTerm(1.0, vars.pXSVD[x][s][v - 1][d]);
                     }
                     for (int x = 0; x < pm.getServers().size(); x++)
                        if (pm.getServers().get(x).getParent().equals(nodeN))
                           expr.addTerm(-1.0, vars.pXSVD[x][s][v][d]);

                     expr2.addConstant(-1);
                     expr2.addTerm(1.0, vars.rSPD[s][p][d]);
                     model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "functionSequenceOrder");
                  }
               }
         }
   }


Arguably the most complex constraint, the method functionSequenceOrder ensures that a traffic demand :math:`\lambda^s_k` is only to traverse functions in a set order. This constraint is implemented in the code as follows:

The first few loops

.. code-block:: java

      for (int s = 0; s < pm.getServices().size(); s++)
         for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
               for (int v = 1; v < pm.getServices().get(s).getFunctions().size(); v++) {
                  for (int n = 0; n < pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().size(); n++) {

make sure that all following operations are valid and executed for all services :math:`s` , that are element of a set of service chains :math:`S` , for all demands :math:`\lambda`, that are element of a set of traffic demands :math:`\Lambda_s` , for all paths :math:`p` , that are element of a set of admissible paths :math:`P_s` , for all functions :math:`v` , that are element of an ordered set of functions :math:`V_s` , starting with a function :math:`v_1` , excluding the start function :math:`v_0` ,  and for all nodes :math:`n` , that are element of an ordered set of nodes :math:`\Pi^s_p`  that are traversed by a path :math:`p` for a service :math:`s`.

Following up

.. code-block:: java

                     GRBLinExpr expr = new GRBLinExpr();
                     GRBLinExpr expr2 = new GRBLinExpr();
                     Node nodeN = pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(n);

define two new expressions and a node named nodeN that is set to be the currently regarded node :math:`n` , traversed by a path :math:`p` for a service :math:`s`.

.. code-block:: java

                     for (int m = 0; m <= n; m++) {
                        Node nodeM = pm.getServices().get(s).getTrafficFlow().getPaths().get(p).getNodePath().get(m);
                        for (int x = 0; x < pm.getServers().size(); x++)
                           if (pm.getServers().get(x).getParent().equals(nodeM))
                              expr.addTerm(1.0, vars.pXSVD[x][s][v - 1][d]);
                     }

then instigates a summatory function over all nodes :math:`m` , that are part of the set :math:`\Pi^s_p`  and lesser in value than the node :math:`n` , and over all servers :math:`x` , that are element of a set of servers :math:`X_m` , consisting of the servers allocated in node :math:`m` , for a function :math:`f_{x',k}^{(v-1),s}`, if the current node/node parent is equal to the nodeM. nodeM is defined herby as a current node :math:`m`, that is traversed by a path :math:`p` for a service :math:`s`.


The lines

.. code-block:: java

                            for (int x = 0; x < pm.getServers().size(); x++)
	                            if (pm.getServers().get(x).getNodeParent().equals(nodeN))
	                                expr.addTerm(-1.0, variables.pXSVD[x][s][v][d]);

then add a term that equals a summatory function over all servers :math:`x` , that are an element of a set of servers :math:`X_n` , consisting of all servers in the node :math:`n` , for a variable :math:`f_{x,k}^{v,s}` , multiplied by minus 1, if the current node/node parent is equal to the previously defined nodeN.

Interpreted as a mathematical term this first expression may take this form:

.. math::
    :nowrap:

        \begin{equation}
         \Bigg( \sum_{n' = 0}^{n} \sum_{x' \in X_{n'}} f_{x',k}^{(v-1),s} \Bigg) + \Bigg( \sum_{x \in X_n} - f_{x,k}^{v,s} \Bigg)
         \end{equation}

Continuing in the code

.. code-block:: java

                            expr2.addConstant(-1);
	                        expr2.addTerm(1.0, variables.rSPD[s][p][d]);
	                        model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "functionSequenceOrder");


expression *expr2* will be added the constant (-1) and the variable :math:`z_{p}^{k,s}`.
This expression is then set as greater equal to the previous expression expr and the results will be returned to *functionSequenceOrder*.









Replication constraints
-----------------------



Constrain VRC2
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation}
        \forall s \in \mathbb{S}, \forall v \in {\mathbb{V}_s}:    \quad      \sum_{x \in  \mathbb{X}} f_x^{v,s} =  F^{v,s}_R \sum_{p \in  \mathbb{P}_s} z_{p}^s + 1 -F^{v,s}_R
        \end{equation}





.. code-block:: java

   private void pathsConstrainedByFunctions() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int x = 0; x < pm.getServers().size(); x++)
               expr.addTerm(1.0, vars.pXSV[x][s][v]);
            if ((boolean) pm.getServices().get(s).getFunctions().get(v).getAttribute("replicable")) {
               GRBLinExpr expr2 = new GRBLinExpr();
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  expr2.addTerm(1.0, vars.rSP[s][p]);
               model.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, "pathsConstrainedByFunctions");
            } else
               model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "pathsConstrainedByFunctions");
         }
   }


This next constraint pathConstrainedByFunctions is defined to check the replicability of a function, determined by a parameter :math:`F_R^{v,s}`. It is set to run as follows:

First

.. code-block:: java

        for (int s = 0; s < pm.getServices().size(); s++)
	        for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {


makes sure that all following operations are valid and to be executed for all services :math:`s`, an element of a set service chains :math:`S`, and for all functions :math:`v`, that are element of a set of ordered functions :math:`V_s`  for a service :math:`s`.


.. code-block:: java

                for (int x = 0; x < pm.getServers().size(); x++)
	                expr.addTerm(1.0, variables.pXSV[x][s][v]);

will then give us a summatory function over all servers :math:`x`, that are element of the set of servers :math:`X` in the network, for a variable :math:`f_x^{v,s}`.

This first half of the method describes this formula:

.. math::
    :nowrap:

        \begin{equation}
	\forall s \in S, \forall v \in V_s:  \sum_{x \in X} f_x^{v,s}
	\end{equation}


In the next lines of code this if-loop is initiated

.. code-block:: java

            if ((boolean) pm.getServices().get(s).getFunctions().get(v).getAttribute("replicable")) {
               GRBLinExpr expr2 = new GRBLinExpr();
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  expr2.addTerm(1.0, vars.rSP[s][p]);
               model.getGrbModel().addConstr(expr, GRB.EQUAL, expr2, "pathsConstrainedByFunctions");
            } else
               model.getGrbModel().addConstr(expr, GRB.EQUAL, 1.0, "pathsConstrainedByFunctions");

For all replicable functions :math:`v` of the service :math:`s` a new expression is defined as a summatory function over all paths :math:`p`, that are element of a set of admissible paths :math:`P_s`  for the service :math:`s`, for a variable :math:`z_{p}^s`.

This new expression is then set as equal to the first expression, mentioned above. So if the loop is true, this formula will be taking effect:

.. math::
    :nowrap:

        \begin{equation}
	\forall s \in S, \forall v \in V_s:  \sum_{x \in X} f_x^{v,s} = \sum_{p \in P_s} z_{p}^s
	\end{equation}

If the loop is false however, meaning if the function is not replicable, the first expression will just be equal to :math:`1` , which would translate to:

.. math::
    :nowrap:

        	\begin{equation}
	\forall s \in S, \forall v \in V_s:  \sum_{x \in X} f_x^{v,s} = 1
	\end{equation}

Both results would be returned to *pathConstrainedByFunctions*, regardless if the function is replicable or not.

At this point it is noteworthy, that we can summarize the if-loop into one formula by introducing a variable :math:`F_R^{v,s}` , that can take the values :math:`1` for a replicable function of a service :math:`s` or :math:`0` for a non replicable function. Doing this we have to make sure that in both cases the original values of the two equations is not changed. In this the variable :math:`F_R^{v,s}`  acts as a stand-in for the if-loop, with :math:`F_R^{v,s} = 1` canceling out :math:`(1- F_R^{v,s})` ensuring that only the summatory function will be considered, and with :math:`F_R^{v,s} = 0` canceling out the summatory function so that the left half is only equal to :math:`1`.



Constrain VRC1
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{pathsConstrainedByFunctions}
	    \forall s \in S, \forall v \in V_s:  \sum_{x \in X} f_x^{v,s} \leq F_v^{s} \sum_{p \in P_s} t_{p}^s + 1 - F_v^{s}
	    \end{equation}


.. code-block:: java

   private void pathsConstrainedByFunctionsVRC1() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int x = 0; x < pm.getServers().size(); x++)
               expr.addTerm(1.0, vars.pXSV[x][s][v]);
            if ((boolean) pm.getServices().get(s).getFunctions().get(v).getAttribute("replicable")) {
               GRBLinExpr expr2 = new GRBLinExpr();
               for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
                  expr2.addTerm(1.0, vars.rSP[s][p]);
               model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, "pathsConstrainedByFunctions");
            } else
               model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, 1.0, "pathsConstrainedByFunctions");
         }
   }




The constrain defined by VRC1 is almost identical to constrain VRC2 described above. The difference is the :math:`\leq` condition, which establishes the rigth side of the equation as an upper bound. In the code this can be seen from *model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, "pathsConstrainedByFunctions")*;



Constrain VRC3
^^^^^^^^^^^^^^


.. math::
    :nowrap:

        \begin{equation} \label{VNFvmax}  \qquad
             \forall s \in \mathbb{S}, \forall v \in {\mathbb{V}_s}:   \quad    F^{v,s}_{Rmin} + 1  \leq \sum_{x \in \mathbb{X}} f_x^{v,s}   \leq F^{v,s}_{Rmax} + 1
        \end{equation}


.. code-block:: java

   private void constraintVRC3() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++) {
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int x = 0; x < pm.getServers().size(); x++)
                    expr.addTerm(1.0, vars.pXSV[x][s][v]);
                boolean replicable = (boolean) pm.getServices().get(s).getFunctions().get(v).getAttribute("replicable");
                if (replicable) {
                    int minRep = (int) pm.getServices().get(s).getAttribute("minReplica") + 1;
                    int maxRep = (int) pm.getServices().get(s).getAttribute("maxReplica") + 1;
                    model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, minRep, "constraintVRC3");
                    model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxRep, "constraintVRC3");
                } else {
                    model.getGrbModel().addConstr(expr, GRB.EQUAL, 1, "constraintVRC3");
                }
            }
        }
    }





VNF assignment constraints
--------------------------

Constrain VSC1
^^^^^^^^^^^^^^


.. math::
    :nowrap:

        \begin{equation} \label{max-server-vnf-chain}   \qquad
        \forall s \in  \mathbb{S}, \forall x \in \mathbb{X}: \quad   \sum_{v \in  \mathbb{V}_s}  f_x^{v,s} \leq   \hat{\text{V}}^s_{x}  \equiv \hat{\text{V}}^s
        \end{equation}


.. code-block:: java

    private void constraintVSC1() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int x = 0; x < pm.getServers().size(); x++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                    expr.addTerm(1.0, vars.pXSV[x][s][v]);
                int maxVNF = (int) pm.getServices().get(s).getAttribute("maxVNFserver");
                model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxVNF, "constraintVSC1");
            }
    }


Constrain VSC2
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{max-server-SFC-chain}   \qquad
         \forall x \in \mathbb{X}: \quad   \sum_{s \in  \mathbb{S}}  f_x^s \leq  \hat{\text{S}_x}
        \end{equation}


.. code-block:: java

     private void constraintVSC2() throws GRBException {
        for(int x = 0; x < pm.getServers().size(); x++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int s = 0; s < pm.getServices().size(); s++)
                expr.addTerm(1.0, vars.pXS[x][s]);
            int maxSFC = pm.getServers().get(x).getParent().getAttribute("MaxSFC");
            model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxSFC, "constraintVSC2");
        }
    }




Constrain VSC3
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation} \label{max-flow-vnf}  \qquad
             \forall s \in \mathbb{S}, \forall v \in {\mathbb{V}_s}, \forall x \in \mathbb{X} :   \quad      \sum_{k=1}^{| \Lambda_s|}  f_{x,k}^{v,s} \leq     \tilde{\Lambda}^{F_{NF}(v,s)}
        \end{equation}


.. code-block:: java

     private void constraintVSC3() throws GRBException {
        for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                for (int x = 0; x < pm.getServers().size(); x++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                        expr.addTerm(1.0, vars.pXSVD[x][s][v][d]);
                    int maxSubflow = (int) pm.getServices().get(s).getFunctions().get(v).getAttribute("maxsubflows");
                    model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxSubflow, "constraintVSC3");
                }
    }




Constrain DIC1
^^^^^^^^^^^^^^

.. math::
    :nowrap:

            \begin{multline} \label{VNFproc-dedicated}   \qquad
        \forall x \in \mathbb{X}, \forall s \in \mathbb{S}, \forall v \in {\mathbb{V}_s}|  F_M^{v,s} =0:   \\
            L_T^{F_{NF}(v,s)}   \sum_{k=1 }^{|\Lambda_s|}    \lambda^s_k  \cdot f_{x,k}^{v,s}   \  \leq   \hat{ \Theta}^{F_{NF}(v,s)}_x  \cdot C^{F_{NF}(v,s)}_{P}  \text{  , }
        \end{multline}



.. code-block:: java

     private void constraintDIC1() throws GRBException {
        for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
                for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
                    List<Integer> sharedNF = (List<Integer>) pm.getServices().get(s).getAttribute("sharedNF");
                        if (sharedNF.get(v) == 0) {
                            double load = (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load");
                            GRBLinExpr expr = new GRBLinExpr();
                            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                                expr.addTerm(load * pm.getServices().get(s).getTrafficFlow().getDemands().get(d), vars.pXSVD[x][s][v][d]);
                            int maxLoad = (int) pm.getServices().get(s).getFunctions().get(v).getAttribute("maxLoad");
                            int maxInt = (int) pm.getServices().get(s).getFunctions().get(v).getAttribute("maxInstances");
                            model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, maxLoad * maxInt, "constraintDIC1");
                        }
                }
    }









Constrain PDC1
^^^^^^^^^^^^^^

.. math::
    :nowrap:

    \begin{multline}   \forall s \in \mathbb{S},  \forall p \in \mathbb{P}_s,  k = 1, ..., |\Lambda_s| :    \\
         \hat{D}^{k,s}_{p}  =     z^s_p  \sum_{ \forall e \in  \mathbb{E} } D_e \cdot  \delta_{e}(p)  +   z^{k,s}_{p} \sum_{ \forall v \in\mathbb{V}_s}   D^{F_{NF}(s,v)}   \leq  D_s
    \end{multline}



.. code-block:: java

    private void serviceDelay() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++)
            for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
               GRBLinExpr serviceDelayExpr = new GRBLinExpr();
               serviceDelayExpr.add(linkDelayExpr(s, p));
               serviceDelayExpr.add(processingDelayExpr(s, p, d));
               model.getGrbModel().addConstr(serviceDelayExpr, GRB.LESS_EQUAL
                       , (int) pm.getServices().get(s).getAttribute("max_delay"), "");
               model.getGrbModel().addConstr(serviceDelayExpr, GRB.EQUAL, vars.dSPD[s][p][d], "");
        }
    } }






Variable number of VNF instances
--------------------------------


Constrain DVC1
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation}
            \forall x \in \mathbb{X}, \forall s \in \mathbb{S}, \forall v \in {\mathbb{V}_s}| F_M^{v,s} =0: cp_{x}^{v,s}  =   L_T^{F_{NF}(v,s)}  \sum_{k}   \lambda^s_k  \cdot f_{x,k}^{v,s}  \leq   \hat{ \eta}^{v,s}_x  \cdot C^{F_{NF}(v,s)}_{P}
        \end{equation}


.. code-block:: java


     private void constraintDVC1() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               List<Integer> sharedNF = (List<Integer>) pm.getServices().get(s).getAttribute("sharedNF");
                  if (sharedNF.get(v) == 0) {
                     double load = (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load");
                     GRBLinExpr expr = new GRBLinExpr();
                     GRBLinExpr expr2 = new GRBLinExpr();
                     int maxLoad = (int) pm.getServices().get(s).getFunctions().get(v).getAttribute("maxLoad");
                     for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                        expr.addTerm(load * pm.getServices().get(s).getTrafficFlow().getDemands().get(d), vars.pXSVD[x][s][v][d]);
                     }
                     expr2.addTerm(maxLoad, vars.nXSV[x][s][v]);
                     model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, "constraintDVC1");
                  }
            }
    }


Constrain DVC2
^^^^^^^^^^^^^^

.. math::
    :nowrap:

          \begin{equation}
            f_{x}^{v,s}  \leq \hat{  \eta}^{v,s}_x  \leq  f_{x}^{v,s} \cdot \hat{  \Theta}^{F_{NF}(v,s)}_x
        \end{equation}



.. code-block:: java


    private void constraintDVC2() throws GRBException {
      for (int s = 0; s < pm.getServices().size(); s++)
         for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
            for (int x = 0; x < pm.getServers().size(); x++) {
               GRBLinExpr expr = new GRBLinExpr();
               GRBLinExpr expr2 = new GRBLinExpr();
               GRBLinExpr expr3 = new GRBLinExpr();
               int maxInst = (int) pm.getServices().get(s).getFunctions().get(v).getAttribute("maxInstances");
               expr.addTerm(1.0, vars.pXSV[x][s][v]);
               expr2.addTerm(1.0, vars.nXSV[x][s][v]);
               expr3.addTerm(maxInst, vars.pXSV[x][s][v]);
               model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, "constraintDVC2");
               model.getGrbModel().addConstr(expr2, GRB.LESS_EQUAL, expr3, "constraintDVC");
            }
   }



Constrain DVC3
^^^^^^^^^^^^^^

.. math::
    :nowrap:

        \begin{equation}
         \forall x \in \mathbb{X}, \forall s \in \mathbb{S}, \forall v \in {\mathbb{V}_s}| F_M^{v,s} =0: L_T^{F_{NF}(v,s)}  \sum_{k }  \lambda^s_k  \cdot f_{x,k}^{v,s}    \leq   \hat{ \eta}^{v,s}_x  \cdot  C^{F_{NF}(v,s)}_{P} <     C^{F_{NF}(v,s)}_{P}  +   L_T^{F_{NF}(v,s)} \sum_{k}   \lambda^s_k  \cdot f_{x,k}^{v,s}
     \end{equation}

.. code-block:: java

     private void constraintDVC3() throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++)
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               List<Integer> sharedNF = (List<Integer>) pm.getServices().get(s).getAttribute("sharedNF");
                  if (sharedNF.get(v) == 0) {
                     GRBLinExpr expr = new GRBLinExpr();
                     GRBLinExpr expr2 = new GRBLinExpr();
                     double load = (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load");
                     int maxLoad = (int) pm.getServices().get(s).getFunctions().get(v).getAttribute("maxLoad");
                     for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                        expr.addTerm(load * pm.getServices().get(s).getTrafficFlow().getDemands().get(d), vars.pXSVD[x][s][v][d]);
                     expr2.addTerm(maxLoad, vars.nXSV[x][s][v]);
                     model.getGrbModel().addConstr(expr, GRB.LESS_EQUAL, expr2, "constraintDVC3");
                     expr.addConstant(maxLoad);
                     model.getGrbModel().addConstr(expr, GRB.GREATER_EQUAL, expr2, "constraintDVC3");
                  }
            }
    }


















Network / server utilization and capacity constraints
-----------------------------------------------------


Constraint LTC1 and OFC1
^^^^^^^^^^^^^^^^^^^^^^^^

**Korregieren von Text und Code**


.. math::
    :nowrap:

    \begin{equation}\label{link-traffic}
    \forall e \in  \mathbb{E} :   \quad   \gamma_{e}   = \sum_{s \in  \mathbb{S}}  \sum_{p \in \mathbb{P}_s}   \sum_{k=1 }^{|\Lambda_s|}     \lambda^s_k \cdot  z_{p}^{k,s}  \cdot \delta_{e}(p)   \leq C_{e} \text{ ,}
    \end{equation}

The constraint *setLinkUtilizationExpr()* is meant to check if a link is utilized in consideration of the paths that might traverse the link, the bandwidth of the traffic demand :math:`\lambda^s_k` and the maximum capacity of the link :math:`C_e`.


.. code-block:: java

   private void linkUtilization() throws GRBException {
      for (int l = 0; l < pm.getLinks().size(); l++) {
         GRBLinExpr expr = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {
               if (!pm.getServices().get(s).getTrafficFlow().getPaths().get(p).contains(pm.getLinks().get(l)))
                  continue;
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                  expr.addTerm((double) pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                          / (int) pm.getLinks().get(l).getAttribute("capacity"), vars.rSPD[s][p][d]);
            }
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
               for (int p = 0; p < pm.getPaths().size(); p++) {
                  if (!pm.getPaths().get(p).contains(pm.getLinks().get(l)))
                     continue;
                  double traffic = 0;
                  for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++)
                     traffic += pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                             * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load");
                  expr.addTerm(traffic / (int) pm.getLinks().get(l).getAttribute("capacity"), vars.sSVP[s][v][p]);
               }
         model.getGrbModel().addConstr(expr, GRB.EQUAL, vars.uL[l], "linkUtilization");
         linearCostFunctions(expr, vars.kL[l]);
      }
   }



The method itself is performed as followed:

The first loop

.. code-block:: java

        for (int l = 0; l < pm.getLinks().size(); l++) {

makes sure that all links :math:`e` (index variable l), element of the set of links, are to be considered when executing the following operations.

Starting a new expression with

.. code-block:: java

         GRBLinExpr expr = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int p = 0; p < pm.getServices().get(s).getTrafficFlow().getPaths().size(); p++) {

the loops then express the summatories over all service chains :math:`s` , element of the set of service chains :math:`S` and all paths :math:`p` , element of the set of admissible paths :math:`P_s` for the service chain :math:`s`.

The subsequent operation

.. code-block:: java

                    if (!pm.getServices().get(s).getTrafficFlow().getPaths().get(p).contains(pm.getLinks().get(l)))
                        continue;

makes sure that the operation will only continue if the current service chain s and the currently used path p contain the link :math:`e` we are looking at. If that is not the case the operation will end here. In the mathematical model this is portrayed by the parameter :math:`\delta_e(p)` , that will enter the equation as multiplier by :math:`1` , if the link :math:`e` is used by path p and service chain :math:`s` , or by :math:`0` , if it is not. In case of a multiplication with :math:`0` , the whole equation will equal :math:`0` and the observed link will not be utilized.

On the other hand, if the parameter :math:`\delta_e(p)` equals :math:`1`, the following will be executed:

.. code-block:: java

                    for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++)
                        expr.addTerm((double) pm.getServices().get(s).getTrafficFlow().getTrafficDemands().get(d)
                                / (int) pm.getLinks().get(l).getAttribute("capacity"), variables.rSPD[s][p][d]);
                }

Taking the sum over all traffic demands :math:`\lambda^s_k` , that are element of a set of traffic demands :math:`\Lambda_s` for a service :math:`s` , the demand :math:`\lambda^s_k` will be divided by the link capacity :math:`C_e` and multiplied with the variable :math:`z_{p}^{k,s}`.

The next code line

.. code-block:: java

            model.getGrbModel().addConstr(expr, GRB.EQUAL, variables.uL[l], "setLinkUtilizationExpr");

defines the equation as the link utilization :math:`u_e`, returning the results to *setLinkUtilizationExpr()*. This defines the constrain LTC1.


The last line of code

.. code-block:: java

            setLinearCostFunctions(expr, variables.kL[l]);

sends the link utilization to the method *setLinearCostFunctions* for further computing the penalty cost function, which defines the constrain

.. math::
    :nowrap:

        \begin{equation} \textbf{OFC1} \qquad
	    \forall e \in E, \forall y \in Y: k_e \geq y \big( u_{e} \big)
	    \end{equation}





Constrain DNSC1 / OFC2 or DVSC1
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


The number of used instances per NF have only an impact on the processing overhead created by each of them on the server. For a fixed and given number of VNF instances per NF this overhead follows to be given as

.. math::
    :nowrap:

            \begin{equation}
	    \forall s \in  S, \forall v \in V_s, \forall x \in X:  \frac{co_{x}^{v,s}}{C_x} =  \frac{  f_{x}^{v,s} \cdot   \hat{  \Theta}^{F_{NF}(v,s)}_x \cdot L_O^{F_{NF}(v,s)}    }{C_x}
	    \end{equation}


For a variable number of VNF instances per NF this overhead follows to be given as


.. math::
    :nowrap:

        \begin{equation}
	    \forall s \in  S, \forall v \in V_s, \forall x \in X:  \frac{co_{x}^{v,s}}{C_x} =  \frac{  \hat{  \eta}^{v,s}_x \cdot L_O^{F_{NF}(v,s)}    }{C_x}
	    \end{equation}


In both cases the VNF related processing load is given by

.. math::
    :nowrap:

        \begin{equation}
	    \forall s \in  S, \forall v \in V_s, \forall x \in X:  \frac{cp_{x}^{v,s}}{C_x} = \sum_{k}  \frac{\lambda^s_k \cdot f_{x,k}^{v,s} \cdot L_T^{F_{NF}(v,s)}}{C_x}
	    \end{equation}



Finally, the utilization of the server follows to be constraint by


.. math::
    :nowrap:

        \begin{equation}
	      \forall x \in X:  u_x = \sum_{s \in S}  \sum_{v \in V}  \frac{cp_{x}^{v,s} +  co_{x}^{v,s}  }{C_x}   \leq  1
	    \end{equation}




.. code-block:: java

    private void se
    rverUtilization(boolean isOverheadVariable, GRBModel initialModel) throws GRBException {
      for (int x = 0; x < pm.getServers().size(); x++) {
         GRBLinExpr serverUtilizationExpr = new GRBLinExpr();
         for (int s = 0; s < pm.getServices().size(); s++)
            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
               for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getDemands().size(); d++) {
                  serverUtilizationExpr.addTerm((pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                                  * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load"))
                                  / pm.getServers().get(x).getCapacity()
                          , vars.pXSVD[x][s][v][d]);
               }
               if (isOverheadVariable) {
                 GRBLinExpr variableOverheadExpr = new GRBLinExpr();
                  variableOverheadExpr.addTerm((double) pm.getServices().get(s).getFunctions().get(v).getAttribute("overhead") / pm.getServers().get(x).getCapacity()
                                , vars.nXSV[x][s][v]);
                        serverUtilizationExpr.add(variableOverheadExpr);
               } else {
                  GRBLinExpr fixOverheadExpr = new GRBLinExpr();
                  fixOverheadExpr.addTerm((double) pm.getServices().get(s).getFunctions().get(v).getAttribute("overhead")
                                  * (int) pm.getServices().get(s).getFunctions().get(v).getAttribute("maxInstances")
                                  / pm.getServers().get(x).getCapacity()
                          , vars.pXSV[x][s][v]);
                  serverUtilizationExpr.add(fixOverheadExpr);
               }
            }
         model.getGrbModel().addConstr(serverUtilizationExpr, GRB.EQUAL, vars.uX[x], "serverUtilization");
         linearCostFunctions(serverUtilizationExpr, vars.kX[x]);
      }
     }





Similar to the previous constraint *setServerUtilizationExpr()* is an operation, that is supposed to check the utilization of a server within the service in consideration of the bandwidth of the traffic demands, the load ratio of the functions and the maximum capacity of the server.
This method is running as followed:

Similar to the previous method, the loop

.. code-block:: java

        for (int x = 0; x < pm.getServers().size(); x++)

ensures that the following operations will be valid and executed for all servers x, element of the set of servers X in the network.

Following loops

.. code-block:: java

            for (int s = 0; s < pm.getServices().size(); s++)
	            for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++) {
	                for (int d = 0; d < pm.getServices().get(s).getTrafficFlow().getTrafficDemands().size(); d++) {


all translate to summatories over all service chains :math:`s` , element of the set of service chains :math:`S` , over all functions :math:`v` , element of the ordered set of functions :math:`V_s` in service chain :math:`s` , and over all traffic demands :math:`\lambda^s_k` , that are element of the set of demands :math:`\Lambda_s`.

The subsequent commands

.. code-block:: java

                    serverUtilizationExpr.addTerm((pm.getServices().get(s).getTrafficFlow().getDemands().get(d)
                                  * (double) pm.getServices().get(s).getFunctions().get(v).getAttribute("load"))
                                  / pm.getServers().get(x).getCapacity()
                          , vars.pXSVD[x][s][v][d]);

are to be interpreted as a demand :math:`\lambda^s_k` , determined by the previous loop, multiplied with a load ratio :math:`L_T^{F_{NF}(v,s)}` , influenced by the current function :math:`v` .  The resulting product will be divided by the maximum server capacity :math:`C_x` and multiplied with the parameter :math:`f_{x,k}^{v,s}`.

A possible way to summarize this operation would be

.. math::
    :nowrap:

        \begin{equation}
        \forall x \in X: u_{x}  = \sum_{s \in S} \sum_{v \in V_s}  \sum_{k}  \lambda^s_k \cdot f_{x,k}^{v,s} \cdot L_T^{F_{NF}(v,s)}
         \end{equation}

The following if-loop determines alternative commands that are to be executed depending on the need.

.. code-block:: java

               if (isOverheadVariable) {
                 GRBLinExpr variableOverheadExpr = new GRBLinExpr();
                  variableOverheadExpr.addTerm((double) pm.getServices().get(s).getFunctions().get(v).getAttribute("overhead") / pm.getServers().get(x).getCapacity()
                                , vars.nXSV[x][s][v]);
                        serverUtilizationExpr.add(variableOverheadExpr);
               } else {
                  GRBLinExpr fixOverheadExpr = new GRBLinExpr();
                  fixOverheadExpr.addTerm((double) pm.getServices().get(s).getFunctions().get(v).getAttribute("overhead")
                                  * (int) pm.getServices().get(s).getFunctions().get(v).getAttribute("maxInstances")
                                  / pm.getServers().get(x).getCapacity()
                          , vars.pXSV[x][s][v]);
                  serverUtilizationExpr.add(fixOverheadExpr);
               }

If the boolean *isOverheadVariable* is true the expression *variableOverheadExpr* will be defined as the given overhead divided by the server capacity and multiplied by the variable :math:`n_{x}^{v,s}`, with *variableOverheadExpr* being added to *serverUtilizationExpr*.

If the boolean is false however, the expression *fixOverheadExpr* will be defined as the overhead multiplied with a fixed maximum number of instances that will then be divided by the capacity and multiplied with a variable :math:`f_{x}^{v,s}`. This time *fixOverheadExpr* will be added to *serverUtilizationExpr*.

With the following line

.. code-block:: java

         model.getGrbModel().addConstr(serverUtilizationExpr, GRB.EQUAL, vars.uX[x], "serverUtilization");


*serverUtilizationExpr*, no matter how it is defined, will be set equal to the server utilization :math:`u_x`, and is then returning the results to *setServerUtilizationExpr()*, which defines the constrain DNSC1.

The last line


.. code-block:: java

         linearCostFunctions(serverUtilizationExpr, vars.kX[x]);

sends the server utilization to the method *setLinearCostFunctions* for further computing the penalty cost function, which defines the constrain


.. math::
    :nowrap:

        \begin{equation}  \textbf{OFC2} \qquad
	    \forall x \in X, \forall y \in Y: k_{x} \geq y \big( u_{x} \big)
	    \end{equation}





Optimization models
===================

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


This very first method setObjectiveFunction() in this class will take whatever expressions are returned to it and decide whether they will be minimized or maximized.
Therefor it will check the boolean isMaximization for a true or false. If the boolean is false the method will take whatever expression is returned by the following methods in this class and minimize the function it is given.
If the boolean is false it will maximize whatever the following methods in this class will return to it.


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





Objective Function for Optimization Models
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The graphical interface allows to select a combination of different objective functions, which are in detail OF1; a combination of OF4 + OF5; or OF2+OF3.
The weighting factors are given by the input parameters.



.. code-block:: java

    private static GRBLinExpr generateExprForObjectiveFunction(OptimizationModel model, String obj) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        double weightLinks = pm.getWeights()[0] / pm.getLinks().size();
        double weightServers = pm.getWeights()[1] / pm.getServers().size();
        double weightServiceDelays = pm.getWeights()[2] / (pm.getPaths().size() * 100);
        switch (obj) {
            case NUM_OF_SERVERS_OBJ:
                expr.add(model.usedServersExpr());
                break;
            case COSTS_OBJ:
                expr.add(model.linkCostsExpr(weightLinks));
                expr.add(model.serverCostsExpr(weightServers));
                expr.add(model.serviceDelayExpr(weightServiceDelays));
                break;
            case UTILIZATION_OBJ:
                expr.add(model.linkUtilizationExpr(weightLinks));
                expr.add(model.serverUtilizationExpr(weightServers));
                break;
     }
     return expr;

