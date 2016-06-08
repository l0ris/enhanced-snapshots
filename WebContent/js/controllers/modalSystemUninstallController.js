'use strict';

angular.module('web')
    .controller('modalSystemUninstallCtrl', function ($scope, $modalInstance, System) {
        $scope.state = 'ask';

        $scope.delete = function () {
            var deletionData = {
                instanceId: $scope.instanceId,
                removeS3Bucket: $scope.removeS3Bucket
            };

            System.delete(deletionData).then(function () {
                $scope.state = "done";
            }, function(e){
                $scope.delError = e;
                $scope.state = "failed";
            });
        }
    });