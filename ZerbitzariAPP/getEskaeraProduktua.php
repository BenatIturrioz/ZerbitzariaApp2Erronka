<?php
/*$servername = "192.168.115.188";
$username = "1taldea";
$password = "1taldea";
$dbname = "erronka1";*/
$servername = "localhost";
$username = "root";
$password = "1WMG2023";
$dbname = "erronka1";

header('Content-Type: application/json'); // Asegura que la respuesta sea JSON

try {
    // Crear conexión
    $conn = new PDO("mysql:host=$servername;dbname=$dbname;charset=utf8", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // SQL para seleccionar las filas con el mayor erreserba_id
    $sql = "SELECT * 
            FROM eskaeraproduktua 
            WHERE erreserba_id = (SELECT MAX(erreserba_id) FROM eskaeraproduktua)";
    
    $stmt = $conn->prepare($sql);
    $stmt->execute();

    // Obtener los resultados
    $result = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Verificar si se encontraron filas
    if ($result) {
        echo json_encode(["success" => true, "data1" => $result]);
    } else {
        echo json_encode(["success" => false, "message" => "No se encontraron registros."]);
    }
} catch (PDOException $e) {
    echo json_encode(["success" => false, "message" => "Error en la consulta: " . $e->getMessage()]);
}

// Cerrar la conexión
$conn = null;
?>
