'use strict';

angular.module('web')
    .controller('modalSettingsUpdateCtrl', ['$scope', '$modalInstance', 'System', 'Tasks', '$rootScope', function ($scope, $modalInstance, System, Tasks, $rootScope) {
        $scope.state = 'ask';

        $scope.settings.ec2Instance.instanceIDs =  $scope.settings.ec2Instance.instanceIDs.split(", ");
        var newSettings = angular.copy($scope.settings);
        if (!newSettings.mailConfiguration.fromMailAddress) newSettings.mailConfiguration = null;
        //deletion of Arrays from model per request of backend
        delete newSettings.systemProperties.volumeTypeOptions;

        $scope.updateSettings = function () {
            $rootScope.isLoading = true;
            System.send(newSettings).then(function () {
                $scope.state = "done";
                $rootScope.isLoading = false;
            }, function (e) {
                $scope.state = "failed";
                $rootScope.isLoading = false;
            });
        };
    }]);