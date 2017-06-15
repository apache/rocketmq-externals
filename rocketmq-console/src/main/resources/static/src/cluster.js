/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

app.controller('clusterController', ['$scope','$location','$http','Notification','remoteApi','tools', function ($scope,$location,$http,Notification,remoteApi,tools) {
    $scope.clusterMap = {};//cluster:brokerNameList
    $scope.brokerMap = {};//brokerName:{id:addr}
    $scope.brokerDetail = {};//{brokerName,id:detail}
    $scope.clusterNames = [];
    $scope.selectedCluster = "";
    var callback = function (resp) {
        if (resp.status == 0) {
            $scope.clusterMap = resp.data.clusterInfo.clusterAddrTable;
            $scope.brokerMap = resp.data.clusterInfo.brokerAddrTable;
            $scope.brokerDetail = resp.data.brokerServer;
            $.each($scope.clusterMap,function(clusterName,clusterBrokersNames){
                $scope.clusterNames.push(clusterName);
            });
            if ($scope.clusterNames.length > 0) {
                $scope.selectedCluster = $scope.clusterNames[0];
            }
            $scope.brokers = tools.generateBrokerMap($scope.brokerDetail,$scope.clusterMap,$scope.brokerMap);
            $scope.switchCluster();
        }else{
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    }

    remoteApi.queryClusterList(callback);

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
