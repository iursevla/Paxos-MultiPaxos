package messages

//Used to send prepare message from proposer to acceptors
case class PREPARE(k:String, n:Long)
