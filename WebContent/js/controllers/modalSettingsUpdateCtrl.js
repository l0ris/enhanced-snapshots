'use strict';

angular.module('web')
    .controller('modalSettingsUpdateCtrl', function ($scope, $modalInstance, System, Tasks, $rootScope) {
        $scope.state = 'ask';

        var newSettings = angular.copy($scope.settings);
        //deletion of Arrays from model per request of backend
        delete newSettings.systemProperties.volumeTypeOptions;

        var sendUpdateRequest = function (newSettings) {
            $rootScope.isLoading = true;
            System.send(newSettings).then(function () {
                $scope.state = "done";
                $rootScope.isLoading = false;
            }, function (e) {
                $scope.state = "failed";
                $rootScope.isLoading = false;
            });
        };

        $scope.updateSettings = function () {
            var isNoRunning = true;
            if (newSettings.sdfs.sdfsLocalCacheSize != $scope.initialSettings.sdfs.sdfsLocalCacheSize) {
                Tasks.get().then(function (data) {
                    isNoRunning = data.every(function (task) {
                        return task.status !== "running"
                    });

                    if(isNoRunning) {
                        sendUpdateRequest(newSettings);
                    } else {
                        $scope.state = "busy"
                    }
                }, function (e) {
                    $scope.state = "failed";
                });
            } else {
                sendUpdateRequest(newSettings);
            }

        }
    });