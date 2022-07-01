# MonARCh: Monitoring Architecture for SPARQL Result Changes

MonARCh is an architecture based on the actor model implementation Akka which monitors federated SPARQL queries (as well as raw queries enabling them 
converting to federated queries via a query federator engine such as WoDQA) over diverse range of rdf datasets.

Agents register their federated queries to MonARCh via a client actor system. Then MonARCh schedules itself for executing sub queries according to change 
frequencies of datasets. If it detects any change in the dataset related to sub query result, it generates new main result using hash join algorithm
and notifies the relevant agent with new result.

## Using MonARCh as Service in Evaluation Execution

### Preparing the Environment

Firstly SPARQL endpoints of DBpedia, Nytimes and Stockmarket datasets should be set up and running.

After that DBpedia and Nytimes data is need to be insterted into the datasets. Then using ArtificialDataGenerator in [seagent/datasetupdater](https://github.com/seagent/datasetupdater) Stockmarket dataset is created from scratch and data enrichment is made for DBpedia and Nytimes datasets.

Note: In order to track the actor count and query count metrics, Redis server is needed to be set up and configured to be used by RedisStore class in MonARCh source code.

### Preparing the Executable Codes

After the environment has been set up, executable codes of MonARCh and DatasetUpdater are need to be prepared. MonARCh is written in Scala, and DatasetUpdater is written in Java.

#### <ins>MonARCh</ins>

In order to build the executable binaries for MonARCh, simply execute following commands inside 'monarch-master' folder after cloning or downloading source code:

```console
sbt clean
sbt stage
```
Binaries are generated under *'target/universal/stage'* folder as *bin* and *lib* folders. Simply create  put these folders under a wrapping folder like *monarch*, then executables for MonARCh are ready.

#### <ins>DatasetUpdater</ins>

In order to build the executable binaries for DatasetUpdater, simply execute following commands inside 'datasetupdater/DatasetUpdater' folder after cloning or downloading source code:

```console
mvn clean
mvn install
```
Binaries are generated under the *'target'* folder as *DatasetUpdater-0.0.1-SNAPSHOT-allinone.jar*. Simply rename the jar as *DatasetUpdater.jar*, then executable for DatasetUpdater is ready.

