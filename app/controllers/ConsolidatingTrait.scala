package controllers

import play.api.libs.json.{Json, Reads, JsObject, JsString}
import play.api.db.Database
//import play.api.mvc.Controller
import models.{Cookies, IssueDB, IssuesDB, DbApi}
import utils.Jira

trait ConsolidatingTrait extends DbApi with Jira {
  
  // for right convertion from json to objects
  implicit val dateReadsISO8601UTCtoLocal = Reads.dateReads("yyyy-MM-dd'T'HH:mm:ss.SSSX")
  implicit val issueDBReader  = Json.reads[IssueDB]
  implicit val issuesDBReader = Json.reads[IssuesDB]  
  
  def saveChanges(issues: IssuesDB, myCookies: Cookies, jiraSaveURL: String)(implicit db: Database) {
    saveIssues(issues)
    saveIssues(issues, myCookies, jiraSaveURL)    
  }
  
  override def getIssueDB(task_id: String, fullName: String)(implicit db: Database): IssueDB = getIssue(task_id, fullName) 
  override def inReportDB(task_id: String)(implicit db: Database): Boolean = inReport(task_id)
  
}