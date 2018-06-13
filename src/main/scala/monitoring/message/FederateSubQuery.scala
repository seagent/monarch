package monitoring.message

@SerialVersionUID(51L)
case class FederateSubQuery (query: String, endpoints: Seq[String]) extends Serializable
