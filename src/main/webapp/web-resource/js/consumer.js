(function () {
    var module = angular.module('consumerApp', ['tm.pagination', 'ngDialog','localytics.directives','ui-notification']);
    module.run(function ($http) {
        $http.defaults.headers.common = {'X-Requested-With': 'XMLHttpRequest'};
    });
    module.controller('consumerController', function ($scope, ngDialog, $http,Notification) {
        $scope.paginationConf = {
            currentPage: 1,
            totalItems: 0,
            itemsPerPage: 10,
            pagesLength: 15,
            perPageOptions: [10],
            rememberPerPage: 'perPageItems',
            onChange: function () {
                $scope.showConsumerGroupList(this.currentPage,this.totalItems);
            }
        };
        $scope.allConsumerGrouopList = [];
        $scope.consumerGrouoShowList = [];
        $http({
            method: "GET",
            url: "/consumer/groupList.query"
        }).success(function (resp) {
            if(resp.status ==0){
                $scope.allConsumerGrouopList = resp.data;
                console.log($scope.allConsumerGrouopList);
                console.log(JSON.stringify(resp));
                $scope.showConsumerGroupList(1,$scope.allConsumerGrouopList.length);
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
        $scope.filterStr="";
        $scope.$watch('filterStr', function() {
            $scope.filterList(1);
        });
        $scope.filterList = function (currentPage) {
            var lowExceptStr =  $scope.filterStr.toLowerCase();
            var canShowList = [];
            $scope.allConsumerGrouopList.forEach(function(element) {
                console.log(element)
                if (element.group.toLowerCase().indexOf(lowExceptStr) != -1){
                    canShowList.push(element);
                }
            });
            $scope.paginationConf.totalItems =canShowList.length;
            var perPage = $scope.paginationConf.itemsPerPage;
            var from = (currentPage - 1) * perPage;
            var to = (from + perPage)>canShowList.length?canShowList.length:from + perPage;
            $scope.consumerGrouoShowList = canShowList.slice(from, to);
        };


        $scope.showConsumerGroupList = function (currentPage,totalItem) {
            if($scope.filterStr != ""){
                $scope.filterList(currentPage);
                return;
            }
            var perPage = $scope.paginationConf.itemsPerPage;
            var from = (currentPage - 1) * perPage;
            var to = (from + perPage)>totalItem?totalItem:from + perPage;
            $scope.consumerGrouoShowList = $scope.allConsumerGrouopList.slice(from, to);
            $scope.paginationConf.totalItems = totalItem ;
            console.log($scope.consumerGrouoShowList)
            console.log($scope.paginationConf.totalItems)
        };
        $scope.openAddDialog = function () {
            $scope.openCreateOrUpdateDialog(null);
        };
        $scope.openCreateOrUpdateDialog = function(request){
            var bIsUpdate = true;
            if(request == null){
                request = [{
                    brokerNameList: [],
                    subscriptionGroupConfig: {
                        groupName: "",
                        consumeEnable: true,
                        consumeFromMinEnable: true,
                        consumeBroadcastEnable: true,
                        retryQueueNums: 1,
                        retryMaxTimes: 16,
                        brokerId: 0,
                        whichBrokerWhenConsumeSlowly: 1
                    }
                }];
                bIsUpdate = false;
            }
            console.log(request);
            $http({
                method: "GET",
                url: "/cluster/list.query"
            }).success(function (resp) {
                if(resp.status ==0){
                    console.log(resp);
                    ngDialog.open({
                        template: 'consumerModifyDialog',
                        controller: 'consumerModifyDialogController',
                        data:{
                            consumerRequestList:request,
                            allBrokerNameList:Object.keys(resp.data.brokerServer),
                            bIsUpdate:bIsUpdate
                        }
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        };
        $scope.detail = function(consumerGroupName){
            $http({
                method: "GET",
                url: "/consumer/queryTopicByConsumer.query",
                params:{consumerGroup:consumerGroupName}
            }).success(function (resp) {
                if(resp.status ==0){
                    console.log(resp);
                    ngDialog.open({
                        template: 'consumerTopicViewDialog',
                        // controller: 'addTopicDialogController',
                        data:{consumerGroupName:consumerGroupName,data:resp.data}
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        };

        $scope.client = function(consumerGroupName){
            $http({
                method: "GET",
                url: "/consumer/consumerConnection.query",
                params:{consumerGroup:consumerGroupName}
            }).success(function (resp) {
                if(resp.status ==0){
                    console.log(resp);
                    ngDialog.open({
                        template: 'clientInfoDialog',
                        // controller: 'addTopicDialogController',
                        data:{data:resp.data,consumerGroupName:consumerGroupName}
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        };
        $scope.updateConfigDialog = function(consumerGroupName){
            $http({
                method: "GET",
                url: "/consumer/examineSubscriptionGroupConfig.query",
                params:{consumerGroup:consumerGroupName}
            }).success(function (resp) {
                if(resp.status ==0){
                    console.log(resp);
                    $scope.openCreateOrUpdateDialog(resp.data);
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });


        };
        $scope.delete = function(consumerGroupName){
            $http({
                method: "GET",
                url: "/consumer/fetchBrokerNameList.query",
                params:{
                    consumerGroup:consumerGroupName
                }
            }).success(function (resp) {
                if(resp.status ==0){
                    console.log(resp);

                    ngDialog.open({
                        template: 'deleteConsumerDialog',
                        controller: 'deleteConsumerDialogController',
                        data:{
                            // allClusterList:Object.keys(resp.data.clusterInfo.clusterAddrTable),
                            allBrokerNameList:resp.data,
                            consumerGroupName:consumerGroupName
                        }
                    });
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }

    })

    module.controller('deleteConsumerDialogController', function ($scope, ngDialog, $http,Notification) {
            $scope.selectedClusterList = [];
            $scope.selectedBrokerNameList = [];
            $scope.delete = function () {
                console.log($scope.selectedClusterList);
                console.log($scope.selectedBrokerNameList);
                console.log($scope.ngDialogData.consumerGroupName);
                $http({
                    method: "POST",
                    url: "/consumer/deleteSubGroup.do",
                    data:{groupName:$scope.ngDialogData.consumerGroupName,
                        brokerNameList:$scope.selectedBrokerNameList}
                }).success(function (resp) {
                    if(resp.status ==0){
                        Notification.info({message: "delete success!", delay: 2000});
                    }else {
                        Notification.error({message: resp.errMsg, delay: 2000});
                    }
                });
            }
        }
    );

    module.controller('consumerModifyDialogController', function ($scope, ngDialog, $http,Notification) {
        $scope.postConsumerRequest = function (consumerRequest) {
            var request = JSON.parse(JSON.stringify(consumerRequest));
            console.log(request);
            $http({
                method: "POST",
                url: "/consumer/createOrUpdate.do",
                data:request
            }).success(function (resp) {
                if(resp.status ==0){
                    Notification.info({message: "update success!", delay: 2000});
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }
        }
    );


}).call(this);
