<?php
	$a = 1;
	ini_set('upload_max_filesize','10M');
	$file_path = "uploads/";
	#$file_path = $file_path . basename( $_FILES['uploaded_file']['name']);
	#$file_path = $file_path . date("YmdHis") . microtime(true);
	$temp_file_path = $file_path . $a;
		
	while(file_exists($temp_file_path))
	{
		$a = $a+1;
		$temp_file_path = $file_path . $a;
	}

	if(move_uploaded_file($_FILES['uploaded_file']['tmp_name'], $temp_file_path)) {
		echo "success";
		
	}
	else {
		echo "fail";
	}
?>
