package messagesMultiPaxos

//Used to send ISELEMENT message from client to server
case class ISELEMENT(k:String, elem:String) extends Super

