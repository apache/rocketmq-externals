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

var module = app;
module.controller('producerController', ['$scope', '$http','Notification',function ($scope, $http,Notification) {
    $scope.selectedTopic=[];
    $scope.producerGroup="";
    $http({
        method: "GET",
        url: "topic/list.query"
    }).success(function (resp) {
        if(resp.status ==0){
            $scope.allTopicList = resp.data.topicList.sort();
            console.log($scope.allTopicList);
        }else {
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    });
    $scope.queryClientByTopicAndGroup = function () {
        $http({
            method: "GET",
            url: "producer/producerConnection.query",
            params:{
                topic:$scope.selectedTopic,
                producerGroup:$scope.producerGroup
            }
        }).success(function (resp) {
            if(resp.status ==0){
                $scope.connectionList = resp.data.connectionSet;
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    }
} ]);