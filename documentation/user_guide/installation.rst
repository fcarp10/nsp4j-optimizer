************
Installation
************

This is the installation guide for the nsp4j-optimizer tool.

Prerequisites
=============

- Apache Maven 3.5.2
- Java 11 or higher
- Git
- Gurobi solver 8.0.0 or higher

Installing and running the nsp4j-optimizer tool
===============================================

1. Install Gurobi in your local maven repository. Change `` $PATH_TO_GUROBI_LIB$`` for the specific path where Gurobi is installed:

.. code-block:: bash

	mvn install:install-file -Dfile=$PATH_TO_GUROBI_LIB$/gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi-solver -Dversion=8.0.0 -Dpackaging=jar

2. Clone git repository:

.. code-block:: bash

	git clone https://carpio@bitbucket.org/carpio/nsp4j-optimizer.git

3. Package the sources:

.. code-block:: bash

	cd /nsp4j-optimizer
	mvn package

4. Run the created jar:
	
.. code-block:: bash

	cd /target
	java -jar nsp4j-optimizer-$VERSION$-jar-with-dependencies.jar

This will launch the application on your local machine trough the port 8080.


NOTE: in Linux or Mac OS distributions you might need to specify the Java library path ``-Djava.library.path=/opt/gurobi810/linux64/lib/``.

NOTE: in Ubuntu, add a file named ``randomLibs.conf`` to the directory ``/etc/ld.so.conf.d/``. Add the next line to this file: ``/opt/gurobi810/linux64/lib/`` (according to your Gurobi version). Finally, run the following command in the terminal: ``sudo ldconfig``.


Running the nsp4j-optimizer tool
================================

Open your browser and access to ``localhost:8080``, you will access to the GUI:

.. image:: /_static/interface.png
    :align: center



Input files name:
^^^^^^^^^^^^^^^^^

The file name  *example* will identify 3 different input files with names The framework requires three different input files:  example.dgs, example.yml and example.txt. The description can be found in section *Input Parameters*.


Objective function:
^^^^^^^^^^^^^^^^^^^

There are  different objective functions that can be used as optimization target. The objective functions are defined in section *Optimization models*.


Weights:
^^^^^^^^
Some of the objective functions can be combination using the weighting parameters. They have the following meaning:

Cost objectives: W1 * Link utilization costs + W2 * Server utilization costs

Utilization objectives: W1 * Link utilization + W2 * Server utilization

Maximal utilization objective: W1 * Link utilization + W2 * Server utilization + W3 * maximum utilization

Model:
^^^^^^

The objective functions defined above can be used to build different optimization models, where each of the objective functions can be chosen as an alternative. The optimization models are:

i) Initial placement

ii) Migration

iii) Replication

iv) Replication and Migration

The Initial Placement is not required for the models ii) - iv) and is restricted to service function chains (SFCs) only using a single path and no replications.  For each model, one of the objective functions can be freely chosen, where in principle all general constraints can be applied.

Constraints:
^^^^^^^^^^^^

Depending on the selected optimization model, some of the constraints are automatically preselected. Further constraints can be added depending on the special topology, VNF architecture and design assumptions. All constraints are documented in section *Constraints*.  It should be noted that although it is possible to select arbitrary constraints, the validity is not checked by the program. A rough overview of the meaning is given below


+-----------+---------------------------------------------------+
| Short     | meaning                                           |
+===========+===================================================+
| RPC1      | only one path per traffic demand                  |
+-----------+---------------------------------------------------+
| RPC2      | bounds for the number of active paths per SFC     |
+-----------+---------------------------------------------------+
| RPC3      | only one path per SFC                             |
+-----------+---------------------------------------------------+
| RPI1      | variable, indicate activate path for SFC          |
+-----------+---------------------------------------------------+
| VAI1      | variable, indicate usage of server by VNF of a SFC|
+-----------+---------------------------------------------------+
| VAI2      | variable, indicate usage of server by a SFC       |
+-----------+---------------------------------------------------+
| VAI3      |variable, indicate usage of server                 |
+-----------+---------------------------------------------------+
| VAC1      | allocates all VNFs on active path                 |
+-----------+---------------------------------------------------+
| VAC2      | per demand one VNF per server                     |
+-----------+---------------------------------------------------+
| VAC3      | VNF sequence order                                |
+-----------+---------------------------------------------------+
| VRC1      | number of replica bounded by active paths         |
+-----------+---------------------------------------------------+
| VRC2      | number of replica equal to active paths           |
+-----------+---------------------------------------------------+
| VRC3      | bound for the number of replica                   |
+-----------+---------------------------------------------------+
| VSC1      | bound number of VNFS per SFCs and server          |
+-----------+---------------------------------------------------+
| VSC2      | bound number of SFCs per server                   |
+-----------+---------------------------------------------------+
| VSC3      |  bound number of demands per VNF, SFCs and server |
+-----------+---------------------------------------------------+
| IPC1      | initial placement as constraints                  |
+-----------+---------------------------------------------------+
| DIC1      | VNF processing load constraint                    |
+-----------+---------------------------------------------------+
| DVC1      | VNF processing load constrain for variable # of VM|
+-----------+---------------------------------------------------+
| DVC2      | variable # of VM instances usage                  |
+-----------+---------------------------------------------------+
| DVC3      | variable # of VMs dimensioning rule               |
+-----------+---------------------------------------------------+
| PDC1      | end-to-end SFC delay constraint                   |
+-----------+---------------------------------------------------+

