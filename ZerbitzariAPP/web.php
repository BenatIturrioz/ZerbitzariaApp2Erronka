<?php
/*$servername = "localhost";
$username = "root";
$password = "1WMG2023";
$dbname = "erronka1";*/
$servername = "localhost";
$username = "root";
$password = "1WMG2023";
$dbname = "erronka1";

$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    die("Conexión fallida: " . $conn->connect_error);
}

// Consultar las categorías (produktumota)
$sql_categories = "SELECT * FROM produktumota";
$result_categories = $conn->query($sql_categories);

// Consultar los productos (produktuak) agrupados por categoría
$sql_products = "SELECT * FROM produktua";
$result_products = $conn->query($sql_products);

// Organizar productos por categoría
$products_by_category = [];
if ($result_products->num_rows > 0) {
    while ($product = $result_products->fetch_assoc()) {
        $products_by_category[$product['mota']][] = $product;
    }
}

?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Menú del Restaurante</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
            background-image: url('pixelcut-export.jpeg'); /* Cambia esto por la URL de tu imagen */
            background-size: cover; /* Ajusta la imagen para cubrir todo el fondo */
            background-position: center; /* Centra la imagen */
            background-repeat: no-repeat; /* Evita que se repita */
        }
        header {
            background-color: #ff7043;
            color: white;
            text-align: center;
            padding: 1rem 0;
        }
        main {
            max-width: 800px;
            margin: 20px auto;
            background: white;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
            border-radius: 8px;
            padding: 20px;
        }
        section {
            margin-bottom: 20px;
        }
        h2 {
            border-bottom: 2px solid #ff7043;
            padding-bottom: 5px;
            margin-bottom: 10px;
        }
        ul {
            list-style: none;
            padding: 0;
        }
        li {
            margin: 5px 0;
        }
    </style>
</head>
<body>
    <header>
        <h1>Menú del Restaurante</h1>
    </header>
    <main>
        <?php
        if ($result_categories->num_rows > 0) {
            while ($category = $result_categories->fetch_assoc()) {
                $category_id = $category['id'];
                $category_name = $category['mota'];
                echo "<section>";
                echo "<h2>" . htmlspecialchars($category_name) . "</h2>";
                echo "<ul>";
                if (!empty($products_by_category[$category_id])) {
                    foreach ($products_by_category[$category_id] as $product) {
                        echo "<li>" . htmlspecialchars($product['izena']) . " - " . htmlspecialchars($product['deskribapena']) . "<span> " . htmlspecialchars($product['prezioa']) . " €</span></li>";
                    }
                } else {
                    echo "<li>No hay productos en esta categoría.</li>";
                }
                echo "</ul>";
                echo "</section>";
            }
        } else {
            echo "<p>No hay categorías disponibles.</p>";
        }
        ?>
    </main>
</body>
</html>
