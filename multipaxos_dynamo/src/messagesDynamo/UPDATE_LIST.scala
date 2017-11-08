package messagesDynamo

/**
 * Send from server S4 to others 
 * values is the list of values 
 */
case class UPDATE_LIST(k:String, mergedClock:Clock, values:Set[String])