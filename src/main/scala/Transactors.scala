package transactors.transactors

import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.util.Logger
import se.scalablesolutions.akka.stm.global._

object Transactors {

  val log = Logger("Actors")

  trait NonZeroAmount {
    def amount: BigDecimal
    require(amount >= BigDecimal(0))
  }

  case class Withdraw(amount: BigDecimal) extends NonZeroAmount
  
  case object WithdrawalOK
  
  case object InsufficientFunds

  case class Deposit(amount: BigDecimal) extends NonZeroAmount
  
  case object DepositOK
  
  case object ShowBalance
  
  case class Balance(amount: BigDecimal) extends NonZeroAmount
  
  case class Transfer(amount: BigDecimal, recipient: ActorRef) extends NonZeroAmount

  class Account extends Transactor {
    var currentAmount = Ref(BigDecimal(0))
    
    def receive = {
      case Withdraw(amount) if (currentAmount.get >= amount) =>
        currentAmount.alter(_ - amount)
        self reply_? WithdrawalOK

      case Withdraw(_) =>
        self reply_? InsufficientFunds
        
      case Deposit(amount) =>
        currentAmount.alter(_ + amount)
        
        if (amount == 100) //Blow up on purpose to show rollback
          throw new IllegalStateException("None shall pass!")
        
        self reply_? DepositOK
        
      case ShowBalance =>
        self reply_? Balance(currentAmount.get)
    }
  }
}

object Run {
  import Transactors._
  
  def apply() = {
    val account1, account2 = actorOf[Account].start
    
    account1 ! Deposit(BigDecimal(1000))  
    atomic {
    //Transfer between accounts
    (account1 !! Withdraw(100)) match {
    
      case Some(WithdrawalOK) => try {
        val depositResult = account2 !! Deposit(100)
          log.info("Transfer result: %s", depositResult.getOrElse("timed out"))
        } catch {
          case e => log.info("Transfer failed")
        }
      case Some(InsufficientFunds) =>
        log.error("Insufficient funds!")
        
      case None =>
        log.error("Got no reply from account1 when withdrawing.")
    }
    
    log.info("Account1 balance:  %s", (account1 !! ShowBalance))
    log.info("Account2 balance:  %s", (account2 !! ShowBalance))
    }
    
    account1.stop
    account2.stop
  }
}

//My output, drowning in other output:
//[INFO] [2010-09-30 17:56:15,666] [run-main] Actors: Transfer result: DepositOK
//[INFO] [2010-09-30 17:56:15,669] [run-main] Actors: Account1 balance:  Some(Balance(900))
//[INFO] [2010-09-30 17:56:15,672] [run-main] Actors: Account2 balance:  Some(Balance(100))
