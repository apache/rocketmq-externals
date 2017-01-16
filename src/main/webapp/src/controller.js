/**
 * Created by tcrow on 2016/3/24 0024.
 */
app.controller('AppCtrl', ['$scope','$rootScope','$cookies','$location','$translate', function ($scope,$rootScope,$cookies,$location,$translate) {
    $scope.changeTranslate = function(langKey){
        $translate.use(langKey);
    }
}]);


app.controller('ClusterController', ['$scope','$location','$http','Notification', function ($scope,$location,$http,Notification) {
    $scope.clusterMap = {};//cluster:brokerNameList
    $scope.brokerMap = {};//brokerName:{id:addr}
    $scope.brokerDetail = {};//{brokerName,id:detail}
    $http({
        method: "GET",
        url: "/cluster/list.query"
    }).success(function (resp) {
        if (resp.status == 0) {
            $scope.clusterMap = resp.data.clusterInfo.clusterAddrTable;
            $scope.brokerMap = resp.data.clusterInfo.brokerAddrTable;
            $scope.brokerDetail = resp.data.brokerServer;

            console.info($scope.clusterMap)
            console.info($scope.brokerDetail)
            var map = {};
            $.each($scope.brokerDetail,function(k,v){
                $.each($scope.clusterMap,function (ck, cv) {
                    if(angular.isUndefined(map[ck])){
                        map[ck] = [];
                    }
                    console.info(k)
                    console.info("ck:"+ck)
                    console.info(cv);
                })
            })
        }else{
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    });

    $scope.showDetail = function (brokerDetail,brokerName,index) {
        $scope.detail = brokerDetail[brokerName][index];
        $scope.brokerName = brokerName;
        $scope.index = index;
        $(".brokerModal").modal();
    }

    $scope.showConfig = function (brokerAddr,brokerName,index) {
        $scope.brokerAddr = brokerAddr;
        $scope.brokerName = brokerName;
        $scope.index = index;
        $http({
            method: "GET",
            url: "cluster/brokerConfig.query",
            params:{brokerAddr:brokerAddr}
        }).success(function (resp) {
            if (resp.status == 0) {
                $scope.brokerConfig = resp.data;
            }else{
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        })
    }
}])

