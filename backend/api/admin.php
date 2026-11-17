<?php
require_once __DIR__ . '/lib.php';
$u = require_user(['admin']);
$pdo = db();
$action = param('action');

function overview(PDO $pdo): array {
    return [
        'pending_spots' => $pdo->query('SELECT s.id, s.name, s.city, s.desc_en AS description, s.image, s.lat, s.lng, u.name AS author
                                        FROM spots s LEFT JOIN users u ON u.id = s.created_by
                                        WHERE s.approved = 0 ORDER BY s.id DESC')->fetchAll(),
        'pending_shops' => $pdo->query('SELECT s.id, s.name, s.city, s.description, s.phone, u.name AS author
                                        FROM shops s LEFT JOIN users u ON u.id = s.owner_id
                                        WHERE s.approved = 0 ORDER BY s.id DESC')->fetchAll(),
        'reviews' => $pdo->query('SELECT r.id, r.rating, r.comment, r.created_at, u.name AS author, s.name AS spot
                                  FROM reviews r JOIN users u ON u.id = r.user_id JOIN spots s ON s.id = r.spot_id
                                  ORDER BY r.created_at DESC LIMIT 100')->fetchAll(),
        'users' => $pdo->query('SELECT id, name, email, role, city, created_at FROM users ORDER BY id')->fetchAll(),
        'stats' => [
            'users'   => (int)$pdo->query('SELECT COUNT(*) FROM users')->fetchColumn(),
            'spots'   => (int)$pdo->query('SELECT COUNT(*) FROM spots WHERE approved = 1')->fetchColumn(),
            'shops'   => (int)$pdo->query('SELECT COUNT(*) FROM shops WHERE approved = 1')->fetchColumn(),
            'catches' => (int)$pdo->query('SELECT COUNT(*) FROM catches WHERE deleted = 0')->fetchColumn(),
            'reviews' => (int)$pdo->query('SELECT COUNT(*) FROM reviews')->fetchColumn(),
        ],
    ];
}

if ($action === 'overview') ok(overview($pdo));

if ($action === 'approve' || $action === 'reject') {
    $table = param('kind') === 'shop' ? 'shops' : 'spots';
    $id = (int)param('id', 0);
    if ($action === 'approve') {
        $pdo->prepare("UPDATE $table SET approved = 1 WHERE id = ?")->execute([$id]);
    } else {
        if ($table === 'shops') {
            $pdo->prepare('DELETE p FROM products p JOIN shops s ON s.id = p.shop_id WHERE s.id = ? AND s.approved = 0')->execute([$id]);
        }
        $pdo->prepare("DELETE FROM $table WHERE id = ? AND approved = 0")->execute([$id]);
    }
    ok(overview($pdo));
}

if ($action === 'set_role') {
    $role = param('role');
    $id = (int)param('id', 0);
    if (!in_array($role, ['fisher', 'seller', 'admin'], true)) fail('invalid_input');
    if ($id === (int)$u['id']) fail('cannot_change_self');
    $pdo->prepare('UPDATE users SET role = ? WHERE id = ?')->execute([$role, $id]);
    ok(overview($pdo));
}

if ($action === 'delete_review') {
    $pdo->prepare('DELETE FROM reviews WHERE id = ?')->execute([(int)param('id', 0)]);
    ok(overview($pdo));
}

fail('unknown_action');
