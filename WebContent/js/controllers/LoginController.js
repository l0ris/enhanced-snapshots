'use strict';

angular.module('web')
    .controller('LoginController', ['$scope', '$state', '$stateParams', '$stomp', 'Auth', 'System', 'Storage', 'toastr', '$window',
        function ($scope, $state, $stateParams, $stomp, Auth, System, Storage, toastr, $window) {

        //LOGGING OUT ---------------------
        if ($stateParams.err && $stateParams.err == 'session') {
            toastr.warning('You were logged out. Please re-login', 'Session expired.');
        }

        var currentUser = Storage.get("currentUser");
        var ssoMode = Storage.get("ssoMode");

        if (currentUser !== null && currentUser !== undefined) {
            if (ssoMode && ssoMode.ssoMode) {
                $window.location.href = "/saml/logout";
            }
            Auth.logOut();
        }

        if (currentUser && currentUser.length > 1) {
            if (ssoMode && ssoMode.ssoMode) {
                $window.location.href = "/saml/logout";
            }
            Auth.logOut();
        }
        //------------------------------------

        // Show loader instead of login page if ssoMode is true ----------
            $scope.isLoading = !!(ssoMode && ssoMode.ssoMode);
        //---------------------------------------------

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
    }]);