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

module.controller('messageTraceController', ['$scope', 'ngDialog', '$http','Notification',function ($scope, ngDialog, $http,Notification) {
    $scope.allTopicList = [];
    $scope.selectedTopic =[];
    $scope.key ="";
    $scope.messageId ="";
    $scope.queryMessageByTopicAndKeyResult=[];
    $scope.queryMessageByMessageIdResult={};
    $scope.queryMessageTraceListsByTopicAndKeyResult=[];

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
    $scope.timepickerBegin = moment().subtract(1, 'hour').format('YYYY-MM-DD HH:mm');
    $scope.timepickerEnd = moment().add(1,'hour').format('YYYY-MM-DD HH:mm');
    $scope.timepickerOptions ={format: 'YYYY-MM-DD HH:mm', showClear: true};
    
    $scope.queryMessageByTopicAndKey = function () {
        console.log($scope.selectedTopic);
        console.log($scope.key);
        $http({
            method: "GET",
            url: "message/queryMessageByTopicAndKey.query",
            params: {
                topic: $scope.selectedTopic,
                key:$scope.key
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                console.log(resp);
                $scope.queryMessageByTopicAndKeyResult = resp.data;
                console.log($scope.queryMessageByTopicAndKeyResult);
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };

    $scope.queryMessageByMessageId = function (messageId,topic) {
        $http({
            method: "GET",
            url: "messageTrace/viewMessage.query",
            params: {
                msgId: messageId,
                topic:topic
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                console.log(resp);
                $scope.queryMessageByMessageIdResult = resp.data;
                console.log($scope.queryMessageByTopicAndKeyResult);
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };

    $scope.queryMessageTraceByMessageId = function (messageId,topic) {
        $http({
            method: "GET",
            url: "messageTrace/viewMessageTraceDetail.query",
            params: {
                msgId: messageId,
                topic:topic
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                console.log(resp);
                ngDialog.open({
                    template: 'messageTraceDetailViewDialog',
                    controller: 'messageTraceDetailViewDialogController',
                    data:resp.data
                });
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };
}]);

module.controller('messageTraceDetailViewDialogController',['$scope', 'ngDialog', '$http','Notification', function ($scope, ngDialog, $http,Notification) {

        $scope.showExceptionDesc = function (errmsg) {
            if(errmsg == null){
                errmsg = "Don't have Exception"
            }
            ngDialog.open({
                template: 'operationResultDialog',
                data:{
                    result:errmsg
                }
            });
        };
    }]
);