package main

import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import actors._
import messages._

object Main {
  val NUMBER_OF_SERVERS = 3
  val NUMBER_OF_CLIENTS = 3
  val NUMBER_OF_KEYS = 1000
  val GOD:String = "God"
  var SERVER:String = "server_"
  var CLIENT:String = "client_"
  
  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem("akka4scala"); // Create the actor system that will spawn our actors

    var god = actorSystem.actorOf(Props[God], GOD)//The guy that controls the steps of the write
    
    //Create 3 clients
    var clientList = List[ActorRef]()
    for (i <- 1 to NUMBER_OF_CLIENTS) {
      val client = actorSystem.actorOf(Props[Client], CLIENT + i)
      clientList ::= client
    }

    //Create 3 servers map.get(k)
    var serverList = List[ActorRef]()
    for (i <- 1 to NUMBER_OF_SERVERS) {
      val server = actorSystem.actorOf(Props[Server], SERVER + i)
      serverList ::= server
    }

    //Send messages between servers so they know each other now (aka Become friends)
    for (server <- serverList) {
      for (otherServer <- serverList) {
        if (server != otherServer) {
          otherServer.tell(FriendRequest, server);
        }
      }
    }

    //Let each client know one server to write and read from and between god/clients and god/servers
    for (i <- 0 to NUMBER_OF_CLIENTS - 1) {
      clientList(i).tell(FriendRequest, serverList(i))
      clientList(i).tell(FriendRequest, god)
      god.tell(FriendRequest, serverList(i))
    }
  }
}
