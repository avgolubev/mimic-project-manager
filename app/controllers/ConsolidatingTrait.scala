package controllers

import play.api.db.Database
//import play.api.mvc.Controller
import models.{Cookies, IssueDB, IssuesDB, DbApi}
import utils.Jira

trait ConsolidatingTrait extends DbApi with Jira {
  def saveChanges(issues: IssuesDB, myCookies: Cookies, jiraSaveURL: String)(implicit db: Database) {
    saveIssues(issues)
    saveIssues(issues, myCookies, jiraSaveURL)    
  }
  
  override def getIssueDB(task_id: String, fullName: String)(implicit db: Database): IssueDB = getIssue(task_id, fullName) 
  override def inReportDB(task_id: String)(implicit db: Database): Boolean = inReport(task_id)
  
}