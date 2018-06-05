(function () {
"use strict";

angular.module('app')
.controller('IssuesController', IssuesController);

IssuesController.$inject = ['IssuesService', '$scope'];
function IssuesController(IssuesService, $scope) {
  const $ctrl = this;
  $ctrl.error = "";
  $ctrl.showAll = false;

  $ctrl.getIssues = IssuesService.getIssues;

  $ctrl.prepareChanges = IssuesService.prepareChanges;
  $ctrl.hideIssue      = IssuesService.hideIssue;
  $ctrl.cancelChanges  = IssuesService.cancelChanges;

  $ctrl.saveChanges = () => {
    const promise = IssuesService.saveChanges();
    promise
      .then(function(response){
        $ctrl.error = response;
      }, function(response) {
        $ctrl.error = response;
      });
  };

  $ctrl.refreshIssues = () => {
    const promise = IssuesService.getData();
    promise
      .then(function(response){
        $ctrl.error = response;
      });

   console.log("# of Watchers: ", $scope.$$watchersCount);
   console.log("$ctrl.getIssues:", $ctrl.getIssues());
 };

 $ctrl.exportIssues = (filter) => {
   const promise = IssuesService.exportIssues(filter);
   promise
     .then((response) => {
       if(response.error) $ctrl.error = "Ошибка экспорта файла.";
       else {
         const blob = new Blob([response.data], {type: response.contentType});
         const objectUrl = URL.createObjectURL(blob);
         const a  = document.createElement('a');
         a.href     = objectUrl;
         a.target   = '_blank';
         a.download = 'report.xls';
         document.body.appendChild(a);
         a.click();
       }
     });
 };

 // начальная загрузка данных
 $ctrl.refreshIssues();

}

})();
