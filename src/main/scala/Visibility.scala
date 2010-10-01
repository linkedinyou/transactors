package transactors.visibility

import java.util.concurrent._
import java.util.concurrent.locks._
import se.scalablesolutions.akka.util.Logger

object Visibility {
  val log = Logger("Visibility")
  
  class Worker(val name: String) extends Thread(name) {
    
    log.info("Creating %s",name)
  
    var shouldKeepWorking = true
    
    def stopWorking() {
      log.info("%s should stop working",name)
      shouldKeepWorking = false
    }
    
    override def run {
      var counter = 0
      log.info("%s is going to work.",name) 
      while(shouldKeepWorking) {
        counter += 1
      }
      log.info("%s is done for the day",name)
    }
  } 
}

object Run {

  def apply() = {
    import Visibility._

    val worker = new Worker("Working class hero")
    worker.start

    log.info("Waiting 10 seconds until we tell %s to stop working.",worker.name)
    Thread.sleep(10000) //10 seconds
    worker.stopWorking()
    
    worker
  }
}