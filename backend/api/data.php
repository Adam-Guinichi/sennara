<?php
require_once __DIR__ . '/lib.php';
require_user();
$pdo = db();

$spots = $pdo->query('SELECT s.*,
        (SELECT ROUND(AVG(r.rating), 2) FROM reviews r WHERE r.spot_id = s.id) AS avg_rating,
        (SELECT COUNT(*) FROM reviews r WHERE r.spot_id = s.id) AS n_reviews,
        (SELECT COUNT(*) FROM catches c WHERE c.spot_id = s.id AND c.deleted = 0) AS n_catches
    FROM spots s WHERE s.approved = 1 ORDER BY s.id')->fetchAll();

$top = [];
foreach ($pdo->query('SELECT spot_id, species, COUNT(*) AS n FROM catches
        WHERE deleted = 0 AND spot_id > 0 AND species <> "" GROUP BY spot_id, species ORDER BY n DESC') as $row) {
    if (!isset($top[$row['spot_id']])) $top[$row['spot_id']] = $row['species'];
}
foreach ($spots as &$s) {
    $s['avg_rating'] = $s['avg_rating'] === null ? 0 : (float)$s['avg_rating'];
    $s['top_species'] = $top[$s['id']] ?? '';
}
unset($s);

ok([
    'spots'     => $spots,
    'fish'      => $pdo->query('SELECT * FROM fish ORDER BY id')->fetchAll(),
    'spot_fish' => $pdo->query('SELECT sf.* FROM spot_fish sf JOIN spots s ON s.id = sf.spot_id WHERE s.approved = 1')->fetchAll(),
    'shops'     => $pdo->query('SELECT * FROM shops WHERE approved = 1 ORDER BY name')->fetchAll(),
    'products'  => $pdo->query('SELECT p.* FROM products p JOIN shops s ON s.id = p.shop_id WHERE s.approved = 1 ORDER BY p.price')->fetchAll(),
    'tutorials' => $pdo->query('SELECT * FROM tutorials ORDER BY sort_order, id')->fetchAll(),
    'rules'     => $pdo->query('SELECT * FROM rules ORDER BY sort_order, id')->fetchAll(),
    'server_time' => time(),
]);
