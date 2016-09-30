'use strict';

angular.module('web')
    .service('Storage', [function () {

        return {

            get: function (key) {
                return JSON.parse(sessionStorage.getItem(key));
            },

            save: function (key, data) {
                sessionStorage.setItem(key, JSON.stringify(data));
            },

            remove: function (key) {
                sessionStorage.removeItem(key);
            },

            clearAll : function () {
                sessionStorage.clear();
            }
        };
    }]);