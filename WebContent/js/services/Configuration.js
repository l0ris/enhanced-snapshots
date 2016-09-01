'use strict';

angular.module('web')
    .service('Configuration', function ($q, $http, BASE_URL) {
        var url = BASE_URL + 'rest/configuration';

        var _get = function (type) {
            var deferred = $q.defer();
            $http({
                url: url + "/" + type,
                method: 'GET'
            }).then(function (data, status) {
                deferred.resolve(data, status);
            }, function (data, status) {
                deferred.reject(data, status)
            });
            return deferred.promise;
        };

        var _send = function (type, item, timeout, files) {
            var deferred = $q.defer();

            if (files) {
                _sendFiles(item, files).then(function (result) {
                    console.info("Files uploaded successfully");
                }, function () {
                    return deferred.reject()
                })
            }

            var request = {
                url: url + "/" + type,
                method: "POST",
                data: item || {}
            };
            if (timeout) {request.timeout = timeout;}

            $http(request).then(function () {
                deferred.resolve()
            }, function (data, status) {
                deferred.reject(data, status)
            });
            return deferred.promise;
        };

        var _sendFiles = function (item, files) {
            var deferred = $q.defer();
            var formData = new FormData();
            var namesArray = [];

            for (var key in files) {
                formData.append('file', files[key]);
                namesArray.push(files[key].name);
            }

            formData.append('name', namesArray);

            $http({
                url: url + "/uploadFiles",
                method: "POST",
                data: formData,
                transformRequest: angular.identity,
                transformResponse: angular.identity,
                headers: {'Content-Type': undefined}
            }).then(function () {
                deferred.resolve()
            }, function (error) {
                console.warn(error.data);
                deferred.reject();
            });

            //files are sent separately from other setting. That's why
            //they should be removed from settings collection before the later is sent
            delete item.sso;

            return deferred.promise;
        };

        return {
            get: function (type) {
                return _get(type);
            },
            send: function (type, item, timeout, files) {
                return _send(type, item, timeout, files);
            }
        }
    });