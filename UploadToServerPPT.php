<?php
	error_reporting(E_ALL);
	ini_set("display_errors",1);
	#$file_path = "./";
	$file_path = basename( $_FILES['uploaded_file']['name']);
	move_uploaded_file($_FILES['uploaded_file']['tmp_name'], $file_path);
	
	$exe = "sudo soffice --headless --convert-to pdf ".$file_path." --outdir .";
	exec($exe);

	$ext = strrchr($file_path, ".");
	$file = explode($ext, $file_path);

	$exe = "sudo gs -dNOPAUSE -sDEVICE=jpeg -sOutputFile=/var/www/uploads/%d ".$file[0]."."."pdf -r600x600 -dCOLORSCREEN -dDOINTERPOLATE -dJPEGQ=95";
	exec($exe);
?>
