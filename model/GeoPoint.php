<?php
class GeoPoint {
    private $ref_id;
    private $coord_lat;
    private $coord_lng;
    private $horodatage;
    private $device_code;

    public function __construct($ref_id, $coord_lat, $coord_lng, $horodatage, $device_code) {
        $this->ref_id     = $ref_id;
        $this->coord_lat  = $coord_lat;
        $this->coord_lng  = $coord_lng;
        $this->horodatage = $horodatage;
        $this->device_code = $device_code;
    }

    public function getRefId()      { return $this->ref_id; }
    public function getCoordLat()   { return $this->coord_lat; }
    public function getCoordLng()   { return $this->coord_lng; }
    public function getHorodatage() { return $this->horodatage; }
    public function getDeviceCode() { return $this->device_code; }

    public function setRefId($v)      { $this->ref_id = $v; }
    public function setCoordLat($v)   { $this->coord_lat = $v; }
    public function setCoordLng($v)   { $this->coord_lng = $v; }
    public function setHorodatage($v) { $this->horodatage = $v; }
    public function setDeviceCode($v) { $this->device_code = $v; }
}
?>