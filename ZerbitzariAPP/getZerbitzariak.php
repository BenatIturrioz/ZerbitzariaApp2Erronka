<?php
/*$servername = "192.168.115.188";
$username = "1taldea";
$password = "1taldea";
$dbname = "erronka1";*/
$servername = "localhost";
$username = "root";
$password = "1WMG2023";
$dbname = "erronka1";

// Crear conexión
$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    die(json_encode(array("error" => "Conexión fallida: " . $conn->connect_error)));
}

// Consulta SQL con parámetros
$sql = "SELECT id, erabiltzaileIzena FROM erabiltzailea WHERE langilea_mota = 1";

// Ejecutar consulta
$result = $conn->query($sql);

$erabiltzaileIzena = array();

// Verificar si hay resultados
if ($result && $result->num_rows > 0) {
    // Almacenar los resultados
    while($row = $result->fetch_assoc()) {
        $erabiltzaileIzena[] = $row; // Agregar solo el valor necesario
    }
} else {
    // Si no hay resultados
    $erabiltzaileIzena = array();
}

// Devolver los resultados como JSON
echo json_encode($erabiltzaileIzena);

// Cerrar la conexión
$conn->close();
?>
