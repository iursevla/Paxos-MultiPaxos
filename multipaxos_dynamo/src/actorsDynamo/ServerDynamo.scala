package actorsDynamo

import akka.actor.Actor
import akka.actor.ActorRef
import main._
import messagesDynamo._
import messagesMultiPaxos.FriendRequest

class ServerDynamo extends Actor {
  var INSERT: Int = 1
  var REMOVE: Int = 2
  var LIST_OR_ISELEM: Int = 3
  var friends = List[ActorRef]() //Outros servers --> Done
  var map: Map[String, StructDynamo] = Map() //K,<np, na, va> -> https://www.tutorialspoint.com/scala/scala_maps.htm

  var posOfThisServerInTheClock: Int = self.path.name.last.toInt - 49; //Position of this server in the clock
  var ops: List[StructDynamo] = List[StructDynamo]() // Replies for this particular request sent by client
  var clientToReply: ActorRef = null // Cliente que enviou o pedido
  var NSERVERS = MainGlobal.NUMBER_OF_SERVERS //
  var countOps = 0 //Numero da ronda de operacoes enviadas

  def receive = {
    case FriendRequest => { this.addFriends(sender); }

    /****************************************** INSERT *************************************************/
    case INSERT(k, elem) => { //Server que recebe do cliente<<-
      //println("Received insert from " + sender.path.name + " to key = " + k + " elem = " + elem)
      clientToReply = sender
      sendMessageToQuorum(k, new CAN_INSERT_ELEM(k, elem, countOps))
    }

    case CAN_INSERT_ELEM(k, elem, opNumber) => { //Server do Quorum<<-
      if (map.contains(k))
        sender.tell(new CAN_INSERT_ELEM_REPLY(k, elem, map.get(k).get.getClock(), map.get(k).get.values, opNumber), self);
      else {
        addKeyToMapIfNotExists(k)
        sender.tell(new CAN_INSERT_ELEM_REPLY(k, elem, new Clock(), Set[String](), opNumber), self);
      }
    }

    case CAN_INSERT_ELEM_REPLY(k, elem, clock, values, opNumber) => { //Server que recebe do cliente<<- 
      if (countOps == opNumber) {
        // println("INSERT: IN TIME server = " + sender.path.name)
        ops ::= new StructDynamo(clock, values)
        if (ops.length == MainGlobal.K - 1)
          insertOrRemoveElemReply(k, elem, false)
      }
    }

    case UPDATE_INSERT(k, elem, mergedClock, values) => { //O do quorum 
      updateQuorum(k, elem, mergedClock, values, INSERT)
    }

    /****************************************** REMOVE *************************************************/
    case REMOVE(k, elem) => {
      //      println("Received remove from " + sender.path.name + " to key = " + k + " elem = " + elem)
      clientToReply = sender
      sendMessageToQuorum(k, new CAN_REMOVE_ELEM(k, elem, countOps))
    }

    case CAN_REMOVE_ELEM(k, elem, opNumber) => { //Server do Quorum<<-
      if (map.contains(k))
        sender.tell(new CAN_REMOVE_ELEM_REPLY(k, elem, map.get(k).get.getClock(), map.get(k).get.values, opNumber), self);
      else {
        addKeyToMapIfNotExists(k)
        sender.tell(new CAN_REMOVE_ELEM_REPLY(k, elem, new Clock(), Set[String](), opNumber), self);
      }
    }

    case CAN_REMOVE_ELEM_REPLY(k, elem, clock, values, opNumber) => { //Server que recebe do cliente<<- 
      //server que recebeu o pedido - pode inserir depois de verificar clocks
      if (countOps == opNumber) {
        //println("REMOVE: IN TIME server = " + sender.path.name)
        ops ::= new StructDynamo(clock, values)
        if (ops.length == MainGlobal.K - 1)
          insertOrRemoveElemReply(k, elem, true)
      }
    }

    case UPDATE_REMOVE(k, elem, mergedClock, values) => { //O do quorum //Passo 
      updateQuorum(k, elem, mergedClock, values, REMOVE)
    }

    /*************************************************ISELEMENT***********************************************************/
    case ISELEMENT(k, elem) => {
      clientToReply = sender
      sendMessageToQuorum(k, new HAS_ELEM(k, elem, countOps))
    }

    case HAS_ELEM(k, elem, opNumber) => {
      if (map.contains(k)) {
        var values = map.get(k).get.values
        var clock = map.get(k).get.clock
        sender.tell(new HAS_ELEM_REPLY(k, elem, values, clock, opNumber), self)
      } else
        sender.tell(new HAS_ELEM_REPLY(k, elem, Set[String](), new Clock(), opNumber), self)
    }

    case HAS_ELEM_REPLY(k, elem, values, clock, opNumber) => {
      //      println("ISELEMENT: IN TIME server = " + sender.path.name)
      ops ::= new StructDynamo(clock, values) // Vai dar problemas aqui(1 resposta mais tarde vai contar p as prox ops) 
      if (ops.length == MainGlobal.K)
        listOrHasElementReply(k, elem, false)
    }

    case UPDATE_ISELEMENT(k, elem, mergedClock, values) => {
      updateQuorum(k, elem, mergedClock, values, LIST_OR_ISELEM)
    }

    /****************************************************LIST ELEMENTS **************************************************************/
    case LIST(k) => {
      clientToReply = sender
      sendMessageToQuorum(k, new LIST_ELEMS(k, countOps))
    }

    case LIST_ELEMS(k, opNumber) => {
      if (map.contains(k))
        sender.tell(new LIST_ELEMS_REPLY(k, map.get(k).get.getClock(), map.get(k).get.values, opNumber), self);
      else
        sender.tell(new LIST_ELEMS_REPLY(k, new Clock(), Set[String](), opNumber), self);
    }

    case LIST_ELEMS_REPLY(k, clock, values, opNumber) => {
     // println("ISELEMENT: IN TIME server = " + sender.path.name)
      ops ::= new StructDynamo(clock, values)
      if (ops.length == MainGlobal.K)
        listOrHasElementReply(k, null, true)
    }

    case UPDATE_LIST(k, mergedClock, values) => {
      updateQuorum(k, null, mergedClock, values, LIST_OR_ISELEM)
    }

    case _ => {} //println("sent from " + sender.path.name); println(self.path.name + " friends = " + friends + " contains Self? = " + friends.contains(self)) }
  }

  /*************************************************************/
  /**                       AUXILIARES                        **/
  /*************************************************************/
  def addFriends(friend: ActorRef) {
    friends ::= friend
  }

  def addKeyToMapIfNotExists(k: String) {
    if (!map.contains(k)) { //If !contains key then  
      var timestamp: Long = System.nanoTime() + self.path.name.hashCode()
      var struct: StructDynamo = new StructDynamo(new Clock(), Set[String]())
      map += (k -> struct)
    }
  }

  /**
   * Calculates the hash for any given key and returns the calculated value.
   * This value will be used to send the W/R requests.
   */
  def calcKeyHash(k: String): Int = {
    return Math.abs(k.hashCode()) % (NSERVERS)
  }

  /**
   * Generates K servers to get() or put() the key k.
   */
  def generateServersToSendKey(k: String): List[ActorRef] = {
    var hash = calcKeyHash(k)
    var quorum: List[ActorRef] = List[ActorRef]()

    for (i <- hash + 1 to hash + MainGlobal.K) {
      if (i >= NSERVERS)
        quorum ::= friends(i - NSERVERS)
      else
        quorum ::= friends(i)
    }

    return quorum
  }

  /**
   * Send message to quorum servers of this message.
   */
  def sendMessageToQuorum(k: String, msg: Object) {
    var quorum = generateServersToSendKey(k)
    //countOps += 1 //TODO
    for (q <- quorum)
      q.tell(msg, self)
  }

  /**
   * Used to calculate new clocks and values for can_isert_elem_reply or can_remove_elem_reply.
   */
  def insertOrRemoveElemReply(k: String, elem: String, isDelete: Boolean) {
    var c1 = ops(0).clock
    var c2 = ops(1).clock
    //Merge c1 e c2
    //Somar na pos deste gajo
    //Merge dos values 
    //enviar 
    var newValues = Set[String]()
    var newClock = new Clock()
    for (i <- 0 to c1.elems.size - 1)
      newClock.elems(i) = Math.max(c1.elems(i), c2.elems(i))
    newClock.elems(posOfThisServerInTheClock) += 1
    newValues ++= ops(0).values
    newValues ++= ops(1).values

    if (isDelete) {
      newValues -= elem
      sendMessageToQuorum(k, new UPDATE_REMOVE(k, elem, newClock, newValues))
    } else {
      newValues += elem
      sendMessageToQuorum(k, new UPDATE_INSERT(k, elem, newClock, newValues))
    }

    ops = List[StructDynamo]()
    countOps += 1
    clientToReply.tell(DONE, self)
  }

  /**
   * Used for list_elems_reply or has_element_reply.
   */
  def listOrHasElementReply(k: String, elem: String, isList: Boolean) {
    var c1 = ops(0).clock
    var c2 = ops(1).clock
    var c3 = ops(2).clock

    var newValues = Set[String]()
    var newClock = new Clock()

    for (i <- 0 to c1.elems.size - 1) {
      newClock.elems(i) = Math.max(c3.elems(i), Math.max(c1.elems(i), c2.elems(i)))
    }
    newClock.elems(posOfThisServerInTheClock) += 1
    newValues ++= ops(0).values
    newValues ++= ops(1).values
    newValues ++= ops(2).values

    //println("ISELEMENT: " + newValues.contains(elem))
    var quorum = generateServersToSendKey(k)
    for (serverQuorum <- quorum) {
      if (isList)
        serverQuorum.tell(new UPDATE_LIST(k, newClock, newValues), self)
      else
        serverQuorum.tell(new UPDATE_ISELEMENT(k, elem, newClock, newValues), self)
    }

    //reset dos ops
    ops = List[StructDynamo]()
    countOps += 1
    if (isList)
      clientToReply.tell(new LISTREPLY(newValues), self)
    else
      clientToReply.tell(new ISELEMENTREPLY(k, elem, newValues.contains(elem)), self)
  }

  /**
   * Any update quorum.
   */
  def updateQuorum(k: String, elem: String, mergedClock: Clock, values: Set[String], operation: Int) {
    addKeyToMapIfNotExists(k)
    var oldClock = map.get(k).get.getClock()
    for (i <- 0 to oldClock.elems.size - 1)
      oldClock.elems(i) = Math.max(oldClock.elems(i), mergedClock.elems(i))
    oldClock.elems(posOfThisServerInTheClock) += 1 //Incrementar a pos deste server
    map.get(k).get.clock = oldClock
    map.get(k).get.values ++= values
    if (operation == INSERT)
       map.get(k).get.addElement(elem)
    else if (operation == 2)
      map.get(k).get.deleteElement(elem)
  }
}