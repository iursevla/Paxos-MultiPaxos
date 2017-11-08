package messagesMultiPaxos

//Used to send LISTREPLY message from server to client
case class LISTREPLY(listElem:List[String])

