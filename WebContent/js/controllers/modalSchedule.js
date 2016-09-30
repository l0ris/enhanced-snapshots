/**
 * Created by Administrator on 21.07.2015.
 */
'use strict';

angular.module('web')
    .controller('modalScheduleCtrl', ['$scope', '$modalInstance', '$filter', 'schedule', 'Schedules', function ($scope, $modalInstance, $filter, schedule, Schedules) {

        $scope.Schedules = Schedules;
        $scope.schedule = schedule;

        $scope.isNew = schedule.id == 0;
        $scope.schedule = angular.copy(schedule);
        $scope.isEndless = typeof $scope.schedule.end == 'undefined' || !$scope.schedule.end;

        $scope.weekdays = {
            Monday: false,
            Tuesday: false,
            Wednesday: false,
            Thursday: false,
            Friday: false,
            Saturday: false,
            Sunday: false
        };

        $scope.shortdays = {
            Monday: "Mo",
            Tuesday: "Tu",
            Wednesday: "We",
            Thursday: "Th",
            Friday: "Fr",
            Saturday: "Sa",
            Sunday: "Su"
        };

        //-----------DATE FORMATING---------


        for (var day in $scope.weekdays) {
            $scope.weekdays[day] = $scope.schedule.week.indexOf(day) >= 0;
        }
        ;

        $scope.doEndless = function () {
            if ($scope.isEndless) {
                $scope.schedule.end = "";
            }
            else {
                $scope.schedule.end = $scope.schedule.start;
                $scope.schedule.end.setDate($scope.schedule.start.getDate() + 1);
            }
        };

        // ------------REPEAT-EVERY-----------------------
        $scope.periodicityNum = [1, 2, 3, 4, 5, 10, 15];
        $scope.periodicityWord = ["day", "week", "month", "year"];


        // ---------------- calendar pop-up ----
        $scope.opened = {
            start: false,
            end: false
        };

        $scope.today = function () {
            $scope.dt = new Date();
        };


        $scope.clear = function () {
            $scope.dt = null;
        };

        $scope.calendarOpen = function ($event, which) {
            $event.preventDefault();
            $event.stopPropagation();
            $scope.opened[which] = true;
        };


        // ---------------BUTTONS-------

        $scope.ok = function () {

            $scope.schedule.week = Object.keys($scope.weekdays)
                .filter(function (d) {
                    return $scope.weekdays[d];
                });
            $scope.schedule.start = $filter('date')($scope.schedule.start, 'yyyy-MM-dd hh:mm:ss');
            $scope.schedule.end = (function () {
                if ($scope.schedule.end != null && $scope.schedule.end) {
                    return $filter('date')($scope.schedule.end, 'yyyy-MM-dd hh:mm:ss')
                } else {
                    return ""
                }
            })();

            if ($scope.isNew) {
                Schedules.insert($scope.schedule).then(function () {
                    $modalInstance.close();
                });
            }
            else {
                Schedules.update($scope.schedule).then(function () {
                    $modalInstance.close();
                });
            }

        };

        $scope.cancel = function () {
            $modalInstance.dismiss();
        };

    }]);