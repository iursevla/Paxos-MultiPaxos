package messagesMultiPaxos
import akka.actor.ActorRef

//USED to save all information for a value saved on a key.
class Valor(np: Long, na: Long, va: String) {
  var nprepare: Long = this.np; //Highest prepare
  var naccept: Long = this.na; //Highest accept
  var value: String = this.va; //Value of this key
  var quorum = List[ActorRef]() //Lista de aceites pelo learner

  override def equals(o: Any) = o match {
    case that: Valor => that.value == this.value
    case _ => false
  }
  override def hashCode = value.hashCode
}