# nsp4j-optimizer

## Network and Service management Planning framework for Java - Optimizer


## Build and run

### Prerequisites

- Apache Maven >= 3.5.2
- java (openjdk) >= 11.0.8
- git >= 2.29.2
- Gurobi optimizer >= 8.1.1


### Installing and running the nsp4j-optimizer tool

1. After installing Gurobi Optimizer, find the `gurobi.jar` file and install it
   on your local maven repository. Change the installation path accordingly and
   run:
   
   	```
	mvn install:install-file -Dfile=/opt/gurobi910/linux64/lib/gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi-optimizer -Dversion=9.1.0 -Dpackaging=jar
	```


2. Clone the git repository and package:

	```
	git clone https://github.com/fcarp10/nsp4j-optimizer.git
	cd /nsp4j-optimizer
	mvn package
	```

3. Change the VERSION accordingly and run the jar:
	
	```
	cd /target
	java -jar nsp4j-optimizer-${VERSION}-jar-with-dependencies.jar
	```

- This will launch the application on your local machine trough the port 8080.
  Using a browser, you can access to the GUI in `localhost:8080`.


### Troubleshooting

In some Linux distributions (tested on Ubuntu and Arch based distros) and Mac OS you
might need to specify the Java library path according to your installation path.

    -Djava.library.path=/opt/gurobi910/linux64/lib/

Also, depending which Linux distros (tested on Ubuntu and Arch based distros), you
might need to add a file named `randomLibs.conf` to the directory
`/etc/ld.so.conf.d/` specifying the gurobi installation path:

	echo "/opt/gurobi910/linux64/lib/" > /etc/ld.so.conf.d/randomLibs.conf
	sudo ldconfig

    


