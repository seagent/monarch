package monitoring.message

@SerialVersionUID(52L)
case class ExecuteSubQuery(query:String, endpoint:String) extends Serializable