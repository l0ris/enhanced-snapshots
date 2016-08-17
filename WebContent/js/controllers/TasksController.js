'use strict';

angular.module('web')
    .controller('TasksController', function ($scope, $rootScope, $stateParams, $stomp, Tasks, Storage, $modal, $timeout) {
        $scope.typeColorClass = {
            backup: "primary",
            restore: "success",
            delete: "danger",
            system_backup: "danger"

        };
        $scope.typeIconClass = {
            backup: "cloud-download",
            restore: "cloud-upload",
            delete: "remove",
            system_backup: "cog"
        };
        $scope.manualIconClass = {
            true: "user",
            false: "time"
        };

        $scope.statusPriority = function (task) {
            var priorities = {
                canceled: 5,
                running: 4,
                queued: 3,
                error: 2,
                waiting: 1
            };
            return priorities[task.status] || 0;
        };

        $scope.typePriority = function (task) {
            return parseInt(task.priority) || 0;
        };

        $scope.volumeId = $stateParams.volumeId;

        $scope.tasks = [];
        $rootScope.isLoading = false;
        $scope.refresh = function () {
            $rootScope.isLoading = true;
            Tasks.get($scope.volumeId).then(function (data) {
                $scope.tasks = data;
                updateTaskStatus(false);
                $rootScope.isLoading = false;
            }, function () {
                $rootScope.isLoading = false;
            });
        };
        $scope.refresh();

        $scope.$on("task-status-changed", function (e, d) {
            updateTaskStatus(d);
        });

        var updateTaskStatus = function (msg) {
            if (!msg) {
                msg = Storage.get('lastTaskStatus') || {};
            }
            var task = $scope.tasks.filter(function (t) {
                return t.id == msg.taskId && msg.status != "COMPLETE";
            })[0];

            if (task) {
                if (task.status == 'complete' || task.status == 'queued' || task.status == 'waiting') {
                    $scope.refresh();
                } else {
                    $timeout(function() {
                        task.progress = msg.progress;
                        task.message = msg.message;
                    }, 0);

                    if (msg.progress == 100) {
                        Storage.remove('lastTaskStatus');
                        $scope.refresh();
                    }
                }
            }
        };

        $scope.reject = function (task) {
            $scope.taskToReject = task;

            var rejectInstance = $modal.open({
                animation: true,
                templateUrl: './partials/modal.task-reject.html',
                scope: $scope
            });

            rejectInstance.result.then(function () {
                Tasks.delete(task.id).then(function () {
                    $scope.refresh();
                });
            });
        };

    });