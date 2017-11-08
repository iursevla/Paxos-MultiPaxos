package actors

import akka.actor.Actor
import akka.actor.ActorRef
import scala.util.Random
import scala.concurrent.{ Await, ExecutionContext, Future }
import akka.pattern.ask
import akka.util.Timeout
import java.time.Duration
import messages._
import main._

class Client extends Actor {

  var god: ActorRef = null // The god which will be the master
  var knownServer: ActorRef = null //The server which this client will use to communicate(reads and writes)
  var counterKeys: Int = 0 //To send next key -> key_1 to key_n
  var map: Map[String, String] = Map[String, String]()
  
  def receive = {
    case Read(v: String) => god.tell(DONE, self);

    case NEXTROUND => {
      var randVal: String = generateRandomChar()
      counterKeys += 1
      knownServer.tell(new Write("key_" + counterKeys, randVal), self)
    }

    case _ => {
      if (sender.path.name.contains(Main.GOD)) {
        god = sender
        god.tell(FriendRequest, self)
        god.tell(DONE, self)
      } else {
        knownServer = sender;
      }
    }
  }

  def generateRandomChar(): String = {
    val alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val size = alpha.size
    def randStr(n: Int) = (1 to n).map(x => alpha(Random.nextInt.abs % size)).mkString
    return randStr(1)//(1 to 1).map(x => alpha(Random.nextInt.abs % size)).mkString
  }
}