package messagesDynamo

case class UPDATE_REMOVE(k:String, elem:String, mergedClock:Clock, values:Set[String])