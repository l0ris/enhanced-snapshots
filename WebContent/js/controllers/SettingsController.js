'use strict';

angular.module('web')
    .controller('SettingsController', function ($scope, System, Users, $modal) {
        var currentUser = Users.getCurrent();
        $scope.isAdmin = currentUser.role === "admin";

        $scope.STRINGS = {
            sdfs: {
                sdfsLocalCacheSize: {
                  empty: 'Local Cache Size field cannot be empty.'
                } ,
                volumeSize: {
                 empty: 'Volume Size field cannot be empty.'
                }
            },
            volumeType: {
               empty: 'Volume size for io1 volume type cannot be empty.',
               range: 'Volume size for io1 volume type must be in between 1 and 30.'
            },
            otherSettings: {
                empty: 'All fields are required. Please fill in empty fields.'
            }
        };

        var progressLoader = function () {
            var modalInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.wizard-progress.html',
                scope: $scope
            });

            return modalInstance
        };

        $scope.progressState = 'loading';
        var loader = progressLoader();
        System.get().then(function (data) {
            $scope.settings = data;
            $scope.initialSettings = angular.copy(data);
            $scope.progressState = '';
            loader.dismiss();
        }, function (e) {
            console.log(e);
            $scope.progressState = 'failed';
            loader.dismiss();
        });

        $scope.backup = function () {
            var modalScope = $scope.$new(true);
            $modal.open({
                animation: true,
                templateUrl: './partials/modal.system-backup.html',
                scope: modalScope,
                controller: 'modalSystemBackupCtrl'
            });
        };

        $scope.uninstall = function () {
            $modal.open({
                animation: true,
                templateUrl: './partials/modal.system-uninstall.html',
                controller: 'modalSystemUninstallCtrl'
            });

        };

        $scope.updateSettings = function () {

            var settingsUpdateModal = $modal.open({
                animation: true,
                scope: $scope,
                templateUrl: './partials/modal.settings-update.html',
                controller: 'modalVolumeTypeChangeCtrl'
            });

            settingsUpdateModal.result.then(function () {
                if ($scope.state == "done") {
                    $scope.initialSettings = angular.copy($scope.settings);
                }
            });
        };

        $scope.isNewValues = function () {
           return JSON.stringify($scope.settings) !== JSON.stringify($scope.initialSettings);
        };


    });