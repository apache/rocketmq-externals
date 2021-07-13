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
const PRODUCER_COLOR = '#029e02'
const SUCCESS_COLOR = '#75d874';
const ERROR_COLOR = 'red';
const TRANSACTION_COMMIT_COLOR = SUCCESS_COLOR;
const TRANSACTION_ROLLBACK_COLOR = ERROR_COLOR;
const TRANSACTION_UNKNOWN_COLOR = 'grey'
const TIME_FORMAT_PATTERN = "YYYY-MM-DD HH:mm:ss.SSS";
const DEFAULT_DISPLAY_DURATION = 10 * 1000
// transactionTraceNode do not have costTime, assume it cost 50ms
const transactionCheckCostTime = 50;
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
        $scope.displayGraph = false;
        $scope.graphButtonName = 'Show Graph';
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
                startTime = +messageTraceGraph.producerNode.traceNode.beginTimeStamp;
                endTime = +messageTraceGraph.producerNode.traceNode.endTimeStamp;
            } else {
                messageTraceGraph.subscriptionNodeList.forEach(subscriptionNode => {
                    subscriptionNode.consumeNodeList.forEach(consumeNode => {
                        startTime = Math.min(startTime, consumeNode.beginTimeStamp);
                    })
                })
            }

            function buildNodeColor(traceNode, index) {
                let nodeColor = SUCCESS_COLOR;
                if (traceNode.transactionState) {
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
                if (traceNode.status !== 'success') {
                    nodeColor = ERROR_COLOR;
                }
                if (index === messageGroups.length - 1) {
                    nodeColor = PRODUCER_COLOR;
                }
                return nodeColor;
            }

            function formatXAxisTime(value) {
                let duration = Math.max(0, value - startTime);
                if (duration < 1000)
                    return duration + 'ms';
                duration /= 1000;
                if (duration < 60)
                    return duration + 's';
                duration /= 60;
                if (duration < 60)
                    return duration + 'm';
                duration /= 60;
                return duration + 'h';
            }

            function buildTraceInfo(itemName, itemValue) {
                if (itemValue) {
                    return `${itemName}: ${itemValue}<br />`
                }
                return "";
            }

            function formatNodeToolTip(params) {
                let traceNode = params.data.traceData.traceNode;
                return `
                        costTime: ${traceNode.costTime}ms<br />
                        status: ${traceNode.status}<br />
                        beginTimeStamp: ${new moment(traceNode.beginTimeStamp).format(TIME_FORMAT_PATTERN)}<br />
                        endTimeStamp: ${new moment(traceNode.endTimeStamp).format(TIME_FORMAT_PATTERN)}<br />
                        clientHost: ${traceNode.clientHost}<br />
                        storeHost: ${traceNode.storeHost}<br />
                        retryTimes: ${traceNode.retryTimes}<br />
                        ${buildTraceInfo('msgType', traceNode.msgType)}
                        ${buildTraceInfo('transactionId', traceNode.transactionId)}
                        ${buildTraceInfo('transactionState', traceNode.transactionState)}
                        ${buildTraceInfo('fromTransactionCheck', traceNode.fromTransactionCheck)}
                        `;
            }

            function addTraceData(traceNode, index) {
                data.push({
                    value: [
                        index,
                        traceNode.beginTimeStamp,
                        traceNode.endTimeStamp,
                        traceNode.costTime
                    ],
                    itemStyle: {
                        normal: {
                            color: buildNodeColor(traceNode, index),
                            opacity: 1
                        }
                    },
                    traceData: {
                        traceNode: traceNode
                    }
                });
                endTime = Math.max(traceNode.endTimeStamp, endTime);
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
                    transactionNode.beginTimeStamp = Math.max(messageTraceGraph.producerNode.traceNode.endTimeStamp,
                        transactionNode.endTimeStamp - transactionCheckCostTime);
                    addTraceData(transactionNode, producerNodeIndex)
                    endTime = Math.max(endTime, transactionNode.endTimeStamp);
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
                        text: `${api.value(3)}ms`,
                        textFill: '#fff'
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
            $scope.displayGraph = !$scope.displayGraph;
            if ($scope.displayGraph) {
                $scope.graphButtonName = 'Hide Graph';
                $scope.displayMessageTraceGraph($scope.ngDialogData);
            } else {
                $scope.messageTraceGraph.dispose();
                $scope.graphButtonName = 'Show Graph';
            }
            console.log("here is my data", $scope.ngDialogData)
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