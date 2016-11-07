app.directive('checkPassword', [function () {
    return {
        require: 'ngModel',
        link: function (scope, elem, attrs, ctrl) {
            var firstPassword = '#' + attrs.checkPassword;
            elem.bind('keyup', function () {
                scope.$apply(function () {
                    var firstPass = angular.element(document.querySelector(firstPassword)).val()
                    var v = elem.val() === firstPass;
                    ctrl.$setValidity('passwordmatch', v);
                });
            });
        }
    }
}]);