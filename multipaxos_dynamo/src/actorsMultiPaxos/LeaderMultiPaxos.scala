package actorsMultiPaxos

import akka.actor.Actor
import akka.actor.ActorRef
import messagesMultiPaxos._
import main.MainGlobal
import scala.collection.mutable.Queue
import scala.collection.immutable.Set

/**
 * Server for MultiPaxos algorithm Leader
 */
class LeaderMultiPaxos extends Actor {
  var friends = List[ActorRef]() //Outros servers --> Done
  var map: Map[String, StructMultiPaxos] = Map() //K,<np, na, va> -> https://www.tutorialspoint.com/scala/scala_maps.htm

  def receive = {
    case FriendRequest => {
      this.addFriends(sender);
      if (this.friends.size == MainGlobal.NUMBER_OF_SERVERS - 1)
        this.addAllKeysToMap()
    }

    case ACCEPT(k, n, v, b, replicas) => accept(k, n, v, sender, b, replicas)
    case ACCEPT_OK_TO_LEARNER(k, n, v, toDelete) => accepted(k, n, v, toDelete, sender) //LEARNER

    //TRABALHO2
    case INSERT(k, elem) => {
      map.get(k).get.addPendingOp(sender, new INSERT(k, elem))
      if (map.get(k).get.pendingOps.size == 1)
        performNextOP(k)
    }

    case REMOVE(k, elem) => {
      map.get(k).get.addPendingOp(sender, new REMOVE(k, elem))
      if (map.get(k).get.pendingOps.size == 1)
        performNextOP(k)
    }

    case ISELEMENT(k, elem) => {
      map.get(k).get.addPendingOp(sender, new ISELEMENT(k, elem))
      if (map.get(k).get.pendingOps.size == 1)
        performNextOP(k)
    }

    case LIST(k) => {
      map.get(k).get.addPendingOp(sender, new LIST(k))
      if (map.get(k).get.pendingOps.size == 1)
        performNextOP(k)
    }

    case _ => {
      listAllKeysAndValues()
    }
  }

  /**
   * Used to insert or delete an element with key k.
   * If toDelete is true then the action to perform is a remove. Otherwise it's an insert.
   * USED BY LEADER ONLY
   */
  def write(k: String, elem: String, toDelete: Boolean, senderOfWrite: ActorRef) {
    if (toDelete) //REMOVE
    {
      if (map.get(k).get.hasElement(elem)) //Tem elemento entao -> APAGAR e enviar para as replicas
      {
        var value = map.get(k).get.getValor(elem)
        sendMessageAccept(new ACCEPT(k, value.naccept, elem, true, map.get(k).get.replicas), k)
      } //
      else //Nao existe o elemento entao -> RESPONDER AO CLIENTE
      {
        senderOfWrite.tell(DONEPAXOS, self)
        map.get(k).get.pendingOps.dequeue()
        performNextOP(k)
      }
    } //
    else //INSERT
    {
      if (map.get(k).get.hasElement(elem)) //Ja existe o elemento entao -> RESPONDER AO CLIENTE
      { //
        senderOfWrite.tell(DONEPAXOS, self)
        map.get(k).get.pendingOps.dequeue()
        performNextOP(k)
      } // 
      else // Nao existe o elemento entao -> Add e enviar para as replicas
      {
        addElemToKey(k, elem)
        var value = map.get(k).get.getValor(elem)
        sendMessageAccept(new ACCEPT(k, value.naccept, elem, false, map.get(k).get.replicas), k)
      }
    }
  }

  /**
   * Verifies if elem is one of the values of the given key k.
   * If it is send a reply to client with body as true, otherwise, false.
   * USED BY LEADER ONLY
   */
  def isElement(k: String, elem: String, senderOfIsElement: ActorRef) {
    var hasElement = false;
    if (map.get(k).get.hasElement(elem))
      hasElement = true
    senderOfIsElement.tell(new ISELEMENTREPLY(k, elem, hasElement), self);

    map.get(k).get.pendingOps.dequeue()
    performNextOP(k)
  }

  /**
   * Lists the elements of the given key k.
   * If the key does not exist or does not contain any value then reply with an empty list.
   * USED BY LEADER ONLY
   */
  def list(k: String, senderOfList: ActorRef) {
    senderOfList.tell(new LISTREPLY(map.get(k).get.getValues()), senderOfList);

    map.get(k).get.pendingOps.dequeue()
    performNextOP(k)
  }

  /*************************************************************/
  /**                      ACCEPTOR                           **/
  /*************************************************************/
  /**
   * To accept or to delete a value from a key
   */
  def accept(k: String, n: Long, v: String, senderOfAccept: ActorRef, toDelete: Boolean, replicas: Set[ActorRef]) {
    var destakey: Valor = map.get(k).get.getValor(v)
    if (n >= destakey.nprepare) {
      map.get(k).get.getValor(v).naccept = n
      sendAcceptOKMessage(new ACCEPT_OK_TO_LEARNER(k, map.get(k).get.getValor(v).naccept, v, toDelete), k)
    }
  }

  /*************************************************************/
  /**                       LEARNER                           **/
  /*************************************************************/

  def accepted(k: String, n: Long, v: String, toDelete: Boolean, senderOfAccepted: ActorRef) {
    if (map.get(k).get.getValor(v) == null) return ;
    if (n > map.get(k).get.getValor(v).naccept)
      map.get(k).get.getValor(v).naccept = n
    else if (n < map.get(k).get.getValor(v).naccept) { //HAD ERROR HERE
      return ;
    }

    //Se ja foi aceite/inserida/removida ignorar proximos accept_ok_to_learner para a mesma instancia
    map.get(k).get.getValor(v).quorum ::= senderOfAccepted
    if (map.get(k).get.getValor(v).quorum.size == MainGlobal.K) {
      map.get(k).get.getValor(v).quorum = List[ActorRef]() //TODO vai dar problemas qdo usarmos o quorum como sendo > (K/2) 
      map.get(k).get.pendingOps.head.sender.tell(DONEPAXOS, self)

      if (toDelete)
        map.get(k).get.deleteElement(v)

      map.get(k).get.pendingOps.dequeue()
      performNextOP(k)
    }
  }

  /*************************************************************/
  /**                       AUXILIARES                        **/
  /*************************************************************/
  def sendMessageAccept(msg: ACCEPT, key: String) { //Enviar a mensagem msg para todos os servidores
    var replicas: Set[ActorRef] = map.get(key).get.replicas
    for (replica <- replicas)
      replica.tell(msg, self);
  }

  def sendAcceptOKMessage(msg: ACCEPT_OK_TO_LEARNER, key: String) {
    for (replica <- map.get(key).get.replicas)
      replica.tell(msg, self)
  }

  def addFriends(friend: ActorRef) {
    friends ::= friend
  }

  /**
   * Adds an element to the key k and K replicas (MainMultiPaxos.K)
   */
  def addElemToKey(k: String, v: String) {
    var timestamp: Long = System.nanoTime() + self.path.name.hashCode()
    var valor: Valor = new Valor(0, timestamp, v)
    var struct: StructMultiPaxos = map.get(k).get
    struct.addElement(valor)
  }

  /**
   * Adds all(key_1 to key_100(inclusive)) keys to the map of Key->Value.
   */
  def addAllKeysToMap() {
    for (i <- 1 to 100) {
      var struct = new StructMultiPaxos()
      var arr = scala.util.Random.shuffle(0 to 5)
      struct.addReplica(self)
      struct.addReplica(friends(arr(0)))
      struct.addReplica(friends(arr(1)))
      map += ("key_" + i -> struct)
    }
  }

  /**
   * Used to perform the next operation on the Queue of key k.
   */
  def performNextOP(k: String) {
    if (map.get(k).get.pendingOps.size > 0) {
      var head = map.get(k).get.pendingOps.head
      head.operation match {
        case INSERT(k, elem) => { write(k, elem, false, head.sender) }
        case REMOVE(k, elem) => { write(k, elem, true, head.sender) }
        case ISELEMENT(k, elem) => { isElement(k, elem, head.sender) }
        case LIST(k) => { list(k, head.sender) }
      }
    }
  }

  /**
   * List all keys and respective values on this server
   */
  def listAllKeysAndValues() {
    for ((k, vals) <- map) {
      if (k.equals("key_48")) {
//        print(self.path.name + " key = " + k + " values = " + vals.values.size + " --> ")
//        for (v <- vals.values) {
//          print(v.value + "|")
//        }
//        println()
//        friends(0).tell("", self)
      }
    }
  }

}
  