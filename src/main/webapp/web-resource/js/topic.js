(function () {
    // localytics.directives for chosen
    var module = angular.module('topicApp', ['tm.pagination', 'ngDialog','ae-datetimepicker','localytics.directives','ui-notification']);
    module.run(function ($http) {
        $http.defaults.headers.common = {'X-Requested-With': 'XMLHttpRequest'};
    });
    module.directive('ngConfirmClick', [
        function(){
            return {
                link: function (scope, element, attr) {
                    var msg = attr.ngConfirmClick || "Are you sure?";
                    var clickAction = attr.confirmedClick;
                    element.bind('click',function (event) {
                        if ( window.confirm(msg) ) {
                            scope.$eval(clickAction)
                        }
                    });
                }
            };
        }]);
    module.controller('topicController', function ($scope, ngDialog, $http,Notification) {
        $scope.paginationConf = {
            currentPage: 1,
            totalItems: 0,
            itemsPerPage: 10,
            pagesLength: 15,
            perPageOptions: [10],
            rememberPerPage: 'perPageItems',
            onChange: function () {
                $scope.showTopicList(this.currentPage,this.totalItems);
            }
        };
        $scope.allTopicList = [];
        $scope.topicShowList = [];
        $http({
            method: "GET",
            url: "/topic/list.query"
        }).success(function (resp) {
            if(resp.status ==0){
                $scope.allTopicList = resp.data.topicList.sort();
                console.log($scope.allTopicList);
                console.log(JSON.stringify(resp));
                $scope.showTopicList(1,$scope.allTopicList.length);
            }else {
                Notification.error({message: resp.errMsg, delay: 5000});
            }

        });
        $http({
            method: "GET",
            url: "/management//teamUrl.query"
        }).success(function (resp) {
            if(resp.status ==0){
                var teamUrl = resp.data
                $http({
                    method: "jsonp",
                    url: teamUrl,
                    params: {
                        pageNo:1,
                        pageSize:100
                    }
                }).success(function (resp) {
                    console.log(JSON.stringify(resp))
                })
            }

        });
        $scope.filterStr="";
        $scope.$watch('filterStr', function() {
            $scope.filterList(1);
        });
        $scope.filterList = function (currentPage) {
            var lowExceptStr =  $scope.filterStr.toLowerCase();
            var canShowList = [];
            $scope.allTopicList.forEach(function(element) {
                console.log(element)
                if (element.toLowerCase().indexOf(lowExceptStr) != -1){
                    canShowList.push(element);
                }
            });
            $scope.paginationConf.totalItems =canShowList.length;
            var perPage = $scope.paginationConf.itemsPerPage;
            var from = (currentPage - 1) * perPage;
            var to = (from + perPage)>canShowList.length?canShowList.length:from + perPage;
            $scope.topicShowList = canShowList.slice(from, to);
        };

        $scope.showTopicList = function (currentPage,totalItem) {
            if($scope.filterStr != ""){
                $scope.filterList(currentPage);
                return;
            }
            var perPage = $scope.paginationConf.itemsPerPage;
            var from = (currentPage - 1) * perPage;
            var to = (from + perPage)>totalItem?totalItem:from + perPage;
            console.log($scope.allTopicList);
            console.log(from)
            console.log(to)
            $scope.topicShowList = $scope.allTopicList.slice(from, to);
            $scope.paginationConf.totalItems = totalItem ;
            console.log($scope.topicShowList)
            console.log($scope.paginationConf.totalItems)
        };
        $scope.deleteTopic= function (topic) {
            var url = "/topic/deleteTopic.do";
            $http({
                method: "POST",
                url: url,
                params:{
                    topic:topic
                }
            }).success(function (resp) {
                if(resp.status ==0){
                    Notification.info({message: "delete success!", delay: 2000});
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        };
        $scope.statsView = function (topic) {
            $http({
                method: "GET",
                url: "/topic/stats.query",
                params: {topic: topic}
            }).success(function (resp) {
                if (resp.status == 0) {
                    console.log(JSON.stringify(resp));
                    ngDialog.open({
                        template: 'statsViewDialog',
                        data:{
                            topic:topic,
                            statsData:resp.data
                        }
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            })
        };
        $scope.routerView = function (topic) {
            $http({
                method: "GET",
                url: "/topic/route.query",
                params: {topic: topic}
            }).success(function (resp) {
                if (resp.status == 0) {
                    console.log(JSON.stringify(resp));
                    ngDialog.open({
                        template: 'routerViewDialog',
                        controller: 'routerViewDialogController',
                        data:{
                            topic:topic,
                            routeData:resp.data
                        }
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            })
        };


        $scope.consumerView = function (topic) {
            $http({
                method: "GET",
                url: "/topic/queryConsumerByTopic.query",
                params: {topic: topic}
            }).success(function (resp) {
                if (resp.status == 0) {
                    console.log(JSON.stringify(resp));
                    ngDialog.open({
                        template: 'consumerViewDialog',
                        data:{
                            topic:topic,
                            consumerData:resp.data,
                            consumerGroupCount:Object.keys(resp.data).length
                        }
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            })
        };
        $scope.openDeleteTopicDialog = function (topic) {
                    ngDialog.open({
                        template: 'deleteTopicDialog',
                        controller: 'deleteTopicDialogController',
                        data:{
                            topic:topic,
                            consumerData:"asd"
                        }
                    });
        };

        $scope.openConsumerResetOffsetDialog = function (topic) {

            $http({
                method: "GET",
                url: "/topic/queryTopicConsumerInfo.query",
                params:{
                    topic:topic
                }
            }).success(function (resp) {
                if(resp.status ==0){
                    if(resp.data.groupList == null){
                        Notification.error({message: "don't have consume group!", delay: 2000});
                        return
                    }
                    ngDialog.open({
                        template: 'consumerResetOffsetDialog',
                        controller: 'consumerResetOffsetDialogController',
                        data:{
                            topic: topic,
                            selectedConsumerGroup:[],
                            allConsumerGroupList:resp.data.groupList
                        }
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });

        };


        $scope.openSendTopicMessageDialog = function (topic) {
            ngDialog.open({
                template: 'sendTopicMessageDialog',
                controller: 'sendTopicMessageDialogController',
                data: {
                    topic: topic
                }
            });
        };

        $scope.openUpdateDialog = function (topic) {
            $http({
                method: "GET",
                url: "/topic/examineTopicConfig.query",
                params:{
                    topic:topic
                }
            }).success(function (resp) {
                if(resp.status ==0){
                    $scope.openCreateOrUpdateDialog(resp.data);
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        };

        $scope.openCreateOrUpdateDialog = function (request) {
            var bIsUpdate = true;

            if(request == null){
                request = [{
                    writeQueueNums:16,
                    readQueueNums:16,
                    perm:6,
                    order:false,
                    topicName:"",
                    brokerNameList:[]
                }];
                bIsUpdate = false;
            }
            $http({
                method: "GET",
                url: "/cluster/list.query"
            }).success(function (resp) {
                if(resp.status ==0){
                    console.log(resp);
                    ngDialog.open({
                        template: 'topicModifyDialog',
                        controller: 'topicModifyDialogController',
                        data:{
                            topicRequestList:request,
                            allBrokerNameList:Object.keys(resp.data.brokerServer),
                            bIsUpdate:bIsUpdate
                        }
                    });
                }
            });
        }

        $scope.openAddDialog = function () {
            $scope.openCreateOrUpdateDialog(null);
        }

        });

    module.controller('topicModifyDialogController', function ($scope, ngDialog, $http,Notification) {
        $scope.postTopicRequest = function (topicRequestItem) {
            console.log(topicRequestItem);
            var request = JSON.parse(JSON.stringify(topicRequestItem));
            console.log(request);
            $http({
                method: "POST",
                url: "/topic/createOrUpdate.do",
                data:request
            }).success(function (resp) {
                if(resp.status ==0){
                    Notification.info({message: "success!", delay: 2000});
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }
        }
    );
    module.controller('consumerResetOffsetDialogController', function ($scope, ngDialog, $http,Notification) {
            $scope.timepicker = {};
            $scope.timepicker.date = moment().format('YYYY-MM-DD HH:mm');
            $scope.timepicker.options = {format: 'YYYY-MM-DD HH:mm', showClear: true};
            $scope.resetOffset = function () {
                console.log($scope.timepicker.date);
                console.log($scope.timepicker.date.valueOf());
                console.log($scope.ngDialogData.selectedConsumerGroup);
                $http({
                    method: "POST",
                    url: "/consumer/resetOffset.do",
                    data: {
                        resetTime: $scope.timepicker.date.valueOf(),
                        consumerGroupList: $scope.ngDialogData.selectedConsumerGroup,
                        topic:$scope.ngDialogData.topic,
                        force:true
                    }
                }).success(function (resp) {
                    if(resp.status ==0){
                        ngDialog.open({
                            template: 'resetOffsetResultDialog',
                            data:resp.data
                        });
                    }else {
                        Notification.error({message: resp.errMsg, delay: 2000});
                    }
                })
            }
        }
    );

    module.controller('sendTopicMessageDialogController', function ($scope, ngDialog, $http,Notification) {
        $scope.sendTopicMessage = {
            topic: $scope.ngDialogData.topic,
            key: "key",
            tag:"tag",
            messageBody:"messageBody"
        };
        $scope.send = function () {
            $http({
                method: "POST",
                url: "/topic/sendTopicMessage.do",
                data: $scope.sendTopicMessage
            }).success(function (resp) {
                if(resp.status ==0){
                    ngDialog.open({
                        template: 'sendResultDialog',
                        data:resp.data
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            })
        }
        }

    );

    module.controller('routerViewDialogController', function ($scope, ngDialog, $http,Notification) {
            $scope.deleteTopicByBroker = function (broker) {
                $http({
                    method: "POST",
                    url: "/topic/deleteTopicByBroker.do",
                    params: {brokerName:broker.brokerName,topic:$scope.ngDialogData.topic}
                }).success(function (resp) {
                    if(resp.status ==0){
                        Notification.info({message: "delete success", delay: 2000});
                    }else {
                        Notification.error({message: resp.errMsg, delay: 2000});
                    }
                })
            };
        }

    );
}).call(this);
