import java.util.stream.Collectors

import com.hp.hpl.jena.sparql.engine.binding.Binding
import monitoring.message.{ExecuteSubQuery, FederateSubQuery, Result}
import org.scalatest._

import scala.collection.mutable.{ArrayBuffer, HashMap, ListBuffer}

class CaseClassTest extends FlatSpec with Matchers {

  "Two object instantiated from same case class with same properties" should "implement hashCode and equals exactly same" in {
    val esq = new ExecuteSubQuery("query-1", "endpoint-1")
    val esqSame = new ExecuteSubQuery("query-1", "endpoint-1")
    val esqDifferent = new ExecuteSubQuery("query-2", "endpoint-1")
    assert(esq == esqSame)
    assert(esq != esqDifferent)

    val fsq = new FederateSubQuery("query-3", "endpoint-2" :: "endpoint-3" :: Nil)
    val fsqSame = new FederateSubQuery("query-3", "endpoint-2" :: "endpoint-3" :: Nil)
    val fsqDifferent = new FederateSubQuery("query-3", "endpoint-3" :: Nil)
    assert(fsq == fsqSame)
    assert(fsq != fsqDifferent)


    val bucketMap: HashMap[Int, ArrayBuffer[String]] = HashMap.empty
    val vector = bucketMap.getOrElse(3, ArrayBuffer.empty[String])
    vector += "naber"
    bucketMap += (3 -> vector)
    println(bucketMap.getOrElse(4, update))

    var res = new Result("", Vector.empty)
    res.resultJSON :+ "Naber"
    res = res
    println(res)

    var result = None: Option[Result]


    var someList = new ListBuffer[String]()
    someList += "apple"
    someList += "orange"
    someList += "pear"
    val filtered = someList.filter(_.endsWith("e"))
    someList += "strawberry"
    println(filtered)
    val modified = someList.filter(_.contains("ea")).map(_ + "x")
    println(modified)

  }

  def update {}

}
