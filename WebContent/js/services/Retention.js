'use strict';

angular.module('web')
    .service('Retention', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/retention';

        var _get = function (id) {
            var deferred = $q.defer();
            $http({
                url: url + "/" + id,
                method: 'GET'
            }).success(function (data) {
                deferred.resolve(data);
            });
            return deferred.promise;
        };


        var _update = function (item) {
            return $http({
                url: url,
                method: "POST",
                data: item
            });
        };

        return {
            get: function (id) {
                return _get(id);
            },
            update: function (item) {
                return _update(item);
            }

        }
    }]);