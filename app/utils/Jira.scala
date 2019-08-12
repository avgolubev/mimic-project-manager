package utils

import javax.net.ssl.{HostnameVerifier
                    , SSLSession
                    , HttpsURLConnection
                    , SSLContext
                    , TrustManager
                    , X509TrustManager} 
import java.security.cert.X509Certificate
import java.net.URL
import java.util.{Date, TimeZone, Calendar}
import java.time.temporal._
import java.time._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaj.http._
import play.api.db.Database

import net.sf.jasperreports.engine._
import net.sf.jasperreports.engine.data._
import net.sf.jasperreports.view.JasperViewer
import net.sf.jasperreports.engine.export.{JRXlsExporter}
import net.sf.jasperreports.export.{SimpleExporterInput
                                  , SimpleOutputStreamExporterOutput
                                  , SimpleXlsReportConfiguration}
import java.io.ByteArrayOutputStream

import models._

case class Issue(task_id:      String
               , title:      String
               , status:     String
               , start_date: Option[Date]
               , end_date:   Option[Date]
               , executors:  String
               , customer:   String
               , ord:        String
               , progress:   Int) 
case class Issues(user: String, name: String, issues: Seq[Issue], error: String)

case class JiraIssue(task_id:  String
                   , title:    String
                   , status:   String
                   , end_date: Option[Date]
                   , duration: Option[Int])


trait JiraParseResponse {
  
  def getErrorMessage(str: String): String = {      
    val json: JsValue = Json.parse(str)           
    val arr  = (json \ "errorMessages").asOpt[JsValue]     
    arr match {      
      case Some(JsArray(value)) => ( value map( _.as[String] )) mkString("; ")
      case _ => "Unauthorized"
    }    
  }
  
}
                   
trait JiraAuth extends JiraParseResponse {
  
  ignoreCerts() // вызывается один раз при первом обращении к object Jira
  
  def authenticateHttps (userName: String, password: String, jiraAuthURL: String): Either[String, Cookies] = {
    try{
      val authResult = Http(jiraAuthURL)
                       .postData(s"""{"username":"$userName","password":"$password"}""")
                       .header("Content-Type", "application/json")
                       .header("Charset", "UTF-8")
                       .option(HttpOptions.readTimeout(5000)).asString
                                       
      if(authResult.isSuccess) {
        Right(authResult.cookies)        
      }  
      else 
        Left(
          if(authResult.code == 503) "Service Unavailable"
          else getErrorMessage(authResult.body)
        ) 
      
    } catch {
      case all: Throwable => Left(all.toString())
    }
  }
  
  def ignoreCerts() {
    val trustAllCerts: Array[TrustManager] = Array(
                  new X509TrustManager() {
                    def getAcceptedIssuers(): Array[X509Certificate] = {null}
                    def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
                    def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
                })            
        
    val sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
 
        // Create all-trusting host name verifier
        val allHostsValid = new HostnameVerifier() {
              def verify(hostname: String, session: SSLSession) = {
                true;
            }
        };
 
        // Install the all-trusting host verifier
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)    
  }  
}
                   
trait Jira extends JiraParseResponse {
                 
  def saveIssues(issues: IssuesDB, myCookies: Cookies, jiraSaveURL: String) = {
      val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
      
      for(issueRec <- issues.issues; if issueRec.end_date != None) {
        println(issueRec.end_date)
        var task_id  = issueRec.task_id
        var end_date = sdf.format(issueRec.end_date.get)
               
        var durField = periodToJiraDuration(issueRec.start_date, issueRec.end_date)
                       .map (v => s""", "timetracking": {"remainingEstimate":"$v"} """)
                       .getOrElse("")
                        
        var result = Http(jiraSaveURL + task_id)
                     .put(s"""{"fields": {"duedate" : "$end_date"$durField}}""")
                     .cookies(myCookies)
                     .header("Content-Type", "application/json")
                     .header("Charset", "UTF-8")                   
                     .option(HttpOptions.readTimeout(5000)).asString       
        println(result.body)
      }
                  
  }
   
  def periodToJiraDuration(startDate: Option[Date], endDate:  Option[Date]):  Option[String] = 
    for { 
      start <- startDate
      end   <- endDate
    } yield {        
        val duration = calcDuration(start, end)
        if(duration > 5) {
          val w = duration / 5
          val d = duration % 5
          s"${w}w ${d}d"
        }
        else
          s"${duration}d"        
    }  
  
  
  def getIssues(userName: String, myCookies: Cookies, jiraSearchURL: String)(implicit db: Database): Issues = {
    /*
      {user : "", name : "", issues : [{issue : IS-1234, title : "", status : ""}, {...}, ...], error : ""}
      table issues : {issue, start_date, end_date, user_names, contact, ord}   
    */   
	        
    val issuesResult = Http(jiraSearchURL)
                          .cookies(myCookies)
                          .header("Content-Type", "application/json")
                          .header("Charset", "UTF-8")
                          .params(Seq("jql"        -> s"assignee=$userName"
                                    , "fields"     -> "assignee,summary,status,duedate,timetracking"
                                    , "maxResults" -> "100"))
                          .option(HttpOptions.readTimeout(10000)).asString
                          
    if(issuesResult.isSuccess) {
      val myBody = issuesResult.body
      val myResult = issuesResult.code 
      val json = try {
        Json.parse(issuesResult.body)        
      } catch {
          case ex: Exception => Json.parse("""
                  	{"issues": [{
                  			"key": "ERR",
                  			"fields": {
                  				"summary": "$myBody",
                  				"status": {"name": "result = $myResult"},
                  				"assignee": {"name": "it_andrei",	"displayName": ""}}}]}              
              """)
          
      }
              
      val issues = (json \ "issues").as[JsArray]
      var user   = ""
      var name   = ""    
      val seqJiraIssue =
            for(v <- issues.value) yield {
              user = (v \ "fields" \ "assignee" \ "name").get.as[String]
              name = (v \ "fields" \ "assignee" \ "displayName").get.as[String]
              
              JiraIssue( (v \ "key").get.as[String]
                       , (v \ "fields" \ "summary").get.as[String]
                       , (v \ "fields" \ "status" \ "name").get.as[String]
                       , (v \ "fields" \ "duedate").toOption.flatMap( _.asOpt[Date] )                       
                       , (v \ "fields" \ "timetracking" \ "originalEstimate").toOption
                         .flatMap(v => jiraDurationToDays(v.as[String])) 
              )
            }
        
      val seqIssue = seqJiraIssue
        .filter { x => inReportDB(x.task_id) }
        .map { v =>              
              getIssueDB(v.task_id, name) match {
                case IssueDB(_, start_date, end_date, executors, customer, ord, progress) =>
                  Issue(v.task_id
                      , v.title
                      , v.status 
                      , getStartDateByDur(v.duration, v.end_date) //start_date - из jira или БД
                      , v.end_date                                //end_date - из jira или из БД
                      , executors
                      , customer
                      , ord
                      , progress)  
              }      
            }     
      Issues(user, name, seqIssue, "")
    }
    else
      Issues("", "", Seq(), getErrorMessage(issuesResult.body))      
  }
  
  def getIssueDB(task_id: String, fullName: String)(implicit db: Database): IssueDB
  def inReportDB(task_id: String)(implicit db: Database): Boolean
  
  def jiraDurationToDays(jiraDur: String) = { 
        
    val result = jiraDur.foldLeft((0, "")){
                    (a, c) => c match {
                      case 'd' => (a._1  + Integer.parseInt(a._2), "")
                      case 'w' => (a._1  + 5 * Integer.parseInt(a._2), "")
                      case ' ' => a
                      case _   => (a._1, a._2 + c)
                    }
                 } 
    if(result._1 > 0)
      Some(result._1)
    else
      None
  }
    
  def getStartDateByDur(duration: Option[Int], endDate: Option[Date]): Option[Date] = 
    for {
      dur <- duration 
      end <- endDate 
    } yield {
        var durCnt = dur
        var startDate = end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()     
        while(durCnt > 1) {
          startDate = startDate.minusDays(1)
          if(startDate.getDayOfWeek != DayOfWeek.SATURDAY && startDate.getDayOfWeek != DayOfWeek.SUNDAY)          
            durCnt -= 1                        
      }

      Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

  
  
  //"yyyy-MM-dd'T'HH:mm:ss.ms'Z'"
  implicit val  iso8601DateWrites = new Writes[Date] {
      def writes(d: Date): JsValue = JsString(d.toInstant.toString)
    }
  implicit val issueWrites  = Json.writes[Issue]
  implicit val issuesWrites = Json.writes[Issues]  
  
  def getJsonIssues(userName: String, myCookies: Cookies, jiraSearchURL: String)(implicit db: Database) = {        
    Json.toJson(getIssues(userName, myCookies, jiraSearchURL))
  }
  
  def getReport(filter: String, userName: String, myCookies: Cookies, jiraSearchURL: String
      , jasperReportFile: String)(implicit db: Database): Array[Byte] = {
                              
    def formatDate(date: Option[Date]) = {      
      import java.text.SimpleDateFormat
      val formater = new SimpleDateFormat("dd.MM.yyyy")
      date map(formater.format(_)) getOrElse ""
    }
    
    def toLong(date: Option[Date]) = 
      date map(_.getTime) getOrElse Long.MaxValue        
    
    
    val issues = getIssues(userName, myCookies, jiraSearchURL).issues
    
    val issuesFiltered = (filter match {      
      case "notempty" => issues.filter { f => f.start_date != None && f.end_date != None}
      case _          => issues  // "all"
    })
    .sortWith((x, y) => toLong(x.start_date) < toLong(y.start_date))
         
    val issuesMapArray: Array[Object] = issuesFiltered
        .map { v => 
        val params: java.util.Map[String, Object] = new java.util.HashMap()
        params.put("task_id", v.task_id)  
        params.put("title",   v.title)
        params.put("status",  v.status)
        for(start <- v.start_date)
          params.put("start_date", start)
        for(end <- v.end_date)
          params.put("end_date", end)
        params.put("executors",  v.executors)
        params.put("customer",   v.customer)
        params.put("ord",        v.ord)
        params.put("progress", new java.lang.Integer(v.progress))
        for(start <- v.start_date; end <- v.end_date)
          params.put("duration", new java.lang.Integer(calcDuration(start, end)))        
        
        params
/*        
        Map("task_id" -> v.task_id) +   
           ("title"   -> v.title)   +  
           ("status"  -> v.status)
        
        if(v.start_date != None)
          ("start_date" -> v.start_date.get) 
        if(v.end_date != None)
          ("end_date" -> v.end_date.get)
        ("executors" ->  v.executors)
        ("customer"  ->  v.customer)
        ("ord" ->        v.ord)
        ("progress" -> new java.lang.Integer(v.progress))
        if(v.start_date != None && v.end_date != None)
          ("duration" -> new java.lang.Integer(calcDuration(v.start_date.get, v.end_date.get)))    
  */        
      }
      .toArray      
    
    toByteArray(jasperReportFile, issuesMapArray)            
  }
  
  private def toByteArray(jasperReportLayout: String, data: Array[Object]): Array[Byte] = {        
    val parameters: java.util.Map[String, Object] = new java.util.HashMap()  
    val jasperPrint = JasperFillManager.fillReport(jasperReportLayout
                                                 , parameters
                                                 , new JRMapArrayDataSource(data))                                                             
    val out       = new ByteArrayOutputStream()
    val xlsExport = new JRXlsExporter()
    xlsExport.setExporterInput(new SimpleExporterInput(jasperPrint))
    xlsExport.setExporterOutput(new SimpleOutputStreamExporterOutput(out))
    val configuration = new SimpleXlsReportConfiguration()
    configuration.setOnePagePerSheet(false)
    configuration.setDetectCellType(true)
    configuration.setCollapseRowSpan(true)
    xlsExport.setConfiguration(configuration)
    xlsExport.exportReport()
    out.toByteArray()            
  }
  
  def calcDuration(date1: Date, date2: Date): Int = {
      def toLocalDate(dt: java.util.Date) = 
        (new Date(dt.getTime)).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            
      val start = toLocalDate(date1)
      val end   = toLocalDate(date2)
      var next  = start
      var result = 1
      while(next.isBefore(end)) {
        if(next.getDayOfWeek != DayOfWeek.SATURDAY && next.getDayOfWeek != DayOfWeek.SUNDAY)
          result += 1
        next = next.plusDays(1)
      }
      result            
  }  
           
}