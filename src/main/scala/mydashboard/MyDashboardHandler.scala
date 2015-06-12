package mydashboard

import dashboard.server.{DashboardResponse, DashboardRequest, DashboardHandler}
import spray.http.{StatusCodes, HttpEntity, HttpMethods, HttpMethod}
import spray.routing.PathMatchers

/**
 * @author nader albert
 * @since  11/06/2015.
 */
class MyDashboardHandler extends DashboardHandler{

  override def reactions: Set[(DashboardRequest,DashboardResponse)] =
   Set(
     //(DashboardRequest(HttpMethods.GET, "dashboard" / " home"), DashboardResponse(HttpEntity("home"),StatusCodes.OK)),
      (DashboardRequest(HttpMethods.GET, separateOnSlashes("dashboard/timetable") ), DashboardResponse(HttpEntity("timetable"),StatusCodes.OK))
    )

}
