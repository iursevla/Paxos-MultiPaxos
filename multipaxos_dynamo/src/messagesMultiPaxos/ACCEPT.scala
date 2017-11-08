package messagesMultiPaxos

import akka.actor.ActorRef
import scala.collection.immutable.Set

//Used to send ACCEPT message from proposer to acceptor
/**
 * @param 			na - highest prepare
 * @param 			va - Value to replicate or to delete
 * @param toDelete - Whether to create or delete this key(k) from the server this message is sent to
 * @param replicas - Set of replicas/servers where this key will be replicated
 */
case class ACCEPT(k:String, na:Long, va:String, toDelete:Boolean, replicas: Set[ActorRef]) extends Super

