<?php
require_once __DIR__ . '/lib.php';
$u = require_user();
if (param('action') !== 'add') fail('unknown_action');

$name = param('name', '');
$lat = (float)param('lat', 0);
$lng = (float)param('lng', 0);
if ($name === '' || ($lat == 0 && $lng == 0)) fail('invalid_input');

$desc = param('description', '');
$type = param('type', 'shore');
if (!in_array($type, ['shore', 'rock', 'port', 'lagoon'], true)) $type = 'shore';
$approved = $u['role'] === 'admin' ? 1 : 0;
$photo = save_upload('photo', 'spot');

db()->prepare('INSERT INTO spots (slug, name, name_ar, city, city_ar, lat, lng, type, best_time, desc_en, desc_fr, desc_ar, image, created_by, approved)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)')
    ->execute(['', $name, $name, param('city', ''), param('city', ''), $lat, $lng, $type, 'dawn', $desc, $desc, $desc, $photo, $u['id'], $approved]);

ok(['approved' => $approved === 1, 'photo' => $photo]);
