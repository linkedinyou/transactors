package transactors.racetrack

import java.util.concurrent._
import java.util.concurrent.locks._
import java.util.concurrent.atomic._
import java.util.HashMap
import se.scalablesolutions.akka.util.Logger

object Racetrack {
  val log = Logger("Racetrack")
  def createRacer(name: String, map: HashMap[String, String], removes: String, shouldContinue: AtomicBoolean) = new Thread(name) {
  
    override def run {
      log.info("%s enters the racetrack, ready to add and remove '%s'!",name,removes)
    
      var crashes = 0
      var successes = 0
      while(shouldContinue.get) {
        if (map.get("value") == removes) {
          val valueRemoved = map.remove("value")
          if (removes != valueRemoved) {
            crashes += 1
            log.info("%s thought it removed '%s' but in fact removed '%s', now at %d crashes.",name,removes,valueRemoved, crashes)
          } else {
            successes += 1
            if (successes % 100000 == 0)
              log.info("%s is has another 100000 successes!!")
          }
        }
        
        map.put("value",removes)
        
        if (crashes >= 10)
          shouldContinue.set(false)
      }
      
      log.info("%s managed to get %d successes and %d crashes",name,successes,crashes)
    }
  }
}

object Run {
  import Racetrack._
  def apply(racers: Map[String,String]) {
    val shouldContinue = new AtomicBoolean(true)
    val map = new HashMap[String,String]
    racers foreach { (entry) =>
      val (name,removes) = (entry._1, entry._2)
      val r = createRacer(name,map,removes,shouldContinue)
      r.start
    }
  }
}

//2 racers
//Run(Map("The Cookie Monster" -> "cookie", "The Swedish Chef" -> "Bork! Bork!"))

//3 racers
//Run(Map("The Cookie Monster" -> "COOKIE!!!",
//        "The Swedish Chef"   -> "Bork! Bork!",
//        "Leeroy Jenkins"     -> "LEEEEEEEEEEROYYYYY!!!!"))