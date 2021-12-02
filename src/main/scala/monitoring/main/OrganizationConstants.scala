package monitoring.main

object OrganizationConstants {
  /**
    * TODO: Sorgu federated yapılacak
    */
  val STOCK_QUERY_TEMPLATE: String = "SELECT ?nytCompany ?articleCount ?stockPrice WHERE {" +
    "<%s> <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany. " +
    "?nytCompany <http://data.nytimes.com/elements/associated_article_count> ?articleCount." +
    "?nytCompany <http://stockmarket.com/elements/stockPrice> ?stockPrice}"
  val FEDERATED_STOCK_QUERY_TEMPLATE: String = "SELECT  ?nytCompany ?articleCount ?stockPrice WHERE {" +
    "SERVICE <http://155.223.25.4:8890/dbpedia/sparql> {" +
    "<%s> <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany." +
    "}SERVICE <http://155.223.25.1:8890/nytimes/sparql> {" +
    "?nytCompany <http://data.nytimes.com/elements/associated_article_count> ?articleCount." +
    "}SERVICE <http://155.223.25.2:8890/stockmarket/sparql> {" +
    "?nytCompany <http://stockmarket.com/elements/stockPrice> ?stockPrice. }}"

  val FEDERATED_TYPED_STOCK_QUERY_TEMPLATE: String = "SELECT  ?nytCompany ?articleCount ?stockPrice WHERE {" +
    "SERVICE <http://155.223.25.4:8890/dbpedia/sparql> {" +
    "<%s> <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany." +
    "}SERVICE <http://155.223.25.1:8890/nytimes/sparql> {" +
    "?nytCompany <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://data.nytimes.com/elements/Company>." +
    "?nytCompany <http://data.nytimes.com/elements/associated_article_count> ?articleCount." +
    "}SERVICE <http://155.223.25.2:8890/stockmarket/sparql> {" +
    "?nytCompany <http://stockmarket.com/elements/stockPrice> ?stockPrice. }}"

  val FEDERATED_FILTERED_TYPED_STOCK_QUERY_TEMPLATE: String = "SELECT  ?nytCompany ?articleCount ?stockPrice WHERE {" +
    "SERVICE <http://155.223.25.4:8890/dbpedia/sparql> {" +
    "<%s> <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany." +
    "}SERVICE <http://155.223.25.1:8890/nytimes/sparql> {" +
    "?nytCompany <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://data.nytimes.com/elements/Company>." +
    "?nytCompany <http://data.nytimes.com/elements/associated_article_count> ?articleCount." +
    "FILTER (?articleCount > 40)" +
    "}SERVICE <http://155.223.25.2:8890/stockmarket/sparql> {" +
    "?nytCompany <http://stockmarket.com/elements/stockPrice> ?stockPrice. }}"

  val RESOURCE_PATH = "src/main/resources/"
  val VOID_PATH: String = RESOURCE_PATH + "void/"
  val OWL_SAME_AS = "http://www.w3.org/2002/07/owl#sameAs"

  def createStockQuery(companyURI: String, index: Int) = {
    s"""SELECT ?nytCompany$index ?articleCount$index ?stockPrice$index WHERE {
       |<$companyURI> <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany$index.
       |?nytCompany$index <http://data.nytimes.com/elements/associated_article_count> ?articleCount$index.
       |?nytCompany$index <http://stockmarket.com/elements/stockPrice> ?stockPrice$index}""".stripMargin
  }

  def createFederatedGenericDbpediaQuery(index: Int) = {
    s"""SERVICE <http://155.223.25.4:8890/dbpedia/sparql> {"
      "|?dbpediaCompany$index <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Company>."
      "|?dbpediaCompany$index <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany$index."
      "|}SERVICE <http://155.223.25.1:8890/nytimes/sparql> {"
      "|?nytCompany$index <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://data.nytimes.com/elements/Company>."
      "|?nytCompany$index <http://data.nytimes.com/elements/associated_article_count> ?articleCount$index."
      "|}SERVICE <http://155.223.25.2:8890/stockmarket/sparql> {"
      "|?nytCompany$index <http://stockmarket.com/elements/stockPrice> ?stockPrice$index. }}""".stripMargin
  }

  def generateFederatedQuery(selectionDBP: String, selectionNYT: String,selectionSTK: String): String={

    var (lowerBound,upperBound,reputationValues,marketValues)=(0,999999,"","")
    var filterDBpedia=""
    var valuesNytimes=""
    var valuesStockmarket=""

    selectionDBP match {
      case "ALL" => filterDBpedia=""
      case "4500" => lowerBound=10000; upperBound=100000;
      case "4000" => lowerBound=20000; upperBound=100000;
      case "3500" => lowerBound=30000; upperBound=100000;
      case "3000" => lowerBound=40000; upperBound=100000;
      case "2500" => lowerBound=50000; upperBound=100000;
      case "2000" => lowerBound=60000; upperBound=100000;
      case "1500" => lowerBound=70000; upperBound=100000;
      case "1000" => lowerBound=80000; upperBound=100000;
      case "500" => lowerBound=90000; upperBound=100000;
    }

    selectionNYT match {
      case "ALL" => valuesNytimes=""
      case "4000" => reputationValues="('Very High'^^xsd:string)('High'^^xsd:string)('Medium'^^xsd:string)('Low'^^xsd:string)";
      case "3000" => reputationValues="('Very High'^^xsd:string)('High'^^xsd:string)('Medium'^^xsd:string)";
      case "2000" => reputationValues="('Very High'^^xsd:string)('High'^^xsd:string)";
      case "1000" => reputationValues="('Very High'^^xsd:string)";
    }

    selectionSTK match {
      case "ALL" => valuesStockmarket=""
      case "4000" => marketValues="('NYSE'^^xsd:string)('TSE'^^xsd:string)('FWB'^^xsd:string)('LSE'^^xsd:string)";
      case "3000" => marketValues="('NYSE'^^xsd:string)('TSE'^^xsd:string)('FWB'^^xsd:string)";
      case "2000" => marketValues="('NYSE'^^xsd:string)('TSE'^^xsd:string)";
      case "1000" => marketValues="('NYSE'^^xsd:string)";
    }

    filterDBpedia=s"FILTER (strstarts(str(?dbpediaCompany), 'http://dbpedia.org/resource/company-')&&(?staffCount>$lowerBound&&?staffCount<=$upperBound))"
    valuesNytimes=s"VALUES (?reputation) {$reputationValues}"
    valuesStockmarket=s"VALUES (?market) {$marketValues}"

    val templateQuery=s"""
       |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
       |PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
       |PREFIX dbo: <http://dbpedia.org/ontology/>
       |PREFIX dbpedia: <http://dbpedia.org/resource/>
       |PREFIX nytimes: <http://data.nytimes.com/elements/>
       |PREFIX stockmarket: <http://stockmarket.com/elements/>
       |PREFIX owl: <http://www.w3.org/2002/07/owl#>
       |Select * where {
       |service <http://155.223.25.4:8890/dbpedia/sparql> {
       |	?dbpediaCompany rdf:type dbo:Company.
       |	?dbpediaCompany dbo:numberOfStaff ?staffCount.
       |	?dbpediaCompany owl:sameAs ?nytCompany. $filterDBpedia}
       |service <http://155.223.25.1:8890/nytimes/sparql> {
       |	?nytCompany rdf:type nytimes:Company.
       |	?nytCompany nytimes:reputation ?reputation. $valuesNytimes}
       |service <http://155.223.25.2:8890/stockmarket/sparql> {
       |	?nytCompany rdf:type stockmarket:Company.
       |	?nytCompany stockmarket:market ?market.
       |	?nytCompany stockmarket:currency ?currency. $valuesStockmarket}
       |}
       |""".stripMargin

    templateQuery
  }

}
