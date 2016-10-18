'use strict';

angular.module('web')
    .service('Tasks', ['$q', '$http', 'Storage', 'BASE_URL', function ($q, $http, Storage, BASE_URL) {
        var url = BASE_URL + 'rest/task';

        var getAll = function (volumeId) {
            var deferred = $q.defer();
            $http.get(url + (volumeId ? "/" + volumeId : "")).then(function (result) {
                deferred.resolve(result.data);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var _getRegular = function (vol) {
            var deferred = $q.defer();
            $http.get(url + '/regular/' + vol).then(function (result) {
                deferred.resolve(result.data);
            }, function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var save = function (item) {
            return $http({
                url: url,
                method: 'PUT',
                data: item
            })
        };

        var remove = function (id) {
            return $http({
                url: url + "/" + id,
                method: "DELETE"
            })
        };

        var add = function (item) {
            return $http({
                url: url,
                method: "POST",
                data: item
            });
        };

        return {
            get: function (volumeId) {
                return getAll(volumeId);
            },
            getRegular: function (vol) {
                return _getRegular(vol);
            },
            update: function (item) {
                return save(item);
            },
            insert: function (item) {
                return add(item);
            },
            delete: function (id) {
                return remove(id);
            }
        }
    }]);