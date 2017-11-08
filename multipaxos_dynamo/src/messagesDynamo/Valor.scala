package messagesDynamo

import akka.actor.ActorRef

//USED to save all information for a value saved on a key.
class Valor(vall: String, clck: Clock) {
  var value: String = this.vall;

  override def equals(o: Any) = o match {
    case that: Valor => that.value == this.value
    case _ => false
  }
  override def hashCode = value.hashCode
}