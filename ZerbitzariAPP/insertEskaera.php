<?php
/*$servername = "192.168.115.188";
$username = "1taldea";
$password = "1taldea";
$dbname = "erronka1";*/
$servername = "localhost";
$username = "root";
$password = "1WMG2023";
$dbname = "erronka1";

header('Content-Type: application/json'); // Establece el tipo de respuesta como JSON

try {
    $conn = new PDO("mysql:host=$servername;dbname=$dbname;charset=utf8", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Comienza la transacción
    $conn->beginTransaction();

    // SQL para insertar en eskaeraproduktua
    $sqlInsert = "INSERT INTO eskaeraproduktua (erreserba_id, produktu_izena, produktuaKop, prezioa, eginda)
                  VALUES (:erreserba_id, :produktuaIzena, :produktuaKop, :prezioa, 0)";  // Añadimos la columna eginda con valor 0 (no hecho)
    $stmtInsert = $conn->prepare($sqlInsert);

    // SQL para actualizar la cantidad en produktua
    $sqlUpdate = "UPDATE produktua 
                  SET kantitatea = kantitatea - :produktuaKop 
                  WHERE izena = :produktuaIzena";
    $stmtUpdate = $conn->prepare($sqlUpdate);

    // Obtén los datos del cuerpo de la solicitud
    $data = json_decode(file_get_contents("php://input"), true);

    // Verifica si se recibieron datos
    if (is_array($data) && count($data) > 0) {
        // Itera sobre los productos recibidos
        foreach ($data as $item) {
            // Inserta en la tabla eskaeraproduktua
            $stmtInsert->bindParam(':erreserba_id', $item['erreserba_id'], PDO::PARAM_INT);
            $stmtInsert->bindParam(':produktuaIzena', $item['produktu_izena'], PDO::PARAM_STR);
            $stmtInsert->bindParam(':produktuaKop', $item['produktuaKop'], PDO::PARAM_INT);
            $stmtInsert->bindParam(':prezioa', $item['prezioa'], PDO::PARAM_STR);
            $stmtInsert->execute();  // Inserta con eginda = 0

            // Actualiza en la tabla produktua, restando la cantidad
            $stmtUpdate->bindParam(':produktuaIzena', $item['produktu_izena'], PDO::PARAM_STR);
            $stmtUpdate->bindParam(':produktuaKop', $item['produktuaKop'], PDO::PARAM_INT);
            $stmtUpdate->execute();
        }

        // Si todo fue exitoso, realiza el commit
        $conn->commit();

        echo json_encode(["success" => true, "message" => "Registros insertados y actualizado el inventario con éxito."]);
    } else {
        echo json_encode(["success" => false, "message" => "No se recibieron datos válidos."]);
    }
} catch (PDOException $e) {
    // Si ocurre un error, hace rollback y muestra el error
    $conn->rollBack();
    echo json_encode(["success" => false, "message" => "Error al insertar o actualizar: " . $e->getMessage()]);
}

$conn = null;
?>
