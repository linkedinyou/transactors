package transactors

import java.util.concurrent._
import java.util.concurrent.locks._
import se.scalablesolutions.akka.util.Logger

object DeadlyEmbrace {

  val barrier = new CyclicBarrier(2)

  val lockA = new ReentrantLock { override def toString = "lockA" }
  val lockB = new ReentrantLock { override def toString = "lockB" }
  
  def planned(name: String, lock1: ReentrantLock, lock2: ReentrantLock) = new Thread(name) {
    val log = Logger(name)
    override def run = {
      lock1.lock
      log.info("%s locked %s",name,lock1)
      barrier.await
      log.info("%s going for %s",name,lock2)
      lock2.lock
      log.info("%s got them both!",name)
      
      lock1.unlock
      lock2.unlock
    }
  }
  
  def provoke(name: String, lock1: ReentrantLock, lock2: ReentrantLock) = new Thread(name) {
    val log = Logger(name)
    override def run = {
      while(true) {
        lock1.lock
        log.info("%s locked %s",name,lock1)
      
        log.info("%s going for %s",name,lock2)
        lock2.lock
        log.info("%s got them both!",name)
      
        lock1.unlock
        lock2.unlock
      }
    }
  }
}

object Run {
  import DeadlyEmbrace._
  val threadA = provoke("threadA",lockA,lockB)
  val threadB = provoke("threadB",lockB,lockA)
  threadA.start
  threadB.start
}