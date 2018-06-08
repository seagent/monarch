import monitoring.message.{ExecuteSubQuery, FederateSubQuery}
import org.scalatest._

class CaseClassTest extends FlatSpec with Matchers {

  "Two object instantiated from same case class with same properties" should "implement hashCode and equals exactly same" in {
    val esq=ExecuteSubQuery("query-1","endpoint-1")
    val esqSame=ExecuteSubQuery("query-1","endpoint-1")
    val esqDifferent=ExecuteSubQuery("query-2","endpoint-1")
    assert(esq==esqSame)
    assert(esq!=esqDifferent)

    val fsq=FederateSubQuery("query-3","endpoint-2"::"endpoint-3"::Nil);
    val fsqSame=FederateSubQuery("query-3","endpoint-2"::"endpoint-3"::Nil);
    val fsqDifferent=FederateSubQuery("query-3","endpoint-3"::Nil);
    assert(fsq==fsqSame)
    assert(fsq!=fsqDifferent)
  }

}
