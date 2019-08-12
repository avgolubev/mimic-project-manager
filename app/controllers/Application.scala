package controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration

import play.api.mvc._
import play.api.db._
import play.api.libs.json.{Json, Reads, JsObject, JsString}

import play.api.data._
import play.api.data.Forms._

import java.sql.{Connection, PreparedStatement, ResultSet, SQLException}
import java.util.logging.{Level, Logger}

import views.html._
import models._

import jp.t2v.lab.play2.auth.AuthElement
import scala.concurrent.Future
import play.api.inject.ApplicationLifecycle

import anorm.{ Macro, RowParser, SQL, SqlParser, ToStatement, sqlToSimple, BatchSql, NamedParameter }
import anorm.SqlParser.scalar

import scala.util.{Try, Success, Failure}

@Singleton
class Application @Inject()(
    @NamedDatabase("derby") implicit val derby: Database, configuration: Configuration, lifecycle: ApplicationLifecycle) 
    extends Controller with AuthElement  with AuthConfigImpl with ConsolidatingTrait {

  lifecycle.addStopHook { () =>
    Future.successful{
      println("stopping apache derby") 
      java.sql.DriverManager.getConnection("jdbc:derby:;shutdown=true")
    }
  }  
  
  val jiraURL           = configuration.underlying.getString("jira.url")         
  val jiraSearchPath    = configuration.underlying.getString("jira.searchPath")  
  val jiraSavePath      = configuration.underlying.getString("jira.savePath")    
  val jasperReportFile  = configuration.underlying.getString("jasper.reportFile")
  val pairJiraURL       = ("jiraURL", JsString(jiraURL))
         
  def index = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    Ok(views.html.index(loggedIn.userName))
  }

  def issues = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val jsonIssues = getJsonIssues(loggedIn.userName, loggedIn.jiraCookies, jiraURL + jiraSearchPath).as[JsObject] + pairJiraURL
    Ok(jsonIssues)
  }

  def save = StackAction(BodyParsers.parse.json[Seq[IssueDB]], AuthorityKey -> NormalUser) { implicit request =>
    val issues = IssuesDB(request.body)
    saveChanges(issues, loggedIn.jiraCookies, jiraURL + jiraSavePath)
    Ok(Json.obj("status" -> true))
  }


  def hide = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val status = request.body.asJson
                 .map(v => hideIssue((v \ "task_id").as[String]))
                 .getOrElse(false)
    Ok(Json.obj("status" -> status))
  }

  def report(filter: String) = StackAction(AuthorityKey -> NormalUser) { implicit request =>

    val r = getReport(filter, loggedIn.userName, loggedIn.jiraCookies, jiraURL + jiraSearchPath, jasperReportFile)

    Ok(r)
    .withHeaders(
//         "Content-Type"              -> "application/pdf"
//       , "Content-Disposition"       -> "attachment; filename=report.pdf"
//         "Content-Type"              -> "application/vnd.ms-excel"
        "Content-Disposition"       -> "attachment; filename=report.xls"
//       , "Content-Transfer-Encoding" -> "binary"
    ).as("application/vnd.ms-excel")
  }
  
}
