object Fibonacci01 {
  def fib(n: Int): Int = {
    def go(n: Int): Int = {
      if (n <= 1) n
      else go(n-1) + go(n-2)
    }
    go(n)
  }

  def main(args: Array[String]): Unit = {
    val n1 = 5
    val n2 = 10
    println("The Fibonaaci of %d is %d".format(n1, fib(n1)))
    println("The Fibonaaci of %d is %d".format(n2, fib(n2)))
  }
}
