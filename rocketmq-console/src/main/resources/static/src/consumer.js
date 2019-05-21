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

module.controller('consumerController', ['$scope', 'ngDialog', '$http','Notification',function ($scope, ngDialog, $http,Notification) {
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
    $scope.sortKey = null;
    $scope.sortOrder=1;
    $scope.intervalProcessSwitch = false;
    $scope.intervalProcess = null;
    $scope.allConsumerGrouopList = [];
    $scope.consumerGroupShowList = [];
    $scope.sortByKey = function (key) {
        $scope.paginationConf.currentPage=1;
        $scope.sortOrder = -$scope.sortOrder;
        $scope.sortKey = key;
        $scope.doSort();
    };

    $scope.doSort = function (){// todo  how to change this fe's code ? (it's dirty)
        if($scope.sortKey == 'diffTotal'){
            $scope.allConsumerGrouopList.sort(function(a,b) {return (a.diffTotal > b.diffTotal) ? $scope.sortOrder : ((b.diffTotal > a.diffTotal) ? -$scope.sortOrder : 0);} );
        }
        if($scope.sortKey == 'group'){
            $scope.allConsumerGrouopList.sort(function(a,b) {return (a.group > b.group) ? $scope.sortOrder : ((b.group > a.group) ? -$scope.sortOrder : 0);} );
        }
        if($scope.sortKey == 'count'){
            $scope.allConsumerGrouopList.sort(function(a,b) {return (a.count > b.count) ? $scope.sortOrder : ((b.count > a.count) ? -$scope.sortOrder : 0);} );
        }
        if($scope.sortKey == 'consumeTps'){
            $scope.allConsumerGrouopList.sort(function(a,b) {return (a.consumeTps > b.consumeTps) ? $scope.sortOrder : ((b.consumeTps > a.consumeTps) ? -$scope.sortOrder : 0);} );
        }
        $scope.filterList($scope.paginationConf.currentPage)
    };
    $scope.refreshConsumerData = function () {
        //Show loader
        $('#loaderConsumer').removeClass("hide-myloader");

        $http({
            method: "GET",
            url: "consumer/groupList.query"
        }).success(function (resp) {
            if(resp.status ==0){
                $scope.allConsumerGrouopList = resp.data;
                console.log($scope.allConsumerGrouopList);
                console.log(JSON.stringify(resp));
                $scope.showConsumerGroupList($scope.paginationConf.currentPage,$scope.allConsumerGrouopList.length);

                //Hide loader
                $('#loaderConsumer').addClass("hide-myloader");
            }else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };
    $scope.monitor = function(consumerGroupName){
        $http({
            method: "GET",
            url: "monitor/consumerMonitorConfigByGroupName.query",
            params:{consumeGroupName:consumerGroupName}
        }).success(function (resp) {
            // if(resp.status ==0){
                ngDialog.open({
                    template: 'consumerMonitorDialog',
                    controller: 'consumerMonitorDialogController',
                    data:{consumerGroupName:consumerGroupName,data:resp.data}
                });
            // }else {
            //     Notification.error({message: resp.errMsg, delay: 2000});
            // }
        });
    };


    $scope.$watch('intervalProcessSwitch', function () {
        if ($scope.intervalProcess != null) {
            clearInterval($scope.intervalProcess);
            $scope.intervalProcess = null;
        }
        if ($scope.intervalProcessSwitch) {
            $scope.intervalProcess = setInterval($scope.refreshConsumerData, 10000);
        }
    });


    $scope.refreshConsumerData();
    $scope.filterStr="";
    $scope.$watch('filterStr', function() {
        $scope.paginationConf.currentPage=1;
        $scope.filterList(1)
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
        $scope.consumerGroupShowList = canShowList.slice(from, to);
    };


    $scope.showConsumerGroupList = function (currentPage,totalItem) {
        var perPage = $scope.paginationConf.itemsPerPage;
        var from = (currentPage - 1) * perPage;
        var to = (from + perPage)>totalItem?totalItem:from + perPage;
        $scope.consumerGroupShowList = $scope.allConsumerGrouopList.slice(from, to);
        $scope.paginationConf.totalItems = totalItem ;
        console.log($scope.consumerGroupShowList)
        console.log($scope.paginationConf.totalItems)
        $scope.doSort()
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
            url: "cluster/list.query"
        }).success(function (resp) {
            if(resp.status ==0){
                console.log(resp);
                ngDialog.open({
                    preCloseCallback: function(value) {
                        // Refresh topic list
                        $scope.refreshConsumerData();
                    },
                    template: 'consumerModifyDialog',
                    controller: 'consumerModifyDialogController',
                    data:{
                        consumerRequestList:request,
                        allClusterNameList:Object.keys(resp.data.clusterInfo.clusterAddrTable),
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
            url: "consumer/queryTopicByConsumer.query",
            params:{consumerGroup:consumerGroupName}
        }).success(function (resp) {
            if(resp.status ==0){
                console.log(resp);
                ngDialog.open({
                    template: 'consumerTopicViewDialog',
                    controller: 'consumerTopicViewDialogController',
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
            url: "consumer/consumerConnection.query",
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
            url: "consumer/examineSubscriptionGroupConfig.query",
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
            url: "consumer/fetchBrokerNameList.query",
            params:{
                consumerGroup:consumerGroupName
            }
        }).success(function (resp) {
            if(resp.status ==0){
                console.log(resp);

                ngDialog.open({
                    preCloseCallback: function(value) {
                        // Refresh topic list
                        $scope.refreshConsumerData();
                    },
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

}])
module.controller('consumerMonitorDialogController', function ($scope, ngDialog, $http,Notification) {
        $scope.createOrUpdateConsumerMonitor = function () {
            $http({
                method: "POST",
                url: "monitor/createOrUpdateConsumerMonitor.do",
                params:{consumeGroupName:$scope.ngDialogData.consumerGroupName,
                    minCount:$scope.ngDialogData.data.minCount,
                    maxDiffTotal:$scope.ngDialogData.data.maxDiffTotal}
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


module.controller('deleteConsumerDialogController', ['$scope', 'ngDialog', '$http','Notification',function ($scope, ngDialog, $http,Notification) {
        $scope.selectedClusterList = [];
        $scope.selectedBrokerNameList = [];
        $scope.delete = function () {
            console.log($scope.selectedClusterList);
            console.log($scope.selectedBrokerNameList);
            console.log($scope.ngDialogData.consumerGroupName);
            $http({
                method: "POST",
                url: "consumer/deleteSubGroup.do",
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
    }]
);

module.controller('consumerModifyDialogController', ['$scope', 'ngDialog', '$http','Notification',function ($scope, ngDialog, $http,Notification) {
        $scope.postConsumerRequest = function (consumerRequest) {
            var request = JSON.parse(JSON.stringify(consumerRequest));
            console.log(request);
            $http({
                method: "POST",
                url: "consumer/createOrUpdate.do",
                data:request
            }).success(function (resp) {
                if(resp.status ==0){
                    Notification.info({message: "update success!", delay: 2000});
                }else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        }
    }]
);

module.controller('consumerTopicViewDialogController', ['$scope', 'ngDialog', '$http', 'Notification', function ($scope, ngDialog, $http, Notification) {
        $scope.consumerRunningInfo = function (consumerGroup, clientId, jstack) {
            $http({
                method: "GET",
                url: "consumer/consumerRunningInfo.query",
                params: {
                    consumerGroup: consumerGroup,
                    clientId: clientId,
                    jstack: jstack
                }
            }).success(function (resp) {
                if (resp.status == 0) {
                    ngDialog.open({
                        template: 'consumerClientDialog',
                        data:{consumerClientInfo:resp.data,
                        clientId:clientId}
                    });
                } else {
                    Notification.error({message: resp.errMsg, delay: 2000});
                }
            });
        };
    }]
);



