<?php
header("Content-Type: application/json");

/*$servername = "192.168.115.188";
$username = "1taldea";
$password = "1taldea";
$dbname = "erronka1";*/
$servername = "localhost";
$username = "root";
$password = "1WMG2023";
$dbname = "erronka1";
try {
    $conn = new PDO("mysql:host=$servername;dbname=$dbname;charset=utf8", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Obtener la fecha y hora actuales
    $dataNow = date("Y-m-d H:i:s");

    $sql = "INSERT INTO eskaera (langilea_id, erreserba_id, mahaia_id, data, prezioTotala, ordaindua) 
            VALUES (:langilea_id, :erreserba_id, :mahaia_id, :data, :prezioTotala, :ordaindua)";
    $stmt = $conn->prepare($sql);

    $requestData = json_decode(file_get_contents("php://input"), true);

    if (is_array($requestData) && count($requestData) > 0) {
        foreach ($requestData as $item) {
            // Asignar los parámetros de la consulta
            $stmt->bindParam(':langilea_id', $item['langilea_id'], PDO::PARAM_INT);
            $stmt->bindParam(':erreserba_id', $item['erreserba_id'], PDO::PARAM_INT);
            $stmt->bindParam(':mahaia_id', $item['mahaia_id'], PDO::PARAM_INT);
            $stmt->bindParam(':data', $dataNow, PDO::PARAM_STR);  // Usar la fecha actual para el campo "data"
            $stmt->bindParam(':prezioTotala', $item['prezioTotala'], PDO::PARAM_STR);
            $stmt->bindParam(':ordaindua', $item['ordaindua'], PDO::PARAM_INT);

            // Ejecutar la inserción
            $stmt->execute();
        }

        echo json_encode(["success" => true, "message" => "Registros insertados con éxito."]);
    } else {
        echo json_encode(["success" => false, "message" => "No se recibieron datos válidos."]);
    }
} catch (PDOException $e) {
    echo json_encode(["success" => false, "message" => "Error al insertar: " . $e->getMessage()]);
}

$conn = null;
?>
