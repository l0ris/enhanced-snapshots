'use strict';

angular.module('web')
    .controller('RegistrationController', ['$scope', '$state', 'Users', '$modal', function ($scope, $state, Users, $modal) {
        $scope.passwordError = "";
        $scope.userExists = "";
        var userData = {};

        $scope.registerUser = function () {
            if ($scope.passwordReg === $scope.passwordConf) {
                // check if user already exists
                Users.getAll().then(function (data) {
                    var unique = true;

                    if (data) {
                        for (var i = 0; i < data.length; i++) {
                            if (data[i].email === userData.email) {
                                unique = false;
                                $scope.userExists = "User with such E-mail already exists";
                                break;
                            }
                        }
                    }

                    if (unique) {
                        userData = {
                            firstName: $scope.firstName,
                            lastName: $scope.lastName,
                            email: $scope.userEmail,
                            password: $scope.passwordReg
                        };

                        Users.insert(userData).then(function () {
                            var modalInstance = $modal.open({
                                animation: true,
                                templateUrl: './partials/modal.user-added.html'
                            });

                            modalInstance.result.then(function () {
                                $state.go('login');
                            });
                        });

                    }
                });
            }
            else {
                $scope.passwordError = "Password does not match"
            }
        };
    }]);




