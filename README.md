# MonARCh: Monitoring Architecture for SPARQL Result Changes

MonARCh is an architecture based on the actor model implementation Akka which monitors federated SPARQL queries (as well as raw queries enabling them 
converting to federated queries via a query federator engine such as WoDQA) over diverse range of rdf datasets.

Agents register their federated queries to MonARCh via a client actor system. Then MonARCh schedules itself for executing sub queries according to change 
frequencies of datasets. If it detects any change in the dataset related to sub query result, it generates new main result using hash join algorithm
and notifies the relevant agent with new result.

# Using MonARCh in Evaluation Execution

Firstly SPARQL endpoints of DBpedia, Nytimes and Stockmarket datasets should be set up and running.

After that DBpedia and Nytimes data is need to be insterted into the datasets. Then using ArtificialDataGenerator in [seagent/datasetupdater](https://github.com/seagent/datasetupdater) Stockmarket dataset is created from scratch and data enrichment is made for DBpedia and Nytimes datasets.
