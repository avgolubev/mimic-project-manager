package controllers

import javax.inject.Inject
import play.api.Configuration
import play.api.mvc._
import jp.t2v.lab.play2.auth.LoginLogout
import play.api.data.Forms._
import play.api.data._
import scala.concurrent.Future
import models._
import views.html._
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class Authentication @Inject()(configuration: Configuration) extends Controller with LoginLogout with AuthConfigImpl {
  
  val jiraAuthURL = configuration.underlying.getString("jira.url") + 
                    configuration.underlying.getString("jira.authPath")

  val loginForm = Form {
    mapping("userName" -> nonEmptyText
          , "password" -> nonEmptyText
    )(Account.authenticate(_, _, jiraAuthURL))(_.map(u => (u.userName, "")))
     .verifying("Invalid user name or password", result => result.isDefined)
  }
  
//---------------------------------------------------------------------------------------------------------
  def login = Action {
    Ok( views.html.login(loginForm)(false) )
  }    
  
  def logout = Action.async { implicit request => 
    gotoLogoutSucceeded.map(_.flashing( "success" -> "You've been logged out" ))    
  }
     
  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors)(true))),  
      user => gotoLoginSucceeded(user.get)      
    )    
  }  
  
}
