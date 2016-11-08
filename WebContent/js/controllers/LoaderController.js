'use strict';

angular.module('web')
    .controller('LoaderController', ['Users', '$state', '$q', 'System', 'Storage', 'Auth',
        function (Users, $state, $q, System, Storage, Auth) {

            var promises = [System.get(), Users.refreshCurrent()];
            $q.all(promises).then(function (results) {
                if (results[0].ssoMode != undefined) {
                    //response for System.get
                    Storage.save("ssoMode", {"ssoMode": results[0].ssoMode});
                }
                //response for Users.refreshCurrent
                if (typeof(results[1].data) != 'string' && results[1].status === 200) {
                    $state.go('app.volume.list');
                } else {
                    Auth.logOut();
                    $state.go('login');
                }
            }, function (err) {
            });
    }]);