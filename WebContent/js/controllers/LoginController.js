'use strict';

angular.module('web')
    .controller('LoginController', ['$rootScope', '$scope', '$state', '$stateParams', '$stomp', 'Auth', 'System', 'Storage', 'toastr', '$window', 'refreshUserResult',
        function ($rootScope, $scope, $state, $stateParams, $stomp, Auth, System, Storage, toastr, $window, refreshUserResult) {

        $rootScope.isLoading = true;

        //LOGGING OUT ---------------------
        if ($stateParams.err && $stateParams.err == 'session') {
            toastr.warning('You were logged out. Please re-login', 'Session expired.');
        }

        var currentUser = Storage.get("currentUser");
        var ssoMode = Storage.get("ssoMode");

        // if (currentUser !== null && currentUser !== undefined) {
        //     if (ssoMode && ssoMode.ssoMode) {
        //         $window.location.href = "/saml/logout";
        //     }
        //     Auth.logOut();
        // }

        if (currentUser && currentUser.length > 1) {
            if (ssoMode && ssoMode.ssoMode) {
                $window.location.href = "/saml/logout";
            }
            Auth.logOut();
        }
        //------------------------------------

        // Show loader instead of login page if ssoMode is true ----------
            if (refreshUserResult === true) {
                $rootScope.isLoading = true;
                window.location = "/saml/login";
            } else {
                if (refreshUserResult === 200 && currentUser && ssoMode && ssoMode.ssoMode) {
                    $rootScope.isLoading = true;
                    $state.go('loader');
                } else if (refreshUserResult === 200 && currentUser && ssoMode && !ssoMode.ssoMode) {
                    $rootScope.isLoading = false;
                } else {
                    $rootScope.isLoading = !!(ssoMode && ssoMode.ssoMode);
                }
            }

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
        };


    }]);