<?php
require_once __DIR__ . '/lib.php';
$u = require_user();
$changes = json_decode(param('changes', '[]'), true);
if (!is_array($changes)) fail('invalid_input');

$add = db()->prepare('INSERT IGNORE INTO favorites (user_id, spot_id) VALUES (?,?)');
$del = db()->prepare('DELETE FROM favorites WHERE user_id = ? AND spot_id = ?');
foreach ($changes as $c) {
    $spot = (int)($c['spot_id'] ?? 0);
    if ($spot <= 0) continue;
    if (!empty($c['fav'])) $add->execute([$u['id'], $spot]); else $del->execute([$u['id'], $spot]);
}

$st = db()->prepare('SELECT spot_id FROM favorites WHERE user_id = ? ORDER BY created_at');
$st->execute([$u['id']]);
ok(['favorites' => array_map('intval', array_column($st->fetchAll(), 'spot_id'))]);
