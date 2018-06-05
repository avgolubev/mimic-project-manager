package controllers

import jp.t2v.lab.play2.auth._
import play.api.mvc.Results.{Redirect, Forbidden, Unauthorized}
import play.api.mvc.{Action, Controller, RequestHeader, Result}
import play.api.http.Status
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

import models._


trait AuthConfigImpl extends AuthConfig{
     
  type Id = Account
  
  type User = Account
  
  type Authority = Role
  
  val idTag: ClassTag[Id] = classTag[Id]
  
  val sessionTimeoutInSeconds: Int = 3600
     
  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = Future.successful(Some(id)) 
    
  
  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Redirect(routes.Application.index))
  
  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Redirect(routes.Authentication.login))
    
  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = Future.successful {
  request.headers.get("X-Requested-With") match {
    case Some("XMLHttpRequest") => Unauthorized("Authentication failed")
    case _ => Redirect(routes.Authentication.login)
  }
}    
    
  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden("no permission"))
  }
  
  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    (user.role, authority) match {
      case (Administrator, _)       => true
      case (NormalUser, NormalUser) => true
      case _                        => false
    }
  }
  
  
  
}