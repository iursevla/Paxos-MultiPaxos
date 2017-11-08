package messagesMultiPaxos

import akka.actor.ActorRef
import scala.collection.immutable.Set
import scala.collection.mutable.Queue

/**
 * Data structure used to save data belonging to a key.
 * K -> StructMultiPaxos (V1, V2, V3..., Vn)
 */
class StructMultiPaxos {
  var values = Set[Valor](); //Lista de valores associados à key //e.g. key_1 -> X,Y,Y,3
  var replicas = Set[ActorRef]() //Replica que contem esta key
  var pendingOps = Queue[OP]() //Pending operations for this key
  
  /**
   * Elimina todos os valores elem da lista
   */
  def deleteElement(elem: String) {
    values = values.filter(_.value != elem);
  }

  /**
   * Adiciona o valor elem ao final da lista
   */
  def addElement(elem: Valor) {
    values += elem;
  }

  /**
   * Verifica se o valor elem existe na lista
   * @return TRUE caso exista
   * @return FALSE caso contrario
   */
  def hasElement(elem: String): Boolean = {
    for (valor <- values) 
      if(valor.value.equals(elem))
        return true
    return false;
  }

  /**
   * Devolve todos os valores da lista
   * @return List[String]
   */
  def getValues(): List[String] = {
    var vals = List[String]();
    for (valor <- values) {
      vals ::= valor.value
    }
    return vals;
  }
  
  def getValor(elem: String): Valor = {
    for(value <- values)
      if(value.value.equals(elem))
        return value
    return null
  }

  def addReplica(actor:ActorRef) {
    replicas += actor
  }
  
  def addPendingOp(sender: ActorRef, op: Super){
    pendingOps += new OP(sender, op)
  }
  
  /**
   * np (highest prepare)
   * na (highest sequence number)
   * va (highest accept)
   */
  override def toString: String = "(" + values + ")" //"(" + np + ", " + na + ", " + va + ")"

}