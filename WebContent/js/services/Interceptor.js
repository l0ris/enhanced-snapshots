/**
 * Created by avas on 31.07.2015.
 */

angular.module('web')
    .factory('Interceptor', ['$q', 'Exception', function ($q, Exception) {

        return {
            responseError: function (rejection) {
                if (rejection.status === 401) {
                    var ssoLoginPage = "/saml/login";
                    var localLoginPage = "#/login?err=session";
                    var isSso = rejection.data &&
                        rejection.data.loginMode &&
                        rejection.data.loginMode === "SSO";

                    window.location = isSso ? ssoLoginPage : localLoginPage;
                } else if (rejection.status === 500 && rejection.data.localizedMessage) {
                    Exception.handle(rejection);
                }
                return $q.reject(rejection);
            }
        }
    }]);