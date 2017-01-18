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
app.controller('AppCtrl', ['$scope','$rootScope','$cookies','$location','$translate', function ($scope,$rootScope,$cookies,$location,$translate) {
    $scope.changeTranslate = function(langKey){
        $translate.use(langKey);
    }
}]);

app.controller('dashboardCtrl', ['$scope','Notification','remoteApi','tools', function ($scope,Notification,remoteApi,tools) {

    var callback = function (resp) {
        if (resp.status == 0) {
            var clusterMap = resp.data.clusterInfo.clusterAddrTable;
            var brokerMap = resp.data.clusterInfo.brokerAddrTable;
            var brokerDetail = resp.data.brokerServer;
            var clusterMap = tools.generateBrokerMap(brokerDetail,clusterMap,brokerMap);
            $scope.brokerArray = [];
            $.each(clusterMap,function(clusterName,brokers){
                $.each(brokers,function(i,broker){
                    $scope.brokerArray.push(broker)
                })
            })

            var array = [1.1,1.5,1.3];

            var sort = array.sort(function(value1,value2){
                if(value1 < value2){
                    return value1;
                }
            });

            console.info(sort);
            console.info($scope.brokerArray)

        }else{
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    }

    remoteApi.queryClusterList(callback);

    // remoteApi.queryTopic(function(resp){
    //     if(resp.status ==0){
    //         $scope.topicList = resp.data.topicList;
    //         var xAxisData = $scope.topicList;
    //         var data = [5, 20, 36, 10, 10, 20,5, 20, 36, 10];
    //         initChart(xAxisData,data);
    //     }else {
    //         Notification.error({message: resp.errMsg, delay: 2000});
    //     }
    // })

    var initChart = function(xAxisData,data){
        var myChart = echarts.init(document.getElementById('main'));
        // 指定图表的配置项和数据
        var option = {
            title: {
                text: 'TOPIC TOP 10'
            },
            tooltip: {},
            legend: {
                data:['TPS']
            },
            grid: { // 控制图的大小，调整下面这些值就可以，
                x: 40,
                x2: 100,
                y2: 150,// y2可以控制 X轴跟Zoom控件之间的间隔，避免以为倾斜后造成 label重叠到zoom上
            },
            axisPointer : {            // 坐标轴指示器，坐标轴触发有效
                type : 'shadow'        // 默认为直线，可选为：‘line‘ | ‘shadow‘
            },
            xAxis: {
                data: xAxisData,
                axisLabel: {
                    inside: false,
                    textStyle: {
                        color: '#000000'
                    },
                    rotate: 60,
                    interval:0
                },
                axisTick: {
                    show: true
                },
                axisLine: {
                    show: true
                },
                z: 10
            },
            yAxis: {},
            series: [{
                name: 'TPS',
                type: 'bar',
                data: data
            }]
        };

        // 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
    }




}]);


