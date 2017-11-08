package actorsDynamo

import akka.actor.Actor
import akka.actor.ActorRef
import scala.util.Random
import main.MainGlobal
import messagesDynamo._
import messagesMultiPaxos.FriendRequest

class ClientDynamo extends Actor {
  var randomOPList = List[Int](); //cria o vector de operaçoes
  var countDoneOps: Int = 0 //Number of operations already done
  var knownServer: ActorRef = null

  def receive = {
    case NEXTROUND => {
      generateAllOps(); sendNextOP()
      MainGlobal.timeDynamo = System.nanoTime
    }
    case DONE => { sendNextOP() }

    case ISELEMENTREPLY(k, elem, hasElem) => { //System.err.println("Has elem " + elem + " in key " + k + " = " + hasElem); 
      sendNextOP()
    }

    case LISTREPLY(values) => { //System.err.println("LIST = " + values); 
      sendNextOP()
    }

    case FriendRequest => { //println(self.path.name + " sender = " + sender.path.name);
      this.knownServer = sender
    }
    case _ => { println(self.path.name + " RECEIVED UNEXPECTED MESSAGE") }
  }

  //  /**
  //   * Generates a 10K list of operations to perform, and shuffles them.
  //   */
  //  def generateAllOps() {
  //    for (i <- 0 to 10000) {
  //      if (i < 4000)
  //        randomOPList ::= 0;
  //      else if (i < 5000)
  //        randomOPList ::= 1;
  //      else if (i < 9000)
  //        randomOPList ::= 2;
  //      else
  //        randomOPList ::= 3;
  //    }
  //    randomOPList = scala.util.Random.shuffle(randomOPList);
  //  }

  /**
   * This method randomly generates 10K operations to perform and shuffles them.
   * 4K inserts, 1K Removes, 4K isElement, 1K List
   */
  def generateAllOps() {
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
        knownServer.tell(new INSERT(randomKey, randomElem), self)
      else if (op == 1)
        knownServer.tell(new REMOVE(randomKey, randomElem), self)
      else if (op == 2)
        knownServer.tell(new ISELEMENT(randomKey, randomElem), self)
      else
        knownServer.tell(new LIST(randomKey), self)

      countDoneOps += 1;
    } else {

      System.err.println("GOODBYE from " + self.path.name)
      MainGlobal.itsEndDynamo += 1
      if (MainGlobal.itsEndDynamo < 3)
        context.stop(self) //ACTOR chegou ao fim
      else {
        knownServer.tell("", self)
        //context.system.shutdown()
        println("Time taken Dynamo: " + (System.nanoTime - MainGlobal.timeDynamo) / 1e6 + "ms")
        println("Dynamo " + (MainGlobal.OPS / (((System.nanoTime - MainGlobal.timeDynamo) / 1e6) / 1000)).toInt + " Ops/sec")
      }
    }
  }

  def generateRandomChar(): String = {
    val alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val size = alpha.size
    def randStr(n: Int) = (1 to n).map(x => alpha(Random.nextInt.abs % size)).mkString
    return randStr(1) //(1 to 1).map(x => alpha(Random.nextInt.abs % size)).mkString
  }
}