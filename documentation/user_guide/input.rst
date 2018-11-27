****************
Input Parameters
****************

The framework requires three different input files: one describing the network topology, one with all paths listed and one for the specific parameters. All three files must have exactly the same name with different extensions:\*.dgs for the topology, \*.txt for the paths and \*.yml for the parameters.

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
    chain: [1, 3, 5]
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
        "maxSharedSFC": 1,
        "maxSharedVNF": 1,
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
        "maxSharedSFC": 5,
        "maxSharedVNF": 3,
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
| ``weights``        | weight of migration, server and link costs      |
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