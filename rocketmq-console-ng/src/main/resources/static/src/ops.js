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

app.controller('opsController', ['$scope','$location','$http','Notification','remoteApi','tools', function ($scope,$location,$http,Notification,remoteApi,tools) {
    $scope.namesvrAddrList = [];
    $scope.useVIPChannel = true;
    $http({
        method: "GET",
        url: "ops/homePage.query"
    }).success(function (resp) {
        if (resp.status == 0) {
            $scope.namesvrAddrList = resp.data.namesvrAddrList;
            $scope.useVIPChannel = resp.data.useVIPChannel
        }else{
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    });

    $scope.updateNameSvrAddr = function () {
        $http({
            method: "POST",
            url: "ops/updateNameSvrAddr.do",
            params:{nameSvrAddrList:$scope.namesvrAddrList.join(";")}
        }).success(function (resp) {
            if (resp.status == 0) {
                Notification.info({message: "SUCCESS", delay: 2000});
            }else{
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };
    $scope.updateIsVIPChannel = function () {
        $http({
            method: "POST",
            url: "ops/updateIsVIPChannel.do",
            params:{useVIPChannel:$scope.useVIPChannel}
        }).success(function (resp) {
            if (resp.status == 0) {
                Notification.info({message: "SUCCESS", delay: 2000});
            }else{
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    }
}]);
