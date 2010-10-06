$(document).ready(function() {
	oTable = $('#listContest').dataTable({
		"bProcessing": true,
		"bServerSide": true,
		"sAjaxSource": "contest/listContest.action",
		"iDisplayLength": 25,
		"bAutoWidth": false,
		"bStateSave": true,
		"aaSorting": [[ 2, "desc" ]],

		"aoColumns": [
		  			{ 
		  				"sClass": "id"
		  			},
		  			{
		  				"fnRender": function ( oObj ) {
			  				return "<a href='contest/viewContest.action?cid=" + oObj.aData[0] + "'>" + oObj.aData[1] + "</a>";
		  				},
		  				"sClass": "title"
		  			},
		  			{ 
		  				"sClass": "time"
		  			},
		  			{ 
		  				"bSortable": false,
		  				"sClass": "time"
		  			},
		  			{
		  				"bSortable": false,
		  				"sClass": "center status"
		  			},
		  			{ 
		  				"bSortable": false,
		  				"sClass": "center type"
		  			},
		  			{ 
		  				"fnRender": function ( oObj ) {
		  					return "<a href='user/profile.action?uid=" + oObj.aData[7] + "'>" + oObj.aData[6] + "</a>";
		  				},
		  				"sClass": "center"
		  			},
		  			{
		  				"bVisible": false
		  			},
		  			{ 
		  				"fnRender": function ( oObj ) {

		  					if (oObj.aData[8] == 1) {
		  						return "<a href='contest/toEditContest.action?cid=" + oObj.aData[0] + "'><img height='15px' border='0' src='images/wrench.gif' /></a>" + (oObj.aData[4] == 'Scheduled' ? "&nbsp;<a href='javascript:void(0)' onclick='comfirmDeleteContest(\"" + oObj.aData[0] + "\")'><img height='15px' border='0' src='images/recycle.gif' /></a>" : "&nbsp;&nbsp;&nbsp;");
		  					} else return "";
		  				},
		  				"bUseRendered": false, 
		  				"bSearchable": false,
		  				"bSortable": false,
		  				"sClass": "id"
		  			}
		  		],
		"fnServerData": function ( sSource, aoData, fnCallback ) {
			$.ajax( {
				"dataType": 'json', 
				"type": "POST", 
				"url": sSource, 
				"data": aoData, 
				"success": fnCallback
			} );
		},
		"fnRowCallback": function( nRow, aData, iDisplayIndex, iDisplayIndexFull ) {
		    $('td:eq(4)', nRow).addClass(aData[4]);
		    $('td:eq(5)', nRow).addClass(aData[5]);
		    nRow.className += " " + aData[4];
			return nRow;
		},
		"bJQueryUI": true,
		"sPaginationType": "full_numbers"
	});
} );