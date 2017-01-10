/**
 * Created by tcrow on 2016/12/2 0002.
 */
'use strict';
var app = angular.module('app', [
    'ngAnimate',
    'ngCookies',
    'ngRoute',
    'pascalprecht.translate'
]).run(
        ['$rootScope','$location','$cookies',
            function ($rootScope,$location,$cookies) {
                var filter = function(url){
                    var outFilterArrs = []
                    outFilterArrs.push("/login");
                    outFilterArrs.push("/reg");
                    outFilterArrs.push("/logout");
                    outFilterArrs.push("/404");
                    var flag = false;
                    $.each(outFilterArrs,function(i,value){
                        if(url.indexOf(value) > -1){
                            flag = true;
                            return false;
                        }
                    });
                    return flag;
                }

                // if(angular.isDefined($cookies.get("isLogin")) && $cookies.get("isLogin") == 'true'){
                //     chatApi.login();
                // }


                $rootScope.$on('$routeChangeSuccess', function(evt, current, previous) {
                    // if(angular.isUndefined($rootScope.userInfo)){
                    //     $rootScope.userInfo = {};
                    // }
                    // if(angular.isDefined($cookies.get("isLogin")) && $cookies.get("isLogin") == 'true'){
                    //     $rootScope.userInfo.path = $location.url();
                    //     $rootScope.userInfo.userLevel = $cookies.get("userLevel");
                    //     $rootScope.userInfo.phone = $cookies.get("phone").indexOf("00086") == 0 ? $cookies.get("phone").replace("00086",""):$cookies.get("phone");
                    //     $rootScope.userInfo.authInfo = $cookies.get("authInfo");
                    //     $rootScope.userInfo.isLogin = $cookies.get("isLogin");
                    //     $rootScope.userInfo.fullName = $cookies.get("fullName");
                    //     var loginInfo = JSONbig.parse($cookies.get("loginInfo"));
                    //     $rootScope.userInfo.logo = angular.isDefined(loginInfo.enterprise)? loginInfo.enterprise.logo:'/bootstrap-dashbord/img/faces/marc.jpg';
                    //     $rootScope.userInfo.system_uid = angular.isDefined(loginInfo.user)? loginInfo.user.systemUid.toString():'';
                    // }

                    //初始化material UI控件
                    $.material.init();
                });

                //路由跳转检查过滤器，未登陆的用户直接跳转到登陆页面，已登陆但未实名用户跳转到实名页面
                $rootScope.$on('$routeChangeStart',function (evt, next,current) {
                    //登陆和注册页面豁免检查是否登陆状态
//                     if(angular.isUndefined($rootScope.userInfo)){
//                         $rootScope.userInfo = {};
//                     }
//                     if(filter($location.url())){
//                         $rootScope.userInfo.showLeft = false;
//                         return;
//                     }else{
//                         $rootScope.userInfo.showLeft = true;
//                     }
//                     if(angular.isDefined($cookies.get("loginInfo")) ){
//                         //判断是否完善企业信息，未完善直接跳转完善页面
//                         if($location.url().indexOf("/user/modifyInfo") != 0 && $cookies.get("isSetEnterpriseInfo") == "false"){
//                             //todo 为方便开发和测试，暂时不强制跳转实名认证页面，生产环境需要删除
// //                            $location.path("/user/modifyInfo");
//                         }
//                     }else{
//                         $location.path("/login");
//                     }
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

app.provider('getDictName', function () {

    var dictList = [];

    this.init = function () {
        var url = "/src/data/dict.json";//无法使用common服务类，地址只能写死
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

app.config(['$routeProvider', '$httpProvider','$cookiesProvider','getDictNameProvider','$sceProvider','$translateProvider',
    function ($routeProvider, $httpProvider ,$cookiesProvider,getDictNameProvider,$sceProvider,$translateProvider) {
        //关闭html校验，存在安全隐患，但目前没问题，使用ng-bind-html需要注意，防止跨站攻击
        $sceProvider.enabled(false);
        //前端字典项目初始化
        getDictNameProvider.init();

        /**
         * 内部方法用于销毁Alert框
         * @param ele
         * @param showTime
         */
        var removeAlertMsg = function(ele,showTime){
            if(showTime > -1){
                setTimeout(function(){
                    $(ele).remove();
                },showTime);
            }
        }

        var alertMsg = function (classes,errorMsg) {
            $(".alertModal").modal();
            var alertEle = new Date().getTime(); //标记警告框
            $(".alertModalBody").prepend('<div class="alert alert-dismissible ' + classes + ' ' + alertEle +'">\
                        <button type="button" class="close" data-dismiss="alert">×</button>\
                        <strong>'+ errorMsg + '</strong>\
                        </div>'
            )
            removeAlertMsg("." +alertEle,5000);
        }

        //设置ajax默认配置
        $.ajaxSetup({
            type: "POST",
            contentType: 'application/json',
            timeout : 5000, //超时时间设置，单位毫秒
            converters:{
                "text json": JSONbig.parse
            },
            beforeSend:function (req) {
//                $("#loadingModal").modal('show',{backdrop:'static',keyboard:false});
            },
            complete:function (xhr, ts) {
                // $("#loadingModal").modal('hide');
//                var count = 0;
//                var thread = setInterval(function () {
//                     if($("#loadingModal").data('bs.modal').isShown){
//                         $("#loadingModal").modal('hide');
//                         clearInterval(thread);
//                     }
//                     count++;
//                     if(count > 10){
//                         clearInterval(thread);
//                     }
//                },500);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                alertMsg("alert-danger",'系统开小差了，请稍后再试');
            }
        });

        //todo 设置cookies默认参数，目前不做特殊配置，直接使用默认配置
        // var now = new Date();
        // var time = now.getTime();
        // var expireTime = time + 1000*36000;
        // now.setTime(expireTime);
        // $cookiesProvider.defaults.domain = "gojobnow.mrdwy.com";
        // $cookiesProvider.defaults.expires  = now.toGMTString();
        // $cookiesProvider.defaults.path = '/';
        // $cookiesProvider.defaults.secure = true;

        // 头部配置
        // $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded;charset=utf-8';
        // $httpProvider.defaults.headers.post['Accept'] = 'application/json, text/javascript, */*; q=0.01';
        // $httpProvider.defaults.headers.post['X-Requested-With'] = 'XMLHttpRequest';

        /**
         * 重写angular的param方法，使angular使用jquery一样的数据序列化方式  The workhorse; converts an object to x-www-form-urlencoded serialization.
         * @param {Object} obj
         * @return {String}
         */
        var param = function (obj) {
            var query = '', name, value, fullSubName, subName, subValue, innerObj, i;

            for (name in obj) {
                value = obj[name];

                if (value instanceof Array) {
                    for (i = 0; i < value.length; ++i) {
                        subValue = value[i];
                        fullSubName = name + '[' + i + ']';
                        innerObj = {};
                        innerObj[fullSubName] = subValue;
                        query += param(innerObj) + '&';
                    }
                }
                else if (value instanceof Object) {
                    for (subName in value) {
                        subValue = value[subName];
                        fullSubName = name + '[' + subName + ']';
                        innerObj = {};
                        innerObj[fullSubName] = subValue;
                        query += param(innerObj) + '&';
                    }
                }
                else if (value !== undefined && value !== null)
                    query += encodeURIComponent(name) + '=' + encodeURIComponent(value) + '&';
            }

            return query.length ? query.substr(0, query.length - 1) : query;
        };

        // Override $http service's default transformRequest
        $httpProvider.defaults.transformRequest = [function (data) {
            return angular.isObject(data) && String(data) !== '[object File]' ? param(data) : data;
        }];

        $routeProvider.when('/', {
            templateUrl: '/view/pages/index.html'
        }).when('/demo', {
            templateUrl: '/view/pages/demo.html',
            controller:'DemoCtrl'
        }).when('/404', {
            templateUrl: '/404'
        }).otherwise('/404');

        $translateProvider.translations('en',en);
        $translateProvider.translations('zh',zh);
        $translateProvider.preferredLanguage('en');
        $translateProvider.useCookieStorage();

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