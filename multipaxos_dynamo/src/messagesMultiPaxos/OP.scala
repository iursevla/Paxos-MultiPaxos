package messagesMultiPaxos

import akka.actor.ActorRef

//Used to save an operation on the queue for later, when needed
class OP(senderS:ActorRef, operationS:Super) {
  val sender = this.senderS; //Client that sent the operation
  val operation = this.operationS;  //Operation to perform
}