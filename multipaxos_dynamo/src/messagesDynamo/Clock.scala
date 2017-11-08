package messagesDynamo

class Clock {
  var elems: Array[Int] = Array[Int](0, 0, 0, 0, 0, 0, 0)
  //TODO var clock=[] clock para onde esta key estiver guardada

  override def equals(o: Any) = o match { //Se elems[i]>=that[i]
    case that: Clock => {
      var thisOrThat = true
      for (i <- 0 to elems.size - 1) {
        if (elems(i) < that.elems(i) || that.elems(i) < elems(i))
          false
      }
      true
      //that.elems == this.elems

    }
    case _ => false
  }

  def setClock(elems: Array[Int]) {
    this.elems = elems
  }
  
  def printAllElems():String = {
    var s: String = ""
    for(i <- 0 to elems.size - 1){
      s += elems(i) + "|"
    }
    return s
  }

  override def toString: String = "((" + printAllElems() + "))"
}