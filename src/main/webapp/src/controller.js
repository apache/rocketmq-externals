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

app.controller('dashboardCtrl', ['$scope','$translate','Notification','remoteApi','tools', function ($scope,$translate,Notification,remoteApi,tools) {

    $translate('BROKER').then(function (broker) {
        $scope.BROKER_TITLE = broker;
    }, function (translationId) {
        $scope.BROKER_TITLE = translationId;
    });

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

            //sort the brokerArray
            $scope.brokerArray.sort(function(broker1,broker2){
                var tps1 = parseFloat(broker1.getTotalTps.split(' ')[0]);
                var tps2 = parseFloat(broker2.getTotalTps.split(' ')[0]);
                return tps2-tps1;
            });

            var xAxisData = [],
                data = [];

            $.each($scope.brokerArray,function(i,broker){
                xAxisData.push(broker.address);
                data.push(broker.getTotalTps.split(' ')[0]);
            })
            initChart(xAxisData,data);
        }else{
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    }

    var initChart = function(xAxisData,data){
        var myChart = echarts.init(document.getElementById('main'));
        // 指定图表的配置项和数据
        var option = {
            title: {
                text: $scope.BROKER_TITLE + ' TOP 10'
            },
            tooltip: {},
            legend: {
                data:['TPS']
            },
            grid: {
                x: 40,
                x2: 100,
                y2: 150,
            },
            axisPointer : {
                type : 'shadow'
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

    remoteApi.queryClusterList(callback);

    var initBrokerLineChart = function(xAxisData,data){
        var myChart = echarts.init(document.getElementById('line'));
        // 指定图表的配置项和数据
        var option = {
            title: {
                text: '动态数据 + 时间坐标轴'
            },
            tooltip: {
                trigger: 'axis',
                formatter: function (params) {
                    params = params[0];
                    var date = new Date(params.name);
                    return date.getDate() + '/' + (date.getMonth() + 1) + '/' + date.getFullYear() + ' : ' + params.value[1];
                },
                axisPointer: {
                    animation: false
                }
            },
            xAxis: {
                type: 'time',
                splitLine: {
                    show: false
                }
            },
            yAxis: {
                type: 'value',
                boundaryGap: [0, '100%'],
                splitLine: {
                    show: false
                }
            },
            series: [{
                name: '模拟数据',
                type: 'line',
                showSymbol: false,
                hoverAnimation: false,
                data: data
            }]
        };

        // 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
    }

    remoteApi.queryBrokerHisData('2017-01-01',function(resp){
        console.info(resp);
    })

}]);


