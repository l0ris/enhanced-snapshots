'use strict';

angular.module('web')
    .controller('LogoutController', ['$state', 'Auth', 'Storage', '$window',
        function ($state, Auth, Storage, $window) {

        var currentUser = Storage.get("currentUser");
        var ssoMode = Storage.get("ssoMode");

        if (ssoMode && ssoMode.ssoMode) {

            $window.location.href = "/saml/logout";
        } else {
            Auth.logOut();
            $state.go('login');
        }

    }]);