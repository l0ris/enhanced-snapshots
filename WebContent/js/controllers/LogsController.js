'use strict';

angular.module('web')
    .controller('LogsController', function ($location, $anchorScroll, $stomp, $scope, $rootScope, $state, $timeout, $q, System) {
        $scope.followLogs = false;
        $scope.logs = [];

        var maxLogs;
        $rootScope.isLoading = true;
        var collection = [];
        var subCollection = [];
        var initSubCollectionLength = 0;
        var logTypes = {
            warn: "warning",
            info: "info",
            error: "error",
            debug: ""
        };
        var counterStarted = false;

        System.get().then(function (settings) {
            maxLogs = settings.systemProperties.logsBuffer;

            $stomp
                .connect('/rest/ws')
                .then(function (frame) {
                    $rootScope.isLoading = false;
                    $scope.logsListener = $stomp.subscribe('/logs', function (payload, headers, res) {
                        updateLogs(res);
                        if ($scope.followLogs) {
                            var lastLogId = 'log-' + ($scope.logs.length ? $scope.logs.length - 1 : 0);
                            $location.hash(lastLogId);
                            $anchorScroll();
                        }
                    });

                }, function (e) {
                    $rootScope.isLoading = false;
                    console.log(e);
                }
            );

            function updateLogs(msg) {
                msg.body = JSON.parse(msg.body);
                // get log type, which can be error, info, etc.
                var getType = function (log) {
                    var logTypeRaw = (log.split(']')[0]).split('[').reverse()[0];
                    var logType = logTypeRaw.toLowerCase().trim();
                    return logTypes[logType]
                };

                var saveLogs = function (log) {
                    subCollection.push(log);
                    if (!counterStarted) {
                        counterStarted = true;
                        $timeout(function () {
                            var logsAdded = subCollection.length - initSubCollectionLength;
                            counterStarted = false;
                            updateLogsCollection(subCollection, logsAdded);
                        }, 500);
                    }
                };

                function updateLogsCollection(logsCollection, logsAdded) {
                    // yes, it's a magic number :) Logs are guaranteed to be displayed smoothly at
                    // this speed of 15 logs/half-sec
                    if (logsAdded < 15) {
                        sendToView(logsCollection);
                    } else {
                        // if speed of logs is more than 30 log/sec (15 logs/half-sec) => update view
                        // once per second 'till logs finished
                        if (logsCollection.length) {
                            $timeout( function () {
                                sendToView(logsCollection);
                            }, 1000)
                        }

                    }

                    //reduces array length if total logs are more than user wants
                    function checkLength() {
                        if (collection.length > (maxLogs)) {
                            collection = collection.slice(-maxLogs);
                        }
                    }

                    function sendToView(logsCollection) {
                        collection = collection.concat(logsCollection);
                        checkLength();

                        subCollection = [];
                        initSubCollectionLength = 0;
                        $scope.$apply(function () {
                            $scope.logs = collection;
                        });
                    }
                }

                for (var i = 0; i < msg.body.length; i++) {
                    var logObject = {
                        type: getType(msg.body[i]),
                        message: msg.body[i]
                    };
                    saveLogs(logObject);
                }
            }
        });

        $rootScope.$on('$stateChangeStart', function (event, toState, toParams, fromState, fromParams, options) {
            $rootScope.isLoading = false;
            //check is needed for cases when user comes to LOGS tab, which will also trigger this event
            if (fromState.name === 'app.logs' && $scope.logsListener) {
                $scope.logsListener.unsubscribe();
            }
        })
    });