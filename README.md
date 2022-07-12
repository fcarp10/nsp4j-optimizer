# nsp4j-optimizer

Network and Service management Planning framework for Java - Optimizer

Prerequisites

- [Apache Maven](https://maven.apache.org/)
- [openjdk 11](https://openjdk.org/) (or higher)
- [Gurobi optimizer v8.1](https://www.gurobi.com/) (or higher)


### Build and run

1. Install `gurobi.jar` library on your local maven repository:
   
   	```shell
	mvn install:install-file -Dfile=/opt/gurobi910/linux64/lib/gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi-optimizer -Dversion=9.1.0 -Dpackaging=jar
	```


2. Clone, install maven plugins and package:

	```shell
	git clone https://github.com/fcarp10/nsp4j-optimizer.git
	cd nsp4j-optimizer/
	mvn install
	mvn package
	```

3. Run:
	
	```shell
	cp src/main/resources/scenarios/{7nodes.dgs,7nodes.txt,7nodes.yml} target
	cd target/
	java -jar nsp4j-optimizer-${VERSION}-jar-with-dependencies.jar
	```

4. Access to the dashboard in `localhost:8082`, `load` the default topology and
   `run` to start the optimizer.

5. Check in `target/results/` for the generated results.


### Troubleshooting

You may need to specify to the Java library path, your Gurobi installation path
as follows:

    -Djava.library.path=/opt/gurobi910/linux64/lib/

In Linux, you may need to add a file named `randomLibs.conf` to the directory
`/etc/ld.so.conf.d/` specifying the gurobi installation path:

	echo "/opt/gurobi910/linux64/lib/" > /etc/ld.so.conf.d/randomLibs.conf
	sudo ldconfig

    


