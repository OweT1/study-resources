object Sorted02 {
  def isSorted[A](as: Array[A], gt: (A,A) => Boolean): Boolean = {
    @annotation.tailrec
    def go(pos1: Int, pos2: Int): Boolean = {
      if (pos2 >= as.length) true
      else gt(as(pos1), as(pos2)) && go(pos2, pos2+1)
    }
    go(0, 1)
  }

  def main(args: Array[String]): Unit = {
    val sortedArr = Array(1, 2, 3, 4)
    val unsortedArr = Array(1, 3, 2, 4)
    println(isSorted(sortedArr, (a: Int, b: Int) => a <= b))   // true
    println(isSorted(unsortedArr, (a: Int, b: Int) => a <= b)) // false
  }
}
