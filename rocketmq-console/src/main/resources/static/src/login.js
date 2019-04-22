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

app.controller('loginController', ['$scope','$location','$http','Notification','$cookies','$window', function ($scope,$location,$http,Notification,$cookies, $window) {
    $scope.login = function () {
        if(!$("#username").val()) {
    		alert("用户名不能为空");
    		return;
    	}
    	if(!$("#password").val()) {
    		alert("密码不能为空");
    		return;
    	}

        $http({
            method: "POST",
            url: "login/login.do",
            params:{username:$("#username").val(), password:$("#password").val()}
        }).success(function (resp) {
            if (resp.status == 0) {
                Notification.info({message: 'Login successful, redirect now', delay: 2000});
                $window.sessionStorage.setItem("username", $("#username").val());
                //alert("XXXXX resp.data="+resp.data.sessionId);
                //$window.sessionStorage.setItem("sessionId", resp.data.sessionId);
                window.location = "/";
                initFlag = false;
            } else{
                Notification.error({message: resp.errMsg, delay: 2000});
            }
        });
    };
}]);
