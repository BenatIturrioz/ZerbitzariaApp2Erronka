<?php
$servername = "localhost";
$username = "root";
$password = "1WMG2023";
$dbname = "erronka1";

// Crear conexión
$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar si la conexión fue exitosa
if ($conn->connect_error) {
    die(json_encode(["success" => false, "message" => "Conexión fallida: " . $conn->connect_error]));
}

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $erreserba_id = $_POST["erreserba_id"] ?? null;

    if ($erreserba_id != null) {
        // Obtener los datos del pedido antes de eliminarlo
        $querySelect = "SELECT produktu_izena, produktuaKop FROM eskaeraproduktua WHERE erreserba_id = ?";
        $stmtSelect = $conn->prepare($querySelect);
        $stmtSelect->bind_param("i", $erreserba_id);
        $stmtSelect->execute();
        $result = $stmtSelect->get_result();

        if ($result->num_rows > 0) {
            // Iniciar transacción
            $conn->begin_transaction();

            try {
                while ($row = $result->fetch_assoc()) {
                    $produktuaIzena = $row["produktu_izena"];
                    $produktuaKop = $row["produktuaKop"];

                    // Sumar la cantidad eliminada de nuevo al stock
                    $sqlUpdate = "UPDATE produktua SET kantitatea = kantitatea + ? WHERE izena = ?";
                    $stmtUpdate = $conn->prepare($sqlUpdate);
                    $stmtUpdate->bind_param("is", $produktuaKop, $produktuaIzena);
                    $stmtUpdate->execute();
                }

                // Eliminar el pedido después de haber actualizado el stock
                $queryDelete = "DELETE FROM eskaeraproduktua WHERE erreserba_id = ?";
                $stmtDelete = $conn->prepare($queryDelete);
                $stmtDelete->bind_param("i", $erreserba_id);
                $stmtDelete->execute();

                if ($stmtDelete->affected_rows > 0) {
                    $conn->commit();
                    echo json_encode(["success" => true, "message" => "Eskaera eliminada y stock actualizado"]);
                } else {
                    $conn->rollback();
                    echo json_encode(["success" => false, "message" => "No se encontró la eskaera"]);
                }
            } catch (Exception $e) {
                $conn->rollback();
                echo json_encode(["success" => false, "message" => "Error en la transacción: " . $e->getMessage()]);
            }
        } else {
            echo json_encode(["success" => false, "message" => "No se encontró la eskaera"]);
        }

        // Cerrar los statements
        $stmtSelect->close();
        $stmtUpdate->close();
        $stmtDelete->close();
    } else {
        echo json_encode(["success" => false, "message" => "ID no recibido"]);
    }
}

// Cerrar la conexión
$conn->close();
?>
