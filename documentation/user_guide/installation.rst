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

There are 3 different combination of weighted functions that can be used as optimization target. The objective functions are defined in section *Optimization models*.

Model:
^^^^^^

The objective functions defined above can be used to build different optimization models, where each of the objective functions can be chosen as an alternative. The optimization mmodels are i) Initial placement; ii) Migration; iii) Replication; and iv) Replication and mMigration. For each model, one of the objective functions can be freely chosen, where in principle all general constraints can be applied.

Constraints:
^^^^^^^^^^^^

Depending on the selected optimization model, some of the constraints are automatically preselected. Further constraints can be added depending on the special topology, VNF architecture and design assumptions. All constraints are documented in section *Constraints*.  It should be noted that although it is possible to select arbitrary constraints, the validity is not checked by the program.