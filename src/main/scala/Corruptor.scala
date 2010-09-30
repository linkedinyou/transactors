package transactors

import java.util.concurrent._
import java.util.concurrent.locks._
import se.scalablesolutions.akka.util.Logger

object Corruptor {
  val log = Logger("Corruptor")
  
  def createAppender(text: String, output: StringBuilder, startSignal: CountDownLatch, stopSignal: CountDownLatch) = new Thread {
    override def run = {
      startSignal.await
      
      for(i <- 0 to 10000) output append text
      
      stopSignal.countDown
    }
  }
}

object Run {
  import Corruptor._
  
  def apply(texts: List[String]) = {
    val startSignal = new CountDownLatch(1)
    val stopSignal = new CountDownLatch(texts.size)
    val output = new StringBuilder(1024)
    
    for(text <- texts) {
      val appender = createAppender(text,output,startSignal,stopSignal)
      appender.start
    }
    
    startSignal.countDown
    stopSignal.await
    
    val errors = texts.foldLeft(output.toString) { (output,text) => output.replace(text,"") }
    
    log.info("Errors: [%s]",errors)
  }
}