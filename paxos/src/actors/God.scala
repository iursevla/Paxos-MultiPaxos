package actors

import akka.actor.Actor
import akka.actor.ActorRef
import messages._
import main._
import akka.actor.PoisonPill

class God extends Actor {
  var clients = List[ActorRef]() //All clients that god will contact
  var servers = List[ActorRef]() //To verify values of each key
  var counterDonesThisRound: Int = 0 //Number of clients that are done for this round
  var counterKeysDone: Int = 0 // How many keys were already done

  var tempClients = List[ActorRef]() //Para cada ronda guardar quem mandou a mensagem

  var listOfMaps = List[Map[String, Struct]]() //Para guardar o mapa de kyes para cada cliente 
  var isStarting: Boolean = true
  var startingTime: Long = 0

  def receive = {
    case FriendRequest => {
      if (sender.path.name.contains(Main.CLIENT)) clients ::= sender
      else servers ::= sender
    }
    case DONE => {
      if (!tempClients.contains(sender)) { //nao contiver o client nesta ronda
        tempClients ::= sender
        counterDonesThisRound += 1

        if (counterDonesThisRound == Main.NUMBER_OF_CLIENTS) { //This ronda
          if (counterKeysDone == Main.NUMBER_OF_KEYS) { //End of the algorithm
            System.err.println("KeysDone = " + counterKeysDone)
            println("Time taken: " + (System.nanoTime - startingTime) / 1e6 + "ms")
            println( (Main.NUMBER_OF_KEYS/(((System.nanoTime - startingTime) / 1e6)/1000)).toInt + " Ops/sec")
            Thread.sleep(Main.NUMBER_OF_KEYS)
            getAllMaps();
          } else { // NEXT ROUND
            if (isStarting) {
              startingTime = System.nanoTime //Get starting time
              isStarting = false
            }
            for (client <- clients) {
              client.tell(NEXTROUND, self)
            }
            tempClients = List[ActorRef]()
          }
          counterKeysDone += 1 //Proxima ronda logo aumenta as keys
          counterDonesThisRound = 0 //Nesta ronda nenhum esta feito
        }
      }
    }

    case MAPMESSAGE(map) => {
      listOfMaps ::= map
      if (listOfMaps.size == clients.size) { //Se ja tivermos todos os mapas entao podemos verificar igualdade
        compareMaps()
        System.exit(0)
      }
    }
  }

  //Verify all keys are equal 
  def compareMaps() { //comparar mapas 2 a 2
    for (i <- 0 to clients.size - 2) {
      var map1 = listOfMaps(i)
      var map2 = listOfMaps(i + 1)
      compareTwoMaps(map1, map2, i, i + 1)
    }
    compareTwoMaps(listOfMaps(0), listOfMaps(clients.size - 1), 0, 2)
  }

  def compareTwoMaps(map1: Map[String, Struct], map2: Map[String, Struct], i: Int, j: Int) {
    var equalN: Int = 0;
    for ((k, v) <- map1) {
      var value: String = v.va1;
      if ((map2.get(k).get.va1 == value))
        equalN += 1
    }
  }

  def getAllMaps() {
    for (server <- servers) {
      server.tell("GIVEMETHEMAPS", self)
    }
  }
}