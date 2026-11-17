<?php
require_once __DIR__ . '/lib.php';
$u = require_user(['seller', 'admin']);
$action = param('action');
$pdo = db();

function my_shop(PDO $pdo, $uid) {
    $st = $pdo->prepare('SELECT * FROM shops WHERE owner_id = ?');
    $st->execute([$uid]);
    return $st->fetch() ?: null;
}

function shop_payload(PDO $pdo, $uid): array {
    $shop = my_shop($pdo, $uid);
    $products = [];
    if ($shop) {
        $st = $pdo->prepare('SELECT * FROM products WHERE shop_id = ? ORDER BY name');
        $st->execute([$shop['id']]);
        $products = $st->fetchAll();
    }
    return ['shop' => $shop, 'products' => $products];
}

if ($action === 'mine') ok(shop_payload($pdo, $u['id']));

if ($action === 'save') {
    $name = param('name', '');
    if ($name === '') fail('invalid_input');
    $f = [$name, param('city', ''), param('address', ''), param('phone', ''),
          (float)param('lat', 0), (float)param('lng', 0), param('description', '')];
    $shop = my_shop($pdo, $u['id']);
    if ($shop) {
        $pdo->prepare('UPDATE shops SET name=?, city=?, address=?, phone=?, lat=?, lng=?, description=? WHERE id=?')
            ->execute(array_merge($f, [$shop['id']]));
    } else {
        $pdo->prepare('INSERT INTO shops (name, city, address, phone, lat, lng, description, owner_id, approved) VALUES (?,?,?,?,?,?,?,?,0)')
            ->execute(array_merge($f, [$u['id']]));
    }
    ok(shop_payload($pdo, $u['id']));
}

if ($action === 'product_save') {
    $shop = my_shop($pdo, $u['id']);
    if (!$shop) fail('no_shop');
    $name = param('name', '');
    if ($name === '') fail('invalid_input');
    $cat = param('category', 'other');
    if (!in_array($cat, ['bait', 'rod', 'reel', 'line', 'hook', 'other'], true)) $cat = 'other';
    $price = (float)param('price', 0);
    $stock = (int)param('in_stock', 1) ? 1 : 0;
    $id = (int)param('id', 0);
    if ($id > 0) {
        $pdo->prepare('UPDATE products SET name=?, category=?, price=?, in_stock=? WHERE id=? AND shop_id=?')
            ->execute([$name, $cat, $price, $stock, $id, $shop['id']]);
    } else {
        $pdo->prepare('INSERT INTO products (shop_id, name, category, price, in_stock) VALUES (?,?,?,?,?)')
            ->execute([$shop['id'], $name, $cat, $price, $stock]);
    }
    ok(shop_payload($pdo, $u['id']));
}

if ($action === 'product_delete') {
    $shop = my_shop($pdo, $u['id']);
    if (!$shop) fail('no_shop');
    $pdo->prepare('DELETE FROM products WHERE id=? AND shop_id=?')->execute([(int)param('id', 0), $shop['id']]);
    ok(shop_payload($pdo, $u['id']));
}

fail('unknown_action');
