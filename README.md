# nsp4j-optimizer

#### Network and Service management Planning framework for Java - Optimizer


##Installation

This is the installation guide for the nsp4j-optimizer tool.

### Prerequisites

- Apache Maven 3.5.2 or higher
- Java 11 or higher
- Git
- Gurobi solver 9 or higher

### Installing and running the nsp4j-optimizer tool


- Install Gurobi in your local maven repository. Change `` $PATH_TO_GUROBI_LIB$`` for the specific path where Gurobi is installed:


	mvn install:install-file -Dfile=$PATH_TO_GUROBI_LIB$/gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi-solver -Dversion=8.0.0 -Dpackaging=jar

- Clone git repository and package:


	git clone https://github.com/fcarp10/nsp4j-optimizer.git
	cd /nsp4j-optimizer
	mvn package

- Run the created jar:
	

	cd /target
	java -jar nsp4j-optimizer-$VERSION$-jar-with-dependencies.jar

- This will launch the application on your local machine trough the port 8080. Open your browser and access to `localhost:8080`, you will access to the GUI.


- In some Linux or Mac OS distributions you might need to specify the Java library path according to your installation path.

    
    -Djava.library.path=/opt/gurobi810/linux64/lib/
    

- In some Linux distribution you might need to add a file named `randomLibs.conf` to the directory `/etc/ld.so.conf.d/`:


    sudo nano /etc/ld.so.conf.d/randomLibs.conf
    
    
- Then, add the next line to that file, according to your Gurobi installation path and version, and run `ldconfig`:
 
    
    /opt/gurobi810/linux64/lib/
    sudo ldconfig

    


