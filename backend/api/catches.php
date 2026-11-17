<?php
require_once __DIR__ . '/lib.php';
$u = require_user();
$pdo = db();
$incoming = json_decode(param('catches', '[]'), true);
if (!is_array($incoming)) fail('invalid_input');

const FIELDS = ['spot_id', 'spot_name', 'fish_id', 'species', 'weight_g', 'length_cm', 'bait', 'released', 'note', 'lat', 'lng', 'caught_at', 'updated_at', 'deleted'];

$find = $pdo->prepare('SELECT id, updated_at FROM catches WHERE uuid = ? AND user_id = ?');
$ins = $pdo->prepare('INSERT INTO catches (uuid, user_id, ' . implode(', ', FIELDS) . ') VALUES (?,?' . str_repeat(',?', count(FIELDS)) . ')');
$upd = $pdo->prepare('UPDATE catches SET ' . implode(' = ?, ', FIELDS) . ' = ? WHERE id = ?');

foreach ($incoming as $c) {
    if (empty($c['uuid']) || strlen($c['uuid']) > 36) continue;
    $vals = [
        (int)($c['spot_id'] ?? 0), mb_substr((string)($c['spot_name'] ?? ''), 0, 100), (int)($c['fish_id'] ?? 0),
        mb_substr((string)($c['species'] ?? ''), 0, 80), max(0, (int)($c['weight_g'] ?? 0)), max(0, (int)($c['length_cm'] ?? 0)),
        mb_substr((string)($c['bait'] ?? ''), 0, 40), !empty($c['released']) ? 1 : 0, mb_substr((string)($c['note'] ?? ''), 0, 255),
        (float)($c['lat'] ?? 0), (float)($c['lng'] ?? 0), (int)($c['caught_at'] ?? 0), (int)($c['updated_at'] ?? 0),
        !empty($c['deleted']) ? 1 : 0,
    ];
    $find->execute([$c['uuid'], $u['id']]);
    $row = $find->fetch();
    if (!$row) {
        $ins->execute(array_merge([$c['uuid'], $u['id']], $vals));
    } elseif ((int)$row['updated_at'] <= $vals[12]) {
        $upd->execute(array_merge($vals, [$row['id']]));
    }
}

$st = $pdo->prepare('SELECT uuid, ' . implode(', ', FIELDS) . ' FROM catches WHERE user_id = ? ORDER BY caught_at DESC');
$st->execute([$u['id']]);
ok(['catches' => $st->fetchAll()]);
