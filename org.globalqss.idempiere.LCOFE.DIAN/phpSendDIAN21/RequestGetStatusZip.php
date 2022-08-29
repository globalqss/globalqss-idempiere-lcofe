<?php
// Example:
// php RequestGetStatusZip.php 1000000 2 getStatusZip_face_f0830085359003b02376f 553bc48851ed5ba685ec46064ad608e5d16ed562a4d262904679882f49a6d6b2861e6298f8a9da3a232875cc177ba1a5
require 'vendor/autoload.php';
date_default_timezone_set('America/Bogota');
use Stenfrank\UBL21dian\Client;
use Stenfrank\UBL21dian\Templates\SOAP\GetStatusZip;

$clientID = $argv[1];
$env = $argv[2];
$home_path_var = $_SERVER["HOME"];
$ini = parse_ini_file($home_path_var . '/.CertDIAN21_' . $clientID . '.ini');
$pathCertificate = $ini['pathCert'];
$passwors = $ini['passCert'];
$baseOutFile = $argv[3];

$getStatusZip = new GetStatusZip($pathCertificate, $passwors);
if ($env == "1") {
    $getStatusZip->To = 'https://vpfe.dian.gov.co/WcfDianCustomerServices.svc?wsdl';
}
$getStatusZip->trackId = $argv[4];
$getStatusZip->sign();

$client = new Client($getStatusZip);

file_put_contents($baseOutFile . '_request.xml', $getStatusZip->xml);
file_put_contents($baseOutFile . '_answer.xml', $client->getResponse());
print("OK\n");
?>
