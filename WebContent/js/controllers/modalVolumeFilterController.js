'use strict';

angular.module('web')
    .controller('modalVolumeFilterCtrl', ['$scope', '$modalInstance', 'Regions', 'Storage', 'tags', 'instances', function ($scope, $modalInstance, Regions, Storage, tags, instances) {
        $scope.tags = tags;
        $scope.instances = instances;
        $scope.globalRegion = {
            location: "",
            name: "GLOBAL",
            id: ""
        };
        $scope.sliderOptions = {
            from: 0,
            to: 16384,
            step: 4,
            dimension: " GiB",
            skin: "plastic"
        };

        Regions.get().then(function (regions) {
            $scope.regions = regions
        });

        //$scope.selectedRegion = $scope.globalRegion;

        $scope.clear = function () {
            var defaultFilter = {
                volumeId: "",
                name: "",
                size: "0;16384",
                instanceID: "",
                region: $scope.globalRegion,
                tags: []
            };
            $scope.filter = angular.copy(defaultFilter);
        };

        if (Storage.get('VolumeFilter')) {
            $scope.filter = Storage.get('VolumeFilter');
        } else {
            $scope.clear();
        }

        $scope.ok = function () {
            var f = $scope.filter;
            var stAdvancedFilter = {
                "volumeId": {
                    "type": "str",
                    "value": f.volumeId
                },
                "volumeName": {
                    "type": "str",
                    "value": f.name
                },
                "size": {
                    "type": "int-range",
                    "value": {
                        "lower": parseInt(f.size.split(";")[0], 10),
                        "higher": parseInt(f.size.split(";")[1], 10)
                    }
                },
                "instanceID": {
                    "type": "str-strict",
                    "value": f.instanceID
                },
                "availabilityZone": {
                    "type": "str",
                    "value": f.region.id
                },
                "tags": {
                    "type": "array-inc",
                    "value": f.tags
                }
            };

            Storage.save('VolumeFilter', f);
            $modalInstance.close(stAdvancedFilter);
        }


    }]);