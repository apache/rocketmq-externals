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

app.service('tools', ['$http', function ($http) {

    var ctx = "";
    var dashboardRefreshTime = 5000; // todo improve. when data size is large,request is too slow

    var generateBrokerMap = function (brokerServer, clusterAddrTable, brokerAddrTable) {
        var map = {};
        $.each(brokerServer, function (brokerName, brokerStatusList) { // broker
            $.each(clusterAddrTable, function (clusterName, brokerNameList) { //clusterAddrTable
                if (angular.isUndefined(map[clusterName])) {
                    map[clusterName] = [];
                }
                $.each(brokerNameList, function (listIndex, clusterBrokerName) {
                    if (clusterBrokerName == brokerName) {
                        $.each(brokerStatusList, function (index, brokerStatus) {
                            brokerStatus.split = brokerName;
                            brokerStatus.index = index;
                            brokerStatus.address = brokerAddrTable[clusterBrokerName].brokerAddrs[index];
                            brokerStatus.brokerName = brokerAddrTable[clusterBrokerName].brokerName;
                            map[clusterName].push(brokerStatus);
                        })
                    }
                })
            })
        });
        return map;
    };

    var fastSort = function (arrayToSort, propertyToSortWith, sortDirection) {
        // temporary holder of position and sort-value
        var map = arrayToSort.map(function (e, i) {
            if (typeof e[propertyToSortWith] === 'string') {
                return { index: i, value: e[propertyToSortWith].toLowerCase() };
            }
            else {
                return { index: i, value: e[propertyToSortWith] };
            }

        })

        // sorting the map containing the reduced values
        map.sort(function (a, b) {
            if (sortDirection === "ascending") {
                return +(a.value > b.value) || +(a.value === b.value) - 1;
            }
            else {
                return +(a.value < b.value) || +(a.value === b.value) - 1;
            }

        });

        // container for the resulting order
        var result = map.map(function (e) {
            return arrayToSort[e.index];
        });
        return result;
    };

    return {
        generateBrokerMap:generateBrokerMap,
        fastSort:fastSort,
        ctx:ctx,
        dashboardRefreshTime:dashboardRefreshTime
    }
}])



