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

	git clone https://github.com/fcarp10/nsp4j-optimizer.git

3. Package the sources:

.. code-block:: bash

	cd /nsp4j-optimizer
	mvn package

4. Run the created jar:
	
.. code-block:: bash

	cd /target
	java -jar nsp4j-optimizer-$VERSION$-jar-with-dependencies.jar

This will launch the application on your local machine trough the port 8080. Open your browser and access to ``localhost:8080``, you will access to the GUI:

.. image:: /_static/interface.png
    :align: center

NOTE: in Linux or Mac OS distributions you might need to specify the Java library path ``-Djava.library.path=/opt/gurobi810/linux64/lib/``, according to your installation path.

NOTE: in some Linux distribution you might need to add a file named ``randomLibs.conf`` to the directory ``/etc/ld.so.conf.d/``. Then, add the next line to this file: ``/opt/gurobi810/linux64/lib/`` (according to your Gurobi installation path and version). Finally, run the following command in the terminal: ``sudo ldconfig``.