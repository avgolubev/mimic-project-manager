package models

import scala.concurrent.{Future}
import play.api.cache._
import javax.inject.Inject
import utils.JiraAuth

case class Account (userName: String, role: Role, jiraCookies: Cookies) 

object Account extends JiraAuth {
  
  def authenticate(userName: String, password: String, jiraAuthURL: String): Option[Account] = {
    
    authenticateHttps(userName, password, jiraAuthURL) match {
      case Left(error) => {
        println(error)
        None
      }
      case Right(cookies) => Some( Account(userName, NormalUser, cookies) )      
    }    
  }  
    
  def role = NormalUser
  
}