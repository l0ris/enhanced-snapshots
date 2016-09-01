'use strict';

angular.module('web')
    .service('Users', function ($q, $http, Storage, BASE_URL) {
        var url = BASE_URL + "rest/user";
        var storageKey = '_users';

        var getUsers = function () {
            var deferred = $q.defer();
            $http({
                url: url,
                method: 'GET'
            }).then(function (result) {
                deferred.resolve(result.data);
            },function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var add = function (user) {
            var deferred = $q.defer();
            $http({
                url: url,
                method: 'POST',
                data: user
            }).then(function (result) {
                deferred.resolve(result.data);
            },function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var updateUser = function (user) {
            return $http({
                url: url,
                method: 'PUT',
                data: user
            })
        };

        var getCurrentUser = function () {
            return Storage.get('currentUser')
        };

        var refreshCurrentUser = function () {
            var deferred = $q.defer();
            $http({
                url: url + "/currentUser",
                method: 'GET',
            }).then(function (result) {
                Storage.save('currentUser', result.data);
                deferred.resolve(result.data);
            },function (e) {
                deferred.reject(e);
            });
            return deferred.promise;
        };

        var remove = function (email) {
            return $http({
                url: url + "/" + email,
                method: 'DELETE'
            })
        };

        return {
            insert: function (user) {
                return add(user);
            },

            delete: function (email) {
                return remove(email);
            },

            update: function (user) {
                return updateUser(user);
            },

            getCurrent: function () {
                return getCurrentUser();
            },

            refreshCurrent: function () {
                return refreshCurrentUser();
            },

            getAll: function () {
                return getUsers().then(function (data) {
                    return data;
                })
            }
        }
    });