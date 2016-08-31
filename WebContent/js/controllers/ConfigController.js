'use strict';

angular.module('web')
    .controller('ConfigController', function ($scope, Volumes, Configuration, $modal, $state) {
        var DELAYTIME = 600*1000;
        $scope.STRINGS = {
            s3: {
                empty: 'Bucket name field cannot be empty',
                new: 'New bucket will be created as',
                existing: 'Existing bucket will be used'
            },
            db: {
                isValid: {
                    true: 'Database exists',
                    false: 'No database found'
                },
                hasAdminUser: {
                    false: 'You will need to create a new user on the next step'
                }
            },
            sdfs: {
                name: {
                    new: 'New volume will be created as',
                    existing: 'Existing volume will be used'
                },
                point: 'At mounting point:',
                size: 'Would you like to update volume size?'
            }
        };

        $scope.iconClass = {
            true: 'ok',
            false: 'cog'
        };

        $scope.statusColorClass = {
            true: 'success',
            false: 'danger'
        };

        $scope.isCustomBucketName = false;
        $scope.isNameWrong = false;
        $scope.wrongNameMessage = '';
        $scope.isValidInstance = true;
        $scope.selectBucket = function (bucket) {
            $scope.selectedBucket = bucket;
            Configuration.get('bucket/' + encodeURIComponent(bucket.bucketName) + '/metadata').then(function (result) {
                //property settings.db.hasAdmin is a legacy code which should be changed. Currently this field is replaced
                // with value from result.data.hasAdmin of this function. Speak to Kostya for more details
                $scope.settings.db.hasAdmin = result.data.hasAdmin;
            }, function (err) {
                console.warn(err);
            });
        };

        var wizardCreationProgress = function () {
            var modalInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.wizard-progress.html',
                scope: $scope
            });

            modalInstance.result.then(function () {
                $state.go('login')
            }, function () {
            });

            return modalInstance
        };

        var getCurrentConfig = function () {
            $scope.progressState = 'loading';
            var loader = wizardCreationProgress();

            Configuration.get('current').then(function (result, status) {
                $scope.settings = result.data;
                $scope.selectedBucket = (result.data.s3 || [])[0] || {};
                loader.dismiss();
            }, function (data, status) {
                $scope.isValidInstance = false;
                $scope.invalidMessage = data.data.localizedMessage;
                loader.dismiss();
            });
        };

        getCurrentConfig();

        $scope.sendSettings = function () {
            var volumeSize = $scope.isNewVolumeSize ? $scope.sdfsNewSize : $scope.settings.sdfs.volumeSize;

            var settings = {
                bucketName: $scope.selectedBucket.bucketName,
                volumeSize: volumeSize
            };

            if (!$scope.settings.db.hasAdmin) {
                $scope.userToEdit = {
                    isNew: true,
                    admin: true
                };

                var userModalInstance = $modal.open({
                    animation: true,
                    templateUrl: './partials/modal.user-edit.html',
                    scope: $scope
                });

                userModalInstance.result.then(function () {
                    settings.user = $scope.userToEdit;
                    delete settings.user.isNew;

                    $scope.progressState = 'running';
                    Configuration.send('current', settings, DELAYTIME, $scope.settings.sso).then(function () {
                        $scope.progressState = 'success';
                    }, function () {
                        $scope.progressState = 'failed';
                    });

                    wizardCreationProgress();

                });
            } else {
                $scope.progressState = 'running';

                    Configuration.send('current', settings, undefined, $scope.settings.sso).then(function () {
                        $scope.progressState = 'success';
                    }, function (data, status) {
                        $scope.progressState = 'failed';
                    });

                    wizardCreationProgress();
            }
        };

        $scope.validateName = function () {
            Configuration.get('bucket/' + encodeURIComponent($scope.selectedBucket.bucketName)).then(function (result) {
                $scope.isNameWrong = !result.data.valid;
                $scope.wrongNameMessage = result.data.message;
            }, function (data, status) {
            });
        };
    });