package controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration

//import play.api.Play.current
import play.api.mvc._
import play.api.db._
import play.api.libs.json.{Json, Reads}

import play.api.data._
import play.api.data.Forms._

import java.sql.{Connection, PreparedStatement, ResultSet, SQLException}
import java.util.logging.{Level, Logger}

//import scala.concurrent.Future
//import play.api.cache.CacheApi
import views.html._
import models._
import utils._

//import com.fasterxml.jackson.databind.node.ObjectNode

import jp.t2v.lab.play2.auth.AuthElement
import play.api.inject.ApplicationLifecycle

import anorm.{ Macro, RowParser, SQL, SqlParser, ToStatement, sqlToSimple, BatchSql, NamedParameter }
import anorm.SqlParser.scalar

import scala.util.{Try, Success, Failure}

//lifecycle: ApplicationLifecycle

@Singleton
class Application @Inject()(
    @NamedDatabase("derby") implicit val derby: Database
  , configuration: Configuration) extends Controller with AuthElement  with AuthConfigImpl {

  val jiraSearchURL    = configuration.underlying.getString("jira.searchURL")    //"https://jira.corp.motiv/rest/api/2/search"
  val jiraSaveURL      = configuration.underlying.getString("jira.saveURL")      //"https://jira.corp.motiv/rest/api/2/issue/"
  val jasperReportFile = configuration.underlying.getString("jasper.reportFile") //"c:/!/reports/excel_report.jasper"

  def index = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    Ok(views.html.index(loggedIn.userName))
  }

  def issues = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    Ok(Jira.getJsonIssues(loggedIn.userName, loggedIn.jiraCookies, jiraSearchURL))
  }

  implicit val dateReadsISO8601UTCtoLocal = Reads.dateReads("yyyy-MM-dd'T'HH:mm:ss.SSSX")
  implicit val issueDBReader  = Json.reads[IssueDB]
  implicit val issuesDBReader = Json.reads[IssuesDB]

  def saveChanges = StackAction(BodyParsers.parse.json[Seq[IssueDB]], AuthorityKey -> NormalUser) { implicit request =>
    val issues = IssuesDB(request.body)
    IssueDB.saveIssues(issues)
    Jira.saveIssues(issues, loggedIn.jiraCookies, jiraSaveURL)

    Ok(Json.obj("status" -> true))
  }


  def hideIssue = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    val status = request.body.asJson
                 .map(v => IssueDB.hideIssue((v \ "task_id").as[String]))
                 .getOrElse(false)
    Ok(Json.obj("status" -> status))
  }

  def report(filter: String) = StackAction(AuthorityKey -> NormalUser) { implicit request =>

    val r = Jira.getReport(filter, loggedIn.userName, loggedIn.jiraCookies, jiraSearchURL, jasperReportFile)

    Ok(r)
    .withHeaders(
//         "Content-Type"              -> "application/pdf"
//       , "Content-Disposition"       -> "attachment; filename=report.pdf"
//         "Content-Type"              -> "application/vnd.ms-excel"
        "Content-Disposition"       -> "attachment; filename=report.xls"
//       , "Content-Transfer-Encoding" -> "binary"
  )
  .as("application/vnd.ms-excel")
  }
  
}
