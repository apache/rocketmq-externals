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
'use strict';
var initFlag = false;
var loginFlag = false;
var app = angular.module('app', [
    'ngAnimate',
    'ngCookies',
    'ngRoute',
    'ngDialog',
    'ngMaterial',
    'ngSanitize',
    'material.svgAssetsCache',
    'ui-notification',
    'tm.pagination',
    'ae-datetimepicker',
    'localytics.directives',
    'pascalprecht.translate'
]).run(
        ['$rootScope','$location','$cookies','$http', '$window','Notification',
            function ($rootScope,$location,$cookies,$http, $window, Notification) {
                var init = function(callback){
                    if (initFlag) return;
                    initFlag = true;

                    var url =  '/login/check.query';
                    var setting = {
                                type: "GET",
                                timeout:15000,
                                success:callback,
                                async:false
                            }
                     //sync invoke
                     $.ajax(url,setting)
                }
                console.log('initFlag0='+ initFlag + ' loginFlag0==='+loginFlag);

                $rootScope.$on('$locationChangeStart', function (event, next, current) {
                   // redirect to login page if not logged in and trying to access a restricted page
                   init(function(resp){
                          if (resp.status == 0) {
                            // console.log('resp.data==='+resp.data);
                            var loginInfo = resp.data;
                            loginFlag = loginInfo.loginRequired;
                            if (!loginInfo.logined) {
                              $window.sessionStorage.clear();
                            }
                          }else {
                             Notification.error({message: "" + resp.errMsg, delay: 2000});
                          }
                   });

                   console.log('initFlag='+ initFlag + ' loginFlag==='+loginFlag);
                   $rootScope.username = '';
                   if (loginFlag || loginFlag == "true") {
                        var username = $window.sessionStorage.getItem("username");

                        if (username != null) {
                          $rootScope.username = username;
                        }

                        // console.log("username " + $rootScope.username);
                        var restrictedPage = $.inArray($location.path(), ['/login']) === -1;
                        if (restrictedPage && !username) {
                          var callback = $location.path();
                          $location.path('/login');
                        }
                   }

                  });


                $rootScope.$on('$routeChangeSuccess', function() {
                    var pathArray = $location.url().split("/");
                    var index = pathArray.indexOf("");
                    if(index >= 0){
                        pathArray.remove(index);
                    }
                    $rootScope.path = pathArray[0];

                    //初始化material UI控件
                    $.material.init();
                });

                $rootScope.$on('$routeChangeStart',function (evt, next,current) {
                    window.clearInterval($rootScope._thread);
                })
            }
        ]
    ).animation('.view', function () {
        return {
            animate: function (element, className, from, to, done) {
                //styles
            }
        }
    });

app.factory('abc', function ($http, $window) {
console.log('xxxxxxx');
                    $http({
                         method: "GET",
                         url: "/login/check.query"
                     }).success(function (resp) {
                         if (resp.status == 0) {
                             alert(resp.data)
                         }
                     });
                     return 1;
});

app.provider('getDictName', function () {

    var dictList = [];

    this.init = function () {
        var url = "src/data/dict.json";//无法使用common服务类，地址只能写死
        var params = {};
        $.get(url, params, function (ret) {
            dictList = ret;
        })
    }

    this.$get = function () {
        return function (dictType, value) {
            for (var i = 0; i < dictList.length; i++) {
                var dict = dictList[i];
                if (dict.TYPE == dictType && dict.DICT_VALUE == value) {
                    return dict.DICT_NAME;
                }
            }
        }
    }
})

app.config(['$routeProvider', '$httpProvider','$cookiesProvider','getDictNameProvider','$sceProvider','$translateProvider','$mdThemingProvider',
    function ($routeProvider, $httpProvider ,$cookiesProvider,getDictNameProvider,$sceProvider,$translateProvider,$mdThemingProvider) {
        //关闭html校验，存在安全隐患，但目前没问题，使用ng-bind-html需要注意，防止跨站攻击
        $sceProvider.enabled(false);
        //前端字典项目初始化
        getDictNameProvider.init();

        //init angular
        $mdThemingProvider.theme('default')
            .primaryPalette('pink')
            .accentPalette('light-blue');


        //设置ajax默认配置
        $.ajaxSetup({
            type: "POST",
            contentType: 'application/json',
            cache:false,
            timeout : 5000, //超时时间设置，单位毫秒
            converters:{
                "text json": JSONbig.parse
            }
        });

        // check login status


        $httpProvider.defaults.cache = false;

        $routeProvider.when('/', {
            templateUrl: 'view/pages/index.html',
            controller:'dashboardCtrl'
        }).when('/login', {
            templateUrl: 'view/pages/login.html',
            controller:'loginController'
        }).when('/cluster', {
            templateUrl: 'view/pages/cluster.html',
            controller:'clusterController'
        }).when('/topic', {
            templateUrl: 'view/pages/topic.html',
            controller:'topicController'
        }).when('/consumer', {
            templateUrl: 'view/pages/consumer.html',
            controller:'consumerController'
        }).when('/producer', {
            templateUrl: 'view/pages/producer.html',
            controller:'producerController'
        }).when('/message', {
            templateUrl: 'view/pages/message.html',
            controller:'messageController'
        }).when('/messageTrace', {
            templateUrl: 'view/pages/messageTrace.html',
            controller:'messageTraceController'
        }).when('/ops', {
            templateUrl: 'view/pages/ops.html',
            controller:'opsController'
        }).when('/404', {
            templateUrl: 'view/pages/404.html'
        }).otherwise('/404');

        $translateProvider.translations('en',en);
        $translateProvider.translations('zh',zh);
        $translateProvider.preferredLanguage('en');
        $translateProvider.useCookieStorage();
//        $translateProvider.useSanitizeValueStrategy('sanitize');

    }]);

app.filter('range', function() {
    return function(input, range) {
        var total = parseInt(range.totalPage) + 1;
        var count = 5;
        for (var i = range.start; i<total; i++) {
            if(count > 0){
                input.push(i);
                count -- ;
            }else {
                break;
            }
        }
        return input;
    };
});


app.filter('dict',['getDictName',function(getDictName){
    return function(value,type){
        return getDictName(type,value);
    }
}])

/**
 * 数组扩展方法，移除数组中某一元素或某一段元素
 * @param from 需要移除元素的索引开始值（只传一个参数表示单独移除该元素）
 * @param to 需要移除元素的索引结束值
 * @returns {*}
 */
Array.prototype.remove = function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};

/**
 * 根据元素值查询数组中元素的索引
 * @param val
 * @returns {number}
 */
Array.prototype.indexOf = function(val) {
    for (var i = 0; i < this.length; i++) {
        if (this[i] == val) return i;
    }
    return -1;
};