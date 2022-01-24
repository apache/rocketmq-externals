var reg = /\[issue[\ ]+\#([0-9]+)\]/gi;

var res_arr = []
var res_arr_not_sured = []

$(document).ready(function(){
	// var el = $('<p>这是一个P标签</p>');
	// console.log($(".new-discussion-timeline")[0].children[0].children[0].children[1].children[1])
	// el.after($(".new-discussion-timeline")[0].children[0].children[0].children[1].children[1])
	var $pull_requests = $("a[data-hovercard-type='pull_request']");
	if($pull_requests.length <=0 ){
		LOG("这个页面没有找到任何pr和issue")
		return;
	}
	var index=0, issue_id = 0;
	var release_title="", pr_href = "" , issue_href="";
	
	res_arr.push("RocketMQ plugin start:\n")
	res_arr.push("## Feature");
	res_arr.push("<ul>");
	res_arr.push("\n\n");
	res_arr.push("</ul>");
	res_arr.push("</ul>\n");
	res_arr.push("## Improvement\n<ul>");
	
	for(var $pr of $pull_requests){
		index++;
		issue_id = get_issue_id_by_pr_title($pr.text);
		if (!issue_id){
			continue;
		}

		release_title = "[ISSUE-" + issue_id+ "] - " + $pr.text.replace(reg, "").trim();
		pr_href = "https://github.com" + $pr.href;
		issue_href = "https://github.com/apache/rocketmq/issues/" + issue_id;
		res_arr.push("<li>[<a href='"+issue_href+"'>ISSUE-"+issue_id+"</a>] - "+ release_title+"</li>");
	}
	res_arr.push("</ul>\n");
	res_arr.push("## Bug");
	res_arr.push("<ul>\n\n</ul>\n");
	res_arr.push("## Document and code style improvement\n");
	res_arr.push("<ul>\n\n</ul>");
	res_arr.push("\n");

	if(res_arr_not_sured.length>0){
		res_arr.push("Issues can't be read cause of wrong title format, format it as: [ISSUE #issue_id] xxx, then refresh this page.\n");
		res_arr.push(res_arr_not_sured.join("\n"))
	}
	
	res_arr.push("\n")
	res_arr.push("RocketMQ plugin done:\n")
	LOG(res_arr.join("\n"));
}); 

function get_issue_id_by_pr_title(pr_title){
	var result = "";
	if (!reg.test(pr_title)){
		res_arr_not_sured.push("[wrong format title for issue] " + pr_title.trim())
	}else{
		result = pr_title.match(reg)[0].match(/[0-9]+/)[0]
	}
	return result;
}
function LOG(msg){
	console.log(msg)
}