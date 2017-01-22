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
    $scope.clusterNames = [];
    $http({
        method: "GET",
        url: "/cluster/list.query"
    }).success(function (resp) {
        if (resp.status == 0) {
            $scope.clusterMap = resp.data.clusterInfo.clusterAddrTable;
            $scope.brokerMap = resp.data.clusterInfo.brokerAddrTable;
            $scope.brokerDetail = resp.data.brokerServer;
            $.each($scope.clusterMap,function(clusterName,clusterBrokersNames){
                $scope.clusterNames.push(clusterName);
            })
            var map = {};
            $.each($scope.brokerDetail,function(k,v){
                $.each($scope.clusterMap,function (ck, cv) {
                    if(angular.isUndefined(map[ck])){
                        map[ck] = [];
                    }
                    $.each(cv,function(cvi,cvv){
                        if(cvv == k){
                            var index = 0;
                            $.each(v,function(vi,vv){
                                vv.split = k;
                                vv.index = index;
                                vv.address = $scope.brokerMap[cvv].brokerAddrs[index];
                                vv.brokerName = $scope.brokerMap[cvv].brokerName;
                                map[ck].push(vv);
                                index++;
                            })
                        }
                    })
                })
            })
            $scope.brokers = map;
            $scope.selectedCluster = 'DefaultCluster'; //Default select DefaultCluster
            $scope.switchCluster();
        }else{
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    });

    $scope.switchCluster = function(){
        $scope.instances = $scope.brokers[$scope.selectedCluster];
    }

    $scope.showDetail = function (brokerName,index) {
        $scope.detail = $scope.brokerDetail[brokerName][index];
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
                $(".configModal").modal();
            }else{
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        })
    }
}])

