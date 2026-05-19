<?php
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    include_once 'service/CoordService.php';
    storeNewPoint();
}

function storeNewPoint() {
    $lat    = $_POST['coord_lat'];
    $lng    = $_POST['coord_lng'];
    $horo   = $_POST['horodatage'];
    $dcode  = $_POST['device_code'];

    $svc   = new CoordService();
    $point = new GeoPoint(null, $lat, $lng, $horo, $dcode);
    $svc->insert($point);

    echo "Coordonnees enregistrees";
}
?>