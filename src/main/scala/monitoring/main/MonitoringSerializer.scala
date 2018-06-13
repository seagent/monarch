package monitoring.main

import akka.serialization.Serializer
import io.protoless.messages.{Decoder, Encoder}
import monitoring.message.{ExecuteSubQuery, FederateQuery, FederateSubQuery}
import io.protoless.generic.auto._
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

    //Json.toBytes(Json.toJson[FederateQuery](obj.asInstanceOf[FederateQuery]))
    obj match {
      case fq: FederateQuery => Encoder[FederateQuery].encodeAsBytes(fq)
      case fsq: FederateSubQuery => Encoder[FederateSubQuery].encodeAsBytes(fsq)
      case esq: ExecuteSubQuery => Encoder[ExecuteSubQuery].encodeAsBytes(esq)
      case _ => Array[Byte]()
    }

    //#...
  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  def fromBinary(
                  bytes: Array[Byte],
                  clazz: Option[Class[_]]): AnyRef = {
   /* val fq=classOf[FederateQuery]
    val fsq=classOf[FederateSubQuery]
    val esq=classOf[ExecuteSubQuery]
    //Json.fromJson[clazz.type](Json.parse(bytes))
    clazz.get match {
      case fq=>Decoder[FederateQuery].decode(bytes).getOrElse().asInstanceOf[FederateQuery]
      case fsq=>Decoder[FederateSubQuery].decode(bytes).getOrElse().asInstanceOf[FederateSubQuery]
      case esq=>Decoder[ExecuteSubQuery].decode(bytes).getOrElse().asInstanceOf[ExecuteSubQuery]
      case _=> null
    }*/
    if (clazz.get == classOf[FederateQuery]) {
      return Decoder[FederateQuery].decode(bytes).getOrElse().asInstanceOf[FederateQuery]
    }
    else if (clazz.get == classOf[FederateSubQuery]) {
      return Decoder[FederateSubQuery].decode(bytes).getOrElse().asInstanceOf[FederateSubQuery]
    }
    else if (clazz.get == classOf[ExecuteSubQuery]) {
      return Decoder[ExecuteSubQuery].decode(bytes).getOrElse().asInstanceOf[ExecuteSubQuery]
    }
    return null
    //Json.parse(bytes).as[FederateQuery]
  }
}