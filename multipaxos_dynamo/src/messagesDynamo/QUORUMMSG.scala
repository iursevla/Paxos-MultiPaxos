package messagesDynamo

import akka.actor.ActorRef

//Para guardar quais sao os servers principais 
case class QUORUMMSG(q:List[ActorRef]) {
 var quorum = this.q; 
}