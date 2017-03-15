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
app.service('remoteApi', ['$http','tools', function ($http,tools) {
    var queryTopic = function(callback){
        $http({
            method: "GET",
            url: "topic/list.query"
        }).success(callback);
    }

    var queryClusterList = function(callback){
        $http({
            method: "GET",
            url: "cluster/list.query"
        }).success(callback);
    }

    var queryBrokerHisData = function(date,callback){
        var url = 'dashboard/broker.query';
        var data = {date:date};
        var setting = {
            type: "GET",
            data:data,
            timeout:15000,//data is too large,so master set time out is long enough
            success:callback
        }
        $.ajax(url,setting)
    }

    var queryTopicHisData = function(date,topicName,callback){
        var url =  'dashboard/topic.query';
        var data = {date:date,topicName:topicName};
        var setting = {
            type: "GET",
            data:data,
            timeout:15000,//data is too large,so master set time out is long enough
            success:callback
        }
        $.ajax(url,setting)
    }

    var queryTopicCurrentData = function(callback){
        var url = 'dashboard/topicCurrent';
        var setting = {
            type: "GET",
            timeout:15000,//data is too large,so master set time out is long enough
            success:callback
        }
        $.ajax(url,setting)
    }


    return {
        queryTopic:queryTopic,
        queryClusterList:queryClusterList,
        queryBrokerHisData:queryBrokerHisData,
        queryTopicHisData:queryTopicHisData,
        queryTopicCurrentData:queryTopicCurrentData
    }
}])

