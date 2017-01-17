/**
 * Created by tcrow on 2017/1/12 0012.
 */

var module = app;
module.controller('producerController', ['$scope', '$http','Notification',function ($scope, $http,Notification) {
    $scope.selectedTopic=[];
    $scope.producerGroup="";
    $http({
        method: "GET",
        url: "/topic/list.query"
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
            url: "/producer/producerConnection.query",
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