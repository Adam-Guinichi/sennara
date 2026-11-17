<?php
require_once __DIR__ . '/lib.php';
$action = param('action');

const LEVELS = ['beginner', 'intermediate', 'expert'];
const STYLES = ['shore', 'rock', 'port', 'lagoon', 'boat'];

function clean_styles(string $csv): string {
    return implode(',', array_values(array_intersect(STYLES, array_map('trim', explode(',', $csv)))));
}

if ($action === 'register') {
    $name = param('name', '');
    $email = strtolower(param('email', ''));
    $pass = param('password', '');
    $role = param('role', 'fisher');
    if ($name === '' || !filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($pass) < 6) fail('invalid_input');
    if (!in_array($role, ['fisher', 'seller'], true)) $role = 'fisher';
    $st = db()->prepare('SELECT id FROM users WHERE email = ?');
    $st->execute([$email]);
    if ($st->fetch()) fail('email_taken');
    $token = bin2hex(random_bytes(24));
    db()->prepare('INSERT INTO users (name, email, password_hash, role, phone, city, api_token) VALUES (?,?,?,?,?,?,?)')
        ->execute([$name, $email, password_hash($pass, PASSWORD_DEFAULT), $role, param('phone', ''), param('city', ''), $token]);
    $st = db()->prepare('SELECT * FROM users WHERE email = ?');
    $st->execute([$email]);
    ok(['user' => public_user($st->fetch()), 'token' => $token]);
}

if ($action === 'login') {
    $st = db()->prepare('SELECT * FROM users WHERE email = ?');
    $st->execute([strtolower(param('email', ''))]);
    $u = $st->fetch();
    if (!$u || !password_verify(param('password', ''), $u['password_hash'])) fail('bad_credentials', 401);
    $token = bin2hex(random_bytes(24));
    db()->prepare('UPDATE users SET api_token = ? WHERE id = ?')->execute([$token, $u['id']]);
    ok(['user' => public_user($u), 'token' => $token]);
}

if ($action === 'update') {
    $u = require_user();
    $name = param('name', $u['name']);
    if ($name === '') fail('invalid_input');
    $level = param('level', $u['level']);
    if (!in_array($level, LEVELS, true)) $level = $u['level'];
    $styles = clean_styles(param('styles', $u['styles']));
    db()->prepare('UPDATE users SET name = ?, phone = ?, city = ?, level = ?, styles = ? WHERE id = ?')
        ->execute([$name, param('phone', $u['phone']), param('city', $u['city']), $level, $styles, $u['id']]);
    $st = db()->prepare('SELECT * FROM users WHERE id = ?');
    $st->execute([$u['id']]);
    ok(['user' => public_user($st->fetch())]);
}

if ($action === 'me') {
    ok(['user' => public_user(require_user())]);
}

fail('unknown_action');
