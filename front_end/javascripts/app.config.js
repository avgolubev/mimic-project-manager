(function () {
"use strict";

angular.module('app')

.config(function($httpProvider) {
  $httpProvider.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';
})

.constant('get_issues_URL', 'issues')
.constant('save_issues_URL', 'savechanges')
.constant('get_report_URL', 'report')
.constant('hide_issue_URL', 'hideissue');

})();
