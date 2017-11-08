package actorsMultiPaxos

import akka.actor.Actor
import akka.actor.ActorRef
import scala.util.Random
import messagesMultiPaxos._
import main.MainGlobal

class ClientMultiPaxos extends Actor {
  var leaderServer: ActorRef = null //The server which this client will use to communicate(reads and writes)
  var randomOPList = List[Int](); //Random list of operations to perform
  var countDoneOps: Int = 0 //Number of operations already done

  def receive = {
    case DONEPAXOS => { sendNextOP() } //println("CLIENT --> " + self.path.name + " received DONE MSG")
    case ISELEMENTREPLY(k, elem, bool) => { sendNextOP() } //println("CLIENT --> " + self.path.name + " received ISELEMENTREPLY MSG")
    case LISTREPLY(l: List[String]) => { sendNextOP() } //println("CLIENT --> " + self.path.name + " received LISTREPLY MSG")
    case FriendRequest => {
      leaderServer = sender
      generateAllOperations()
      MainGlobal.timeMultiPaxos = System.nanoTime
      sendNextOP(); //Ja pode comecar a enviar ops para o leader
    }

    case _ => {
      System.err.println("SOMETHING WENT REALLY WRONG HERE --> " + self.path.name)
    }
  }

  /**
   * Sends the next operation on the operation "queue" to the Paxos Leader.
   */
  def sendNextOP() {
    //println(self.path. name + " " + countDoneOps)
    if (countDoneOps < MainGlobal.OPS) {
      val rnd = new scala.util.Random
      var range = 1 to 100
      var randomKey = "key_" + range(rnd.nextInt(range.length))
      var randomElem = generateRandomChar()

      var op = randomOPList(countDoneOps);
      // println("countDoneOps = " + countDoneOps)
      //      if (self.path.name.last - 48 == 1) {
      //        op = 0
      //      } else if (self.path.name.last - 48 == 2) {
      //        op = 1
      //      } else if (self.path.name.last - 48 == 3) {
      //        op = 2
      //      }
      if (op == 0)
        leaderServer.tell(new INSERT(randomKey, randomElem), self)
      else if (op == 1)
        leaderServer.tell(new REMOVE(randomKey, randomElem), self)
      else if (op == 2)
        leaderServer.tell(new ISELEMENT(randomKey, randomElem), self)
      else
        leaderServer.tell(new LIST(randomKey), self)

      countDoneOps += 1;
    } else {

      System.err.println("GOODBYE from " + self.path.name)
      MainGlobal.itsEndMultiPaxos += 1
      if (MainGlobal.itsEndMultiPaxos < 3)
        context.stop(self) //ACTOR chegou ao fim
      else {
        leaderServer.tell("", self)
        println("Time taken MultiPaxos: " + (System.nanoTime - MainGlobal.timeMultiPaxos) / 1e6 + "ms")
        println("MultiPaxos " + (MainGlobal.OPS / (((System.nanoTime - MainGlobal.timeMultiPaxos) / 1e6) / 1000)).toInt + " Ops/sec")
        //context.system.shutdown()
      }
    }
  }

  /**
   * This method randomly generates 10K operations to perform and shuffles them.
   * 4K inserts, 1K Removes, 4K isElement, 1K List
   */
  def generateAllOperations() {
    for (i <- 0 to MainGlobal.OPS - 1) {
      if (i < MainGlobal.INSERTS)
        randomOPList ::= 0;
      else if (i < MainGlobal.INSERTS + MainGlobal.REMOVES)
        randomOPList ::= 1;
      else if (i < (MainGlobal.INSERTS * 2) + MainGlobal.REMOVES)
        randomOPList ::= 2;
      else
        randomOPList ::= 3;
    }

    randomOPList = scala.util.Random.shuffle(randomOPList);
  }

  def generateRandomChar(): String = {
    val alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val size = alpha.size
    def randStr(n: Int) = (1 to n).map(x => alpha(Random.nextInt.abs % size)).mkString
    return randStr(1) //(1 to 1).map(x => alpha(Random.nextInt.abs % size)).mkString
  }
}