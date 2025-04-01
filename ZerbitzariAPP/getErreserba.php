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

// Consulta para obtener el valor más alto de erreserbaId
$sql = "SELECT MAX(erreserba_id) as erreserba_id FROM eskaeraproduktua";
$result = $conn->query($sql);

// Verificar si hay resultados
if ($result && $result->num_rows > 0) {
    $row = $result->fetch_assoc();
    $maxErreserbaId = $row['erreserba_id'];
    echo json_encode(array("erreserba_id" => $maxErreserbaId));
} else {
    echo json_encode(array("error" => "No se encontraron resultados"));
}

// Cerrar la conexión
$conn->close();
?>

