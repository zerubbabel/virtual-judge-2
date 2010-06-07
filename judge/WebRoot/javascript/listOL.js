$(document).ready(function() {
	$('#listOL').dataTable({
		"aaSorting": [[ 5, "asc" ]],
		"bPaginate": false,
		"bLengthChange": false,
//		"bFilter": false,
//		"bSort": false,
		"bInfo": false,
		"bAutoWidth": false,
		"aoColumns": [{"sType": "html"},
		              {"sType": "html"},
		              {"sType": "html"},
		              {"sType": "html"},
		              {"sType": "html"},
		              {"sType": "date"}
		              ]
	});
} );
