<?php
include_once 'contract/IRepository.php';
include_once 'model/GeoPoint.php';
include_once 'db/DbLink.php';

class CoordService implements IRepository {
    private $db;

    public function __construct() {
        $this->db = new DbLink();
    }

    public function insert($point) {
        $query = "INSERT INTO coordonnee(coord_lat, coord_lng, horodatage, device_code)
                  VALUES(:clat, :clng, :horo, :dcode)";

        $stmt = $this->db->getHandler()->prepare($query);
        $stmt->execute([
            ':clat'  => $point->getCoordLat(),
            ':clng'  => $point->getCoordLng(),
            ':horo'  => $point->getHorodatage(),
            ':dcode' => $point->getDeviceCode()
        ]);
    }

    public function modify($obj)    {}
    public function remove($obj)    {}
    public function fetchById($id)  {}
    public function fetchAll()      {}
}
?>