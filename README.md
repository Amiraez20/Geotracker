# TP — GeoTracker : Application Mobile de Géolocalisation connectée à un Backend PHP/MySQL

## Objectifs pédagogiques

À la fin de ce TP, il est possible de :

- Récupérer la latitude et la longitude d'un smartphone Android
- Comprendre le rôle des permissions Android liées à la localisation et au réseau
- Envoyer des données depuis une application Android vers un service PHP
- Enregistrer les coordonnées dans une base de données MySQL
- Structurer un mini projet mobile connecté à un backend

---

## Résultat attendu

L'application doit :

- Détecter une position géographique
- Afficher les informations récupérées (latitude, longitude, altitude, précision)
- Envoyer latitude, longitude, horodatage et identifiant du device au serveur
- Insérer ces données dans la table `coordonnee` de MySQL

---

## Vue d'ensemble de l'architecture

```
Smartphone Android
      │
      │  HTTP POST (Volley)
      ▼
Serveur PHP (saveCoord.php)
      │
      │  PDO
      ▼
Base MySQL (geotrack > coordonnee)
```

**Partie serveur :** base MySQL + classes PHP + script de réception  
**Partie mobile :** permissions Android + GPS + envoi HTTP avec Volley

---

## PARTIE 1 — Base de données MySQL

### Étape 1 — Création de la base et de la table

Se connecter à **phpMyAdmin** et exécuter le script SQL suivant :

```sql
CREATE DATABASE IF NOT EXISTS geotrack
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE geotrack;

CREATE TABLE coordonnee (
    ref_id       INT AUTO_INCREMENT PRIMARY KEY,
    coord_lat    DOUBLE NOT NULL,
    coord_lng    DOUBLE NOT NULL,
    horodatage   DATETIME NOT NULL,
    device_code  VARCHAR(50) NOT NULL
);
```

**Description des champs :**

| Champ | Type | Rôle |
|---|---|---|
| `ref_id` | INT AUTO_INCREMENT | Identifiant unique de chaque enregistrement |
| `coord_lat` | DOUBLE | Coordonnée nord-sud (latitude) |
| `coord_lng` | DOUBLE | Coordonnée est-ouest (longitude) |
| `horodatage` | DATETIME | Date et heure d'envoi |
| `device_code` | VARCHAR(50) | Identifiant unique du smartphone |

### Vérification — Capture phpMyAdmin

> **📸 Screenshot à insérer ici :**  
> Capture de phpMyAdmin montrant la base `geotrack` et la table `coordonnee` avec ses colonnes.

---

## PARTIE 2 — Serveur PHP

### Étape 2 — Structure des dossiers

Créer le dossier `geotrack` dans le répertoire web du serveur local :

| Logiciel | Chemin |
|---|---|
| WAMP | `C:\wamp64\www\geotrack\` |
| XAMPP | `C:\xampp\htdocs\geotrack\` |
| MAMP | `/Applications/MAMP/htdocs/geotrack/` |

Arborescence à créer :

```
geotrack/
│
├── model/
│   └── GeoPoint.php
│
├── db/
│   └── DbLink.php
│
├── contract/
│   └── IRepository.php
│
├── service/
│   └── CoordService.php
│
└── saveCoord.php
```

### Étape 3 — Classe métier `model/GeoPoint.php`

```php
<?php
class GeoPoint {
    private $ref_id;
    private $coord_lat;
    private $coord_lng;
    private $horodatage;
    private $device_code;

    public function __construct($ref_id, $coord_lat, $coord_lng, $horodatage, $device_code) {
        $this->ref_id      = $ref_id;
        $this->coord_lat   = $coord_lat;
        $this->coord_lng   = $coord_lng;
        $this->horodatage  = $horodatage;
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
```

### Étape 4 — Classe de connexion `db/DbLink.php`

```php
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
```

### Étape 5 — Interface `contract/IRepository.php`

```php
<?php
interface IRepository {
    public function insert($obj);
    public function modify($obj);
    public function remove($obj);
    public function fetchById($id);
    public function fetchAll();
}
?>
```

### Étape 6 — Service `service/CoordService.php`

```php
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

    public function modify($obj)   {}
    public function remove($obj)   {}
    public function fetchById($id) {}
    public function fetchAll()     {}
}
?>
```

### Étape 7 — Point d'entrée `saveCoord.php`

```php
<?php
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    include_once 'service/CoordService.php';
    storeNewPoint();
}

function storeNewPoint() {
    $lat   = $_POST['coord_lat'];
    $lng   = $_POST['coord_lng'];
    $horo  = $_POST['horodatage'];
    $dcode = $_POST['device_code'];

    $svc   = new CoordService();
    $point = new GeoPoint(null, $lat, $lng, $horo, $dcode);
    $svc->insert($point);

    echo "Coordonnees enregistrees";
}
?>
```

### Test Postman du script PHP

Avant de lancer l'app Android, tester le serveur indépendamment :

- **Méthode :** POST  
- **URL :** `http://localhost/geotrack/saveCoord.php`  
- **Body → form-data :**

| Clé | Valeur exemple |
|---|---|
| `coord_lat` | `48.8566` |
| `coord_lng` | `2.3522` |
| `horodatage` | `2026-05-19 12:00:00` |
| `device_code` | `test123` |

La réponse attendue est : `Coordonnees enregistrees`

<img width="1919" height="1006" alt="POSTMANTESTSUCCES" src="https://github.com/user-attachments/assets/33419dcc-8193-4858-9d10-0f09c80889ef" />


<img width="1919" height="935" alt="POSTMANRESULT" src="https://github.com/user-attachments/assets/57b4dbfe-b6ae-4fcd-9f3a-fe9729e39053" />

<img width="1169" height="80" alt="Capture d&#39;écran 2026-05-19 174346" src="https://github.com/user-attachments/assets/7422fcc4-4284-49fc-906a-1c96cf8e72f7" />



---

## PARTIE 3 — Application Android

### Étape 8 — Création du projet Android Studio

- Ouvrir Android Studio
- Créer un nouveau projet nommé **GeoTracker**
- Choisir **Empty Activity**
- Langage : **Java**

### Étape 9 — Permissions dans `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

Ajouter aussi dans la balise `<application>` pour autoriser le HTTP non sécurisé :

```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

### Étape 10 — Dépendance Volley dans `build.gradle`

```gradle
implementation 'com.android.volley:volley:1.2.1'
```

Cliquer sur **Sync Now** après l'ajout.

### Étape 11 — Layout `activity_main.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="20dp">

    <TextView
        android:id="@+id/tvCoordonnees"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="En attente du signal GPS..."
        android:textSize="17sp" />
</LinearLayout>
```

### Étape 12 — `MainActivity.java`

> ⚠️ Remplacer `VOTRE_IP` par l'adresse IP locale de la machine (ex : `192.168.1.42`).  
> Sur émulateur, utiliser `10.0.2.2` à la place.

```java
package com.example.geotracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private double valLat;
    private double valLng;
    private double valAlt;
    private float  valPrecision;

    private RequestQueue fileRequetes;
    private TextView tvCoordonnees;

    private static final String ENDPOINT = "http://VOTRE_IP/geotrack/saveCoord.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCoordonnees = findViewById(R.id.tvCoordonnees);
        fileRequetes  = Volley.newRequestQueue(getApplicationContext());

        LocationManager gestLoc =
            (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                }, 10);
            return;
        }

        gestLoc.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            60000,
            150,
            new LocationListener() {

                @Override
                public void onLocationChanged(Location loc) {
                    valLat       = loc.getLatitude();
                    valLng       = loc.getLongitude();
                    valAlt       = loc.getAltitude();
                    valPrecision = loc.getAccuracy();

                    String affichage = "Latitude  : " + valLat
                        + "\nLongitude : " + valLng
                        + "\nAltitude  : " + valAlt
                        + "\nPrécision : " + valPrecision + " m";

                    tvCoordonnees.setText(affichage);
                    Toast.makeText(getApplicationContext(), affichage, Toast.LENGTH_LONG).show();

                    envoyerPoint(valLat, valLng);
                }

                @Override
                public void onStatusChanged(String fournisseur, int statut, @NonNull Bundle extras) {
                    String libStatut = "";
                    switch (statut) {
                        case LocationProvider.OUT_OF_SERVICE:          libStatut = "HORS_SERVICE"; break;
                        case LocationProvider.TEMPORARILY_UNAVAILABLE: libStatut = "INDISPONIBLE"; break;
                        case LocationProvider.AVAILABLE:               libStatut = "DISPONIBLE";   break;
                    }
                    Toast.makeText(getApplicationContext(),
                        "Fournisseur " + fournisseur + " → " + libStatut,
                        Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProviderEnabled(@NonNull String fournisseur) {
                    Toast.makeText(getApplicationContext(),
                        "GPS activé : " + fournisseur, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProviderDisabled(@NonNull String fournisseur) {
                    Toast.makeText(getApplicationContext(),
                        "GPS désactivé : " + fournisseur, Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void envoyerPoint(final double lat, final double lng) {
        StringRequest requete = new StringRequest(
            Request.Method.POST,
            ENDPOINT,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String reponse) {
                    Toast.makeText(getApplicationContext(),
                        reponse, Toast.LENGTH_SHORT).show();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError erreur) {
                    Toast.makeText(getApplicationContext(),
                        "Échec de l'envoi", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                String identifiant = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID
                );

                params.put("coord_lat",   String.valueOf(lat));
                params.put("coord_lng",   String.valueOf(lng));
                params.put("horodatage",  fmt.format(new Date()));
                params.put("device_code", identifiant);

                return params;
            }
        };

        fileRequetes.add(requete);
    }
}
```

---

## PARTIE 4 — Tests et vérifications

### Étape 13 — Test côté serveur

1. Démarrer Apache et MySQL via WAMP/XAMPP
2. Vérifier la base `geotrack` et la table `coordonnee` dans phpMyAdmin
3. Tester avec Postman (voir Étape 7)
4. Vérifier qu'une ligne apparaît dans la table

### Étape 14 — Test sur émulateur Android Studio

1. Remplacer `VOTRE_IP` par `10.0.2.2` dans `ENDPOINT`
2. Lancer l'émulateur
5. Vérifier qu'une nouvelle ligne apparaît dans phpMyAdmin

https://github.com/user-attachments/assets/c49055c1-c4ad-4d30-8c5f-e3c470a5543d



---
## Récapitulatif des changements de nommage

| Élément original | Élément utilisé dans ce projet |
|---|---|
| Base `localisation` | Base `geotrack` |
| Table `position` | Table `coordonnee` |
| Champ `imei` | Champ `device_code` |
| Champ `date_position` | Champ `horodatage` |
| `Position.php` | `GeoPoint.php` |
| `Connexion.php` | `DbLink.php` |
| `IDao.php` | `IRepository.php` |
| `PositionService.php` | `CoordService.php` |
| `createPosition.php` | `saveCoord.php` |
| Projet `LocalisationSmartphone` | Projet `GeoTracker` |
| Variable `locationManager` | Variable `gestLoc` |
| Méthode `addPosition()` | Méthode `envoyerPoint()` |
| Variable `requestQueue` | Variable `fileRequetes` |
| Constante `insertUrl` | Constante `ENDPOINT` |

---

## Bonnes pratiques

- Vérifier que le smartphone et le serveur sont sur le **même réseau local**
- Utiliser l'adresse IP locale correcte dans `ENDPOINT` (`10.0.2.2` pour l'émulateur, IP LAN pour un vrai téléphone)
- S'assurer que le **GPS est activé** sur le téléphone ou simulé sur l'émulateur
- Tester le script PHP avec Postman **avant** de lancer l'application Android
- Vérifier les permissions au démarrage de l'application

---

## Synthèse

Ce TP permet de construire un mini système de géolocalisation mobile connecté à un backend PHP/MySQL. Il mobilise des notions importantes en développement mobile et web : permissions Android, GPS, requêtes HTTP avec Volley, structuration orientée objet côté PHP (classes, interface, service) et stockage en base de données via PDO. Le périmètre fonctionnel couvre la récupération de coordonnées, l'envoi par réseau et l'enregistrement distant, sans inclure l'affichage cartographique.
