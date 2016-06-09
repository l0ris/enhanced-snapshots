'use strict';

angular.module('web')
    .controller('modalSystemUninstallCtrl', function ($scope, $modalInstance, System) {
        $scope.state = 'ask';

        $scope.deletionOptions = [{
            name: "Delete S3 bucket",
            value: true
        }, {
            name: "Keep S3 bucket",
            value: false
        }];

        $scope.delete = function () {
            var deletionData = {
                instanceId: $scope.instanceId,
                removeS3Bucket: $scope.removeS3Bucket.value
            };

            System.delete(deletionData).then(function () {
                $scope.state = "done";
            }, function(e){
                $scope.delError = e;
                $scope.state = "failed";
            });
        }
    });