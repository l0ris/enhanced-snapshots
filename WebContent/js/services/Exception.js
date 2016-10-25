'use strict';

angular.module('web')
    .service('Exception', ['toastr', function (toastr) {
        return {
            handle: function (error){
                toastr.error((error.data || {}).localizedMessage || "Error occurred!");
                console.log(error);
            }
        };
    }]);