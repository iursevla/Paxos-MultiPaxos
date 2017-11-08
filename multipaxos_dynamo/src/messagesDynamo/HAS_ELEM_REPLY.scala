package messagesDynamo

case class HAS_ELEM_REPLY(k:String, elem: String, values: Set[String], clock: Clock, opNumber:Int)