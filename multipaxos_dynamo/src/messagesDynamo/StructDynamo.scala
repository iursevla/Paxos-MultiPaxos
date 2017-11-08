package messagesDynamo

import akka.actor.ActorRef
import scala.collection.immutable.Set
import scala.collection.mutable.Queue

/**
 * Data structure used to save data belonging to a key.
 * K -> StructMultiPaxos (V1, V2, V3..., Vn)
 */
class StructDynamo(clk:Clock,vals:Set[String]) {
	var clock: Clock = this.clk
  var values = this.vals //Lista de valores associados à key //e.g. key_1 -> X,Y,Y,3
  
  /**
   * Elimina todos os valores elem da lista
   */
  def deleteElement(elem: String) {
    values = values.filter(_ != elem);
//    for (i <- 0 to values.size - 1) {
//      if (values(i).value.equals(elem)) {
//        values(i).isDeleted = true
//        return true;
//      }
//    }
//    return false;
    //values = values.filter(_.value != elem);
  }

  /**
   * Adiciona o valor elem ao final da lista
   */
  def addElement(elem: String) {
    values += elem;
  }

  /**
   * Verifica se o valor elem existe na lista
   * @return TRUE caso exista
   * @return FALSE caso contrario
   */
  def hasElement(elem: String): Boolean = {
    return values.contains(elem)
  }

  /**
   * Devolve todos os valores da lista
   * @return List[String]
   */
  def getValues(): List[String] = {
    return values.toList
  }

//  def getValor(elem: String): String = {
//    for (value <- values)
//      if (value.value.equals(elem))
//        return value
//    return null
//  }
  
  def getClock(): Clock = {
    return this.clock;
  }

  /**
   * np (highest prepare)
   * na (highest sequence number)
   * va (highest accept)
   */
  override def toString: String = "(" + values + ")" //"(" + np + ", " + na + ", " + va + ")"

}