(function () {
    var module = angular.module('clusterApp', ['tm.pagination', 'ngDialog']);
    module.run(function ($http) {
        $http.defaults.headers.common = {'X-Requested-With': 'XMLHttpRequest'};
    });
    module.controller('clusterController', function ($scope, ngDialog, $http) {
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
            }
        })

        $scope.showDetail = function (brokerDetail,brokerName,index) {
            ngDialog.open({
                template: 'detailBrokerInfoDialog',
                data: {
                    detail:brokerDetail[brokerName][index],
                    brokerName:brokerName,
                    index:index
                }});
        }

        $scope.showConfig = function (brokerAddr,brokerName,index) {
            $http({
                method: "GET",
                url: "cluster/brokerConfig.query",
                params:{brokerAddr:brokerAddr}
            }).success(function (resp) {
                if (resp.status == 0) {
                    ngDialog.open({
                        template: 'detailBrokerInfoDialog',
                        data: {
                            detail:resp.data,
                            brokerName:brokerName,
                            index:index
                        }});
                }
            })
        }
    })

}).call(this);
