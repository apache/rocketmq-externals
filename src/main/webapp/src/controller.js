/**
 * Created by tcrow on 2016/3/24 0024.
 */
app.controller('AppCtrl', ['$scope','$rootScope','$cookies','$location','$translate', function ($scope,$rootScope,$cookies,$location,$translate) {
    $scope.gotoDemoPage = function(){
        $location.path("/demo");
    }

    $scope.changeTranslate = function(langKey){
        $translate.use(langKey);
    }
}]);

app.controller('DemoCtrl', ['$scope','$rootScope','$cookies','$location', function ($scope,$rootScope,$cookies,$location) {
    $scope.msg = 'hello world!!!!';
}]);
