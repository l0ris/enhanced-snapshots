app.directive('uploadedFile', function(){
    return {
        scope: {
            'uploadedFile': '='
        },
        link: function(scope, el, attrs){
            el.bind('change', function(event){
                var file = event.target.files[0];
                scope.uploadedFile = file ? file : undefined;
                scope.$apply();
            });
        }
    };
});