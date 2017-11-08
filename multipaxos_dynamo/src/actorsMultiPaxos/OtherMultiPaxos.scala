package actorsMultiPaxos

import akka.actor.Actor
import akka.actor.ActorRef
import messagesMultiPaxos._
import scala.collection.immutable.Set
import main.MainGlobal

/**
 * Server for paxos algorithm
 */
class OtherMultiPaxos extends Actor {
  var friends = List[ActorRef]() //Outros servers --> Done
  var map: Map[String, StructMultiPaxos] = Map() //K,<np, na, va> -> https://www.tutorialspoint.com/scala/scala_maps.htm

  def receive = {
    case FriendRequest => { this.addFriends(sender); }
    case ACCEPT(k, n, v, b, replicas) => accept(k, n, v, sender, b, replicas) //ACCEPTOR
    case ACCEPT_OK_TO_LEARNER(k, n, v, toDelete) => accepted(k, n, v, toDelete, sender) //LEARNER
    case _ => {listAllKeysAndValues()}
  }

  /*************************************************************/
  /**                      ACCEPTOR                           **/
  /*************************************************************/
  def accept(k: String, n: Long, v: String, sender: ActorRef, toDelete: Boolean, replicas: Set[ActorRef]) {
    addKeyToMapIfNotExists(k, v);
    var destakey: StructMultiPaxos = map.get(k).get

    destakey.replicas = replicas
    if (n >= destakey.getValor(v).nprepare) {
      destakey.getValor(v).naccept = n

      map += (k -> destakey);
      sendAcceptOKMessage(new ACCEPT_OK_TO_LEARNER(k, n, v, toDelete), k) //MANDAR TAMBEM PARA O LIDER
    }
  }

  /*************************************************************/
  /**                       LEARNER                           **/
  /*************************************************************/

  def accepted(k: String, n: Long, v: String, toDelete: Boolean, sender: ActorRef) {
    addKeyToMapIfNotExists(k, v);
    var destakey: StructMultiPaxos = map.get(k).get

    if (n > destakey.getValor(v).naccept) {
      destakey.getValor(v).naccept = n
      //destakey.quorum = List[ActorRef]()
    } else if (n < destakey.getValor(v).naccept)
      return ;

    destakey.getValor(v).quorum ::= sender;
    
    if (destakey.getValor(v).quorum.size == MainGlobal.K){
      destakey.getValor(v).quorum = List[ActorRef]()
      if (toDelete){
        var sizeOfKeys = destakey.values.size;
        destakey.deleteElement(v)
      }
    }
     map += (k -> destakey); 
  }

  /*************************************************************/
  /**                       AUXILIARES                        **/
  /*************************************************************/
  def sendMessage(msg: Object) { //Enviar a mensagem msg para todos os servidores
    for (friend <- friends)
      friend.tell(msg, self);
    self.tell(msg, self);
  }

  def addFriends(friend: ActorRef) {
    friends ::= friend
  }

  def addKeyToMapIfNotExists(k: String, v: String) {
    if (!map.contains(k)) { //If !contains key then  
      var struct: StructMultiPaxos = new StructMultiPaxos()
      var timestamp: Long = System.nanoTime() + self.path.name.hashCode()
      var valor: Valor = new Valor(0, timestamp, v)
      struct.addElement(valor)
      map += (k -> struct)
    } else {
      addElemToKey(k, v)
    }
  }

  def addElemToKey(k: String, v: String) {
    var timestamp: Long = System.nanoTime() + self.path.name.hashCode()
    var valor: Valor = new Valor(0, timestamp, v)
    var struct: StructMultiPaxos = map.get(k).get
    struct.addElement(valor)
    map += (k -> struct)
  }

  def sendAcceptOKMessage(msg: ACCEPT_OK_TO_LEARNER, k: String) {
    var destakey: StructMultiPaxos = map.get(k).get
    for (replica <- destakey.replicas)
      replica.tell(msg, self)
  }

  /**
   * List all keys and respective values on this server
   */
  def listAllKeysAndValues() {
    for ((k, vals) <- map) {
      if (k.equals("key_48")) {
//        print(self.path.name +  " key = " + k + " values = " + vals.values.size + " --> ")
//        for (v <- vals.values) {
//          print(v.value + "|")
//        }
//        println()
      }
    }
  }
}
  