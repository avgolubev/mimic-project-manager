"use strict";!function(){angular.module("app",["xeditable","ui.bootstrap"]).run(["editableOptions",function(editableOptions){editableOptions.theme="bs3"}])}(),function(){angular.module("app").config(["$httpProvider",function($httpProvider){$httpProvider.defaults.headers.common["X-Requested-With"]="XMLHttpRequest"}]).constant("get_issues_URL","issues").constant("save_issues_URL","savechanges").constant("get_report_URL","report").constant("hide_issue_URL","hideissue")}(),function(){function IssuesTableController(){function doEvent($event){$event.preventDefault(),$event.stopPropagation()}var $ctrl=this;$ctrl.sortItem=null,$ctrl.reverse=!1,$ctrl.openedStart={},$ctrl.openedEnd={},$ctrl.sortBy=function(name){$ctrl.reverse=$ctrl.sortItem===name&&!$ctrl.reverse,$ctrl.sortItem=name},$ctrl.openStart=function($event,$index){doEvent($event),$ctrl.openedStart[$index]=!$ctrl.openedStart[$index]},$ctrl.openEnd=function($event,$index){doEvent($event),$ctrl.openedEnd[$index]=!$ctrl.openedEnd[$index]},$ctrl.prepareChanges=function(pTask_id,pField,pData){$ctrl.onChanges({task_id:pTask_id,field:pField,data:pData})},$ctrl.saveChanges=function(){$ctrl.onSave()},$ctrl.cancel=function(){$ctrl.onCancel(),console.log("Cancel changes.")},$ctrl.hideIssue=function(pTask_id){$ctrl.onHide({task_id:pTask_id})},$ctrl.hideIssue2=function(pIndex){$ctrl.items.splice(pIndex,1)}}angular.module("app").component("issuesTable",{templateUrl:"assets/issues-table.html",bindings:{items:"<",onChanges:"&",onCancel:"&",onSave:"&",onHide:"&"},controller:IssuesTableController})}(),function(){function IssuesController(IssuesService,$scope){var $ctrl=this;$ctrl.error="",$ctrl.showAll=!1,$ctrl.getIssues=IssuesService.getIssues,$ctrl.prepareChanges=IssuesService.prepareChanges,$ctrl.hideIssue=IssuesService.hideIssue,$ctrl.cancelChanges=IssuesService.cancelChanges,$ctrl.saveChanges=function(){IssuesService.saveChanges().then(function(response){$ctrl.error=response},function(response){$ctrl.error=response})},$ctrl.refreshIssues=function(){IssuesService.getData().then(function(response){$ctrl.error=response}),console.log("# of Watchers: ",$scope.$$watchersCount),console.log("$ctrl.getIssues:",$ctrl.getIssues())},$ctrl.exportIssues=function(filter){IssuesService.exportIssues(filter).then(function(response){if(response.error)$ctrl.error="Ошибка экспорта файла.";else{var blob=new Blob([response.data],{type:response.contentType}),objectUrl=URL.createObjectURL(blob),a=document.createElement("a");a.href=objectUrl,a.target="_blank",a.download="report.xls",document.body.appendChild(a),a.click()}})},$ctrl.refreshIssues()}angular.module("app").controller("IssuesController",IssuesController),IssuesController.$inject=["IssuesService","$scope"]}(),function(){function IssuesService($http,$window,get_issues_URL,save_issues_URL,get_report_URL,hide_issue_URL){function getNewPeriod(pIssue1,pIssue2,pIntersectionType){var nDt3=null,nDt4=null,period=0;switch(null!=pIssue2.start_date&&null!=pIssue2.end_date&&(period=pIssue2.end_date.getDate()-pIssue2.start_date.getDate()),pIntersectionType){case 1:nDt3=plusDays(pIssue1.end_date,1),null!=pIssue2.end_date&&(nDt4=plusDays(nDt3,period));break;case 2:nDt4=plusDays(pIssue1.start_date,-1),null!=pIssue2.end_date&&(nDt3=plusDays(nDt4,-period));break;case 4:nDt3=plusDays(pIssue1.end_date,1),nDt4=plusDays(nDt3,period)}return[nDt3,nDt4]}function plusDays(pDate,pDays){return new Date(pDate.getFullYear(),pDate.getMonth(),pDate.getDate()+pDays)}function getIndexByTask_id(pTask_id,issues){for(var index,i=0;i<issues.length;i++)if(issues[i].task_id===pTask_id){index=i;break}return index}function getIssueByTask_id(pTask_id,issues){var currentIssue=null;for(var index in issues)if(currentIssue=issues[index],currentIssue.task_id==pTask_id)break;return currentIssue}var srv=this;srv.issues={},srv.issuesClone={},srv.changedTaskIds={},srv.allowCrossing=!0,srv.getIssues=function(){return srv.issues},srv.exportIssues=function(filter){return $http.get(get_report_URL+"/"+filter,{responseType:"blob"}).then(function(response){return{data:response.data,contentType:response.headers("Content-Type")}},function(response){return{error:!0}})},srv.getData=function(){return $http.get(get_issues_URL).then(function(response){return response.data.issues.forEach(function(issue){null!=issue.start_date&&(issue.start_date=new Date(issue.start_date)),null!=issue.end_date&&(issue.end_date=new Date(issue.end_date))}),srv.issues=response.data,srv.issuesClone=angular.copy(srv.issues),{}},function(response){return $window.location.href="/login",response.data.error})},srv.getChangedIssues=function(){var resArray=[];for(var tId in srv.changedTaskIds)srv.issues.issues.forEach(function(issue){issue.task_id===tId&&resArray.push({task_id:issue.task_id,start_date:issue.start_date,end_date:issue.end_date,executors:issue.executors,customer:issue.customer,ord:issue.ord,progress:issue.progress})});return resArray},srv.saveChanges=function(){var resArray=srv.getChangedIssues();return console.log("saveChanges",resArray),$http.put(save_issues_URL,resArray).then(function(response){return srv.changedTaskIds={},console.log("Changes was saved",response.data),response.data},function(response){return console.log("Error has occurred when saving changes",response.data),response.data})},srv.cancelChanges=function(){srv.issues=angular.copy(srv.issuesClone),console.log("Changes were canceled.")},srv.hideIssue=function(task_id){return console.log("hideIssue",task_id),$http.put(hide_issue_URL,{task_id:task_id}).then(function(response){var index=getIndexByTask_id(task_id,srv.issues.issues);void 0===index?console.log("The issue was not found",task_id):(srv.issues.issues.splice(index,1),srv.issuesClone.issues.splice(index,1),console.log("The issue was hidden",task_id))},function(response){console.log("Errors occurred when hiding the issue",task_id)})},srv.addChangedTaskId=function(pTask_id){srv.changedTaskIds[pTask_id]=1,console.log("srv.changedTaskIds",srv.changedTaskIds)},srv.getIntersectionType=function(pTask_id1,pTask_id2){var d1=null,d2=null,d3=null,d4=null,intersectionType=-1,fullCompare=function(pD1,pD2,pD3,pD4){var res=0;return pD1<pD3&&pD2>=pD3&&pD2<pD4&&(res=1),pD3<pD1&&pD4>=pD1&&pD4<pD2&&(res=2),pD1<=pD3&&pD4<=pD2&&(res=3),(pD1>=pD3&&pD4>pD2||pD1>pD3&&pD4>=pD2)&&(res=4),res};return srv.issues.issues.forEach(function(issue){issue.task_id==pTask_id1&&(d1=issue.start_date,d2=issue.end_date),issue.task_id==pTask_id2&&(d3=issue.start_date,d4=issue.end_date)}),null!=pTask_id1&&null!=pTask_id2&&(intersectionType=0,null!=d1&&null!=d2&&null!=d3&&(intersectionType=null==d4?d3<d1||d3>d2?0:1:fullCompare(d1,d2,d3,d4)),null!=d3&&null!=d4&&null!=d1&&(intersectionType=null==d2?d1<d3||d1>d4?0:2:fullCompare(d1,d2,d3,d4))),intersectionType},srv.prepareChanges=function(pTask_id,pField,pData){var currentIssue=getIssueByTask_id(pTask_id,srv.issues.issues);switch(pField){case"start_date":if(null!=currentIssue.end_date&&pData>currentIssue.end_date){var period=0;null!=currentIssue.start_date&&(period=currentIssue.end_date.getDate()-currentIssue.start_date.getDate()),currentIssue.end_date=plusDays(pData,period)}currentIssue[pField]=new Date(pData.getTime());break;case"end_date":if(null!=currentIssue.start_date&&pData<currentIssue.start_date){var period=0;null!=currentIssue.end_date&&(period=currentIssue.end_date.getDate()-currentIssue.start_date.getDate()),currentIssue.start_date=plusDays(pData,-period),srv.addChangedTaskId(pTask_id)}currentIssue[pField]=new Date(pData.getTime());break;default:currentIssue[pField]=pData}srv.addChangedTaskId(pTask_id),srv.allowCrossing||srv.issues.issues.forEach(function(nextIssue){if(nextIssue.task_id!=pTask_id&&("start_date"===pField||"end_date"===pField)){var tp=srv.getIntersectionType(currentIssue.task_id,nextIssue.task_id);if(console.log("IntersectionType:",tp),tp>0){var newPeriod=getNewPeriod(currentIssue,nextIssue,tp);nextIssue.start_date=newPeriod[0],nextIssue.end_date=newPeriod[1],srv.addChangedTaskId(nextIssue.task_id)}}}),console.log("srv.changedTaskIds",srv.changedTaskIds),console.log("srv.issues",srv.issues)}}angular.module("app").service("IssuesService",IssuesService),IssuesService.$inject=["$http","$window","get_issues_URL","save_issues_URL","get_report_URL","hide_issue_URL"]}();
//# sourceMappingURL=app.js.map
