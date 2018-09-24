****************
Input Parameters
****************

The tool requires three different input files: the topology, the paths and the parameters files. All three files must have the same name with different extensions. The extension must be \*.dgs, \*.txt and \*.yml for the topology, paths and parameters, respectively.

Topology file
=============

The desired network topology is described using the GraphStream guidelines in a file with extesion \*.dgs. For instance:

.. code-block:: text

	DGS004
	test 0 0

    an n1 x:100 y:150 capacity:1000 serversNode: 1 type: 0
    an n2 x:150 y:100 capacity:1000 serversNode: 1 type: 0
    an n3 x:200 y:100 capacity:1000 serversNode: 1 type: 0
    an n4 x:175 y:200 capacity:1000 serversNode: 1 type: 0
    an n5 x:250 y:150 capacity:1000 serversNode: 1 type: 0
    an n6 x:300 y:150 capacity:1000 serversNode: 1 type: 0
    an n7 x:350 y:100 capacity:1000 serversNode: 1 type: 0
    an n8 x:350 y:200 capacity:1000 serversNode: 1 type: 0
    an n9 x:400 y:150 capacity:1000 serversNode: 1 type: 0

    ae n1n2 n1 > n2 capacity:1000
    ae n1n4 n1 > n4 capacity:1000
    ae n2n1 n2 > n1 capacity:1000
    ae n2n3 n2 > n3 capacity:1000
    ae n3n2 n3 > n2 capacity:1000
    ae n3n5 n3 > n5 capacity:1000
    ae n3n7 n3 > n7 capacity:1000
    ae n4n1 n4 > n1 capacity:1000
    ae n4n5 n4 > n5 capacity:1000
    ae n5n3 n5 > n3 capacity:1000
    ae n5n4 n5 > n4 capacity:1000
    ae n5n6 n5 > n6 capacity:1000
    ae n6n5 n6 > n5 capacity:1000
    ae n6n7 n6 > n7 capacity:1000
    ae n6n8 n6 > n8 capacity:1000
    ae n7n3 n7 > n3 capacity:1000
    ae n7n6 n7 > n6 capacity:1000
    ae n7n9 n7 > n9 capacity:1000
    ae n8n6 n8 > n6 capacity:1000
    ae n8n9 n8 > n9 capacity:1000
    ae n9n7 n9 > n7 capacity:1000
    ae n9n8 n9 > n8 capacity:1000


``an`` adds a node. The command is followed by the unique node identifier allowing to identify this node compared to others. This name can be a single word or a string delimited by the double quote character. Values x and y on the server represent the coordinates of the nodes. For each node can be specified the number of servers (i.e serversNode), the capacity of each server (i.e. capacity) and the type of server.

``ae`` adds an link. This command must be followed by the unique identifier of the link, and then the identifiers of two other nodes. For each link, the specific capacity can be specified (i.e. capacity)/

For further information on the use of a DGS file see `<http://graphstream-project.org/doc/Advanced-Concepts/The-DGS-File-Format/>`_


Paths file
==========

The admissible paths for the topology must be specified in the path file with the same name as the topology but with extension \*.txt. For instance:

.. code-block:: text

	[n1, n2, n3, n7, n9]
	[n1, n4, n5, n3, n7, n9]
	[n1, n4, n5, n6, n7, n9]

This paths can be externally generated or generated using the already included KShortestPath tool. (TO BE EXTENDED)

Parameters file (TO BE UPDATED)
===============================

On this file the main characteristics of the network can be stablished by setting the desired values of the variables on the *Config.yml* file:


.. code-block:: yaml

	networkFile: network
	pathsFile: paths
	gap: 0
	weights: [0.0, 1.0]
	serverCapacity: 1000
	serversPerNode: 1
	linkCapacity: 1000
	maxReplicas: 3
	functionTypes:
	  0:
	    type: 0
	    replicable: false
	    load: 1.0
	  1:
	    type: 1
	    replicable: true
	    load: 1.0
	  2:
	    type: 2
	    replicable: false
	    load: 1.0
	serviceTypes:
	  0:
	    id: 0
	    chain: [0, 1, 2]
	trafficFlows:
	  - src: "n1"
	    dst: "n9"
	    serviceId: 0
	  - src: "n2"
	    dst: "n9"
	    serviceId: 0
	minDemands: 3
	maxDemands: 3
	minBw: 100
	maxBw: 100


+-------------------------------------------------------------------+
| Variables of *config.yml*                                         |
+====================+==============================================+
| ``networkFile``    | name of the file containing the network data |
+--------------------+----------------------------------------------+
| ``pathsFile``      | name of the file containing the paths data   |
+--------------------+----------------------------------------------+
| ``gap``            | gap optimization value                       |
+--------------------+----------------------------------------------+
| ``weights``        | weight of migration, server and link costs   |
+--------------------+----------------------------------------------+
| ``serverCapacity`` | capacity of the server measured on  units    |
+--------------------+----------------------------------------------+
| ``serversPerNode`` | number of servers on each node               |
+--------------------+----------------------------------------------+
| ``linkCapacity``   | capacity of the links measured on  units     |
+--------------------+----------------------------------------------+
| ``maxReplicas``    | maximum number of allowed replicas           |
+--------------------+----------------------------------------------+
| ``functionTypes``  | Virtual Network Functions on the network     |
+--------------------+----------------------------------------------+
| ``type``           | identifier of the VNF                        |
+--------------------+----------------------------------------------+
| ``replicable``     | indicates if the VNF can be replicable       |
+--------------------+----------------------------------------------+
| ``load``           | load of the VNF                              |
+--------------------+----------------------------------------------+
| ``serviceTypes``   | Service Function Chains on the network       |
+--------------------+----------------------------------------------+
| ``id``             | identifier of the SFC                        |
+--------------------+----------------------------------------------+
| ``trafficFlows``   | traffic flows on the network                 |
+--------------------+----------------------------------------------+
| ``src``            | source node of the traffic flow              |
+--------------------+----------------------------------------------+
| ``dst``            | destination node of the traffic flow         |
+--------------------+----------------------------------------------+
| ``serviceId``      | identifier of the traffic flow               |
+--------------------+----------------------------------------------+
| ``minDemands``     | minimum number of possible traffic demands   |
+--------------------+----------------------------------------------+
| ``maxDemands``     | maximum number of possible traffic demands   |
+--------------------+----------------------------------------------+
| ``minBw``          | minimum Bandwidth                            |
+--------------------+----------------------------------------------+
| ``maxBw``          | maximum Bandwidth                            |
+--------------------+----------------------------------------------+





