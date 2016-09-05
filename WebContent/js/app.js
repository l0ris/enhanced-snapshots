var app = angular.module('web', ['ui.router', 'angularAwesomeSlider', 'ui.bootstrap', 'smart-table', 'ngTagsInput', 'ngStomp', 'toastr']);

app.constant('BASE_URL', './');

// Settings for table paging
app.constant('ITEMS_BY_PAGE', 25);
app.constant('DISPLAY_PAGES', 7);

app.config(function ($stateProvider, $urlRouterProvider, $httpProvider) {
    $urlRouterProvider.otherwise("/app/volumes");

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

    $stateProvider
        .state('app', {
            abstract: true,
            url: "/app",
            templateUrl: "partials/app.html",
            resolve: {
                authenticated: authenticated
            },
            controller: function ($scope, $rootScope, Storage, toastr) {
                $rootScope.$on('$stateChangeSuccess',
                    function(event, toState, toParams, fromState, fromParams){
                        var notification = Storage.get("notification");
                        if (notification) {
                            toastr.info(notification, undefined, {
                                closeButton: true,
                                timeOut: 20000
                            });
                            Storage.remove("notification");
                        }
                    });
            }
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
            controller: "LoginController"
        })
        .state('registration', {
            url: "/registration",
            templateUrl: "partials/registration.html",
            controller: "RegistrationController"
        });

    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';
    $httpProvider.interceptors.push('Interceptor');
})
    .run(function ($rootScope, $state, $modal, $stomp, toastr, Storage, Users) {
        Users.refreshCurrent().then(function (user) {
            $rootScope.isLoading = false;
        });

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
                        Storage.save('lastTaskStatus', msg);
                        $rootScope.$broadcast("task-status-changed", msg);
                    });
                }, function (e) {
                    console.log(e);
                });
        };


        $rootScope.$on('$stateChangeError', function (e) {
            e.preventDefault();
            $state.go('login');
        });

        $rootScope.errorListener = {};
        $rootScope.taskListener = {};
        if (angular.isDefined($rootScope.getUserName())) { $rootScope.subscribeWS(); }
    });

