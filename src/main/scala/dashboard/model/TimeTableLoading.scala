package dashboard.model
import spray.http.DateTime
import spray.json.{JsString, JsObject, JsValue}


/**
 * @author nader albert
 * @since 28/05/2015.
 */
trait TimeTableLoading {
  val status: ReseedingStatus
  val bundleId: Long
}

case class TimeTableReseed (bundleId: Long, status: ReseedingStatus) extends TimeTableLoading

object TimeTableLoading{
  def writer (timeTableReseed: TimeTableReseed):JsValue =
    JsObject(Map.empty[String, JsValue]
      updated ("bundleId", JsString(timeTableReseed.bundleId.toString))
      updated ("status", timeTableReseed.status match {
        case s: SuccessfulReseed => JsString("Successful")
        case i: InProgressReseed => JsString("In-Progress")
        case f: FailingReseed => JsString("Failed")
        case _ => JsString("invalid status")})
      updated ("statistics", ReseedingStatus.writer(timeTableReseed.status)))
}

private [model] trait ReseedingStatus {
  def startedAt: DateTime
  def finishedAt:Option[DateTime]
  def succeeded: Boolean
  def failed: Boolean
  def inProgress: Boolean
  def successfulTripCount: Int
  def failedTripCount: Int
}

object ReseedingStatus {

  def writer (status: ReseedingStatus): JsObject = JsObject(
    Map.empty[String, JsValue]
      updated ("startedAt", JsString(status.startedAt + ""))
      updated ("finishedAt", JsString(status.finishedAt.toString))
      updated ("succeeded", JsString(status.succeeded.toString))
      updated ("failed", JsString(status.failed.toString))
      updated ("inProgress", JsString(status.inProgress.toString))
      updated ("successfulTripCount", JsString(status.successfulTripCount.toString))
      updated ("failedTripCount", JsString(status.failedTripCount.toString))
  )
}

case class SuccessfulReseed(startedAt:DateTime,
                            finishedAt:Option[DateTime],
                            succeeded:Boolean= true,
                            failed:Boolean= false,
                            inProgress:Boolean= false,
                            successfulTripCount: Int,
                            failedTripCount: Int) extends ReseedingStatus {
}

case class InProgressReseed(startedAt:DateTime,
                            finishedAt:Option[DateTime],
                            successfulTripCount: Int,
                            failedTripCount: Int,
                            succeeded:Boolean= false,
                            failed:Boolean= false,
                            inProgress:Boolean= true) extends ReseedingStatus

case class FailingReseed(startedAt:DateTime,
                         finishedAt:Option[DateTime],
                         succeeded:Boolean= false,
                         failed:Boolean= true,
                         inProgress:Boolean= false,
                         successfulTripCount: Int,
                         failedTripCount: Int,
                         failureReason: String= "unknown reason! ") extends ReseedingStatus