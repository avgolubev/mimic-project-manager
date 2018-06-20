(function () {
"use strict";

angular.module('app')
.service('IssuesService', IssuesService);

IssuesService.$inject = ['$http', '$window', 'get_issues_URL', 'save_issues_URL', 'get_report_URL', 'hide_issue_URL'];
function IssuesService($http, $window, get_issues_URL, save_issues_URL, get_report_URL, hide_issue_URL) {
  var srv = this;
  srv.issues = {};
  srv.issuesClone = {};
  srv.changedTaskIds = {};
  srv.allowCrossing = true;
  srv.getIssues = () => {
	    return srv.issues;
	  };

  srv.exportIssues = (filter) => {
    return $http.get(get_report_URL + "/" + filter, { responseType: "blob"})
    .then(
  		(response) => {
        return {
          data: response.data,
          contentType: response.headers("Content-Type")
        };
  		},
  		(response) => { return {error: true}; }
    );
  };

  srv.getData = () => {
    return $http.get(get_issues_URL)
    .then(
  		(response) => {
	        response.data.issues.forEach( issue => {
	          if(issue.start_date != null)
	            issue.start_date = new Date(issue.start_date);
	          if(issue.end_date != null)
	            issue.end_date = new Date(issue.end_date);
	        });

	  		srv.issues = response.data;
	        srv.issuesClone = angular.copy(srv.issues);
	  		return {};
  		},
  		(response) => {
  			$window.location.href = '/login';
  			return response.data.error;
  		}
    );
  };

  srv.getChangedIssues = () => {
	  var resArray = [];
	  for(var tId in srv.changedTaskIds){

		  srv.issues.issues.forEach( issue => {
			  if(issue.task_id === tId)
          resArray.push(
                    { task_id: 	  issue.task_id
    						  	, start_date: issue.start_date
    						  	, end_date:   issue.end_date
    						  	, executors:  issue.executors
    						  	, customer:   issue.customer
    						  	, ord: 		    issue.ord
    						  	, progress:   issue.progress}
                  );
			  });

		  }
	  return resArray;
  };

  srv.saveChanges = () => {
    const resArray = srv.getChangedIssues();
    console.log("saveChanges", resArray);
    return $http.put(save_issues_URL, resArray)
      .then(
    		(response) => {
          srv.changedTaskIds = {};
          console.log("Changes was saved", response.data);
    		  return response.data;
    		},
    		(response) => {
          console.log("Error has occurred when saving changes", response.data);
    			return response.data;
    		}
      );
  };

  srv.cancelChanges = () => {
    srv.issues = angular.copy(srv.issuesClone);
    console.log("Changes were canceled.");
  };

  srv.hideIssue = (task_id) => {
    console.log("hideIssue", task_id);

    return $http.put(hide_issue_URL, {task_id})
      .then(
    		(response) => {
          var index = getIndexByTask_id(task_id, srv.issues.issues);
          if(index === undefined) {
            console.log("The issue was not found", task_id);
          }
          else {
            srv.issues.issues.splice(index, 1);
            srv.issuesClone.issues.splice(index, 1);
            console.log("The issue was hidden", task_id);
          }
    		},
    		(response) => { console.log("Errors occurred when hiding the issue", task_id); }
      );
  };

  srv.addChangedTaskId = (pTask_id) => {
	  srv.changedTaskIds[pTask_id] = 1;
    console.log("srv.changedTaskIds", srv.changedTaskIds);
  };

  srv.getIntersectionType = (pTask_id1, pTask_id2) => {
	  var d1 = null;
	  var d2 = null;
	  var d3 = null;
	  var d4 = null;
	  var intersectionType = -1;

	  let fullCompare = (pD1, pD2, pD3, pD4) => {
		  var res = 0;
		  if(pD1 < pD3 && pD2 >= pD3 && pD2 < pD4)
			  res = 1;
		  if(pD3 < pD1 && pD4 >= pD1 && pD4 < pD2)
			  res = 2;
		  if(pD1 <= pD3 && pD4 <= pD2)
			  res = 3;
		  if((pD1 >= pD3 && pD4 > pD2) || (pD1 > pD3 && pD4 >= pD2))
			  res = 4;
		  return res;
	  }

	  srv.issues.issues.forEach(issue => {
		  if(issue.task_id == pTask_id1) {
			  d1 = issue.start_date;
			  d2 = issue.end_date;
		  }
		  if(issue.task_id == pTask_id2) {
			  d3 = issue.start_date;
			  d4 = issue.end_date;
		  }
	  });

	  if(pTask_id1 != null && pTask_id2 != null) {
		  intersectionType = 0;
		  if(d1 != null && d2 != null && d3 != null) {
			  if(d4 == null) {
				  if(d3 < d1 || d3 > d2)
					  intersectionType = 0;
				  else
					  intersectionType = 1;
			  }
			  else {
				  intersectionType = fullCompare (d1, d2, d3, d4);
			  }
		  }

		  if(d3 != null && d4 != null && d1 != null) {

			  if(d2 == null) {
				  if(d1 < d3 || d1 > d4)
					  intersectionType = 0;
				  else
					  intersectionType = 2;
			  }
			  else {
				  intersectionType = fullCompare (d1, d2, d3, d4);
			  }
		  }

	  }

	  return intersectionType;
  }

  srv.prepareChanges = (pTask_id, pField, pData) => {

	  const currentIssue = getIssueByTask_id(pTask_id, srv.issues.issues);

	  switch (pField) {
		  case "start_date":
			  if(currentIssue.end_date != null && pData > currentIssue.end_date) {
				  var period = 0;
				  if(currentIssue.start_date != null)
					  period = currentIssue.end_date.getDate() - currentIssue.start_date.getDate();
				  currentIssue.end_date = plusDays(pData, period);
			  }
			  currentIssue[pField] = new Date(pData.getTime());
			  break;

		  case "end_date":
			  if(currentIssue.start_date != null && pData < currentIssue.start_date) {
				  var period = 0;
				  if(currentIssue.end_date != null)
					  period =  currentIssue.end_date.getDate() - currentIssue.start_date.getDate();
				  currentIssue.start_date = plusDays(pData, -period);
				  srv.addChangedTaskId(pTask_id);
			  }
			  currentIssue[pField] = new Date(pData.getTime());
        break;

      default:
        currentIssue[pField] = pData;
    }

    srv.addChangedTaskId(pTask_id);

    // сдвиг периодов задач ,если есть пересечение
    if(! srv.allowCrossing)
  	  srv.issues.issues.forEach( nextIssue => {
  		  if(nextIssue.task_id != pTask_id
            && (pField === "start_date" || pField === "end_date")) {
      		const tp = srv.getIntersectionType(currentIssue.task_id, nextIssue.task_id);
          console.log("IntersectionType:", tp);

      		if(tp > 0) {
      			const newPeriod = getNewPeriod(currentIssue, nextIssue, tp);
      			nextIssue.start_date = newPeriod[0];
      			nextIssue.end_date   = newPeriod[1];
            srv.addChangedTaskId(nextIssue.task_id);
      		}
  			}
  	  });

	  console.log("srv.changedTaskIds", srv.changedTaskIds);
	  console.log("srv.issues", srv.issues);
  };

  function getNewPeriod(pIssue1, pIssue2, pIntersectionType) {
	  var nDt3 = null;
	  var nDt4 = null;
	  var period = 0;
	  if(pIssue2.start_date != null && pIssue2.end_date != null)
		  period = pIssue2.end_date.getDate() - pIssue2.start_date.getDate();

	  switch (pIntersectionType) {
	  	case 1:
	  		nDt3 = plusDays(pIssue1.end_date, 1);
  			if(pIssue2.end_date != null)
  				nDt4 = plusDays(nDt3, period);
	  		break;

	  	case 2:
	  		nDt4 = plusDays(pIssue1.start_date, -1);
  			if(pIssue2.end_date != null)
  				nDt3 = plusDays(nDt4, -period);
	  		break;

	  	case 3, 4:
	  		nDt3 = plusDays(pIssue1.end_date, 1);
	  		nDt4 = plusDays(nDt3, period);
	  }
	  return [nDt3, nDt4];
  }

  function plusDays (pDate, pDays) {
	  return new Date(pDate.getFullYear(), pDate.getMonth(), pDate.getDate() + pDays);
  }

  function getIndexByTask_id(pTask_id, issues) {
	  var index;
	  for(let i = 0; i < issues.length; i++)
		  if(issues[i].task_id === pTask_id) {
        index = i;
			  break;
		  }
	  return index;
  }

  function getIssueByTask_id(pTask_id, issues) {
	  var currentIssue = null;
	  for(let index in issues){
		  currentIssue = issues[index];
		  if(currentIssue.task_id == pTask_id) {
			  break;
		  }
	  }
	  return currentIssue;
  }

}

})();
