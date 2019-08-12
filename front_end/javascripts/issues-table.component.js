
(function () {
"use strict";

angular.module('app')
.component('issuesTable', {
  templateUrl: 'assets/issues-table.html',
  bindings: {
    data: '<'
   ,onChanges: '&'
   ,onCancel:  '&'
   ,onSave:    '&'
   ,onHide:    '&'
  },
  controller: IssuesTableController
});

function IssuesTableController() {
  const $ctrl = this;
  $ctrl.sortItem = null;
  $ctrl.reverse = false;
  $ctrl.openedStart = {};
  $ctrl.openedEnd   = {};

  $ctrl.sortBy = function(name) {
    $ctrl.reverse  = ($ctrl.sortItem === name) ? !$ctrl.reverse : false;
    $ctrl.sortItem = name;
  };

  $ctrl.openStart = ($event, $index) => {
	  doEvent($event);
	  $ctrl.openedStart[$index] = !$ctrl.openedStart[$index];
  };

  $ctrl.openEnd = ($event, $index) => {
    doEvent($event);
    $ctrl.openedEnd[$index] = !$ctrl.openedEnd[$index];
  };

  function doEvent($event) {
    $event.preventDefault();
	  $event.stopPropagation();
  }

  $ctrl.prepareChanges = (pTask_id, pField, pData) => {
	  $ctrl.onChanges({task_id: pTask_id, field: pField, data: pData});
  };

  $ctrl.saveChanges = () => {
	  $ctrl.onSave();
  };

  $ctrl.cancel = () => {
    $ctrl.onCancel();
    console.log("Cancel changes.");
  };

  $ctrl.hideIssue = (pTask_id) => {
    $ctrl.onHide({task_id: pTask_id});
  };

  $ctrl.hideIssue2 = (pIndex) => {
    $ctrl.items.splice(pIndex, 1);
  };

}

})();
