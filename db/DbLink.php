<?php
class DbLink {
    private $handler;

    public function __construct() {
        $host   = 'localhost';
        $schema = 'geotrack';
        $user   = 'root';
        $pass   = '';

        try {
            $this->handler = new PDO(
                "mysql:host=$host;dbname=$schema;charset=utf8mb4",
                $user,
                $pass
            );
            $this->handler->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        } catch (Exception $ex) {
            die('Connexion impossible : ' . $ex->getMessage());
        }
    }

    public function getHandler() {
        return $this->handler;
    }
}
?>