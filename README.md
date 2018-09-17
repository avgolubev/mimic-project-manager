mimic-project-manager
====================

A sample application demonstrating Play Framework using Jira rest api with Apache Derby as DB, 
Anorm for DB data access.
Scalaj-http is for managing rest api.
Authentication and authorization implemented on play2-auth. 

Used technologies:

* Play Framework 2.5.3
* Scala 2.11.8
* Anorm 2.5.2
* Play2-auth 0.14.2
* Scalaj-http 2.3.0


For correct working Apache Derby add 
permission org.apache.derby.security.SystemPermission "engine", "usederbyinternals";
in jdk/jre/lib/security/java.policy file.

To build front-end:
```bat
cd front-end
gulp
```
