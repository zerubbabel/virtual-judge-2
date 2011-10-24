var cid;	//current contest
var cids;	//cid concerned in rank
var startTime;	//contest start time locally
var selectedTime;	//slider
var ti;	//Time Info
var tabs;
var slider;
var problemSet = {};
var statusTable;
var hash;
var oldProblemHash = "#problem/A";
var oldStatusHash;
var rankTable;
var replays = {};
var rankUpdater;
var sliderUpdater;
var statusTimeoutInstance = {};	//status fetch
var oFH;

$(function(){
	
	///////////////////// miscellaneous ///////////////////////
	
	cid = $("#cid").val();

	DWREngine.setAsync(false);
	judgeService.getContestTimeInfo(cid, function(res){
		startTime = new Date().valueOf();
		ti = res;
		$("#time_total span").text(dateFormat(ti[0]));
	});
	DWREngine.setAsync(true);

	/////////////////////   Slider    //////////////////////

	var curTime, exceedMax;
	slider = $( "#time_controller" ).slider({
		range: "min",
		min: 0,
		max: ti[0],
		value: 0,
		start: function() {
			clearInterval(sliderUpdater);
			clearTimeout(rankUpdater);
			curTime = new Date().valueOf();
			sliderUpdater = 0;
		},
		slide: function( event, ui ) {
			if (ui.value > ti[1] + curTime - startTime) {
				exceedMax = true;
				return false;
			}
		},
		stop: function( event, ui ) {
			if (ui.value > ti[1] + curTime - startTime) {
				exceedMax = true;
			}
			if (exceedMax || location.hash.indexOf("#rank") != 0) {
				resetTimeSlider();
				exceedMax = false;
				if (location.hash.indexOf("#rank") == 0 && location.hash != "#rank") {
					location.hash = "#rank";
				}
			} else {
				selectedTime = parseInt(ui.value);
				displayTime();
				location.hash = "#rank/" + selectedTime;
			}
		}
	});
	
	/////////////////////   Tabs    //////////////////////

	tabs = $("#contest_tabs").tabs({
		select: function(event, ui) {
			if (location.hash.indexOf(ui.tab.rel) != 0) {
				if (ui.tab.rel == "#problem") {
					location.hash = oldProblemHash;
				} else {
					location.hash = ui.tab.rel;
				}
			}
			//deal with rank update
			if (location.hash.indexOf("#rank") != 0) {
				clearTimeout(rankUpdater);
				$("div.FixedHeader_Cloned").hide();
			}
		}
	});
	
	/////////////////////   Overview    //////////////////////
	$('#viewContest').dataTable({
		"bPaginate": false,
		"bLengthChange": false,
		"bFilter": false,
		"bSort": false,
		"bInfo": false,
		"bAutoWidth": false
	});
	
	$("span.plainDate").each(function(){
		$(this).html(new Date(parseInt($(this).html())).format("yyyy-MM-dd hh:mm:ss"));
	});
	
	$("#contest_opt a").button();
	
	$("#clone_contest").click(function(){
		var url = this.href;
		doIfLoggedIn(function(){
			location.href = url;
		}, url);
		return false;
	});
	
	/////////////////////    Problem    //////////////////////

	$("#problem_opt").find("a").button();

	$(":radio[name=problem_number]").each(function(){
		$(this).next().html($(this).val());
	});
	
	$("input[name=problem_number]").change(function(){
		location.hash = "#problem/" + $(this).val();
	});
	
	$("#problem_number_container").buttonset().show();
	
	$("#desc_index").change(function(){
		var num = $(":input[name=problem_number]:checked").val();
		var did = $("#desc_index > option:eq(" + this.value + ")").attr("did");
		showDescription(num, this.value);
		$.post("contest/appointDescription.action?cid=" + cid + "&num=" + num + "&id=" + did);
	});
	
	$("#submit").click(function(){
		doIfLoggedIn(function(){
			var num = $(":input[name=problem_number]:checked").val();
			var problem = problemSet[num];
			if (!!problem) {
				$( "#submit_num" ).html(num + " - " + problem.title);
				var languageSelect = $("select#language");
				languageSelect.html("");
				for (i in problem.languageList) {
					languageSelect.append("<option value='" + i + "'>" + problem.languageList[i] + "</option>");
				}
				if ($.cookie("lang_" + problem.oj)) {
					$("select#language").val($.cookie("lang_" + problem.oj));
				}
				$( "#dialog-form-submit" ).dialog("open");
			}
		});
		return false;
	});
	
	$( "#dialog-form-submit" ).dialog({
		autoOpen: false,
		height: 600,
		width: 600,
		position: ['top', 50],
		modal: true,
		buttons: {
			"Submit": function() {
				var num = $(":input[name=problem_number]:checked").val();
				var data = {
					cid: cid,
					num: num,
					language: $("[name=language]").val(),
					isOpen: $(":input[name=isOpen]:checked").val(),
					source: $("[name=source]").val()
				};
				$.cookie("lang_" + problemSet[num].oj, $("select#language").val(), {expires:30, path:'/'});
				$.post('contest/submit.action', data, function(res) {
					if (res == "success") {
						$( "#dialog-form-submit" ).dialog( "close" );
						showStatus();
						$("#reset").trigger("click");
					} else if (res == "practice") {
						$( "#dialog-form-submit" ).dialog( "close" );
						location.href = "problem/status.action";
					} else {
						updateTips(res);
					}
				});
			},
			"Cancel": function() {
				$( this ).dialog( "close" );
			}
		},
		close: function() {
			$("p.validateTips").html("");
			$( this ).find("textarea").val("");
		}
	});

	/////////////////////    Status     //////////////////////
	
	$("#num").prepend("<option value='-'>All</option>");
	$("[name=num]").val("-");
	$("#reset, #filter").button();
	
	$("#form_status").submit(function(){
		var oldHash = location.hash;
		$("[name='un']").val($("[name='un']").val().replace(/%\d\d|\s+/g, ''));
		location.hash = "#status/" + $("[name='un']").val() + "/" + $("[name='num']").val() + "/" + $("[name='res']").val();
		if (location.hash == oldHash) {
			statusTable.fnPageChange( 'first' );
		}
		return false;
	});
	
	$("#reset").click(function(){
		var oldHash = location.hash;
		location.hash = "#status//-/0";
		if (location.hash == oldHash) {
			statusTable.fnPageChange( 'first' );
		}
		return false;
	});
	
	$("a.rejudge").live("click", function(){
		var $row = $(this).parent().parent();
		var id = $row.attr("id");
		$row.removeClass("no");
		$row.removeClass("yes");
		$row.addClass("pending");
		$.post("problem/rejudge.action", {id: id}, function() {
			getResult(id);
		});
		return false;
	});
	
	/////////////////////     Rank      //////////////////////

	if (!$.browser.msie) {
		$("td.meta_td").live({
			mouseenter: function() {
				var curCid = $(this).parent().attr("cid");
				$("tr[cid=" + curCid + "] td.meta_td").addClass("same_td");
			},
			mouseleave:	function() {
				var curCid = $(this).parent().attr("cid");
				$("tr[cid=" + curCid + "] td.meta_td").removeClass("same_td");
			}
		});
	}

	$("td.penalty_td").live({
		mouseenter: function() {
			$(this).text($(this).attr("v0"));
		},
		mouseleave:	function() {
			$(this).text($(this).attr("v1"));
		}
	});
	
	$("tr.disp").live({
		mouseenter: function() {
			$(this).css("background-color", "#CCEEFF")
		},
		mouseleave:	function() {
			$(this).css("background-color", "transparent");
		}
	});

	$( "#dialog-form-rank-setting" ).dialog({
		autoOpen: false,
		width: 950,
		position: ['top', 50],
		modal: true,
		buttons: {
			"Save": function() {
				var ids = cid;
				$("[name=ids]:checked").each(function(){
					ids += "_" + $(this).val();
				});
				var showTeams = $.browser.msie ? 0 : $("[name=showTeams]:checked").val();
				var showNick = $("[name=showNick]:checked").val();

				$.cookie("contest_" + cid, ids, { expires: 3 });
				$.cookie("show_all_teams", showTeams, { expires: 30 });
				$.cookie("show_nick", showNick, { expires: 30 });
				$( this ).dialog( "close" );
				showRank();
			},
			"Cancel": function() {
				$( this ).dialog( "close" );
			}
		}
	});	
	
	$("#rank_setting").button().click(function(){
		var $inst = $( "#dialog-form-rank-setting" )
		$inst.dialog('open');
		if (!$inst.html()) {
			$inst.load("contest/showRankSetting.action?cid=" + cid);
		}
	});

	$(window).scroll(adjustRankTool);

	$("#img_find_me").click(function(){
		$.scrollTo( $("tr.my_tr")[0], 800, {offset: {top:115-$(window).height(), left:0} } );
		return false;
	});

	$("#img_go_top").click(function(){
		$.scrollTo( {top: '0px',left:'0px'}, 800 );
		return false;
	});
	
	
	//////////////////////////////////////////////////////////

	$(window).hashchange( function(){
		hash = location.hash.split("/");
		if (hash[0] == "#problem") {
			showProblem();
		} else if (hash[0] == "#status") {
			showStatus();
		} else if (hash[0] == "#rank") {
			showRank();
		} else {
			showOverview();
		}
	}).hashchange();
	
	$("#contest_tabs").show();

});

function showOverview() {
	tabs.tabs( "select" , "overview" );
	resetTimeSlider();
}

function showProblem() {
	tabs.tabs( "select" , "problem" );
	resetTimeSlider();

	oldProblemHash = location.hash;
	var $numRadio = $("#problem_number_container > input[value=" + hash[1] + "]");
	if ($numRadio.length) {
		$numRadio.prop("checked", "checked");
		$("#problem_number_container").buttonset("refresh");
	}
	var num = $("#problem_number_container input:checked").val();
	if (!problemSet[num]) {
		$.ajax({
			url: "contest/showProblem.action?cid=" + cid + "&num=" + num,
			type: 'get',
			async: false,
			success: function(data) {
				problemSet[num] = data;
			}
		});
	}
	var problem = problemSet[num];
	$("#problem_title").html("<span style='color:green'>" + num + " - </span>" + problem.title);
	$("span.crawlInfo").hide();
	if (problem.timeLimit == 1) {
		$("#crawling").show();
	} else if (problem.timeLimit == 2) {
		$("#crawlFailed").show();
	} else {
		$("#timeLimit").html(problem.timeLimit);
		$("#memoryLimit").html(problem.memoryLimit);
		$("#_64IOFormat").html(problem._64IOFormat);
		$("#problem_status").attr("href", "contest/view.action?cid=" + cid + "#status//" + num + "/0");
		if (problem.pid) {
			$("#problem_practice").attr("href", "problem/viewProblem.action?id=" + problem.pid);
		}
		if (problem.originProblemNumber) {
			$("#problem_origin").button("destroy");
			$("#problem_origin").attr("href", problem.originURL).text(problem.originProblemNumber);
			$("#problem_origin").button();
		}
		$("#crawlSuccess").show();
	}
	$("#desc_index").html("");
	for (i in problem.descriptions) {
		if (problem.descriptions[i].author == '0') {
			problem.descriptions[i].author = "System Crawler";
		}
		$("#desc_index").append("<option did='" + problem.descriptions[i].id + "' value='" + i + "'>" + problem.descriptions[i].author + "  (" + problem.descriptions[i].updateTime + ")" + "</option>");
	}
	showDescription(num, problem.desc_index);
}

function showStatus() {
	tabs.tabs( "select" , "status" );
	resetTimeSlider();

	if (hash.length >= 4) {
		$("[name='un']").val(hash[1]);
		$("[name='num']").val(hash[2]);
		$("[name='res']").val(hash[3]);
	}
	
	if (!statusTable) {
		statusTable = $('#table_status').dataTable({
			"bProcessing": true,
			"bServerSide": true,
			"sAjaxSource": "contest/fetchStatus.action?cid=" + cid,
			"iDisplayLength": 20,
			"bLengthChange": false,
			"bFilter": false,
			"bSort": false,
			"bInfo": false,
			"bAutoWidth": false,
			"bStateSave": true,
			"sPaginationType": "full_numbers",
	
			"aoColumns": [
				{},
				{
					"fnRender": function ( oObj ) {
						return "<a href='user/profile.action?uid=" + oObj.aData[9] + "'>" + oObj.aData[1] + "</a>";
					}
				},
				{
					"fnRender": function ( oObj ) {
						return "<a href='contest/view.action?cid=" + cid + "#problem/" + oObj.aData[2] + "'>" + oObj.aData[2] + "</a>";
					}
				},
				{
					"fnRender": function ( oObj ) {
						var info = oObj.aData[3] == 'Judging Error 1' || oObj.aData[3] == 'Judging Error 2' && $("[name='isSup']").val() != 0 ? oObj.aData[3] + " <a href='#' class='rejudge' ><img border=0 height='15' src='images/refresh.png'/></a>" : oObj.aData[3];
						if (oObj.aData[12]) {
							info = "<a href='contest/fetchSubmissionInfo.action?id=" + oObj.aData[0] + "' rel='facebox'>" + info + "</a>";
						}
						return info;
					},
					"sClass": "result"
				},
				{
					"fnRender": function ( oObj ) {
						return oObj.aData[3] == 'Accepted' ? oObj.aData[4] + " KB" : "";
					},
					"sClass": "memory"
				},
				{ 
					"fnRender": function ( oObj ) {
						return oObj.aData[3] == 'Accepted' ? oObj.aData[5] + " ms" : "";
					},
					"sClass": "time"
				},
				{ 
					"fnRender": function ( oObj ) {
						return oObj.aData[10] ? "<a " + (oObj.aData[10] == 2 ? "class='shared'" : "") + " target='_blank' href='contest/viewSource.action?id=" + oObj.aData[0] + "'>" + oObj.aData[6] + "</a>" : oObj.aData[6];
					},
					"sClass": "language"
				},
				{
					"fnRender": function ( oObj ) {
						return oObj.aData[7] + " B";
					},
					"sClass": "length"
				},
				{
					"fnRender": function ( oObj ) {
						return new Date(parseInt(oObj.aData[8])).format("yyyy-MM-dd hh:mm:ss");
					},
					"sClass": "date"
				},
				{"bVisible": false},
				{"bVisible": false},
				{"bVisible": false},
				{"bVisible": false}
			],
			"fnServerData": function ( sSource, aoData, fnCallback ) {
				var un = $("[name='un']").val();
				var num = $("[name='num']").val();
				var res = $("[name='res']").val();
			
				aoData.push( { "name": "un", "value": un } );
				aoData.push( { "name": "num", "value": num } );
				aoData.push( { "name": "res", "value": res } );

				$.ajax( {
					"dataType": 'json', 
					"type": "POST", 
					"url": sSource, 
					"data": aoData, 
					"success": fnCallback
				} );
			},
			"fnRowCallback": function( nRow, aData, iDisplayIndex, iDisplayIndexFull ) {
				$(nRow).addClass(aData[3]=="Accepted" ? "yes" : aData[3].indexOf("ing") < 0 || aData[3].indexOf("rror") >= 0 ? "no" : "pending");
				$(nRow).attr("id", aData[0]);
				$('a[rel=facebox]', $(nRow)).facebox({
					loadingImage : 'facebox/loading.gif',
					closeImage   : 'facebox/closelabel.png'
				});
				if ($(nRow).hasClass("pending")){
					getResult(aData[0]);
				}
				return nRow;
			}
		});
		$("#table_status_last").remove();
	} else if (location.hash != oldStatusHash) {
		statusTable.fnPageChange( 'first' );
		oldStatusHash = location.hash;
	}
}

function showRank() {
	tabs.tabs( "select" , "rank" );
	clearInterval(sliderUpdater);
	if (/#rank\/\d+/.test(location.hash) == false) {
		resetTimeSlider();
	}

	if (!isNaN(hash[1]) && hash[1] >= 0) {
		if (hash[1] <= Math.min(ti[1] + new Date().valueOf() - startTime, ti[0])) {
			selectedTime = parseInt(hash[1]);
			slider.slider("value", selectedTime);
			displayTime();
		}
	}
	
	if ($.cookie("contest_" + cid) == undefined){
		$.cookie("contest_" + cid, cid, { expires: 3 });
	}
	if ($.cookie("show_all_teams") == undefined){
		$.cookie("show_all_teams", 0, { expires: 30 });
	}
	if ($.cookie("show_nick") == undefined){
		$.cookie("show_nick", 0, { expires: 30 });
	}
	
	if (selectedTime >= 0) {
		updateRankInfo();
	} else {
		$("div.FixedHeader_Cloned").show();
	}

}

function displayTime() {
	var ratio = Math.max(selectedTime, 0) / ti[0];
	$("#time_index").css("width", (2 + 100 * ratio) + "%");
	if (ratio > 0.93) {
		$("#time_total span").text("");
		$("#time_index span").text(dateFormat(selectedTime == ti[0] ? ti[0] : selectedTime - ti[0]));
	} else {
		$("#time_total span").text(dateFormat(ti[0]));
		$("#time_index span").text(dateFormat(selectedTime));
	}
};

function updateRankInfo() {
	cids = $.cookie("contest_" + cid).split("_");

	var cnt = 0;
	for (var i = 0; i < cids.length; i++) {
		if (replays[cids[i]] == undefined) {
			replays[cids[i]] = {};
			judgeService.getRankInfo(cids[i], function(res){
				var curCid = res[0];
				replays[curCid].dataURL = res[1];
				replays[curCid].endTime = new Date().valueOf() + res[2];	//locally
				replays[curCid].beginTime = res[3];	//server side
				replays[curCid].length = res[4];
				replays[curCid].lastFetchTime = 0;
				if (++cnt == cids.length) {
					updateRankData();
				}
			});
		} else if (++cnt == cids.length) {
			updateRankData();
		}
	}
}

function updateRankData() {
	var cnt = 0;
	for (var i = 0; i < cids.length; i++) {
		if (!replays[cids[i]].data || replays[cids[i]].lastFetchTime < Math.min(startTime + selectedTime - ti[1], replays[cids[i]].endTime)) {
			$.getJSON(replays[cids[i]].dataURL + "?" + new Date().valueOf(), function(rankData) {
				var curCid = rankData[0];
				replays[curCid].data = rankData;
				replays[curCid].lastFetchTime = new Date().valueOf();
				if (++cnt == cids.length){
					calcRankTable();
				}
			});
		} else if (++cnt == cids.length){
			calcRankTable();
		}
	}
}

function calcRankTable() {
	var pnum = $("table#viewContest tr").length - 1;
	var sb = {}
	var firstSolveTime = [];
	var totalSubmission = [];
	var correctSubmission = [];
	var myStatus = [];
	var username = {};
	var nickname = {};
	var my_uid_cid = $("#my_account").attr("uid") + "_" + cid;
	
	for (var j = 0; j < pnum; ++j) {
		totalSubmission[j] = correctSubmission[j] = 0;
	}
	$.each(cids, function(i, curCid) {
		if (isNaN(curCid)) {
			return;
		}
		$.each(replays[curCid].data, function(key, s) {
			if (key == 0) {
				return;
			} else if (key == 1) {
				for (uid in s) {
					var name = s[uid];
					username[uid] = name[0];
					nickname[uid] = name[1];
				}
				return;
			}
			if (s[3] * 1000 > selectedTime)return;
			var name = s[0] + "_" + curCid;
			if (!sb[name]){
				sb[name] = [];
			}
			if (sb[name][s[1]] == undefined){
				sb[name][s[1]] = [-1, 0];
			}
			if (sb[name][s[1]][0] < 0){
				totalSubmission[s[1]]++;
				if (s[2]) {
					sb[name][s[1]][0] = s[3];
					if (firstSolveTime[s[1]] == undefined || s[3] < firstSolveTime[s[1]]) {
						firstSolveTime[s[1]] = s[3];
					}
					correctSubmission[s[1]]++;
				} else {
					sb[name][s[1]][1]++;
				}
			}
		});
	});

	var result = [];
	for (name in sb){
		var solve = 0, penalty = 0;
		for (i in sb[name]){
			if (sb[name][i]) {
				if (sb[name][i][0] >= 0) {
					if (name == my_uid_cid) {
						myStatus[i] = 2;
					}
					solve++;
					penalty += sb[name][i][0] + 1200 * sb[name][i][1];
				} else if (name == my_uid_cid) {
					myStatus[i] = 1;
				}
			}
		}
		result.push([name, solve, penalty]);
	}
	result.sort(function(a, b){
		return b[1] - a[1] || a[2] - b[2];
	});
	
	var showNick = $.cookie("show_nick");
	var showAllTeams = $.cookie("show_all_teams");
	var sbHtml = [];
	for (var i = 0; i < result.length; ++i) {
		var curInfo = result[i];
		var splitIdx = curInfo[0].lastIndexOf("_");
		var uid = curInfo[0].substr(0, splitIdx);
		var curCid = curInfo[0].substr(splitIdx + 1);
		if (showAllTeams == 0 && i >= 50 && (cid != curCid || !username[uid])) {
			continue;
		}
		sbHtml.push("<tr class='disp");
		if (cid == curCid) {
			sbHtml.push(" cur_tr");
			if (my_uid_cid == curInfo[0]) {
				sbHtml.push(" my_tr");
			}
		}
		sbHtml.push("' style='background:transparent");
		sbHtml.push(";' cid='" + curCid + "'><td class='meta_td'>" + (i + 1) + "</td><td class='meta_td");
		if (username[uid]) {
			sbHtml.push("'><a target='_blank' href='user/profile.action?uid=" + uid + "'>" + (showNick > 0 ? nickname[uid] || username[uid] : username[uid]) + "</a></td><td class='meta_td");
		} else {
			sbHtml.push(" replay'>" + uid + "</td><td class='meta_td");
		}
		var penaltyInHMS = dateFormat(curInfo[2], 0, 1);
		var penaltyInMinute = dateFormat(curInfo[2], 1, 1);
		sbHtml.push("'>" + curInfo[1] + "</td><td class='meta_td penalty_td' v0='" + penaltyInHMS + "' v1='" + penaltyInMinute + "'>" + penaltyInMinute + "</td>");

		var thisSb = sb[curInfo[0]];
		for (var j = 0; j <= pnum; ++j) {
			var probInfo = thisSb[j];
			if (!probInfo) {
				sbHtml.push("<td />");
			} else {
				sbHtml.push("<td ");
				if (probInfo[0] < 0) {
					sbHtml.push("class='red'");
				} else if (firstSolveTime[j] == probInfo[0]) {
					sbHtml.push("class='solvedfirst'");
				} else {
					sbHtml.push("class='green'");
				}
				sbHtml.push(">" + dateFormat(probInfo[0], 0, 1) + "<br />" + (probInfo[1] ? "<span>(-" + probInfo[1] + ")</span>" : "　") + "</td>");
			}
		}
		sbHtml.push("</tr>");
	}
	$("#rank_tbody").html(sbHtml.join(""));

	sbHtml = [];
	sbHtml.push("<tr style='background:transparent'><th/><th/><th/><th/>");
	var maxCorrectNumber = 0, totalNumber = 0, totalCorrectNumber = 0;
	for (var j = 0; j < pnum; ++j) {
		totalNumber += totalSubmission[j];
		totalCorrectNumber += correctSubmission[j];
		if (maxCorrectNumber < correctSubmission[j]) {
			maxCorrectNumber = correctSubmission[j];
		}
	}
	for (var j = 0; j < pnum; ++j) {
		if (!totalSubmission[j]) {
			sbHtml.push("<th style='background:transparent'/>");
		} else {
			var ratio = maxCorrectNumber ? correctSubmission[j] / maxCorrectNumber : 0.0;
			sbHtml.push("<th style='background-color:" + grayDepth(ratio) + ";color:" + (ratio < .5 ? "black" : "white") + "'>" + (myStatus[j] == 2 ? "<img src='images/yes.png' height='20'/>" : myStatus[j] == 1 ? "<img src='images/no.png' height='20'/>" : "　") + 	"<br />" + correctSubmission[j] + "/" + totalSubmission[j] + "<br />" + Math.floor(100 * correctSubmission[j] / totalSubmission[j]) + "%</th>")
		}
	}
	if (totalNumber) {
		sbHtml.push("<th style='background-color:#D3D6FF'>　<br />" + totalCorrectNumber + "/" + totalNumber + "<br />" + Math.floor(100 * totalCorrectNumber / totalNumber) + "%</th></tr>");
	} else {
		sbHtml.push("<th/>");
	}
	$("#rank_tfoot").html(sbHtml.join(""));

	if (!oFH) {
		oFH = new FixedHeader( document.getElementById('rank_table'), { "bottom": true } );
	} else {
		oFH.fnUpdate();
	}
	$("div.FixedHeader_Cloned").show();
	
	adjustRankTool();
	
	if (sliderUpdater) {
		clearTimeout(rankUpdater);
		rankUpdater = setTimeout(updateRankInfo, 15000);
	}
}

function showDescription(num, desc_index) {
	var problem = problemSet[num];
	var description = problem.descriptions[desc_index];
	$("[name=desc_index]").val(desc_index);
	$("div.hiddable").hide();
	for (elem in description){
		if (description[elem]){
			$("#vj_" + elem).show();
			$("#vj_" + elem + " div").html(description[elem]);
		}
	}
	problem.desc_index = desc_index;
}

function getResult(id){
	judgeService.getResult(id, cb);
}

function cb(back){
	var id = back[0];
	var result = back[1];
	var memory = back[2];
	var time = back[3];
	var info = back[4];
	var $row = $("#" + id);
	if ($row.length){
		if (info) {
			result = "<a href='problem/fetchSubmissionInfo.action?id=" + id + "' rel='facebox'>" + result + "</a>";
		}
		$(".result", $row).html(result);
		$('a[rel=facebox]', $row).facebox({
			loadingImage : 'facebox/loading.gif',
			closeImage   : 'facebox/closelabel.png'
		});
		if (result.indexOf("ing") >= 0 && result.indexOf("rror") < 0){
			clearTimeout(statusTimeoutInstance[id]);
			statusTimeoutInstance[id] = setTimeout("getResult(" + id + ")", 3000);
		} else if (result == "Accepted"){
			$row.removeClass("pending");
			$row.addClass("yes");
			$(".memory", $row).html(memory + " KB");
			$(".time", $row).html(time + " ms");
		} else {
			$row.removeClass("pending");
			$row.addClass("no");
		}
	}
}

function grayDepth(ratio) {
	var res = (Math.floor((1 - ratio) * 0xff) * 0x010101).toString(16);
	while (res.length < 6) res = '0' + res;
	return "#" + res;
}

function dateFormat(time, formatIdx, inSeconds){
	var sign = "";
	if (inSeconds != 1)	time /= 1000;
	if (time == -1)return "　";
	if (time < 0) {
		time = -time;
		sign = "-";
	}
	if (formatIdx == 1){
		return sign + Math.floor(time / 60);
	} else {
		var h = Math.floor(time / 3600);
		var m = Math.floor(time % 3600 / 60);
		var s = Math.floor(time % 60 + 0.5);
		return sign + h + ":" + (m<10?"0":"") + m + ":" + (s<10?"0":"") + s;
	}
}

function comfirmDeleteContest(cid){
	if (confirm("Sure to delete this contest?")){
		location = 'contest/deleteContest.action?cid=' + cid;
	}
}

function resetTimeSlider () {
	clearInterval(sliderUpdater);
	var temp = function(){
		selectedTime = Math.min(ti[1] + new Date().valueOf() - startTime, ti[0]);
		slider.slider("value", Math.max(selectedTime, 0));
		displayTime();
		if (selectedTime > 0 && selectedTime < 1000 && ti[0] > 0) {
			window.location.reload();
		}
	}
	temp();
	sliderUpdater = setInterval(temp, 1000);
};

function isScrolledIntoView(elem) {
    var docViewTop = $(window).scrollTop();
    var docViewBottom = docViewTop + $(window).height();

    var elemTop = $(elem).offset().top;
    var elemBottom = elemTop + $(elem).height();

    return ((elemBottom >= docViewTop) && (elemTop <= docViewBottom));
}

function adjustRankTool() {
	var $myRow = $("tr.my_tr");
	if ($myRow.length && !isScrolledIntoView($myRow[0])) {
		$("#img_find_me").css("visibility", "visible");
	} else {
		$("#img_find_me").css("visibility", "hidden");
	}

	if (!isScrolledIntoView("#contest_title")) {
		$("#img_go_top").css("visibility", "visible");
	} else {
		$("#img_go_top").css("visibility", "hidden");
	}
}
