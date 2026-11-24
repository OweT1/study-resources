object Fibonacci01 {
  def fib(n: Int): Int = {
    @annotation.tailrec
    def go(n: Int, prev: Int, current: Int): Int = {
      if (n <= 0) prev
      else go(n-1, current, prev+current)
    }
    go(n, 0, 1)
  }

  def main(args: Array[String]): Unit = {
    val n1 = 5
    val n2 = 10
    println("The Fibonaaci of %d is %d".format(n1, fib(n1)))
    println("The Fibonaaci of %d is %d".format(n2, fib(n2)))
  }
}
