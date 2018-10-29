************
Installation
************

This is the installation guide for the NFV-optimization tool.

Prerequisites
=============

- Apache Maven 3.5.2
- Java 11 or higher
- Git
- Gurobi solver 8.0.0 or higher

Installing and running the NFV Optimization tool
================================================

1. Install Gurobi in your local maven repository. Change `` $PATH_TO_GUROBI_LIB$`` for the specific path where Gurobi is installed:

.. code-block:: bash

	mvn install:install-file -Dfile=$PATH_TO_GUROBI_LIB$/gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi-solver -Dversion=8.0.0 -Dpackaging=jar

2. Clone git repository:

.. code-block:: bash

	git clone https://FranCarpio@bitbucket.org/FranCarpio/nfv-optimization.git

3. Package the sources:

.. code-block:: bash

	cd /nfv-optimization
	mvn package

4. Run the created jar:
	
.. code-block:: bash

	cd /target
	java -jar nfv-optimization-$VERSION$-jar-with-dependencies.jar

This will launch the application on your local machine trough the port 8080. Open your browser and access to ``localhost:8080``, you will access to the GUI:

.. image:: /_static/interface.png
    :align: center

*NOTE: in Linux or Mac OS distributions you might need to specify the Java library path ``-Djava.library.path=/opt/gurobi810/linux64/lib/``.

**NOTE: in Ubuntu, add a file named ``randomLibs.conf`` to the directory ``/etc/ld.so.conf.d/``. Add the next line to this file: ``/opt/gurobi810/linux64/lib/`` (according to your Gurobi version). Finally, run the following command in the terminal: ``sudo ldconfig``.