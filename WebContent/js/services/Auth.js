'use strict';

angular.module('web')
    .service('Auth', ['Storage', '$q', '$http', 'BASE_URL', function (Storage, $q, $http, BASE_URL) {
        var sessionUrl = BASE_URL + "login";
        var logoutUrl = BASE_URL + "logout";
        var statuses = {
            404: "Service is unavailable",
            401: "Your authentication information was incorrect. Please try again"
        };


        var _login = function (email, pass) {
            var deferred = $q.defer();

            $http({
                method: 'POST',
                url: sessionUrl,
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                transformRequest: function(obj) {
                    var str = [];
                    for(var p in obj)
                        str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
                    return str.join("&");
                },
                data: {email: email, password: pass }
            }).then(function (response) {
                    Storage.save("currentUser", response.data);
                    deferred.resolve(response.data);
                }, function (err, status) {
                    deferred.reject(statuses[status]);
                });

            return deferred.promise;
        };
        
        var _logout= function () {
            Storage.remove("currentUser");
            return $http.get(logoutUrl)
            };

        return {
            logIn: function (email, pass) {
                return _login(email, pass);
            },

            logOut: function () {
                return _logout();
            }
        };
    }]);