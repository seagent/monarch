package monitoring.main

import akka.serialization.Serializer
import monitoring.message._
import play.api.libs.json.Json

class MonitoringSerializer extends Serializer {
  // If you need logging here, introduce a constructor that takes an ExtendedActorSystem.
  // class MyOwnSerializer(actorSystem: ExtendedActorSystem) extends Serializer
  // Get a logger using:
  // private val logger = Logging(actorSystem, this)

  // This is whether "fromBinary" requires a "clazz" or not
  def includeManifest: Boolean = true

  // Pick a unique identifier for your Serializer,
  // you've got a couple of billions to choose from,
  // 0 - 40 is reserved by Akka itself
  def identifier = 1234567

  // "toBinary" serializes the given object to an Array of Bytes
  def toBinary(obj: AnyRef): Array[Byte] = {

    obj match {
      case fq: FederateQuery => Json.toBytes(Json.toJsObject(fq))
      case fsq: FederateSubQuery => Json.toBytes(Json.toJsObject(fsq))
      case esq: ExecuteSubQuery => Json.toBytes(Json.toJsObject(esq))
      case db: DistributeBuckets => Json.toBytes(Json.toJsObject(db))
      case res: Result => Json.toBytes(Json.toJsObject(res))
      case phj: PerformHashJoin => Json.toBytes(Json.toJsObject(phj))
      case rc: ResultChange => Json.toBytes(Json.toJsObject(rc))
      case _ => Array[Byte]()
    }

  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  def fromBinary(
                  bytes: Array[Byte],
                  clazz: Option[Class[_]]): AnyRef = {
    clazz.get.getSimpleName match {
      case "FederateQuery" => Json.parse(bytes).as[FederateQuery]
      case "FederateSubQuery" => Json.parse(bytes).as[FederateSubQuery]
      case "ExecuteSubQuery" => Json.parse(bytes).as[ExecuteSubQuery]
      case "DistributeBuckets" => Json.parse(bytes).as[DistributeBuckets]
      case "Result" => Json.parse(bytes).as[Result]
      case "PerformHashJoin" => Json.parse(bytes).as[PerformHashJoin]
      case "ResultChange" => Json.parse(bytes).as[ResultChange]
      case _ => None
    }
  }
}