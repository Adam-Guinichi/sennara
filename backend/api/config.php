<?php
if (file_exists(__DIR__ . '/config.local.php')) {
    require __DIR__ . '/config.local.php';
}

if (!defined('DB_HOST')) define('DB_HOST', '127.0.0.1');
if (!defined('DB_NAME')) define('DB_NAME', 'sennara');
if (!defined('DB_USER')) define('DB_USER', 'root');
if (!defined('DB_PASS')) define('DB_PASS', '');
if (!defined('DB_CREATE')) define('DB_CREATE', true);

if (!defined('ADMIN_PASSWORD')) define('ADMIN_PASSWORD', 'sennara123');
if (!defined('DEMO_PASSWORD')) define('DEMO_PASSWORD', 'sennara123');
