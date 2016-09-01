/**
 * Created by avas on 31.07.2015.
 */

angular.module('web')
    .factory('Interceptor', function ($q, Exception) {

        return {
            responseError: function (rejection) {
                if (rejection.status === 401) {
                    var ssoLoginPage = "https://qa-sungard.sso.sungardas.io/service/saml2/idp?SAMLRequest=nVNNc9owEP0rGt39gRM%2BrMFkKEymzKQNA04PvSnyGpSRJUcrE%2FrvKxtDOKQcevNo377d9956%2BnCsFDmARWl0RgdhTAloYQqpdxl9yR%2BDCX2YTZFXKqnZvHF7vYH3BtAR36iRnSoZbaxmhqNEpnkFyJxg2%2FmPJ5aEMautcUYYRckcEazzoxZGY1OB3YI9SAEvm6eM7p2rWRQpI7jaG3RsEk%2FiqB0QbbfPlCz9VKm56zZtwejR7zzARu%2B4LUJEE%2FbfHENpIjyRdxRJJIuakkdjBXQyMlpyhUDJaplRPkzE6C2JB3u5K5NxmhSjt%2BHrTpZpmo5l6kG45ojyAJ9tiA2sNDquXUZ96yiIJ8FdnCcDdjdm9%2FfhMEl%2FU7LuxX%2BT%2BmTqLadeTyBk3%2FN8Hayftzklv87heADto2DddHudwW1ifjaezs7OAaIzRl1ZJkw1ja75L8H%2F9ISr5dooKf6QuVLmY2GBO%2B%2BGsw10tlbc3V6hfZFFUHZQ5izXKEE7Gl2m9JcFRReQPxEHR0cWpqq5ldhaAEcu3MWEa9hCeYkbKP%2FLkpswwUTL7Z%2FbE%2FgwtmgjBeH3zFsRtbHubNtXG8364j%2F0fZav%2F67ZXw%3D%3D&SigAlg=http%3A%2F%2Fwww.w3.org%2F2000%2F09%2Fxmldsig%23dsa-sha1&Signature=MCwCFDMR770DUmsxPvzXjo9kFegS3AaWAhQFwUZmyzoyPsaHurbVy7bILSNmUA%3D%3D";
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
    });