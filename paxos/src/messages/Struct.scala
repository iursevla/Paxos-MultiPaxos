package messages

import akka.actor.ActorRef

/**
 * Data structure used to save data belonging to a key.  
 * K -> Struct(np,na,va)
 */
class Struct(np: Long, na: Long, va: String) {
  var quorum = List[ActorRef]() //Lista de aceites pelo learner
  var counterPOK:Int = 0;//Number of prepareOk's received for this Key
  var np1:Long = this.np;//Highest prepare
  var na1:Long = this.na;//Highest accept
  var va1:String = this.va;//Value of this key
  var value1:String = null;//Initial value for this key
  
/**
 * np (highest prepare)
 * na (highest sequence number)
 * va (highest accept)
 */
  override def toString: String = "(" + np + ", " + na + ", " + va + ")"
  
  
}