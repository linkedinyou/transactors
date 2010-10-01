package transactors.stm

import se.scalablesolutions.akka.stm.global._
import se.scalablesolutions.akka.util.Logger

object STM {
  val log = Logger("STM")
  
  class Account(name: String) {
    private val currentAmount = Ref(BigDecimal(0))
    
    def deposit(amount: BigDecimal): Unit = atomic {
      if (amount <= BigDecimal(0))
        throw new IllegalArgumentException("Can't deposit a non-positive value")
      
      currentAmount.alter(_ + amount)
      log.info("Deposited %s to %s, now balance is %s",amount,name,currentAmount.get)
    }
    
    def withdraw(amount: BigDecimal): Unit = atomic {
      if (amount <= BigDecimal(0) || currentAmount.get < amount)
        throw new IllegalArgumentException("Can't withdraw that amount")
        
      currentAmount.alter(_ - amount)
      log.info("Withdrew %s from %s, now balance is %s",amount,name,currentAmount.get)
    }
    
    def balance() = atomic { currentAmount.get }
  }
}

object Run {
  import STM._
  
  def apply() = {
    val account1 = new Account("Account1")
    val account2 = new Account("Account2")
    
    account1.deposit(BigDecimal(1000))
    
    try {
      val transferAmount = BigDecimal(100)
      log.info("Trying to transfer %s from account1 to account2",transferAmount)
      atomic {
        account1.withdraw(transferAmount)
        throw new IllegalStateException("None shall pass!")
        account2.deposit(transferAmount)
      } 
    } catch {
      case e if e.getMessage == "None shall pass!" => log.info("Expected failure")
    }
    
    log.info("Account1 balance:  %s", account1.balance())
    log.info("Account2 balance:  %s", account2.balance())
  }
}