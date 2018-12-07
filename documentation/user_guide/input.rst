****************
Input Parameters
****************

The framework requires three different input files: one describing the network topology, one with all paths listed and one for the specific parameters. All three files must have exactly the same name with different extensions:\*.dgs for the topology, \*.txt for the paths and \*.yml for the parameters. The following example is illustrated in the input files.

.. image:: /_static/example.png
        :align: center


Topology file
=============

The desired network topology is described using the GraphStream guidelines in a file with extension \*.dgs. For instance:

.. code-block:: yaml

    DGS004
    test 0 0

    an n1 x:250 y:150 num_servers:2 server_capacity:1000 processing_delay:10 type:0 MaxSFC:5 VMmax:10
    an n2 x:175 y:200 num_servers:2 server_capacity:1000 processing_delay:10 type:0 MaxSFC:5 VMmax:10
    an n3 x:200 y:100 num_servers:2 server_capacity:1    processing_delay:10 type:0 MaxSFC:5 VMmax:10
    an n4 x:100 y:150 num_servers:2 server_capacity:1    processing_delay:10 type:0 MaxSFC:5 VMmax:10
    an n5 x:150 y:100 num_servers:2 server_capacity:1000 processing_delay:10 type:0 MaxSFC:5 VMmax:10
    an n6 x:350 y:200 num_servers:2 server_capacity:1000 processing_delay:10 type:0 MaxSFC:5 VMmax:10
    an n7 x:350 y:100 num_servers:2 server_capacity:1000 processing_delay:10 type:0 MaxSFC:5 VMmax:10
    an n8 x:400 y:150 num_servers:2 server_capacity:1000 processing_delay:10 type:0 MaxSFC:5 VMmax:10

    ae n1n2 n1 > n2 capacity:1000
    ae n1n3 n1 > n3 capacity:1000
    ae n1n6 n1 > n6 capacity:1000
    ae n1n7 n1 > n7 capacity:1000
    ae n2n1 n2 > n1 capacity:1000
    ae n2n4 n2 > n4 capacity:1000
    ae n3n1 n3 > n1 capacity:1000
    ae n3n5 n3 > n5 capacity:1000
    ae n3n7 n3 > n7 capacity:1000
    ae n4n2 n4 > n2 capacity:1000
    ae n4n5 n4 > n5 capacity:1000
    ae n5n3 n5 > n3 capacity:1000
    ae n5n4 n5 > n4 capacity:1000
    ae n6n1 n6 > n1 capacity:1000
    ae n6n8 n6 > n8 capacity:1000
    ae n7n1 n7 > n1 capacity:1000
    ae n7n3 n7 > n3 capacity:1000
    ae n7n8 n7 > n8 capacity:1000
    ae n8n6 n8 > n6 capacity:1000
    ae n8n7 n8 > n7 capacity:1000


``an`` adds a node. The command is followed by a unique node identifier, that can be a single word or a string delimited by the double quote character. Values x and y on the server represent the coordinates of the nodes. For each node can be specified the number of servers (i.e serversNode), the capacity of each server (i.e. capacity) and the type of server.

``ae`` adds an link. This command must be followed by a unique identifier of the link, following with the identifiers of two connecting nodes. For each link, the specific capacity can be specified (i.e. capacity)/

For further information on the use of a DGS file see `<http://graphstream-project.org/doc/Advanced-Concepts/The-DGS-File-Format/>`_


The next table describes every parameter for the topology file:

+----------------------------------------------------------------------+
| Variables of *example.dgs*                                           |
+======================+===============================================+
| Definition of node parameters                                        |
+----------------------+-----------------------------------------------+
| ``num_servers``      | number of servers per network node            |
+----------------------+-----------------------------------------------+
| ``server_capacity``  | processing capacity of a server               |
+----------------------+-----------------------------------------------+
| ``processing_delay`` | processing delay of a server                  |
+----------------------+-----------------------------------------------+
| ``type``             | type of the node (experimental)               |
+----------------------+-----------------------------------------------+
| ``MaxSFC``           | maximum number of SFCs per server             |
+----------------------+-----------------------------------------------+
| ``VMmax``            | maximum number ov VMs per server              |
+----------------------+-----------------------------------------------+
| Definition of link parameters                                        |
+----------------------+-----------------------------------------------+
|``capacity``          | (bandwidth) capacity of a link                |
+----------------------+-----------------------------------------------+



Paths file
==========

The admissible paths for the topology must be specified in a file with the same name as the topology file, but with extension \*.txt. For instance:

.. code-block:: text

    [n4, n5, n3, n7]
    [n4, n2, n1, n7]
    [n4, n5, n3, n1, n7]
    [n4, n2, n1, n6, n8]
    [n4, n2, n1, n7, n8]
    [n4, n5, n3, n7, n8]


This paths can be externally generated or generated using the included KShortest Path Generator tool. For using this tool, you only need to place \*.dgs file in the same folder with the \*.jar and click the path button from the graphical user interface. All the paths will be generated in a \*.txt file with the same name as the topology file.

Parameters file
===============

This file describes the default parameters for the optimization model. The name of the file has to be the same as the name of the topology file, but with extension \*.yml. An example of a parameter file is:


.. code-block:: yaml

    # optimization parameters
    gap: 0
    weights: [0.5, 0.5, 0]
    # auxiliary parameters
    aux: {
     "overhead": 0,
     "minPathsDefault": 3,
     "maxPathsDefault": 3,
     "iterations": 1000,
     "offset_results": 1,
     "scaling_x": 1.0,
    "scaling_y": 1.0
    }
    # service definitions
    serviceChains:
    - id: 1
     chain: [1, 2, 4, 3]
     attributes: {
        "sharedNF": [1, 0, 0, 1],
        "minPaths": 3,
        "maxPaths": 3,
        "minReplica": 1,
        "maxReplica": 3,
        "maxVNFserver": 10
    }
    - id: 2
    chain: [1, 3, 2]
    attributes: {
        "sharedNF": [1, 1, 0],
        "minPaths": 2,
        "maxPaths": 2,
        "minReplica": 1,
        "maxReplica": 3,
        "maxVNFserver": 5
    }
    # function definitions
    functions:
    - type: 1
    attributes: {
        "replicable": false,
        "load": 1.0,
        "overhead": 10,
        "maxLoad": 200,
        "maxsubflows":  4,
        "maxSharedSFC": 5,
        "maxSharedVNF": 10,
        "maxInstances": 1,
        "delay": 10
    }
    - type: 2
    attributes: {
        "replicable": true,
        "load": 1.0,
        "overhead": 10,
        "maxLoad": 200,
        "maxsubflows": 4,
        "maxSharedSFC": 5,
        "maxSharedVNF": 3,
        "maxInstances": 1,
        "delay": 10
    }
    - type: 3
    attributes: {
        "replicable": true,
        "load": 1.0,
        "overhead": 10,
        "maxLoad": 200,
        "maxsubflows": 4,
        "maxSharedSFC": 1,
        "maxSharedVNF": 1,
        "maxInstances": 1,
        "delay": 10
    }
    - type: 4
    attributes: {
        "replicable": false,
        "load": 1.0,
        "overhead": 10,
        "maxLoad": 200,
        "maxsubflows": 4,
        "maxSharedSFC": 1,
        "maxSharedVNF": 1,
        "maxInstances": 1,
        "delay": 10
    }
    - type: 5
    attributes: {
        "replicable": false,
        "load": 1.0,
        "overhead": 10,
        "maxLoad": 200,
        "maxsubflows": 4,
        "maxSharedSFC": 1,
        "maxSharedVNF": 1,
        "maxInstances": 1,
        "delay": 10
    }
    # traffic flow definitions
    trafficFlows:
    - serviceId: 1
    src: "n4"
    dst: "n8"
    minDem: 3
    maxDem: 3
    minBw: 75
    maxBw: 75
    - serviceId: 2
    src: "n5"
    dst: "n6"
    minDem: 2
    maxDem: 2
    minBw: 150
    maxBw: 150




The next table describes every parameter for the model:

+----------------------------------------------------------------------+
| Variables of *example.yml*                                           |
+====================+=================================================+
| Definition of optimization parameters                                |
+--------------------+-------------------------------------------------+
| ``gap``            | gap optimization value                          |
+--------------------+-------------------------------------------------+
| ``weights``        | cost weights: link (W1), server (W2), delay (W3)|
+--------------------+-------------------------------------------------+
| auxiliary parameters                                                 |
+--------------------+-------------------------------------------------+
|``aux``             | global and default parameter                    |
+--------------------+-------------------------------------------------+
| ``overhead``       |                                                 |
+--------------------+-------------------------------------------------+
| ``minPathsDefault``| minimum number of used paths                    |
+--------------------+-------------------------------------------------+
| ``maxPathsDefault``| maximum number of used paths                    |
+--------------------+-------------------------------------------------+
| ``iterations``     |                                                 |
+--------------------+-------------------------------------------------+
| ``offset_results`` | if 0, numbering starts with 0; else with 1      |
+--------------------+-------------------------------------------------+
| ``scaling_x``      |                                                 |
+--------------------+-------------------------------------------------+
| ``scaling_y``      |                                                 |
+--------------------+-------------------------------------------------+
| Definition of network functions                                      |
+--------------------+-------------------------------------------------+
| ``functions``      | set of network function (NF) types              |
+--------------------+-------------------------------------------------+
| ``type``           | identifier of the function                      |
+--------------------+-------------------------------------------------+
| ``attributes``     | parameters of this network function             |
+--------------------+-------------------------------------------------+
| ``replicable``     | indicates if the NF can be replicated           |
+--------------------+-------------------------------------------------+
| ``load``           | packet rate to processing load ratio            |
+--------------------+-------------------------------------------------+
| ``overhead``       | processing overhead for a NF instance           |
+--------------------+-------------------------------------------------+
| ``maxLoad``        | maximum load the NF can process                 |
+--------------------+-------------------------------------------------+
| ``maxsubflows``    | maximum number of traffic flows for the NF      |
+--------------------+-------------------------------------------------+
| ``maxSharedSFC``   | maximum # of SFC that can share the NF          |
+--------------------+-------------------------------------------------+
| ``maxSharedVNF``   | maximum # of VNFs per SFC that can share the NF |
+--------------------+-------------------------------------------------+
| ``maxinstances``   | maximum # of instances of this NF at a server   |
+--------------------+-------------------------------------------------+
| ``delay``          |                                                 |
+--------------------+-------------------------------------------------+
| Definition of service chains                                         |
+--------------------+-------------------------------------------------+
| ``serviceChains``  | Service Function Chains (SFC) on the network    |
+--------------------+-------------------------------------------------+
| ``id``             | identifier of the SFC                           |
+--------------------+-------------------------------------------------+
| ``chain``          | set of VNFs of the SFC                          |
+--------------------+-------------------------------------------------+
| ``attributes``     | parameters of the SFC                           |
+--------------------+-------------------------------------------------+
| ``sharedNF``       | indicates if a VNF can be shared by other SFC   |
+--------------------+-------------------------------------------------+
| ``minPaths``       | minimum # of active paths usable by the SFC     |
+--------------------+-------------------------------------------------+
| ``maxPaths``       | maximum # of active paths usable by the SFC     |
+--------------------+-------------------------------------------------+
| ``minReplica``     | minimum number of allowed replicas              |
+--------------------+-------------------------------------------------+
| ``maxReplica``     | maximum number of allowed replicas              |
+--------------------+-------------------------------------------------+
| ``maxVNFserver``   | maximum # of VNFs the SFC can place on server   |
+--------------------+-------------------------------------------------+
| Definition of traffic flows on the network                           |
+--------------------+-------------------------------------------------+
| ``trafficFlows``   | set of demands (subflows) a traffic flow contain|
+--------------------+-------------------------------------------------+
| ``serviceId``      | identifier of SFC the traffic flow belongs to   |
+--------------------+-------------------------------------------------+
| ``src``            | source node of the traffic flow                 |
+--------------------+-------------------------------------------------+
| ``dst``            | destination node of the traffic flow            |
+--------------------+-------------------------------------------------+
| ``minDem``         | minimum # of demands of the traffic flow        |
+--------------------+-------------------------------------------------+
| ``maxDem``         | maximum # of demands of the traffic flow        |
+--------------------+-------------------------------------------------+
| ``minBw``          | minimum Bandwidth of a demand                   |
+--------------------+-------------------------------------------------+
| ``maxBw``          | maximum Bandwidth of a demand                   |
+--------------------+-------------------------------------------------+




**************
Output Results
**************

The framework stores all optimization results in one output file with filename *example_optimization-model.json*, which is located at directory /target/results/date/, where *optimization-model* is identical to the chosen optimization model in the interface. All results are displayed with the option *offset_results = 1*.

The first blocks shows the used *input files, objective function* and *optimization model*. The block *constrains* gives an overview of the chosen constrains with the following abbrevations;

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
| IPC1      | initialPlacementAsConstraints    |
+-----------+----------------------------------+
| EXP       |  synchronizationTraffic          |
+-----------+----------------------------------+




.. code-block:: java

  "scenario" : {
    "inputFileName" : "network-6",
    "objectiveFunction" : "costs",
    "maximization" : false,
    "model" : "migration_replication",
    "constraints" : {
      "countNumberOfUsedServers" : true,
      "onePathPerDemand" : true,
      "activatePathForService" : true,
      "pathsConstrainedByFunctions" : true,
      "functionPlacement" : true,
      "oneFunctionPerDemand" : true,
      "mappingFunctionsWithDemands" : true,
      "functionSequenceOrder" : true,
      "noParallelPaths" : false,
      "initialPlacementAsConstraints" : false,
      "synchronizationTraffic" : true
    }
  },


The next block shows results for the binary variable :math:`z^{k,s}_{p}` with the following meaning:


(s,p,k): [s] *"service function chain SFC s"* [ n4, n2, n1, n7, n8] *"list of node of the path"*  [ :math:`\lambda^s_{k}` ]


.. code-block:: java

  "stringVariables" : {
    "rSPD" : [
      "(1,2,1): [1][n4, n2, n1, n7, n8][75]",
      "(1,2,2): [1][n4, n2, n1, n7, n8][75]",
      "(2,1,1): [2][n5, n3, n1, n6][150]",
    ],


The next block is experimentally:


.. code-block:: java

    "dSP" : [
      "(1,2): [n4, n2, n1, n7, n8][162.85]",
      "(2,1): [n5, n3, n1, n6][58.76]"
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


The next block shows results for the binary variable :math:`z^{s}_{p}` with the following meaning:


(s,p): [s] *"SFC s"* [ n4, n2, n1, n7, n8] *"list of node of the path"*


.. code-block:: java


    "rSP" : [
      "(1,2): [1][n4, n2, n1, n7, n8]",
      "(2,1): [2][n5, n3, n1, n6]"
    ],



The next block shows results for the binary variable :math:`f^{v,s}_{x}` with the following meaning:


(x,s,v): [n1_0] *"nodenumber_number-of-server-at-nodenumber"* [s] *"SFC s"* [ :math:`f=F^{v,s}_{NF}` ]  *"function type of v-st VNF os SFC s"*


.. code-block:: java

    "pXSV" : [
      "(1,2,1): [n1_0][2][1]",
      "(2,1,3): [n1_1][1][4]",
      "(3,1,1): [n2_0][1][1]",
    ],


The next block is experimentally:

.. code-block:: java

    "sSVP" : [ ]
  },


The next blocks show averaged results for network wide performance measures, these are the  *luSummary*:  the network wide link utilization; *xuSummary*:  the network wide server utilization; *fuSummary*:  the network wide number of VNF allocations per server; *sdSummary*:  experimentally;

Each result block shows the following details: the network wide average value; the minimum value; the maximum value, the variance



.. code-block:: java

  "luSummary" : [
    0.09,
    0.0,
    0.3,
    0.02
  ],

.. code-block:: java

  "xuSummary" : [
    0.14,
    0.0,
    0.3,
    0.02
  ],

.. code-block:: java

  "fuSummary" : [
    5.06,
    0.0,
    9.0,
    19.93
  ],

.. code-block:: java

  "sdSummary" : [
    110.8,
    58.76,
    162.85,68
    850.89
  ],


The next block shows average results for network wide performance measures. These are *avgPathLength*: average path length in hop; *totalTraffic*: total traffic offered to the network; *trafficLinks*: total traffic on all links of the network; *migrationsNum*: number of migrations; *replicationsNum*: total number of replications; *objVal*: objective value of the chosen objective function.


.. code-block:: java

  "avgPathLength" : 3.5,
  "totalTraffic" : 525.0,
  "trafficLinks" : 1800.0,
  "migrationsNum" : 6,
  "replicationsNum" : 0,
  "objVal" : 0.01,

