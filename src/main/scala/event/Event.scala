package event

/**
 * @author nader albert
 * @since  11/06/2015.
 */
trait Event {

}

class PieEvent(items: Map[String, (String, Int)]) extends Event{
  require(items.values.map(tuple => tuple._2).sum == 100, "total percentages in a pie should be 100")

  def update (currentItems: Map[String, Int]) = ???
}

case class BarEvent(item: String, value: Float) extends Event{
  def update (currentValue: Float) = {
    require(currentValue >= value, "bar valu cannot move backward")
    copy(item, value = currentValue)
  }
}
