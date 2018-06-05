(function () {
'use strict';

angular.module("app", ["xeditable", "ui.bootstrap"])

.run(function(editableOptions) {
  editableOptions.theme = 'bs3';
});

})();
