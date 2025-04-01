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
    die("Conexión fallida: " . $conn->connect_error);
}

// Consulta para seleccionar las mesas que no están asociadas a una fila en 'eskaera' con 'ordaindua = 0'
$sql = " SELECT * FROM erronka1.mahaia m WHERE NOT EXISTS ( SELECT * FROM erronka1.eskaera e WHERE e.mahaia_id = m.mahaia_id AND e.ordaindua = 0 )";

// Ejecutar consulta
$result = $conn->query($sql);

$mesas = array();

if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $mesas[] = $row;
    }
}

echo json_encode($mesas);

// Cerrar conexión
$conn->close();
?>
