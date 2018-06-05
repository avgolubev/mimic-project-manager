package models

import scala.concurrent.{Future}
import play.api.cache._
import javax.inject.Inject
import utils._
import play.api.db.Database

case class Account (userName: String, role: Role, jiraCookies: Cookies) 

object Account {
  
  def authenticate(userName: String, password: String, jiraAuthURL: String)(implicit db: Database): Option[Account] = {
    
    Jira.authenticate(userName, password, jiraAuthURL) match {
      case Left(error) => {
        println(error)
        None
      }
      case Right(cookies) => Some( Account(userName, NormalUser, cookies) )      
    }    
  }  
    
  def role = NormalUser
  
}