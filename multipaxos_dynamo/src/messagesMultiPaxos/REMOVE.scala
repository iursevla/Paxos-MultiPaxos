package messagesMultiPaxos

//Used to send REMOVE message from client to server
case class REMOVE(k:String, elem:String) extends Super

