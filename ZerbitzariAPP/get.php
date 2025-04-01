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

// Consulta para obtener todos los productos
$sql = "SELECT * FROM produktua";
$result = $conn->query($sql);

$produktua = array();

// Verificar si hay resultados
if ($result && $result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $produktua[] = $row;
    }
}

// Devolver los resultados como JSON
echo json_encode($produktua);

// Cerrar la conexión
$conn->close();
?>
