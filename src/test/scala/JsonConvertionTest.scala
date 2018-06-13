import io.protoless.messages.{Decoder, Encoder}
import monitoring.message.FederateQuery
import org.scalatest.{FlatSpec, Matchers}
import io.protoless.generic.auto._
import play.api.libs.json.Json

class JsonConvertionTest extends FlatSpec with Matchers{
  val fqExpected=FederateQuery("federate-query-1")
  val bytes=Encoder[FederateQuery].encodeAsBytes(fqExpected)
  val fqActual=Decoder[FederateQuery].decode(bytes).getOrElse()
  fqActual shouldBe(fqExpected)
}
