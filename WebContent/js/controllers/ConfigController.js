'use strict';

angular.module('web')
    .controller('ConfigController', function ($scope, Volumes, Configuration, $modal, $state, Storage) {
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
		
		if (angular.isUndefined($scope.isSSO)) { $scope.isSSO = false; } 
        
		var wizardCreationProgress = function () {
            var modalInstance = $modal.open({
                animation: true,
                backdrop: false,
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
                if (!$scope.settings.mailConfiguration) {
                    $scope.emails = [];
                    $scope.settings.mailConfiguration = {
                        events: {
                            "error": false,
                            "info": false,
                            "success": false
                        }
                    }
                } else {
                    $scope.emails = $scope.settings.mailConfiguration.recipients || [];
                }

                loader.dismiss();
            }, function (data, status) {
                $scope.isValidInstance = false;
                $scope.invalidMessage = data.data.localizedMessage;
                loader.dismiss();
            });
        };

        getCurrentConfig();

        $scope.emailNotifications = function () {
            $scope.connectionStatus = null;
            var emailNotificationsModalInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.email-notifications.html',
                scope: $scope,
                backdrop: false
            });

            emailNotificationsModalInstance.result.then(function () {
                $scope.settings.mailConfiguration.recipients = $scope.emails;
            })
        };

        $scope.testConnection = function () {
            $scope.settings.mailConfiguration.recipients = $scope.emails;
            var testData = {
                testEmail: $scope.testEmail,
                domain: $scope.settings.domain,
                mailConfiguration: $scope.settings.mailConfiguration
            };

            Configuration.check(testData).then(function (response) {
                $scope.connectionStatus = response.status;
            }, function (error) {
                $scope.connectionStatus = error.status;
            });
        };

        $scope.sendSettings = function () {
            var volumeSize = $scope.isNewVolumeSize ? $scope.sdfsNewSize : $scope.settings.sdfs.volumeSize;

            var getMailConfig = function () {
                if (!$scope.mailConfiguration.fromMailAddress) {
                    return null;
                } else {
                    return $scope.mailConfiguration
                }
            };

            var settings = {
                bucketName: $scope.selectedBucket.bucketName,
                volumeSize: volumeSize,
                ssoMode: $scope.isSSO,
                spEntityId: $scope.entityId,
                mailConfiguration: getMailConfig()
            };

            if (!$scope.settings.db.hasAdmin && !$scope.isSSO) {
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

                    settings.domain = $scope.settings.domain;
                    $scope.progressState = 'running';
                    Configuration.send('current', settings, DELAYTIME).then(function () {
                        $scope.progressState = 'success';
                    }, function () {
                        $scope.progressState = 'failed';
                    });

                    wizardCreationProgress();

                });
            } else {

                if (settings.ssoMode) {
                    settings.user = {email: $scope.adminEmail}
                }

                settings.domain = $scope.settings.domain;
                $scope.progressState = 'running';

                Configuration.send('current', settings, null, $scope.settings.sso).then(function () {
                    $scope.progressState = 'success';
                    Storage.save("ssoMode", {ssoMode: $scope.isSSO});
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