<?php
interface IRepository {
    public function insert($obj);
    public function modify($obj);
    public function remove($obj);
    public function fetchById($id);
    public function fetchAll();
}
?>