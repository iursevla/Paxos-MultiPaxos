package messagesDynamo

/**
 * clock ->  Clock for this key
 * values -> Values that are associated with this key
 */
case class CAN_INSERT_ELEM_REPLY(k:String, elem:String, clock:Clock, values: Set[String], opNumber: Int)