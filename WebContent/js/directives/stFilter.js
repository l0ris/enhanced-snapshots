'use strict';

angular.module('web')
    .directive('stFilter', function () {
        return {
            require: '^stTable',
            scope: {
                stFilter: '='
            },
            link: function (scope, ele, attr, ctrl) {
                var table = ctrl;

                scope.$watch('stFilter', function (val) {
                    ctrl.search(val, 'availabilityZone');
                });

            }
        };
    });