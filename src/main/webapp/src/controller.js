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

app.controller('dashboardCtrl', ['$scope','$translate','$filter','Notification','remoteApi','tools', function ($scope,$translate,$filter,Notification,remoteApi,tools) {

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
                if(i > 9){
                    return false;
                }
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

    var initBrokerLineChart = function(legend,data){
        var series = [];
        var xAxisData = [];
        var flag = true;
        $.each(data,function(key,value){
            var tps = [];
            $.each(value,function(i,tpsValue){
                var tpsArray = tpsValue.split(",");
                if(flag){
                    xAxisData.push($filter('date')(tpsArray[0], "HH:mm:ss"));
                }
                tps.push(tpsArray[1]);
            })
            flag = false;
            var serie = {
                name:key,
                type:'line',
                smooth:true,
                symbol: 'none',
                sampling: 'average',
                data: tps
            }
            series.push(serie);
        })

        var option = {
            legend: {
                data: legend
            },
            color: ["#FF0000", "#00BFFF", "#FF00FF", "#1ce322", "#000000", '#EE7942'],
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: xAxisData
            },
            yAxis: [{
                type: 'value',
                axisLabel: {
                    formatter: '{value} '
                }
            }],
            series: series
        };
        return option;
    }

    var myChart = echarts.init(document.getElementById('line'));

    $scope.isStart = false;

    setInterval(function () {
        if(!$scope.isStart){
            $scope.isStart = true;
            myChart.setOption({
                title: {
                    text: $scope.BROKER_TITLE + ' 5min line'
                },
                toolbox: {
                    feature: {
                        dataZoom: {
                            yAxisIndex: 'none'
                        },
                        restore: {},
                        saveAsImage: {}
                    }
                },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        animation: false
                    }
                },
                yAxis: {
                    type: 'value',
                    boundaryGap: [0, '100%'],
                    axisLabel: {
                        formatter: '{value} '
                    },
                    splitLine: {
                        show: false
                    }
                },
                dataZoom: [{
                    type: 'inside',
                    start: 90,
                    end: 100
                }, {
                    start: 0,
                    end: 10,
                    handleIcon: 'M10.7,11.9v-1.3H9.3v1.3c-4.9,0.3-8.8,4.4-8.8,9.4c0,5,3.9,9.1,8.8,9.4v1.3h1.3v-1.3c4.9-0.3,8.8-4.4,8.8-9.4C19.5,16.3,15.6,12.2,10.7,11.9z M13.3,24.4H6.7V23h6.6V24.4z M13.3,19.6H6.7v-1.4h6.6V19.6z',
                    handleSize: '80%',
                    handleStyle: {
                        color: '#fff',
                        shadowBlur: 3,
                        shadowColor: 'rgba(0, 0, 0, 0.6)',
                        shadowOffsetX: 2,
                        shadowOffsetY: 2
                    }
                }],
                legend: {
                    data: []
                },
                xAxis: {
                    type: 'time',
                    boundaryGap: false,
                    data: []
                },
                series: []
            })
        }
        remoteApi.queryBrokerHisData('2017-01-01',function(resp){
            var data = {}
            var xAxisData = [];
            $.each(resp.data,function(address,values){
                data[address] = values;
                xAxisData.push(address);
            })
            myChart.setOption(initBrokerLineChart(xAxisData,data));
        })

    }, tools.dashboardRefreshTime);




}]);


