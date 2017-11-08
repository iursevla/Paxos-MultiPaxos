package main

import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import actorsMultiPaxos._
import actorsDynamo._
import messagesDynamo.NEXTROUND
import messagesMultiPaxos.FriendRequest

object MainGlobal {
  val NUMBER_OF_SERVERS = 7
  val NUMBER_OF_CLIENTS = 3
  val NUMBER_OF_KEYS = 100
  val OPS = 10000
  val K = 3

  val INSERTS = OPS * 0.4
  val REMOVES = OPS * 0.1

  var SERVER: String = "server_"
  var CLIENT: String = "client_"

  var itsEndMultiPaxos = 0 //Verify when MP is over
  var itsEndDynamo = 0 //Verify when Dynamo is over

  var timeMultiPaxos: Long = 0
  var timeDynamo: Long = 0

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem("akka4scala"); // Create the actor system that will spawn our actors

    //MULTIPAXOS
    //Create 3 clients for each 
    var clientListMultiPaxos = List[ActorRef]()
    for (i <- 1 to NUMBER_OF_CLIENTS) {
      val clientMultiPaxos = actorSystem.actorOf(Props[ClientMultiPaxos], CLIENT + i)
      clientListMultiPaxos ::= clientMultiPaxos
    }

    var leaderMultiPaxos = actorSystem.actorOf(Props[LeaderMultiPaxos], SERVER + "1")
    //Create 6 servers map.get(k) for MultiPaxos
    var serverListMultiPaxos = List[ActorRef]()
    for (i <- 2 to NUMBER_OF_SERVERS) {
      val server = actorSystem.actorOf(Props[OtherMultiPaxos], SERVER + i)
      serverListMultiPaxos ::= server
    }

    //Send messages between servers so they know each other now (aka Become friends)
    for (server <- serverListMultiPaxos) {
      server.tell(FriendRequest, leaderMultiPaxos) //Server conhece o leader
      for (otherServer <- serverListMultiPaxos) {
        if (server != otherServer) {
          otherServer.tell(FriendRequest, server);
        }
      }
    }
    //Leader ficar a conhecer os outros servers existentes
    for (server <- serverListMultiPaxos) {
      leaderMultiPaxos.tell(FriendRequest, server)
    }

    //Let each client know one server to write and read from
    for (i <- 0 to NUMBER_OF_CLIENTS - 1) {
      clientListMultiPaxos(i).tell(FriendRequest, leaderMultiPaxos) //Todos os clientes so conhecem o server 0, este envia para os outros todos
    }
    //END MULTIPAXOS

    //DYANMO
    var clientListDynamo = List[ActorRef]()
    for (i <- 1 to NUMBER_OF_CLIENTS) {
      val clientDynamo = actorSystem.actorOf(Props[ClientDynamo], CLIENT + "D_" + i)
      clientListDynamo ::= clientDynamo
    }

    //Create 7 servers map.get(k) for Dynamo
    var serverListDynamo = List[ActorRef]()
    for (i <- 1 to NUMBER_OF_SERVERS) {
      val serverDynamo = actorSystem.actorOf(Props[ServerDynamo], SERVER + "D_" + i)
      serverListDynamo ::= serverDynamo
    }

    //For each client create one server as "leader"
    var randomServers = randomizeServers(serverListDynamo); //SERVERS THAT BELONG TO WRITE QUORUM
    //println(randomServers)
    for (i <- 0 to K - 1)
      clientListDynamo(i).tell(FriendRequest, randomServers(i))

    //Send messages between servers so they know each other now (aka Become friends)
    for (server <- serverListDynamo) {
      for (otherServer <- serverListDynamo) {
        otherServer.tell(FriendRequest, server);
      }
    }

    for (client <- clientListDynamo) {
      client.tell(NEXTROUND, client)
    }
    //END DYNAMO
  }

  /**
   * Select 3 random servers from the server list to be used one for each client to communicate with.
   */
  def randomizeServers(serverList: List[ActorRef]): List[ActorRef] = {
    var arr = scala.util.Random.shuffle(0 to 6)
    return List[ActorRef](serverList(arr(0)), serverList(arr(1)), serverList(arr(2)))
  }
}
