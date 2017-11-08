package messagesDynamo

case class LIST_ELEMS_REPLY(k:String, clock:Clock, values:Set[String], opNumber:Int)