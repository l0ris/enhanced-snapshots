'use strict';

angular.module('web')
    .service('Backups', ['$q', '$http', 'BASE_URL', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/backup';

        var _getForVolume = function (volume) {
            var deferred = $q.defer();
            $http.get(url + '/' + volume).success(function (data) {
                deferred.resolve(data);
            }).error(function (msg) {
                // TODO: handle 401 here
                deferred.reject(msg);
            });
            return deferred.promise;
        };

        var _delete = function (fileName) {
            return $http.delete(url + '/' + fileName)
                .success(function () {
                    // backup deleted
                })
                .error(function (msg) {
                    // TODO: handle 406
                });
        };

        return {
            getForVolume: function (volume) {
                return _getForVolume(volume);
            },
            delete: function (fileName){
                return _delete(fileName);
            }
        }
    }]);