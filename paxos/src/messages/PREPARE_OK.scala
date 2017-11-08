package messages

//Send Prepare_Ok from acceptor to proposer
//K is the key that should be used to all messages
//na is the highest sequence number
//va is the highest accept  
case class PREPARE_OK(k:String, na:Long, va:String)
