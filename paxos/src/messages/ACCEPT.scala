package messages

//Used to send ACCEPT message from proposer to acceptor
case class ACCEPT(k:String, na:Long, va:String)

