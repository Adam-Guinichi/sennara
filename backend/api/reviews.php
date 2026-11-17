<?php
require_once __DIR__ . '/lib.php';
$u = require_user();
$action = param('action');
$spotId = (int)param('spot_id', 0);

function reviews_of(int $spotId): array {
    $st = db()->prepare('SELECT r.id, r.user_id, r.rating, r.comment, r.created_at, u.name AS author
        FROM reviews r JOIN users u ON u.id = r.user_id WHERE r.spot_id = ? ORDER BY r.created_at DESC LIMIT 50');
    $st->execute([$spotId]);
    $list = $st->fetchAll();
    $avg = count($list) ? round(array_sum(array_column($list, 'rating')) / count($list), 2) : 0;
    return ['reviews' => $list, 'avg' => $avg, 'count' => count($list)];
}

if ($action === 'list') ok(reviews_of($spotId));

if ($action === 'save') {
    $rating = (int)param('rating', 0);
    if ($spotId <= 0 || $rating < 1 || $rating > 5) fail('invalid_input');
    $comment = mb_substr(param('comment', ''), 0, 400);
    db()->prepare('INSERT INTO reviews (spot_id, user_id, rating, comment) VALUES (?,?,?,?)
        ON DUPLICATE KEY UPDATE rating = VALUES(rating), comment = VALUES(comment), created_at = CURRENT_TIMESTAMP')
        ->execute([$spotId, $u['id'], $rating, $comment]);
    ok(reviews_of($spotId));
}

if ($action === 'delete') {
    $id = (int)param('id', 0);
    $st = db()->prepare('SELECT * FROM reviews WHERE id = ?');
    $st->execute([$id]);
    $review = $st->fetch();
    if (!$review) fail('invalid_input');
    if ((int)$review['user_id'] !== (int)$u['id'] && $u['role'] !== 'admin') fail('forbidden', 403);
    db()->prepare('DELETE FROM reviews WHERE id = ?')->execute([$id]);
    ok(reviews_of((int)$review['spot_id']));
}

fail('unknown_action');
