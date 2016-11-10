var app = angular.module('web', ['ui.router', 'angularAwesomeSlider', 'ui.bootstrap', 'smart-table', 'ngTagsInput', 'ngStomp', 'toastr']);

app.constant('BASE_URL', './');

// Settings for table paging
app.constant('ITEMS_BY_PAGE', 25);
app.constant('DISPLAY_PAGES', 7);

app.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', function ($stateProvider, $urlRouterProvider, $httpProvider) {
    $urlRouterProvider.otherwise("/loader");

    var authenticated = ['$rootScope', function ($rootScope) {
        if (angular.isUndefined($rootScope.getUserName())) throw "User not authorized!";
        return true;
    }];

    var isConfig = ['$rootScope', function ($rootScope) {
        if (!$rootScope.isConfigState())  throw "System is not in configuration state!";
        return true;
    }];

    var ssoMode = ['System', '$q', '$rootScope', function (System, $q, $rootScope) {
        $rootScope.isLoading = true;
        var deferred = $q.defer();

        System.get().then(function (data) {
            $rootScope.isLoading = false;
            deferred.resolve(data);
        }, function () {
            $rootScope.isLoading = false;
            deferred.reject(false);
        });

        return deferred.promise;
    }];

    var doRefresh = ['Users', '$q', 'Storage', 'Auth', '$rootScope', 'System',
        function (Users, $q, Storage, Auth, $rootScope, System) {
        $rootScope.isLoading = true;
        var deferred = $q.defer();

        var promises = [System.get(), Users.refreshCurrent()];
        $q.all(promises).then(function (results) {

            if (results[0].ssoMode != undefined) {
                //response for System.get
                Storage.save("ssoMode", {"ssoMode": results[0].ssoMode});
            }

            if (results[1].status === 200) {
                    deferred.resolve(results[1].status)
            } else {
                    deferred.resolve(false)
            }
        }, function (rejection) {
                if (rejection.status === 401) {
                    var isSso = rejection.data &&
                        rejection.data.loginMode &&
                        rejection.data.loginMode === "SSO";

                    deferred.resolve(isSso);
                }

                deferred.resolve(false)
        });

        return deferred.promise;
    }];

    $stateProvider
        .state('app', {
            abstract: true,
            url: "/app",
            templateUrl: "partials/app.html",
            resolve: {
                authenticated: authenticated
            },
            controller: ['$scope', '$rootScope', 'Storage', 'toastr', function ($scope, $rootScope, Storage, toastr) {
                $rootScope.$on('$stateChangeSuccess',
                    function(){
                        var notification = Storage.get("notification");
                        if (notification) {
                            toastr.info(notification, undefined, {
                                closeButton: true,
                                timeOut: 20000
                            });
                            Storage.remove("notification");
                        }
                    });
                $rootScope.isAdmin = (Storage.get("currentUser") || {}).role === 'admin';
            }]
        })
        .state('app.volume', {
            abstract: true,
            template: "<ui-view></ui-view>",
            url: ""
        })
        .state('app.volume.list', {
            url: "/volumes",
            templateUrl: "partials/volumes.html",
            controller: 'VolumesController'
        })
        .state('app.volume.schedule', {
            url: "/schedule/:volumeId",
            templateUrl: "partials/schedule.html",
            controller: 'ScheduleController'
        })

        .state('app.volume.history', {
            url: "/history/:volumeId",
            templateUrl: "partials/history.html",
            controller: 'HistoryController'
        })
        .state('app.volume.tasks', {
            url: "/tasks/:volumeId",
            templateUrl: "partials/tasks.html",
            controller: "TasksController"
        })
        .state('app.tasks', {
            url: "/tasks",
            templateUrl: "partials/tasks.html",
            controller: "TasksController"
        })
        .state('app.settings', {
            url: "/settings",
            templateUrl: "partials/settings.html",
            controller: "SettingsController"
        })
        .state('app.users', {
            url: "/users",
            templateUrl: "partials/users.html",
            controller: "UserController",
            resolve: {
                ssoMode: ssoMode
            }
        })
        .state('app.logs', {
            url: "/logs",
            templateUrl: "partials/logs.html",
            controller: "LogsController"
        })
        .state('config', {
            url: "/config",
            templateUrl: "partials/config.html",
            controller: "ConfigController",
            resolve: {
                isConfig: isConfig
            }
        })
        .state('login', {
            url: "/login?err",
            templateUrl: "partials/login.html",
            controller: "LoginController",
            resolve: {
                refreshUserResult: doRefresh
            }
        })
        .state('loader', {
            url: "/loader",
            template: '<div class="loading">'+
            '<div class="text-center spinner-container">' +
            '<span class="glyphicon glyphicon-refresh text-muted spin"></span>'+
            '</div> </div>',
            controller: "LoaderController"
        })
        .state('logout', {
            url: "/logout",
            template: '<div class="loading">'+
                        '<div class="text-center spinner-container">' +
                        '<span class="glyphicon glyphicon-refresh text-muted spin"></span>'+
                        '</div> </div>',
            controller: "LogoutController"
        })
        .state('registration', {
            url: "/registration",
            templateUrl: "partials/registration.html",
            controller: "RegistrationController"
        });

    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';
    $httpProvider.interceptors.push('Interceptor');
}])
    .run(['$rootScope', '$state', '$modal', '$stomp', 'toastr', 'Storage', 'Users', 'System', '$q',
        function ($rootScope, $state, $modal, $stomp, toastr, Storage, Users, System, $q) {
        $rootScope.isLoading = true;

        var isUserSaved = Storage.get("currentUser");
        var isSsoSaved = Storage.get("ssoMode");

        if (!isUserSaved || !isSsoSaved) {
            var promises = [System.get(), Users.refreshCurrent()];
            $q.all(promises).then(function (results) {
                if (results[0].ssoMode != undefined) {
                    //response for System.get
                    Storage.save("ssoMode", {"ssoMode": results[0].ssoMode});
                }

                //response for Users.refreshCurrent
                if (results[1].data && results[1].data.email) {
                    $state.go('app.volume.list');
                } else {
                    $state.go('login');
                }

                $rootScope.isLoading = false;
            }, function (err) {
                console.log(err);
                $rootScope.isLoading = false;
            });
        }

        $rootScope.getUserName = function () {
            return (Storage.get("currentUser") || {}).email;
        };

        $rootScope.isConfigState = function () {
            return (Storage.get("currentUser") || {}).role === 'configurator';
        };

        $rootScope.subscribeWS = function () {
            $stomp.setDebug(function (args) {
                // console.log(args);
            });

            $stomp
                .connect('/rest/ws')
                .then(function (frame) {
                    $rootScope.errorListener = $stomp.subscribe('/error', function (err) {
                        toastr.error(err.message, err.title);
                    });
                    $rootScope.taskListener = $stomp.subscribe('/task', function (msg) {
                        Storage.save('lastTaskStatus_' + msg.taskId, msg);
                        $rootScope.$broadcast("task-status-changed", msg);
                    });
                }, function (e) {
                    console.log(e);
                });
            };

        $rootScope.$on('$stateChangeError', function (e) {
            e.preventDefault();
            if (Storage.get("ssoMode")) {
                $rootScope.isLoading = true;
            } else {
                $state.go('login');
            }
        });

        $rootScope.errorListener = {};
        $rootScope.taskListener = {};
        if (angular.isDefined($rootScope.getUserName())) { $rootScope.subscribeWS(); }
    }]);