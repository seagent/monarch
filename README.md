# MonARCh: Monitoring Architecture for SPARQL Result Changes

MonARCh is an architecture based on the actor model implementation Akka which monitors federated SPARQL queries (as well as raw queries enabling them 
converting to federated queries via a query federator engine such as WoDQA) over diverse range of rdf datasets.

Agents register their federated queries to MonARCh via a client actor system. Then MonARCh schedules itself for executing sub queries according to change 
frequencies of datasets. If it detects any change in the dataset related to sub query result, it generates new main result using hash join algorithm
and notifies the relevant agent with the new result.

## Using MonARCh as a Service in Evaluation Execution

### Preparing the Environment

Firstly SPARQL endpoints of DBpedia, Nytimes and Stockmarket datasets should be set up and running.

After that DBpedia and Nytimes data is needed to be insterted into the datasets. Then using ArtificialDataGenerator in [seagent/datasetupdater](https://github.com/seagent/datasetupdater) Stockmarket dataset is created from scratch and data enrichment is made for DBpedia and Nytimes datasets.

**<ins>Note:</ins>** In order to track the actor count and query count metrics, Redis server is needed to be set up and configured to be used by RedisStore class in MonARCh source code.

### Preparing the Executable Binaries

After the environment has been set up, executable binaries of MonARCh and DatasetUpdater are needed to be prepared. MonARCh is written in Scala, and DatasetUpdater is written in Java.

#### <ins>MonARCh</ins>

In order to build the executable binaries for MonARCh, simply execute following commands inside <ins>'monarch-master'</ins> folder after cloning or downloading source code:

```console
sbt clean
sbt stage
```
Binaries are generated under *<ins>'target/universal/stage'</ins>* folder as *<ins>bin</ins>* and *<ins>lib</ins>* folders. Simply create  put these folders under a wrapping folder like *<ins>monarch</ins>*, then executables for MonARCh are ready.

#### <ins>DatasetUpdater</ins>

In order to build the executable binaries for DatasetUpdater, simply execute following commands inside *'datasetupdater/DatasetUpdater'* folder after cloning or downloading source code:

```console
mvn clean
mvn install
```
Binaries are generated under the *'<ins>target</ins>'* folder as **DatasetUpdater-0.0.1-SNAPSHOT-allinone.jar**. Simply rename the jar as **DatasetUpdater.jar**, then executable for *DatasetUpdater* is ready.

### Setting up the Cluster

In order to monitor more queries, JVM heap of the cluster nodes need to be set as high as possible with the following command below:

```console
export _JAVA_OPTIONS="-Xmx30g"
```
Note: 30g is just an example amount, just simply change ip with the amount of heap memory you desired


Firstly, in a unix based terminal we need to change the permissions of monarch folder as executable with the command below:

```console
chmod 755 -R monarch/*
```

For the master node simply execute following command by replacing *<ins>ip_address</ins>* with the ip address of that node.

```console
./monarch/bin/app ip_address 2551 clean
```
For joining new nodes to the cluster, simply execute following command by replacing *<ins>ip_address</ins>* with the ip address of that node

```console
./monarch/bin/app ip_address 2551
```

### Setting up DatasetUpdater Programs

For updating Nytimes following command is executed in terminal:

```console
java -cp DatasetUpdater.jar main.MainNytimesUpdater
```

For updating StockMarket following command is executed in terminal:
```console
java -cp DatasetUpdater.jar main.MainStockUpdater
```
### Setting up MonARCh Client Program

Client actor program for sending SPARQL queries to MonARCh is already present under the monarch folder containing the binaries. Also in order to execute the generated Agent for the evaluation below command is executed:

```console
./monarch/bin/agent-app ip_address port query_count query_percent_in_min selectivity sub_res_count_dbp sub_res_count_nyt sub_res_count_stockmarket
```

* *<ins>ip_address:</ins>* ip address of the client node
* *<ins>port:</ins>* port for the client node
* *<ins>query_count:</ins>* total query count that is to be sent to the MonARCh cluster
* *<ins>query_percent_in_min:</ins>* query percentage that is wanted to be sent to the MonARCh cluster per minute until reaching the total query count
* *<ins>selectivity:</ins>* selectivity of that query template (Choices are: MOST, HIGH, MID, LOW, LEAST)
* *<ins>sub_res_count_dbp:</ins>* sub result count that is wanted to be returned from the relevant DBpedia sub query (Choices are: ALL, 4000, 3000, 2000, 1000)
* *<ins>sub_res_count_nyt:</ins>* sub result count that is wanted to be returned from the relevant Nytimes sub query (Choices are: ALL, 4000, 3000, 2000, 1000)
* *<ins>sub_res_count_stock:</ins>* sub result count that is wanted to be returned from the relevant Stockmarket sub query (Choices are: ALL, 4000, 3000, 2000, 1000)

For example below command is for running evaluation by running agent-app on node with ip address as **<ins>'155.223.25.4'</ins>**, port as '2553', query count as '2500', query percentage in min as '0.2' meaning that '500' queries per min, selectivity as 'MID' and sub result count for DBpedia, Nytimes and Stockmarket as 1000 meaning that '%80' sub result filtering ratio:

```console
./monarch/bin/agent-app 155.223.25.4 2553 2500 0.2 MID 1000 1000 1000
```
