package models

import play.api.db._
import anorm.{ Macro, RowParser, SQL, SqlParser, ToStatement, sqlToSimple, BatchSql, NamedParameter }
import java.util.Date

case class IssueDB(task_id: String, start_date: Option[Date], end_date: Option[Date], executors: String, customer: String, ord: String, progress: Int)
case class IssuesDB(issues: Seq[IssueDB]) 

trait DbApi {
    
  def getIssue(task_id: String, fullName: String)(implicit db: Database): IssueDB =     
    db.withConnection { implicit connection =>
      val parser: RowParser[IssueDB] = Macro.namedParser[IssueDB]
         
        SQL("""
            select task_id, start_date, end_date, executors, customer, ord, progress 
              from issues where task_id = {task_id} and visible = true order by start_date
            """)
          .on("task_id" -> task_id)
          .as(parser.*) 
          match {           
            case List() => {
              SQL("""
                insert into issues(task_id, start_date, end_date, executors, customer, ord, progress, visible) 
                            values ({task_id}, {start_date}, {end_date}, {executors}, {customer}, {ord}, {progress}, {visible})
                """)
                .on("task_id"    -> task_id
                  , "start_date" -> Option.empty[Date]
                  , "end_date"   -> Option.empty[Date]
                  , "executors"  -> fullName
                  , "customer"   -> ""
                  , "ord"        -> ""
                  , "progress"   -> 0
                  , "visible"    -> true)
                .executeInsert()
              IssueDB(task_id, None, None, fullName, "", "", 0)
            }
            
            case result => result(0)
          }                          
    }
  
  def saveIssues(issues: IssuesDB)(implicit db: Database): Boolean =
    db.withTransaction { implicit connection =>
      val params: Seq[Seq[NamedParameter]] = 
        issues.issues
        .map {issueRec =>
          Seq[NamedParameter](
              "task_id"    -> issueRec.task_id
            , "start_date" -> issueRec.start_date
            , "end_date"   -> issueRec.end_date
            , "executors"  -> issueRec.executors
            , "customer"   -> issueRec.customer
            , "ord"        -> issueRec.ord
            , "progress"   -> issueRec.progress)        
      }
            
      BatchSql("""
        update issues set start_date={start_date}
                        , end_date={end_date}
                        , executors={executors}
                        , customer={customer}
                        , ord={ord}
                        , progress={progress} 
                    where task_id={task_id}          
      """, params(0), params.tail: _*)
      .execute()
      
      true
    }
  
  def hideIssue(task_id: String)(implicit db: Database): Boolean =     
    db.withConnection { implicit connection =>        
      SQL("""
        update issues set visible = false where task_id={task_id}
        """)
        .on("task_id"    -> task_id)
        .executeUpdate()        
      true
    }                       
    
  
  def inReport(task_id: String)(implicit db: Database): Boolean =
    db.withConnection { implicit connection =>        
        SQL("""
            select count(*) as cnt  
              from issues where task_id = {task_id} and visible = false
            """)
          .on("task_id" -> task_id)
          .as(SqlParser.int("cnt").single) 
          match {           
            case 0 => true             
            case _ => false
          }                          
    }    
    
    //import java.sql.DriverManager
    //DriverManager.getConnection("jdbc:derby:;shutdown=true")
  
}