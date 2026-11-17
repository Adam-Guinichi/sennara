<?php
require_once __DIR__ . '/config.php';
header('Content-Type: application/json; charset=utf-8');

set_exception_handler(function ($e) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => 'server_error', 'detail' => $e->getMessage()]);
    exit;
});

function db(): PDO {
    static $pdo = null;
    if ($pdo === null) {
        $pdo = new PDO(
            'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
            DB_USER, DB_PASS,
            [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ]
        );
    }
    return $pdo;
}

function ok($data = null) {
    echo json_encode(['ok' => true, 'data' => $data], JSON_UNESCAPED_UNICODE);
    exit;
}

function fail(string $error, int $code = 400) {
    http_response_code($code);
    echo json_encode(['ok' => false, 'error' => $error]);
    exit;
}

function param(string $key, $default = null) {
    if (isset($_POST[$key])) return trim((string)$_POST[$key]);
    if (isset($_GET[$key])) return trim((string)$_GET[$key]);
    return $default;
}

function current_user() {
    $token = param('token');
    if (!$token) return null;
    $st = db()->prepare('SELECT * FROM users WHERE api_token = ?');
    $st->execute([$token]);
    return $st->fetch() ?: null;
}

function require_user(?array $roles = null) {
    $u = current_user();
    if (!$u) fail('not_logged_in', 401);
    if ($roles !== null && !in_array($u['role'], $roles, true)) fail('forbidden', 403);
    return $u;
}

function public_user(array $u): array {
    return [
        'id' => (int)$u['id'], 'name' => $u['name'], 'email' => $u['email'], 'role' => $u['role'],
        'phone' => $u['phone'], 'city' => $u['city'], 'level' => $u['level'], 'styles' => $u['styles'],
        'created_at' => $u['created_at'],
    ];
}

function save_upload(string $field, string $prefix): string {
    if (empty($_FILES[$field]) || $_FILES[$field]['error'] !== UPLOAD_ERR_OK) return '';
    if ($_FILES[$field]['size'] > 6 * 1024 * 1024) fail('photo_too_big');
    $info = @getimagesize($_FILES[$field]['tmp_name']);
    if (!$info || $info[2] !== IMAGETYPE_JPEG) fail('invalid_photo');
    if (!is_dir(__DIR__ . '/img/uploads')) @mkdir(__DIR__ . '/img/uploads', 0775, true);
    $name = 'img/uploads/' . $prefix . '_' . bin2hex(random_bytes(6)) . '.jpg';
    if (!move_uploaded_file($_FILES[$field]['tmp_name'], __DIR__ . '/' . $name)) fail('server_error', 500);
    return $name;
}
