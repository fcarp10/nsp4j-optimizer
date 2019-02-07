**************
Output Results
**************

The framework stores all optimization results in one output file with filename *example_optimization-model.json*, which is located at directory /target/results/date/, where *optimization-model* is identical to the chosen optimization model in the interface. All results are displayed with the option *offset_results = 1*.

The first blocks shows the used *input files, objective function*,  *optimization mode* and *optimization model*. The block *constrains* gives an overview of the chosen constrains with the abbrevations shown below.

.. code-block:: java

  "scenario" : {
    "inputFileName" : "network-7a",
    "objectiveFunction" : "num_of_servers",
    "maximization" : false,
    "model" : "initial_placement",
    "constraints" : {
      "RPC1" : true,
      "RPI1" : true,
      "VAI1" : true,
      "VAI2" : true,
      "VAI3" : true,
      "VAC1" : true,
      "VAC2" : true,
      "VAC3" : true,
      "RPC2" : false,
      "RPC3" : true,
      "VRC1" : false,
      "VRC2" : true,
      "VRC3" : false,
      "VSC1" : false,
      "VSC2" : false,
      "VSC3" : false,
      "DIC1" : false,
      "DVC1" : false,
      "DVC2" : false,
      "DVC3" : false,
      "IPC1" : false,
      "PDC1" : false
    }




+-----------+----------------------------------+
| Short     | Function name                    |
+===========+==================================+
| RPC1      | onePathPerDemand                 |
+-----------+----------------------------------+
| RPC2      | numberOfActivePathsBoundByService|
+-----------+----------------------------------+
| RPC3      | noParallelPaths                  |
+-----------+----------------------------------+
| RPI1      | activatePathForService           |
+-----------+----------------------------------+
| VAI1      | mappingFunctionsWithDemands      |
+-----------+----------------------------------+
| VAI2      | constraintVAI2                   |
+-----------+----------------------------------+
| VAI3      | countNumberOfUsedServers         |
+-----------+----------------------------------+
| VAC1      | functionPlacement                |
+-----------+----------------------------------+
| VAC2      | oneFunctionPerDemand             |
+-----------+----------------------------------+
| VAC3      | functionSequenceOrder            |
+-----------+----------------------------------+
| VRC1      | pathsConstrainedByFunctionsVRC1  |
+-----------+----------------------------------+
| VRC2      | pathsConstrainedByFunctions      |
+-----------+----------------------------------+
| VRC3      | constraintVRC3                   |
+-----------+----------------------------------+
| VSC1      | constraintVSC1                   |
+-----------+----------------------------------+
| VSC2      | constraintVSC2                   |
+-----------+----------------------------------+
| VSC3      | constraintVSC3                   |
+-----------+----------------------------------+
| IPC1      | initialPlacementAsConstraints    |
+-----------+----------------------------------+
| DIC1      | constraintDIC1                   |
+-----------+----------------------------------+
| DVC1      | constraintDVC1                   |
+-----------+----------------------------------+
| DVC2      | constraintDVC2                   |
+-----------+----------------------------------+
| DVC3      | constraintDVC3                   |
+-----------+----------------------------------+
| PDC1      | serviceDelay                     |
+-----------+----------------------------------+





The next block shows results for the binary variable :math:`z^{k,s}_{p}` with the following meaning:


(s,p,k): [s] *"service function chain SFC s"* [ n4, n2, n1, n7, n8] *"list of node of the path"*  [ :math:`\lambda^s_{k}` ]


.. code-block:: java

  "stringVariables" : {
    "rSPD" : [
      "(1,2,1): [1][n4, n2, n1, n7, n8][75]",
      "(1,2,2): [1][n4, n2, n1, n7, n8][75]",
      "(2,1,1): [2][n5, n3, n1, n6][150]",
    ],


The next block shows results for the server utilization :math:`u_x` with the following meaning:

 (x) *"global servernumber"* [n1_0] *"nodenumber_number-of-server-at-nodenumber"*  [:math:`u_x`]

.. code-block:: java

    "uX" : [
      "(1): [n1_0][0.3]",
      "(2): [n1_1][0.22499999999999998]",
      "(3): [n2_0][0.22499999999999998]",
    ],


The next block shows results for the link utilization :math:`u_e` with the following meaning:

 (e) *"global linknumber"* [n1_n2] *"link from node n1 to node n2"*  [:math:`u_e`]

.. code-block:: java

    "uL" : [
      "(1): [n1n2][0.0]",
      "(2): [n1n3][0.0]",
      "(3): [n1n6][0.3]",
    ],


The next block shows results for the binary variable :math:`f^{v,s}_{x,k}` with the following meaning:


(x,s,v,k): [n1_0] *"nodenumber_number-of-server-at-nodenumber"* [s] *"SFC s"* [ :math:`f=F^{v,s}_{NF}` ]  *"function type of v-st VNF os SFC s"*  [ :math:`\lambda^s_{k}` ]


.. code-block:: java

    "pXSVD" : [
      "(1,2,1,1): [n1_0][2][1][150]",
      "(1,2,1,2): [n1_0][2][1][150]",
      "(2,1,3,1): [n1_1][1][4][75]",
    ],


The next block shows results for the end-to-end service delay :math:`\hat{D}^{k,s}_{p}` with the following meaning:

(s,p,k): [ n4, n2, n1, n6, n8] *"list of node of the path"*   [ :math:`\hat{D}^{k,s}_{p}` ]

.. code-block:: java

    "dSPD" : [
      "(1,1,1): [n4, n2, n1, n6, n8][72.1]",
      "(1,1,2): [n4, n2, n1, n6, n8][72.1]",
      "(1,1,3): [n4, n2, n1, n6, n8][72.1]",
      "(2,1,1): [n5, n3, n1, n6][37.76]",
      "(2,1,2): [n5, n3, n1, n6][7.76]",
      "(2,3,1): [n5, n3, n7, n8, n6][11.39]",
      "(2,3,2): [n5, n3, n7, n8, n6][41.39]"
    ],




The next block shows results for the binary variable :math:`z^{s}_{p}` with the following meaning:


(s,p): [s] *"SFC s"* [ n4, n2, n1, n7, n8] *"list of node of the path"*


.. code-block:: java


    "rSP" : [
      "(1,2): [1][n4, n2, n1, n7, n8]",
      "(2,1): [2][n5, n3, n1, n6]"
    ],


The next block shows results for the integer variable :math:`\eta^{v,s}_{x}` with the following meaning:

(x,s,v): [m] *"variable number of instances"* of the v-st VNF of SFC s

.. code-block:: java

    "nXSV" : [
      "(1,1,1): [1.0]",
      "(1,1,2): [3.0]",
      "(1,1,3): [2.0]",
      "(13,2,2): [3.0]",
      "(14,2,1): [1.0]",
    ],



The next block shows results for the binary variable :math:`f^{v,s}_{x}` with the following meaning:


(x,s,v): [n1_0] *"nodenumber_number-of-server-at-nodenumber"* [s] *"SFC s"* [ :math:`f=F^{v,s}_{NF}` ]  *"function type of v-st VNF os SFC s"*


.. code-block:: java

    "pXSV" : [
      "(1,2,1): [n1_0][2][1]",
      "(2,1,3): [n1_1][1][4]",
      "(3,1,1): [n2_0][1][1]",
    ],



The next blocks show averaged results for network wide performance measures. Each result block shows the following details:

network wide average value

minimum value

maximum value

variance


Network wide link utilization:

.. code-block:: java

  "luSummary" : [
    0.09,
    0.0,
    0.3,
    0.02
  ],

Network wide server utilization

.. code-block:: java

  "xuSummary" : [
    0.14,
    0.0,
    0.3,
    0.02
  ],

Network wide number of VNF allocations per server

.. code-block:: java

  "fuSummary" : [
    5.06,
    0.0,
    9.0,
    19.93
  ],

Network wide end-to-end service delay of a SFC

.. code-block:: java

  "sdSummary" : [
    110.8,
    58.76,
    162.85,68
    850.89
  ],


The next block shows average results for network wide performance measures. These are:

*avgPathLength*: average path length in hop

*totalTraffic*: total traffic offered to the network

*trafficLinks*: total traffic on all links of the network

*migrationsNum*: number of migrations

*replicationsNum*: total number of replications

*objVal*: objective value of the chosen objective function.


.. code-block:: java

  "avgPathLength" : 3.5,
  "totalTraffic" : 525.0,
  "trafficLinks" : 1800.0,
  "migrationsNum" : 6,
  "replicationsNum" : 0,
  "objVal" : 0.01,

