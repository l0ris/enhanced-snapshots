app.directive('autoScroll', function () {
    return {
        scope: {
            autoScroll: "="
        },
        link: function (scope, element, attr) {

            scope.$watchCollection('autoScroll', function (newValue) {
                if (newValue && JSON.parse(attr.enableScroll))
                {
                    $(element).scrollTop($(element)[0].scrollHeight);
                }
            });
        }
    }
});