package actors

import akka.actor.Actor
import akka.actor.ActorRef
import messages._
import main._

class Server extends Actor {
  import context._
  var friends = List[ActorRef]() //Outros servers --> Done
  var map: Map[String, Struct] = Map() //K,<np, na, va> -> https://www.tutorialspoint.com/scala/scala_maps.htm
  var clientToReply: ActorRef = null;//Client which is used to communicate with

  def receive = {
    case FriendRequest => { this.addFriends(sender); }

    //START OR ANY WRITE
    case Write(k: String, v: String) => { clientToReply = null; propose(k, v); clientToReply = sender }

    //PROPOSER
    case PREPARE_OK(k, na, va) => receivePrepareOK(k, na, va)

    //ACCEPTOR
    case PREPARE(k, n) => prepare(k, n, sender)
    case ACCEPT(k, n, v) => accept(k, n, v, sender)

    //LEARNER
    case ACCEPT_OK_TO_LEARNER(k, n, v) => accepted(k, n, v, sender)

    //END
    case k: String => sender.tell(new MAPMESSAGE(map), self)
  }

  /*************************************************************/
  /**                     PROPOSER                            **/
  /*************************************************************/

  def propose(k: String, v: String) {
    if (!map.contains(k)) {
      addKeyToMapIfNotExists(k, v);
      sendMessage(new PREPARE(k, map.get(k).get.na1))
    }
  }

  def receivePrepareOK(k: String, na: Long, va: String) {
    var destakey: Struct = map.get(k).get
    
    destakey.counterPOK += 1;
    if (destakey.counterPOK > (Main.NUMBER_OF_SERVERS / 2)) {
      if (na >= destakey.na1) {
        destakey.na1 = na;
        sendMessage(new ACCEPT(k, na, va))
      } else {
        sendMessage(new ACCEPT(k, destakey.na1, destakey.value1))
      }
    }
    map += (k -> destakey);
  }

  /*************************************************************/
  /**                      ACCEPTOR                           **/
  /*************************************************************/

  def prepare(k: String, n: Long, sender: ActorRef) {
    addKeyToMapIfNotExists(k, "-1");

    var destakey: Struct = map.get(k).get
    if (n > destakey.np1) { //Entra sempre aqui
      destakey.np1 = n;
      map += (k -> destakey);
      sender.tell(new PREPARE_OK(k, destakey.na1, destakey.va1), self)
    }
  }

  def accept(k: String, n: Long, v: String, sender: ActorRef) {
    addKeyToMapIfNotExists(k, v);

    var destakey: Struct = map.get(k).get
    if (n >= destakey.np1) {
      destakey.na1 = n
      destakey.va1 = v
      map += (k -> destakey);
      sendMessage(new ACCEPT_OK_TO_LEARNER(k, n, v))
    }
  }

  /*************************************************************/
  /**                       LEARNER                           **/
  /*************************************************************/

  def accepted(k: String, n: Long, v: String, sender: ActorRef) {
    addKeyToMapIfNotExists(k, v);

    var destakey: Struct = map.get(k).get
    if (n > destakey.na1) {
      destakey.na1 = n
      destakey.va1 = v
      destakey.quorum = List[ActorRef]()
    } else if (n < destakey.na1)
      return ;

    destakey.quorum ::= sender;
    map += (k -> destakey);
    if (destakey.quorum.size > (Main.NUMBER_OF_SERVERS / 2)) 
      clientToReply.tell(new Read(v), self)
  }

  /*************************************************************/
  /**                       AUXILIARES                        **/
  /*************************************************************/
  def sendMessage(msg: Object) { //Enviar a mensagem msg para todos os servidores
    for (friend <- friends) {
      friend.tell(msg, self);
    }
    self.tell(msg, self);
  }

  def addFriends(friend: ActorRef) {
    friends ::= friend
  }

  def addKeyToMapIfNotExists(k: String, v: String) {
    if (!map.contains(k)) { //If !contains key then  
      var timestamp: Long = System.nanoTime() + self.path.name.hashCode()
      var struct: Struct = new Struct(0, timestamp, v)
      struct.value1 = v
      map += (k -> struct)
    }
  }
}
  