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
const SUCCESS_COLOR = '#75d874';
const ERROR_COLOR = 'red';
const UNKNOWN_COLOR = 'yellow';
const TRANSACTION_COMMIT_COLOR = SUCCESS_COLOR;
const TRANSACTION_ROLLBACK_COLOR = ERROR_COLOR;
const TRANSACTION_UNKNOWN_COLOR = 'grey'
const TIME_FORMAT_PATTERN = "YYYY-MM-DD HH:mm:ss.SSS";
const DEFAULT_DISPLAY_DURATION = 10 * 1000
// transactionTraceNode do not have costTime, assume it cost 50ms
const TRANSACTION_CHECK_COST_TIME = 50;
module.controller('messageTraceController', ['$scope', '$routeParams', 'ngDialog', '$http', 'Notification', function ($scope, $routeParams, ngDialog, $http, Notification) {
    $scope.allTopicList = [];
    $scope.selectedTopic = [];
    $scope.key = "";
    $scope.messageId = $routeParams.messageId;
    $scope.queryMessageByTopicAndKeyResult = [];
    $scope.queryMessageByMessageIdResult = {};
    $scope.queryMessageTraceListsByTopicAndKeyResult = [];

    $http({
        method: "GET",
        url: "topic/list.query",
        params: {
            skipSysProcess: "true"
        }
    }).success(function (resp) {
        if (resp.status == 0) {
            $scope.allTopicList = resp.data.topicList.sort();
            console.log($scope.allTopicList);
        } else {
            Notification.error({message: resp.errMsg, delay: 2000});
        }
    });
    $scope.timepickerBegin = moment().subtract(1, 'hour').format('YYYY-MM-DD HH:mm');
    $scope.timepickerEnd = moment().add(1, 'hour').format('YYYY-MM-DD HH:mm');
    $scope.timepickerOptions = {format: 'YYYY-MM-DD HH:mm', showClear: true};

    $scope.queryMessageByTopicAndKey = function () {
        console.log($scope.selectedTopic);
        console.log($scope.key);
        $http({
            method: "GET",
            url: "message/queryMessageByTopicAndKey.query",
            params: {
                topic: $scope.selectedTopic,
                key: $scope.key
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                console.log(resp);
                $scope.queryMessageByTopicAndKeyResult = resp.data;
                console.log($scope.queryMessageByTopicAndKeyResult);
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };

    $scope.queryMessageByMessageId = function (messageId, topic) {
        $http({
            method: "GET",
            url: "messageTrace/viewMessage.query",
            params: {
                msgId: messageId,
                topic: topic
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                console.log(resp);
                $scope.queryMessageByMessageIdResult = resp.data;
                console.log($scope.queryMessageByTopicAndKeyResult);
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };

    $scope.queryMessageTraceByMessageId = function (messageId, topic) {
        $http({
            method: "GET",
            url: "messageTrace/viewMessageTraceGraph.query",
            params: {
                msgId: messageId,
                topic: topic
            }
        }).success(function (resp) {
            if (resp.status == 0) {
                console.log(resp);
                ngDialog.open({
                    template: 'messageTraceDetailViewDialog',
                    controller: 'messageTraceDetailViewDialogController',
                    data: resp.data
                });
            } else {
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };
}]);

module.controller('messageTraceDetailViewDialogController', ['$scope', '$timeout', 'ngDialog', '$http', 'Notification', function ($scope, $timeout, ngDialog, $http, Notification) {
        $scope.displayMessageTraceGraph = function (messageTraceGraph) {
            let dom = document.getElementById("messageTraceGraph");
            $scope.messageTraceGraph = echarts.init(dom);
            let option;
            let data = [];
            let dataZoomEnd = 100;
            let startTime = Number.MAX_VALUE;
            let endTime = 0;
            let messageGroups = [];
            if (messageTraceGraph.producerNode) {
                startTime = +messageTraceGraph.producerNode.traceNode.beginTimestamp;
                endTime = +messageTraceGraph.producerNode.traceNode.endTimestamp;
            }

            function buildNodeColor(traceNode) {
                if (traceNode.transactionState != null) {
                    switch (traceNode.transactionState) {
                        case 'COMMIT_MESSAGE':
                            return TRANSACTION_COMMIT_COLOR;
                        case 'ROLLBACK_MESSAGE':
                            return TRANSACTION_ROLLBACK_COLOR;
                        case 'UNKNOW':
                            return TRANSACTION_UNKNOWN_COLOR;
                        default:
                            return ERROR_COLOR;
                    }
                }
                switch (traceNode.status) {
                    case 'failed':
                        return ERROR_COLOR;
                    case 'unknown':
                        return UNKNOWN_COLOR;
                    default:
                        return SUCCESS_COLOR;
                }
            }

            function formatXAxisTime(value) {
                let duration = Math.max(0, value - startTime);
                if (duration < 1000)
                    return timeFormat(duration, 'ms');
                duration /= 1000;
                if (duration < 60)
                    return timeFormat(duration, 's');
                duration /= 60;
                if (duration < 60)
                    return timeFormat(duration, 'min');
                duration /= 60;
                return timeFormat(duration, 'h');
            }

            function timeFormat(duration, unit) {
                return duration.toFixed(2) + unit;
            }


            function buildTraceInfo(itemName, itemValue) {
                if (itemValue) {
                    return `${itemName}: ${itemValue}<br />`
                }
                return "";
            }

            function formatCostTimeStr(costTime) {
                if (costTime < 0) {
                    return "";
                }
                let costTimeStr = costTime;
                if (costTime === 0) {
                    costTimeStr = '<1'
                }
                return `${costTimeStr}ms`;
            }

            function buildCostTimeInfo(costTime) {
                if (costTime < 0) {
                    return "";
                }
                return `costTime: ${formatCostTimeStr(costTime)}<br/>`
            }
            function buildTimeStamp(timestamp){
                if(timestamp < 0){
                    return 'N/A';
                }
                return new moment(timestamp).format(TIME_FORMAT_PATTERN);
            }

            function formatNodeToolTip(params) {
                let traceNode = params.data.traceData.traceNode;
                return `
                        ${buildCostTimeInfo(traceNode.costTime)}
                        status: ${traceNode.status}<br />
                        ${buildTraceInfo('beginTimestamp', buildTimeStamp(traceNode.beginTimestamp))}
                        ${buildTraceInfo('endTimestamp', buildTimeStamp(traceNode.endTimestamp))}
                        clientHost: ${traceNode.clientHost}<br />
                        storeHost: ${traceNode.storeHost}<br />
                        retryTimes: ${traceNode.retryTimes < 0 ? 'N/A' : traceNode.retryTimes}<br />
                        ${buildTraceInfo('msgType', traceNode.msgType)}
                        ${buildTraceInfo('transactionId', traceNode.transactionId)}
                        ${buildTraceInfo('transactionState', traceNode.transactionState)}
                        ${buildTraceInfo('fromTransactionCheck', traceNode.fromTransactionCheck)}
                        `;
            }

            function calcGraphTimestamp(timestamp, relativeTimeStamp, duration, addDuration) {
                if (timestamp > 0) {
                    return timestamp;
                }
                if (duration < 0) {
                    return relativeTimeStamp;
                }
                return addDuration ? relativeTimeStamp + duration : relativeTimeStamp - duration;
            }

            function addTraceData(traceNode, index) {
                if (traceNode.beginTimestamp < 0 && traceNode.endTimestamp < 0) {
                    return;
                }
                let beginTimestamp = calcGraphTimestamp(traceNode.beginTimestamp, traceNode.endTimestamp, traceNode.costTime, false);
                let endTimestamp = calcGraphTimestamp(traceNode.endTimestamp, traceNode.beginTimestamp, traceNode.costTime, true);
                if (endTimestamp === beginTimestamp) {
                    endTimestamp = beginTimestamp + 1;
                }
                console.log("beginTimestamp",beginTimestamp,'endTimestamp',endTimestamp);
                data.push({
                    value: [
                        index,
                        beginTimestamp,
                        endTimestamp,
                        traceNode.costTime
                    ],
                    itemStyle: {
                        normal: {
                            color: buildNodeColor(traceNode),
                            opacity: 1
                        }
                    },
                    traceData: {
                        traceNode: traceNode
                    }
                });
                startTime = Math.min(startTime, beginTimestamp);
                endTime = Math.max(endTime, endTimestamp);
            }

            messageTraceGraph.subscriptionNodeList.forEach(item => {
                messageGroups.push(item.subscriptionGroup)
            })
            messageTraceGraph.subscriptionNodeList.forEach((subscriptionNode, index) => {
                subscriptionNode.consumeNodeList.forEach(traceNode => addTraceData(traceNode, index))
            })
            if (messageTraceGraph.producerNode) {
                messageGroups.push(messageTraceGraph.producerNode.groupName)
                let producerNodeIndex = messageGroups.length - 1;
                addTraceData(messageTraceGraph.producerNode.traceNode, producerNodeIndex);
                messageTraceGraph.producerNode.transactionNodeList.forEach(transactionNode => {
                    transactionNode.beginTimestamp = Math.max(messageTraceGraph.producerNode.traceNode.endTimestamp,
                        transactionNode.endTimestamp - TRANSACTION_CHECK_COST_TIME);
                    addTraceData(transactionNode, producerNodeIndex)
                    endTime = Math.max(endTime, transactionNode.endTimestamp);
                })
            }
            let totalDuration = endTime - startTime;
            if (totalDuration > DEFAULT_DISPLAY_DURATION) {
                dataZoomEnd = DEFAULT_DISPLAY_DURATION / totalDuration * 100
            }

            function renderItem(params, api) {
                let messageGroup = api.value(0);
                let start = api.coord([api.value(1), messageGroup]);
                let end = api.coord([api.value(2), messageGroup]);
                let height = api.size([0, 1])[1] * 0.6;

                let rectShape = echarts.graphic.clipRectByRect({
                    x: start[0],
                    y: start[1] - height / 2,
                    width: Math.max(end[0] - start[0], 1),
                    height: height
                }, {
                    x: params.coordSys.x,
                    y: params.coordSys.y,
                    width: params.coordSys.width,
                    height: params.coordSys.height
                });

                return rectShape && {
                    type: 'rect',
                    transition: ['shape'],
                    shape: rectShape,
                    style: api.style({
                        text: formatCostTimeStr(api.value(3)),
                        textFill: '#000'
                    })
                };
            }

            option = {
                tooltip: {
                    formatter: function (params) {
                        return formatNodeToolTip(params);
                    }
                },
                title: {
                    text: messageTraceGraph.producerNode ? messageTraceGraph.producerNode.topic : "",
                    left: 'center'
                },
                dataZoom: [{
                    type: 'slider',
                    filterMode: 'weakFilter',
                    showDataShadow: false,
                    top: 400,
                    start: 0,
                    end: dataZoomEnd,
                    labelFormatter: ''
                }, {
                    type: 'inside',
                    filterMode: 'weakFilter'
                }
                ],
                grid: {
                    height: 300
                },
                xAxis: {
                    min: startTime,
                    scale: true,
                    axisLabel: {
                        formatter: function (value) {
                            return formatXAxisTime(value)
                        }
                    }
                },
                yAxis: {
                    data: messageGroups
                },
                series: [{
                    type: 'custom',
                    renderItem: renderItem,
                    itemStyle: {
                        opacity: 0.8
                    },
                    encode: {
                        x: [1, 2],
                        y: 0
                    },
                    data: data
                }]
            };
            $scope.messageTraceGraph.setOption(option);
        }
        $scope.showGraph = function () {
            $scope.displayMessageTraceGraph($scope.ngDialogData);
        };

        function initGraph() {
            $timeout(function () {
                if (document.getElementById('messageTraceGraph') == null) {
                    initGraph();
                } else {
                    $scope.showGraph();
                }
            }, 50);
        }

        initGraph();
    }]
);