'use strict';

angular.module('web')
    .controller('LoginController', function ($scope, $state, $stateParams, $stomp, Auth, System, Storage, toastr, $window) {
        if ($stateParams.err && $stateParams.err == 'session') {
            toastr.warning('You were logged out. Please re-login', 'Session expired.');
        }
    
        if (angular.isDefined(Storage.get("currentUser"))) {
            
            if (Storage.get("ssoMode") && Storage.get("ssoMode").ssoMode) {
                $window.location.href = "/saml/logout"
            } else {
                Auth.logOut();    
            }
            
        }

        if (Storage.get("currentUser") && Storage.get("currentUser").length > 1) {
            if (Storage.get("ssoMode") && Storage.get("ssoMode").ssoMode) {
                $window.location.href = "/saml/logout"
            } else {
                Auth.logOut();    
            }
        }

        $scope.clearErr = function () {
            $scope.error = "";
        };

        $scope.login = function () {
            Auth.logIn($scope.email, $scope.password).then(function (data) {

                if (data.role === 'configurator') {
                    $state.go('config');
                } else {
                    System.get().then(function (data) {
                        if (data.currentVersion != data.latestVersion) {
                            Storage.save("notification", "Newer version is available! Please, create a new instance from the latest AMI.");
                        }
                        $scope.subscribeWS();
                    }).finally(function () {
                        $state.go('app.volume.list');
                    });
                }
            }, function (res) {
                $scope.error = res;
                $scope.password = "";
            });
        }
    });