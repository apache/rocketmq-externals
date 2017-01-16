(function () {
    var module = angular.module('messageApp', ['tm.pagination', 'ngDialog','ngMaterial', 'ngMessages', 'material.svgAssetsCache','ae-datetimepicker','localytics.directives','ui-notification']);
    module.run(function ($http) {
        $http.defaults.headers.common = {'X-Requested-With': 'XMLHttpRequest'};
    });
    module.controller('messageController', function ($scope, ngDialog, $http,Notification) {
        $scope.allTopicList = [];
        $scope.selectedTopic =[];
        $scope.key ="";
        $scope.messageId ="";
        $scope.queryMessageByTopicResult=[];
        $scope.queryMessageByTopicAndKeyResult=[];
        $scope.queryMessageByMessageIdResult={};
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
        $scope.timepickerBegin = moment().subtract(1, 'hour').format('YYYY-MM-DD HH:mm');
        $scope.timepickerEnd = moment().format('YYYY-MM-DD HH:mm');
        $scope.timepickerOptions ={format: 'YYYY-MM-DD HH:mm', showClear: true};

        $scope.paginationConf = {
            currentPage: 1,
            totalItems: 0,
            itemsPerPage: 20,
            pagesLength: 15,
            perPageOptions: [10],
            rememberPerPage: 'perPageItems',
            onChange: function () {
                $scope.changeShowMessageList(this.currentPage,this.totalItems);
            }
        };


        $scope.queryMessageByTopic = function () {
            console.log($scope.selectedTopic);
            console.log($scope.timepickerBegin)
            console.log($scope.timepickerEnd)
            $http({
                method: "GET",
                url: "/message/queryMessageByTopic.query",
                params: {
                    topic: $scope.selectedTopic,
                    begin: $scope.timepickerBegin.valueOf(),
                    end: $scope.timepickerEnd.valueOf()

                }
            }).success(function (resp) {
                if (resp.status == 0) {
                    console.log(resp);
                    $scope.queryMessageByTopicResult = resp.data;
                    $scope.changeShowMessageList(1,$scope.queryMessageByTopicResult.length);
                    // todo 
                    // console.log($scope.queryMessageByTopicResult);
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        };

        $scope.queryMessageByTopicAndKey = function () {
            console.log($scope.selectedTopic);
            console.log($scope.key);
            $http({
                method: "GET",
                url: "/message/queryMessageByTopicAndKey.query",
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

        $scope.queryMessageByBrokerAndOffset = function (storeHost,commitLogOffset) {
            $http({
                method: "GET",
                url: "/message/viewMessageByBrokerAndOffset.query",
                params: {
                    brokerHost: storeHost.address,
                    port:storeHost.port,
                    offset: commitLogOffset
                }
            }).success(function (resp) {
                if (resp.status == 0) {
                    console.log(resp);
                    ngDialog.open({
                        template: 'messageDetailViewDialog',
                        controller: 'messageDetailViewDialogController',
                        data: resp.data
                    });
                } else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        };

        $scope.queryMessageByMessageId = function (messageId) {
            $http({
                method: "GET",
                url: "/message/viewMessage.query",
                params: {
                    msgId: messageId
                }
            }).success(function (resp) {
                if (resp.status == 0) {
                    console.log(resp);
                    ngDialog.open({
                        template: 'messageDetailViewDialog',
                        controller: 'messageDetailViewDialogController',
                        data:resp.data
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        };


        $scope.changeShowMessageList = function (currentPage,totalItem) {
            var perPage = $scope.paginationConf.itemsPerPage;
            var from = (currentPage - 1) * perPage;
            var to = (from + perPage)>totalItem?totalItem:from + perPage;
            $scope.messageShowList = $scope.queryMessageByTopicResult.slice(from, to);
            $scope.paginationConf.totalItems = totalItem ;
        };
    });

    module.controller('messageDetailViewDialogController', function ($scope, ngDialog, $http,Notification) {

        $scope.resendMessage = function (msgId,consumerGroup) {
            $http({
                method: "POST",
                url: "/message/consumeMessageDirectly.do",
                params: {
                    msgId: msgId,
                    consumerGroup:consumerGroup
                }
            }).success(function (resp) {
                if (resp.status == 0) {
                    alert(JSON.stringify(resp.data));
                    // $('#messageOperateResult').append('<div id="appListAlert" class="alert alert-success alert-dismissible  fade in"> <button type="button" class="close" data-dismiss="alert" aria-label="Close"> <span aria-hidden="true">&times;</span> </button>' + JSON.stringify(resp.data) + '</div>')
                }
                else {
                    alert(resp.errMsg);
                    // $('#messageOperateResult').html('<div id="appListAlert" class="alert alert-danger alert-dismissible  fade in"> <button type="button" class="close" data-dismiss="alert" aria-label="Close"> <span aria-hidden="true">&times;</span> </button>' + result.errMsg + '</div>')
                }
            });
        };
        $scope.showExceptionDesc = function (errmsg) {
            alert(errmsg);
        };
        }
    );

}).call(this);
