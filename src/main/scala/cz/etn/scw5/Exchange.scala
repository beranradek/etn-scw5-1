package cz.etn.scw5

import akka.actor.Actor
import scala.collection.immutable.Queue
import akka.actor.ActorRef

class Exchange extends Actor {

  private var cq = Map[String, Queue[(Quote, ActorRef)]]().withDefaultValue(Queue())
  private var th = List[Trade]()
  private var traders = Set[ActorRef]()

  def receive = {
    case q: Quote =>
      //Thread.sleep(1000)
      println(Thread.currentThread.getName)
      val queue = cq(q.commodity)

      val (prefix, suffix) = queue.span(!_._1.matches(q))

      val (newQueue, newHistory) = if (suffix.isEmpty) {
        // sender -> odesilatel posledni zpravy
        (prefix.enqueue(q -> sender), th)
      } else {
        val trade = Trade(q.commodity, q.quantity, math.max(q.price, suffix.head._1.price))
        traders.foreach(_ ! trade)
        (prefix ++ suffix.tail, th :+ trade)
      }

      cq = cq.updated(q.commodity, newQueue)
      th = newHistory

    case Subscribe(trader) =>
      traders = traders + trader

    case Unsubscribe(trader) =>
      traders = traders - trader
  }

  def commodityQueues = cq

  def tradeHistory = th

  def subcribers = traders
}