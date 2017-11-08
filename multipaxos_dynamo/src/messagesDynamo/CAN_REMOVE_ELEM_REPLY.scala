package messagesDynamo

case class CAN_REMOVE_ELEM_REPLY(k:String, elem:String, clock:Clock, values:Set[String], opNumber:Int)