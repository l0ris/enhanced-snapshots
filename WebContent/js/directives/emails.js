"use strict";

angular.module('web')
    .directive('emails', function() {
        return {
            restrict: 'E',
            scope: { emails: '='},
            template:
            '<div class="input-group" style="clear: both;">' +
                '<input type="email" class="form-control" ng-model="newEmail" placeholder="email"/>' +
                '<span class="input-group-btn" style="width:0px;"></span>' +
                '<span class="input-group-btn"><button class="btn btn-primary" ng-click="add()" ng-disabled="!newEmail"><span class="glyphicon glyphicon-plus"></span></button></span>' +
            '</div>' +
            '<div class="tags" style="margin-top: 5px">' +
                '<div ng-repeat="mail in emails track by $index" class="tag label label-success" ng-click="remove($index)">' +
                    '<span class="glyphicon glyphicon-remove"></span>' +
                    '<div class="tag-value">{{mail}}</div>' +
                '</div>' +
            '</div>',
            link: function ( $scope, $element ) {
                $scope.newEmail = "";
                var inputs = angular.element( $element[0].querySelectorAll('input') );

                // This adds the new tag to the tags array
                $scope.add = function() {
                    if ($scope.newEmail) {
                        $scope.emails.push($scope.newEmail);
                        $scope.newEmail = "";
                    }
                    event.preventDefault();
                };

                // This is the ng-click handler to remove an item
                $scope.remove = function ( idx ) {
                    $scope.emails.splice( idx , 1 );
                };

                // Capture all keypresses
                inputs.bind( 'keypress', function ( event ) {
                    // But we only care when Enter was pressed
                    if ( $scope.newEmail && ( event.keyCode == 13 ) ) {
                        event.preventDefault();
                        $scope.$apply( $scope.add );
                    }
                });
            }
        };
    });