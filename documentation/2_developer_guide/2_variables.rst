*********
Variables
*********

This chapter defines all the variables that the model supports.

**Notation for the Network topology**

+-----------------------------+------------+----------------------------------------------------------------------------------------------------+
| Parameters                  |Short code  | Meaning or long code                                                                               |
+=============================+============+====================================================================================================+
| :math:`\mathbb{N}`          |            | set of nodes, servers, links, services and linear cost functions, respectively                     |
+-----------------------------+------------+----------------------------------------------------------------------------------------------------+
| :math:`\mathbb{E}`          |            | set of nodes, servers, links, services and linear cost functions, respectively                     |
+-----------------------------+------------+----------------------------------------------------------------------------------------------------+
| :math:`\mathbb{X}`          |            | set of nodes, servers, links, services and linear cost functions, respectively                     |
+-----------------------------+------------+----------------------------------------------------------------------------------------------------+
| :math:`\mathbb{X}_n`        |            |  set of servers connexted to node *n*                                                              |
+-----------------------------+------------+----------------------------------------------------------------------------------------------------+
| :math:`\Pi^s_p`             |            |  set of ordered nodes traversed by path *p* of SFC s                                               |
+-----------------------------+------------+----------------------------------------------------------------------------------------------------+
| :math:`\delta_{e}(p)`       |            |  true if path *p* traverses link *e*                                                               |
+-----------------------------+------------+----------------------------------------------------------------------------------------------------+






**Binary decision variables**

+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
|Symbol                       | Short code  | Meaning or long code                                                                               |
+=============================+=============+====================================================================================================+
| :math:`z_{p}^s`             | rSP         | true if service *s* is using path *p*                                                              |
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
| :math:`z_{p}^{k,s}`         | rSPD        | true if traffic demand :math:`\lambda_k` of service *s* is using the path *p*                      |
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
| :math:`f_x^{v,s}`           | pXSV        | true if function *v* of service *s* is allocated in server *x*                                     |
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
| :math:`f_{x,k}^{v,s}`       | pXSVD       | true if function *v* from service *s* is being used in server *x* by traff demand :math:`\lambda_k`|
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
| :math:`f_x`                 | pX          | true if server *x* is used                                                                         |
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+

**Optimization model variables and functions**

+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
|Symbol                       | Short code  | Meaning or long code                                                                               |
+=============================+=============+====================================================================================================+
|:math:`k_e`                  | kL          | utilization cost of link *e*                                                                       |
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
|:math:`u_e`                  | uL          | Constraint integer 0 <= uL <= 1 ; utilization of link *e*                                          |
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
|:math:`k_x`                  | kX          | utilization cost of server *x*                                                                     |
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
|:math:`u_x`                  | uX          | Constraint integer 0 <= uX <= 1 ;utilization of server *x*                                         |
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+
|:math:`\eta_x^{v,s}`         | nXSV        | integer variable 0 <= nXSV <= maxInstance; variable number of VNF instances                        |
+-----------------------------+-------------+----------------------------------------------------------------------------------------------------+

**Variable pX[x]**

.. code-block:: java

    pX = new GRBVar[pm.getServers().size()];
    for (int x = 0; x < pm.getServers().size(); x++)
        this.pX[x] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, Auxiliary.pX + "[" + x + "]");

**Variable kL[l]**

.. code-block:: java

    kL = new GRBVar[pm.getLinks().size()];
    for (int l = 0; l < pm.getLinks().size(); l++)
        kL[l] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, Auxiliary.kL + "[" + l + "]");

**Variable kX[x]**


.. code-block:: java

    kX = new GRBVar[pm.getServers().size()];
    for (int x = 0; x < pm.getServers().size(); x++)
        kX[x] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, Auxiliary.kX + "[" + x + "]");


**Variable uL[l]**


.. code-block:: java

    uL = new GRBVar[pm.getLinks().size()];
    for (int l = 0; l < pm.getLinks().size(); l++)
        uL[l] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, Auxiliary.uL + "[" + l + "]");


**Variable uX[x]**


.. code-block:: java

    uX = new GRBVar[pm.getServers().size()];
    for (int x = 0; x < pm.getServers().size(); x++)
        uX[x] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, Auxiliary.uX + "[" + x + "]");


**Variable nXSV[x,s,v]**


.. code-block:: java

        nXSV = new GRBVar[pm.getServers().size()][pm.getServices().size()][pm.getServiceLength()];
         for (int x = 0; x < pm.getServers().size(); x++)
            for (int s = 0; s < pm.getServices().size(); s++)
               for (int v = 0; v < pm.getServices().get(s).getFunctions().size(); v++)
                  nXSV[x][s][v] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER
                          , Auxiliary.nXSV + "[" + x + "][" + s + "][" + v + "]");