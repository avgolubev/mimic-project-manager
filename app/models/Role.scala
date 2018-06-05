package models

sealed trait Role
case object Administrator extends Role
case object NormalUser extends Role

object Permission {
  
  def valueOf(value: String): Role = value match {
    case "Administrator" => Administrator
    case "NormalUser" => NormalUser
    case _ => throw new IllegalArgumentException()
  }

  def stringValueOf(value: Role): String = value match {
    case Administrator => "Administrator" 
    case NormalUser => "NormalUser" 
    case _ => throw new IllegalArgumentException()
  }  
  
}