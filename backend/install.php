<?php
if (PHP_SAPI !== 'cli') { http_response_code(403); exit('Run this from the command line.'); }
require __DIR__ . '/api/config.php';

$pdo = new PDO('mysql:host=' . DB_HOST . ';charset=utf8mb4', DB_USER, DB_PASS, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
if (DB_CREATE) {
    $pdo->exec('DROP DATABASE IF EXISTS `' . DB_NAME . '`');
    $pdo->exec('CREATE DATABASE `' . DB_NAME . '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
    $pdo->exec('USE `' . DB_NAME . '`');
} else {
    $pdo->exec('USE `' . DB_NAME . '`');
    $pdo->exec('SET FOREIGN_KEY_CHECKS = 0');
    foreach ($pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN) as $table) {
        $pdo->exec('DROP TABLE `' . $table . '`');
    }
    $pdo->exec('SET FOREIGN_KEY_CHECKS = 1');
}

// ---------------------------------------------------------------- tables
$pdo->exec("CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('fisher','seller','admin') NOT NULL DEFAULT 'fisher',
    phone VARCHAR(30) NOT NULL DEFAULT '',
    city VARCHAR(60) NOT NULL DEFAULT '',
    level ENUM('beginner','intermediate','expert') NOT NULL DEFAULT 'beginner',
    styles VARCHAR(80) NOT NULL DEFAULT '',
    api_token VARCHAR(64) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX (api_token)
)");
$pdo->exec("CREATE TABLE spots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(40) NOT NULL DEFAULT '',
    name VARCHAR(100) NOT NULL, name_ar VARCHAR(100) NOT NULL,
    city VARCHAR(60) NOT NULL DEFAULT '', city_ar VARCHAR(60) NOT NULL DEFAULT '',
    lat DOUBLE NOT NULL, lng DOUBLE NOT NULL,
    type ENUM('shore','rock','port','lagoon') NOT NULL DEFAULT 'shore',
    best_time ENUM('dawn','dusk','night','day') NOT NULL DEFAULT 'dawn',
    desc_en TEXT, desc_fr TEXT, desc_ar TEXT,
    image VARCHAR(160) NOT NULL DEFAULT '',
    created_by INT DEFAULT NULL,
    approved TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)");
$pdo->exec("CREATE TABLE fish (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(40) NOT NULL UNIQUE,
    sci_name VARCHAR(80) NOT NULL,
    name_en VARCHAR(80) NOT NULL, name_fr VARCHAR(80) NOT NULL, name_ar VARCHAR(80) NOT NULL,
    family VARCHAR(40) NOT NULL DEFAULT '',
    habitats VARCHAR(60) NOT NULL DEFAULT '',
    month_from TINYINT NOT NULL DEFAULT 1, month_to TINYINT NOT NULL DEFAULT 12,
    baits VARCHAR(80) NOT NULL DEFAULT '',
    techniques VARCHAR(80) NOT NULL DEFAULT '',
    min_size_cm SMALLINT NOT NULL DEFAULT 0,
    min_weight_g SMALLINT NOT NULL DEFAULT 0,
    size_rule ENUM('none','eu_ref','tn_rule') NOT NULL DEFAULT 'none',
    max_cm SMALLINT NOT NULL DEFAULT 0,
    iucn VARCHAR(4) NOT NULL DEFAULT '',
    about_en TEXT, about_fr TEXT, about_ar TEXT,
    image VARCHAR(160) NOT NULL DEFAULT '',
    wikidata VARCHAR(16) NOT NULL DEFAULT ''
)");
$pdo->exec("CREATE TABLE spot_fish (spot_id INT NOT NULL, fish_id INT NOT NULL, PRIMARY KEY (spot_id, fish_id))");
$pdo->exec("CREATE TABLE shops (
    id INT AUTO_INCREMENT PRIMARY KEY,
    owner_id INT DEFAULT NULL,
    osm_id VARCHAR(30) NOT NULL DEFAULT '',
    name VARCHAR(120) NOT NULL,
    city VARCHAR(60) NOT NULL DEFAULT '',
    address VARCHAR(160) NOT NULL DEFAULT '',
    phone VARCHAR(30) NOT NULL DEFAULT '',
    website VARCHAR(160) NOT NULL DEFAULT '',
    lat DOUBLE NOT NULL DEFAULT 0, lng DOUBLE NOT NULL DEFAULT 0,
    description TEXT,
    approved TINYINT NOT NULL DEFAULT 1
)");
$pdo->exec("CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    shop_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    category ENUM('bait','rod','reel','line','hook','other') NOT NULL DEFAULT 'other',
    price DECIMAL(8,3) NOT NULL DEFAULT 0,
    in_stock TINYINT NOT NULL DEFAULT 1,
    INDEX (shop_id)
)");
$pdo->exec("CREATE TABLE tutorials (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(40) NOT NULL DEFAULT '',
    level ENUM('beginner','intermediate','advanced') NOT NULL DEFAULT 'beginner',
    minutes TINYINT NOT NULL DEFAULT 3,
    title_en VARCHAR(150) NOT NULL, title_fr VARCHAR(150) NOT NULL, title_ar VARCHAR(150) NOT NULL,
    body_en TEXT, body_fr TEXT, body_ar TEXT,
    image VARCHAR(160) NOT NULL DEFAULT '',
    video_url VARCHAR(255) NOT NULL DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0
)");
$pdo->exec("CREATE TABLE rules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    kind ENUM('tn','ref','tip') NOT NULL,
    title_en VARCHAR(150) NOT NULL, title_fr VARCHAR(150) NOT NULL, title_ar VARCHAR(150) NOT NULL,
    body_en TEXT, body_fr TEXT, body_ar TEXT,
    source_url VARCHAR(255) NOT NULL DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0
)");
$pdo->exec("CREATE TABLE catches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    user_id INT NOT NULL,
    spot_id INT NOT NULL DEFAULT 0,
    spot_name VARCHAR(100) NOT NULL DEFAULT '',
    fish_id INT NOT NULL DEFAULT 0,
    species VARCHAR(80) NOT NULL DEFAULT '',
    weight_g INT NOT NULL DEFAULT 0,
    length_cm INT NOT NULL DEFAULT 0,
    bait VARCHAR(40) NOT NULL DEFAULT '',
    released TINYINT NOT NULL DEFAULT 0,
    note VARCHAR(255) NOT NULL DEFAULT '',
    lat DOUBLE NOT NULL DEFAULT 0, lng DOUBLE NOT NULL DEFAULT 0,
    caught_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uq_user_uuid (user_id, uuid),
    INDEX (spot_id)
)");
$pdo->exec("CREATE TABLE reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    spot_id INT NOT NULL,
    user_id INT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(400) NOT NULL DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_review (spot_id, user_id)
)");
$pdo->exec("CREATE TABLE favorites (
    user_id INT NOT NULL,
    spot_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, spot_id)
)");

// ---------------------------------------------------------------- starting accounts (one per role)
$u = $pdo->prepare('INSERT INTO users (name, email, password_hash, role, phone, city, level, styles) VALUES (?,?,?,?,?,?,?,?)');
$u->execute(['Admin Sennara', 'admin@sennara.tn', password_hash(ADMIN_PASSWORD, PASSWORD_DEFAULT), 'admin', '', 'Tunis', 'expert', 'rock,boat']);
$u->execute(['Demo Seller', 'seller@sennara.tn', password_hash(DEMO_PASSWORD, PASSWORD_DEFAULT), 'seller', '', 'La Goulette', 'intermediate', 'port']);
$u->execute(['Demo Fisher', 'fisher@sennara.tn', password_hash(DEMO_PASSWORD, PASSWORD_DEFAULT), 'fisher', '', 'Tunis', 'beginner', 'shore,port']);

// ---------------------------------------------------------------- fishing spots
$spots = [
 ['goulette', 'La Goulette – harbour jetty', 'حلق الوادي – رصيف الميناء', 'La Goulette', 'حلق الوادي', 36.8084, 10.3088, 'port', 'dusk',
  'The harbour of La Goulette, the port of Tunis, has long jetties with deep water along the channel. Easy, safe ground for beginners and families. The TGM light railway stops in town.',
  "Le port de La Goulette, port de Tunis, offre de longues jetées avec de l'eau profonde le long du chenal. Un coin facile et sûr pour débuter ou pêcher en famille. Le TGM dessert la ville.",
  'ميناء حلق الوادي، ميناء العاصمة، فيه أرصفة طويلة بمياه عميقة على طول القناة. مكان سهل وآمن للمبتدئين والعائلات. يمرّ قطار الـTGM بالمدينة.'],
 ['sidibou', 'Sidi Bou Said – marina breakwater', 'سيدي بوسعيد – حاجز الميناء', 'Sidi Bou Said', 'سيدي بوسعيد', 36.8668, 10.3518, 'rock', 'dawn',
  'Rocky breakwater of the marina below the village of Sidi Bou Said, with clear water. Float fishing at first light works well for sea bream and sargo.',
  'Digue rocheuse du port de plaisance, sous le village de Sidi Bou Saïd, en eau claire. La pêche au flotteur au lever du jour réussit bien sur la daurade et le sar.',
  'حاجز صخري لميناء الترفيه تحت قرية سيدي بوسعيد، بمياه صافية. الصيد بالعوّامة عند الفجر ناجح مع الورقة والسرغوس.'],
 ['gammarth', 'Gammarth – beach', 'قمرت – الشاطئ', 'Gammarth', 'قمرت', 36.9052, 10.2983, 'shore', 'dusk',
  'Long sandy beach north of La Marsa. Best for surfcasting in the evening, when the beach is quiet and fish come close to shore.',
  'Longue plage de sable au nord de La Marsa. Idéale pour le surfcasting en soirée, quand la plage se vide et que le poisson se rapproche du bord.',
  'شاطئ رملي طويل شمال المرسى. مناسب للسيرفكاستينغ مساءً عندما يهدأ الشاطئ وتقترب الأسماك من الضفة.'],
 ['gharmelh', 'Ghar El Melh – lagoon', 'غار الملح – البحيرة', 'Ghar El Melh', 'غار الملح', 37.1678, 10.1731, 'lagoon', 'dawn',
  'Calm lagoon next to the old port and its three Ottoman forts. Shallow, sheltered water: light tackle and small hooks for mullet and sea bream.',
  'Lagune calme à côté du vieux port et de ses trois forts ottomans. Eau peu profonde et abritée : matériel léger et petits hameçons pour le mulet et la daurade.',
  'بحيرة هادئة بجانب الميناء القديم وحصونه العثمانية الثلاثة. مياه ضحلة ومحمية: معدات خفيفة وصنانير صغيرة للبوري والورقة.'],
 ['capblanc', 'Cap Blanc – Bizerte', 'الرأس الأبيض – بنزرت', 'Bizerte', 'بنزرت', 37.3380, 9.8369, 'rock', 'dawn',
  'White limestone cape north of Bizerte with deep water close to the rocks. For experienced anglers: rocky ground, exposed to north-westerly wind and swell.',
  "Cap calcaire blanc au nord de Bizerte, avec une eau profonde au pied des rochers. Pour pêcheurs confirmés : terrain rocheux exposé au vent et à la houle de nord-ouest.",
  'رأس كلسي أبيض شمال بنزرت بمياه عميقة قرب الصخور. للصيادين المتمرّسين: أرض صخرية معرّضة للرياح والأمواج الشمالية الغربية.'],
 ['bizerte', 'Bizerte – Old Port', 'بنزرت – الميناء القديم', 'Bizerte', 'بنزرت', 37.2768, 9.8753, 'port', 'night',
  'Picturesque old port of Bizerte, a channel lined with houses and fishing boats next to the medina. Quiet evenings and night fishing for mullet and sea bream.',
  'Vieux port pittoresque de Bizerte, un chenal bordé de maisons et de barques à côté de la médina. Soirées calmes et pêche de nuit au mulet et à la daurade.',
  'الميناء القديم في بنزرت، قناة تحيط بها البيوت وقوارب الصيد بجانب المدينة العتيقة. أمسيات هادئة وصيد ليلي للبوري والورقة.'],
 ['tabarka', 'Tabarka – Les Aiguilles', 'طبرقة – الإبر', 'Tabarka', 'طبرقة', 36.9563, 8.7561, 'rock', 'dawn',
  'The famous needle-shaped rocks on the Tabarka seafront, on the coral coast of the north-west. Rich rocky bottoms; take care on wet rocks.',
  'Les célèbres rochers en forme d\'aiguilles du front de mer de Tabarka, sur la côte du corail du nord-ouest. Fonds rocheux riches ; prudence sur les rochers mouillés.',
  'الصخور الإبرية الشهيرة على واجهة طبرقة البحرية، في ساحل المرجان شمال غرب البلاد. قيعان صخرية غنية؛ احذر الصخور المبللة.'],
 ['kelibia', 'Kélibia – fishing port', 'قليبية – ميناء الصيد', 'Kélibia', 'قليبية', 36.8338, 11.1115, 'port', 'night',
  'Busy fishing port below the Kélibia fortress on Cap Bon. Squid come near the harbour lights at night in autumn and winter.',
  "Port de pêche animé au pied de la forteresse de Kélibia, au Cap Bon. Les calamars s'approchent des lumières du port la nuit en automne et en hiver.",
  'ميناء صيد نشيط تحت حصن قليبية في الوطن القبلي. يقترب الكلمار من أضواء الميناء ليلاً في الخريف والشتاء.'],
 ['haouaria', 'El Haouaria – Cap Bon coast', 'الهوارية – ساحل الوطن القبلي', 'El Haouaria', 'الهوارية', 37.0498, 11.0116, 'rock', 'dawn',
  'Wild coast at the tip of Cap Bon, facing the Zembra islands, where strong currents meet. Big fish, but an exposed site: check wind and waves before you go.',
  'Côte sauvage à la pointe du Cap Bon, face aux îles Zembra, là où se croisent de forts courants. Gros poissons, mais site exposé : vérifiez vent et vagues avant de partir.',
  'ساحل برّي في طرف الوطن القبلي قبالة جزيرتي زمبرة وزمبرتة، حيث تلتقي تيارات قوية. أسماك كبيرة لكن المكان مكشوف: تحقّق من الريح والموج قبل الذهاب.'],
 ['hammamet', 'Hammamet – medina beach', 'الحمامات – شاطئ المدينة العتيقة', 'Hammamet', 'الحمامات', 36.3945, 10.6121, 'shore', 'night',
  'Sandy beach right below the walls of the Hammamet medina and its kasbah. Surfcasting after sunset, away from swimmers.',
  'Plage de sable au pied des remparts de la médina de Hammamet et de sa kasbah. Surfcasting après le coucher du soleil, loin des baigneurs.',
  'شاطئ رملي تحت أسوار المدينة العتيقة بالحمامات وقصبتها. سيرفكاستينغ بعد الغروب بعيداً عن السبّاحين.'],
 ['kantaoui', 'Port El Kantaoui – jetty', 'مرسى القنطاوي – الحاجز', 'Hammam Sousse', 'حمام سوسة', 35.8939, 10.5984, 'port', 'dusk',
  'Marina north of Sousse protected by long jetties. Easy access and lights at night; good for mullet, sea bream and garfish.',
  "Port de plaisance au nord de Sousse, protégé par de longues digues. Accès facile et éclairage la nuit ; bon pour le mulet, la daurade et l'orphie.",
  'ميناء ترفيهي شمال سوسة تحميه حواجز طويلة. وصول سهل وإنارة ليلاً؛ جيد للبوري والورقة والجارفيش.'],
 ['monastir', 'Monastir – Ribat rocks', 'المنستير – صخور الرباط', 'Monastir', 'المنستير', 35.7762, 10.8329, 'rock', 'dawn',
  'Rocky shore by the Ribat of Monastir and the marina, with clear, shallow water. Float fishing with bread or shrimp.',
  'Côte rocheuse près du Ribat de Monastir et de la marina, en eau claire et peu profonde. Pêche au flotteur au pain ou à la crevette.',
  'ساحل صخري قرب رباط المنستير والمرسى بمياه صافية وضحلة. صيد بالعوّامة بالخبز أو القمرون.'],
 ['mahdia', 'Mahdia – Cap Afrique', 'المهدية – رأس إفريقيا', 'Mahdia', 'المهدية', 35.5080, 11.0804, 'rock', 'dawn',
  'Rocky headland at the tip of the Mahdia peninsula, by the lighthouse and the seaside cemetery, open to the sea on three sides.',
  "Pointe rocheuse au bout de la presqu'île de Mahdia, près du phare et du cimetière marin, ouverte sur la mer de trois côtés.",
  'رأس صخري في طرف شبه جزيرة المهدية قرب المنارة والمقبرة البحرية، مفتوح على البحر من ثلاث جهات.'],
 ['kerkennah', 'Kerkennah – Sidi Fredj', 'قرقنة – سيدي فرج', 'Kerkennah', 'قرقنة', 34.6860, 11.1493, 'shore', 'day',
  'Very shallow coast of the Kerkennah islands, known for the traditional charfia fishery and for octopus. Reached by ferry from Sfax.',
  'Côte très peu profonde des îles Kerkennah, connue pour la pêche traditionnelle à la charfia et pour le poulpe. Accès par le bac depuis Sfax.',
  'ساحل ضحل جداً في جزر قرقنة، معروف بالصيد التقليدي بالشرفية وبالقرنيط. الوصول بالبطاح من صفاقس.'],
 ['djerba', 'Djerba – Aghir beach', 'جربة – شاطئ أغير', 'Djerba', 'جربة', 33.7603, 11.0159, 'shore', 'dusk',
  'Long beach on the east coast of Djerba with seagrass meadows close to shore. Surfcasting at dusk.',
  'Longue plage sur la côte est de Djerba, avec des herbiers près du bord. Surfcasting au crépuscule.',
  'شاطئ طويل على الساحل الشرقي لجربة مع أعشاب بحرية قريبة من الضفة. سيرفكاستينغ عند الغروب.'],
 ['zarzis', 'Zarzis – fishing port', 'جرجيس – ميناء الصيد', 'Zarzis', 'جرجيس', 33.4944, 11.1152, 'port', 'dusk',
  'Large fishing port of the south-east, on the Gulf of Gabès. Long quays and steady fishing all year round.',
  "Grand port de pêche du sud-est, sur le golfe de Gabès. Longs quais et pêche régulière toute l'année.",
  'ميناء صيد كبير في الجنوب الشرقي على خليج قابس. أرصفة طويلة وصيد منتظم طوال السنة.'],
 ['korbous', 'Korbous – cliffs', 'قربص – الجروف', 'Korbous', 'قربص', 36.8163, 10.5689, 'rock', 'dawn',
  'Steep rocky coast on the western side of Cap Bon, known for its hot springs. Deep water close in; the coast road leads to several rocky points.',
  'Côte rocheuse escarpée sur la face ouest du Cap Bon, connue pour ses sources thermales. Eau profonde près du bord ; la route côtière dessert plusieurs pointes.',
  'ساحل صخري شديد الانحدار في الجهة الغربية من الوطن القبلي، معروف بعيونه الحارة. مياه عميقة قرب الضفة؛ الطريق الساحلية توصل إلى عدّة رؤوس صخرية.'],
 ['rafraf', 'Raf Raf – beach', 'رفراف – الشاطئ', 'Raf Raf', 'رفراف', 37.1908, 10.1816, 'shore', 'dusk',
  'Wide sandy bay facing the islet of Pilau, with rocks at both ends. Surfcasting on the sand, float fishing on the rocky edges.',
  'Large baie de sable face à l\'îlot Pilau, avec des rochers aux deux extrémités. Surfcasting sur le sable, flotteur sur les bords rocheux.',
  'خليج رملي واسع قبالة جزيرة بيلاو، تحيط به صخور من الجهتين. سيرفكاستينغ على الرمل وعوّامة على الأطراف الصخرية.'],
];
$s = $pdo->prepare('INSERT INTO spots (slug, name, name_ar, city, city_ar, lat, lng, type, best_time, desc_en, desc_fr, desc_ar, image) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)');
$spotIds = [];
foreach ($spots as $row) {
    $s->execute(array_merge($row, ['img/spots/' . $row[0] . '.jpg']));
    $spotIds[$row[0]] = (int)$pdo->lastInsertId();
}

// ---------------------------------------------------------------- fish guide
$fish = [
 ['sparus', 'Sparus aurata', 'Gilthead seabream', 'Dorade royale', 'ورقة', 'Sparidae', 'rock,sand,port,lagoon', 9, 1, 'worm,shrimp,crab,mussel', 'bottom,float,surf', 20, 0, 'eu_ref', 70, 'LC', 'Q237046',
  'The most prized sparid of Tunisian anglers, recognisable by the golden band between its eyes. Its strong jaws crush shellfish and crabs.',
  'Le sparidé le plus recherché des pêcheurs tunisiens, reconnaissable au bandeau doré entre les yeux. Ses mâchoires puissantes broient coquillages et crabes.',
  'أشهر أسماك الإسبريات لدى الصيادين التونسيين، تُعرف بالشريط الذهبي بين عينيها. فكّاها القويان يكسران الصدفيات والسرطانات.'],
 ['labrax', 'Dicentrarchus labrax', 'European seabass', 'Loup (bar)', 'قاروص', 'Moronidae', 'rock,sand,port,lagoon', 10, 3, 'lure,live,shrimp,worm', 'lure,surf,float', 25, 0, 'eu_ref', 103, 'NT', 'Q217129',
  'A powerful predator that hunts in the foam of breaking waves, especially in winter and in rough weather.',
  "Un prédateur puissant qui chasse dans l'écume des vagues, surtout en hiver et par mer agitée.",
  'مفترس قوي يصطاد في زبد الأمواج، خاصة في الشتاء وعند اضطراب البحر.'],
 ['dsargus', 'Diplodus sargus', 'White seabream', 'Sar commun', 'سرغوس', 'Sparidae', 'rock,port', 10, 4, 'shrimp,worm,mussel,crab', 'float,bottom', 23, 0, 'eu_ref', 45, 'LC', 'Q1989807',
  'Lives close to rocks and breakwaters and feeds in the white water after a swell. Recognisable by its dark vertical bars.',
  "Vit près des rochers et des digues et se nourrit dans l'écume après un coup de mer. Reconnaissable à ses bandes verticales sombres.",
  'يعيش قرب الصخور والحواجز ويتغذّى في الماء المضطرب بعد هيجان البحر. يُعرف بخطوطه العمودية الداكنة.'],
 ['dvulgaris', 'Diplodus vulgaris', 'Two-banded seabream', 'Sar à tête noire', 'سرغوس ذو الشريطين', 'Sparidae', 'rock,port', 4, 11, 'shrimp,worm,mussel', 'float,bottom', 18, 0, 'eu_ref', 45, 'LC', 'Q232284',
  'A common sargo of harbours and rocky shores, with two black bands: one behind the head and one before the tail.',
  "Sar commun des ports et des côtes rocheuses, avec deux bandes noires : l'une derrière la tête, l'autre avant la queue.",
  'سرغوس شائع في الموانئ والسواحل الصخرية، له شريطان أسودان: الأول خلف الرأس والثاني قبل الذيل.'],
 ['dannularis', 'Diplodus annularis', 'Annular seabream', 'Sparaillon', 'سبارس', 'Sparidae', 'port,sand,lagoon', 4, 10, 'worm,shrimp,bread', 'float,bottom', 12, 0, 'eu_ref', 24, 'LC', 'Q1265801',
  'A small sparid of seagrass meadows and harbours, with a black ring at the base of the tail. Great fun for beginners and children.',
  'Petit sparidé des herbiers et des ports, avec un anneau noir à la base de la queue. Idéal pour les débutants et les enfants.',
  'سمكة صغيرة من الإسبريات تعيش في الأعشاب البحرية والموانئ، لها حلقة سوداء عند قاعدة الذيل. ممتعة للمبتدئين والأطفال.'],
 ['mormyrus', 'Lithognathus mormyrus', 'Sand steenbras', 'Marbré', 'منكوس', 'Sparidae', 'sand', 5, 10, 'worm,mussel', 'surf,bottom', 20, 0, 'eu_ref', 55, 'LC', 'Q2016506',
  'The classic surfcasting fish of Tunisian sandy beaches, silver with thin dark vertical stripes. It digs worms out of the sand.',
  'Le poisson classique du surfcasting sur les plages tunisiennes, argenté avec de fines rayures verticales sombres. Il fouille le sable à la recherche de vers.',
  'سمكة السيرفكاستينغ التقليدية على الشواطئ الرملية التونسية، فضية بخطوط عمودية رفيعة داكنة. تنبش الرمل بحثاً عن الديدان.'],
 ['pagellus', 'Pagellus erythrinus', 'Common pandora', 'Pageot commun', 'مرجان', 'Sparidae', 'boat,sand', 5, 10, 'shrimp,squid,worm', 'bottom', 15, 0, 'eu_ref', 60, 'LC', 'Q1156691',
  'A pink sparid of sandy and rocky bottoms, usually between 20 and 100 m deep. Mostly caught from a boat.',
  'Sparidé rose des fonds sableux et rocheux, généralement entre 20 et 100 m. Se pêche surtout en bateau.',
  'سمكة وردية من الإسبريات تعيش على القيعان الرملية والصخرية، عادة بين 20 و100 متر. تُصاد غالباً من القارب.'],
 ['mullus', 'Mullus surmuletus', 'Striped red mullet', 'Rouget de roche', 'تريليا', 'Mullidae', 'sand,rock', 5, 10, 'worm,shrimp', 'bottom', 11, 0, 'eu_ref', 40, 'LC', 'Q7119',
  'Searches the bottom with its two chin barbels. Small hooks and fresh worms work best.',
  'Fouille le fond avec ses deux barbillons. Petits hameçons et vers frais donnent les meilleurs résultats.',
  'يبحث في القاع بشاربيه تحت الذقن. الصنانير الصغيرة والدود الطازج هي الأنجح.'],
 ['mugil', 'Mugil cephalus', 'Flathead grey mullet', 'Mulet cabot', 'بوري', 'Mugilidae', 'port,lagoon,sand', 1, 12, 'bread,worm', 'float', 0, 0, 'none', 100, 'LC', 'Q234014',
  'Present all year in ports and lagoons, often in schools near the surface. Shy: use a fine line, small hooks and bread paste.',
  "Présent toute l'année dans les ports et les lagunes, souvent en bancs près de la surface. Méfiant : fil fin, petits hameçons et pâte de pain.",
  'موجود طوال السنة في الموانئ والبحيرات، غالباً في أسراب قرب السطح. حذر: استعمل خيطاً رفيعاً وصنانير صغيرة وعجينة الخبز.'],
 ['grouper', 'Epinephelus marginatus', 'Dusky grouper', 'Mérou brun', 'مناني', 'Serranidae', 'rock,boat', 5, 10, 'live,squid,sardine', 'bottom,live', 45, 0, 'eu_ref', 150, 'VU', 'Q192167',
  'A large, slow-growing grouper of deep rocky areas, listed as Vulnerable by the IUCN. Consider releasing it.',
  "Grand mérou des fonds rocheux, à la croissance lente, classé Vulnérable par l'UICN. Pensez à le relâcher.",
  'مناني كبير بطيء النمو يعيش في القيعان الصخرية العميقة، مصنّف «معرّض للخطر» لدى الاتحاد الدولي لحفظ الطبيعة. فكّر في إطلاقه.'],
 ['amberjack', 'Seriola dumerili', 'Greater amberjack', 'Sériole couronnée', 'سريولة', 'Carangidae', 'boat,rock', 6, 11, 'live,lure', 'live,jig', 0, 0, 'none', 190, 'LC', 'Q575704',
  'A fast, very strong predator of headlands and offshore reefs, caught with live bait or heavy jigs.',
  'Prédateur rapide et très puissant des pointes et des tombants au large, pêché au vif ou au jig lourd.',
  'مفترس سريع وقوي جداً يعيش قرب الرؤوس والشعاب البعيدة، يُصاد بالطُّعم الحي أو بالجيغ الثقيل.'],
 ['bluefish', 'Pomatomus saltatrix', 'Bluefish', 'Tassergal', 'سمكة زرقاء', 'Pomatomidae', 'sand,rock,boat', 9, 12, 'lure,sardine,live', 'lure,surf', 0, 0, 'none', 130, 'VU', 'Q461432',
  'An aggressive schooling predator with sharp teeth that hunts small fish along beaches in autumn. Use a thick or steel leader.',
  "Prédateur grégaire agressif aux dents acérées, qui chasse les petits poissons le long des plages en automne. Utilisez un bas de ligne épais ou en acier.",
  'مفترس عدواني يعيش في أسراب وله أسنان حادة، يطارد الأسماك الصغيرة على الشواطئ في الخريف. استعمل خيطاً سميكاً أو فولاذياً.'],
 ['mackerel', 'Scomber colias', 'Atlantic chub mackerel', 'Maquereau espagnol', 'سكمبري', 'Scombridae', 'port,boat,rock', 5, 10, 'sardine,lure,shrimp', 'float,lure', 18, 0, 'eu_ref', 50, 'LC', 'Q1934719',
  'Schools of mackerel enter harbours in the warm months. Small lures or sardine strips catch them fast.',
  'Les bancs de maquereaux entrent dans les ports pendant les mois chauds. Petits leurres ou lanières de sardine les prennent vite.',
  'تدخل أسراب السكمبري الموانئ في الأشهر الدافئة. الطُّعوم الاصطناعية الصغيرة أو شرائح السردين تصطادها بسرعة.'],
 ['garfish', 'Belone belone', 'Garfish', 'Orphie', 'جارفيش', 'Belonidae', 'port,rock', 3, 6, 'sardine,lure', 'float,lure', 0, 0, 'none', 0, 'LC', 'Q643373',
  'A slim surface fish with a long beak and green bones, often seen jumping near harbour entrances in spring.',
  "Poisson de surface fin au long bec et aux arêtes vertes, souvent vu sautant à l'entrée des ports au printemps.",
  'سمكة سطحية نحيفة بمنقار طويل وعظام خضراء، تُرى غالباً تقفز عند مداخل الموانئ في الربيع.'],
 ['salema', 'Sarpa salpa', 'Salema', 'Saupe', 'شلبة', 'Sparidae', 'rock,port', 1, 12, 'bread', 'float', 0, 0, 'none', 51, 'LC', 'Q1130118',
  'A plant-eating sparid with golden stripes that grazes on algae in large schools. Caught on bread or algae.',
  'Sparidé herbivore aux rayures dorées qui broute les algues en grands bancs. Se prend au pain ou aux algues.',
  'سمكة عاشبة من الإسبريات بخطوط ذهبية، ترعى الطحالب في أسراب كبيرة. تُصاد بالخبز أو الطحالب.'],
 ['oblada', 'Oblada melanura', 'Saddled seabream', 'Oblade', 'منوري', 'Sparidae', 'rock,port', 4, 10, 'bread,shrimp', 'float', 0, 0, 'none', 34, 'LC', 'Q1771633',
  'A silvery sparid with a black saddle before the tail. It swims in mid-water near rocks, so fish your bait just under the float.',
  'Sparidé argenté avec une selle noire avant la queue. Il nage entre deux eaux près des rochers : pêchez juste sous le flotteur.',
  'سمكة فضية من الإسبريات لها بقعة سوداء قبل الذيل. تسبح في منتصف الماء قرب الصخور، فاجعل الطُّعم تحت العوّامة مباشرة.'],
 ['octopus', 'Octopus vulgaris', 'Common octopus', 'Poulpe', 'قرنيط', 'Octopodidae', 'rock,port,sand', 11, 3, 'crab,sardine', 'jig', 0, 1000, 'tn_rule', 0, 'LC', 'Q651361',
  'Hides in holes between rocks and matters a lot to Tunisian fisheries. Tunisian rules: minimum weight 1 kg, and no fishing during the yearly closure set by the Ministry.',
  "Se cache dans les trous entre les rochers et compte beaucoup pour la pêche tunisienne. Règles en Tunisie : poids minimal 1 kg, et pêche interdite pendant la fermeture annuelle fixée par le ministère.",
  'يختبئ في الشقوق بين الصخور وله أهمية كبيرة في الصيد التونسي. القواعد في تونس: الوزن الأدنى 1 كغ، والصيد ممنوع خلال فترة الغلق السنوية التي تحدّدها الوزارة.'],
 ['cuttlefish', 'Sepia officinalis', 'Common cuttlefish', 'Seiche', 'شوبي', 'Sepiidae', 'port,sand,lagoon', 2, 5, 'jig,live', 'jig', 0, 0, 'none', 0, 'LC', 'Q756724',
  'Comes into shallow seagrass beds and harbours in spring to breed. Caught with a slowly worked jig.',
  'Entre dans les herbiers peu profonds et les ports au printemps pour se reproduire. Se pêche à la turlutte animée lentement.',
  'يقترب من الأعشاب البحرية الضحلة والموانئ في الربيع للتكاثر. يُصاد بطُعم اصطناعي يُحرَّك ببطء.'],
 ['squid', 'Loligo vulgaris', 'European squid', 'Calmar', 'كلمار', 'Loliginidae', 'port,boat', 10, 2, 'jig', 'jig', 0, 0, 'none', 0, 'DD', 'Q1232698',
  'Attracted to lights at night, especially in autumn and winter. Fish from lit quays with a small squid jig.',
  'Attiré par la lumière la nuit, surtout en automne et en hiver. Pêchez depuis les quais éclairés avec une petite turlutte.',
  'ينجذب إلى الضوء ليلاً، خاصة في الخريف والشتاء. اصطد من الأرصفة المضاءة بطُعم اصطناعي صغير.'],
 ['dorado', 'Coryphaena hippurus', 'Mahi-mahi', 'Coryphène (lampuga)', 'لمبوكة', 'Coryphaenidae', 'boat', 8, 11, 'lure,live,sardine', 'lure', 35, 0, 'tn_rule', 210, 'LC', 'Q14443',
  'A fast, colourful offshore fish that gathers under floating objects. In Tunisia the season opens on 15 August and the minimum size is 35 cm.',
  'Poisson du large, rapide et coloré, qui se regroupe sous les objets flottants. En Tunisie, la saison ouvre le 15 août et la taille minimale est de 35 cm.',
  'سمكة سريعة وملوّنة من عرض البحر تتجمّع تحت الأجسام الطافية. في تونس يُفتح موسمها يوم 15 أوت والحجم الأدنى 35 صم.'],
 ['barracuda', 'Sphyraena viridensis', 'Yellowmouth barracuda', 'Bécune à bouche jaune', 'باراكودا', 'Sphyraenidae', 'rock,port', 6, 10, 'lure,live', 'lure', 0, 0, 'none', 128, 'LC', 'Q2697585',
  'Hunts in groups near rocky points and harbour mouths, mostly at dawn and dusk. Fast lures work well.',
  "Chasse en groupe près des pointes rocheuses et des entrées de port, surtout à l'aube et au crépuscule. Les leurres rapides fonctionnent bien.",
  'تصطاد في مجموعات قرب الرؤوس الصخرية ومداخل الموانئ، خاصة عند الفجر والغروب. الطُّعوم الاصطناعية السريعة ناجحة.'],
 ['sole', 'Solea solea', 'Common sole', 'Sole', 'صول', 'Soleidae', 'sand', 10, 3, 'worm', 'surf,bottom', 20, 0, 'eu_ref', 70, 'DD', 'Q27773',
  'A flatfish that hides in the sand by day and feeds at night. Surfcasting with worms on calm winter nights.',
  "Poisson plat qui se cache dans le sable le jour et se nourrit la nuit. Surfcasting aux vers par nuits calmes d'hiver.",
  'سمكة مفلطحة تختبئ في الرمل نهاراً وتتغذّى ليلاً. سيرفكاستينغ بالدود في ليالي الشتاء الهادئة.'],
 ['dentex', 'Dentex dentex', 'Common dentex', 'Denté commun', 'عضاض', 'Sparidae', 'boat,rock', 4, 10, 'live,squid,jig', 'live,jig', 0, 0, 'none', 100, 'VU', 'Q143503',
  'A large predatory sparid of deep rocky areas, with strong canine teeth. Listed as Vulnerable; usually fished from a boat.',
  'Grand sparidé prédateur des fonds rocheux profonds, aux fortes canines. Classé Vulnérable ; se pêche surtout en bateau.',
  'سمكة مفترسة كبيرة من الإسبريات تعيش في القيعان الصخرية العميقة ولها أنياب قوية. مصنّفة «معرّضة للخطر»؛ تُصاد غالباً من القارب.'],
 ['horsemackerel', 'Trachurus trachurus', 'Atlantic horse mackerel', 'Chinchard', 'قدقود', 'Carangidae', 'port,boat', 4, 11, 'sardine,lure,shrimp', 'float,lure', 15, 0, 'eu_ref', 70, 'VU', 'Q842956',
  'Forms large schools near harbours and is easy to catch with small feathers or bits of sardine. Often used as live bait.',
  'Forme de grands bancs près des ports et se prend facilement aux petites plumes ou aux morceaux de sardine. Souvent utilisé comme vif.',
  'يكوّن أسراباً كبيرة قرب الموانئ ويُصاد بسهولة بالريش الصغير أو قطع السردين. يُستعمل غالباً كطُعم حي.'],
 ['conger', 'Conger conger', 'European conger', 'Congre', 'قنجر', 'Congridae', 'rock,port', 10, 4, 'sardine,squid', 'bottom', 0, 0, 'none', 300, 'LC', 'Q212552',
  'A large eel that hides in holes in breakwaters and comes out at night. Use strong tackle and fish baits on the bottom.',
  'Grande anguille qui se cache dans les trous des digues et sort la nuit. Matériel solide et appâts de poisson posés au fond.',
  'حنش بحري كبير يختبئ في ثقوب الحواجز ويخرج ليلاً. استعمل معدات قوية وطُعماً من السمك على القاع.'],
];
$imageOf = [
 'sparus' => 'sparus_aurata', 'labrax' => 'dicentrarchus_labrax', 'dsargus' => 'diplodus_sargus', 'dvulgaris' => 'diplodus_vulgaris',
 'dannularis' => 'diplodus_annularis', 'mormyrus' => 'lithognathus_mormyrus', 'pagellus' => 'pagellus_erythrinus',
 'mullus' => 'mullus_surmuletus', 'mugil' => 'mugil_cephalus', 'grouper' => 'epinephelus_marginatus', 'amberjack' => 'seriola_dumerili',
 'bluefish' => 'pomatomus_saltatrix', 'mackerel' => 'scomber_colias', 'garfish' => 'belone_belone', 'salema' => 'sarpa_salpa',
 'oblada' => 'oblada_melanura', 'octopus' => 'octopus_vulgaris', 'cuttlefish' => 'sepia_officinalis', 'squid' => 'loligo_vulgaris',
 'dorado' => 'coryphaena_hippurus', 'barracuda' => 'sphyraena_viridensis', 'sole' => 'solea_solea', 'dentex' => 'dentex_dentex',
 'horsemackerel' => 'trachurus_trachurus', 'conger' => 'conger_conger',
];
$f = $pdo->prepare('INSERT INTO fish (slug, sci_name, name_en, name_fr, name_ar, family, habitats, month_from, month_to, baits, techniques,
    min_size_cm, min_weight_g, size_rule, max_cm, iucn, wikidata, about_en, about_fr, about_ar, image) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)');
$fishIds = [];
foreach ($fish as $row) {
    $f->execute(array_merge($row, ['img/fish/' . $imageOf[$row[0]] . '.jpg']));
    $fishIds[$row[0]] = (int)$pdo->lastInsertId();
}

$spotFish = [
 'goulette' => 'mugil,sparus,dvulgaris,dannularis,garfish,mackerel,horsemackerel,squid,cuttlefish,labrax',
 'sidibou' => 'sparus,dsargus,dvulgaris,oblada,salema,labrax,octopus,mugil',
 'gammarth' => 'mormyrus,sparus,labrax,bluefish,sole,mullus',
 'gharmelh' => 'mugil,sparus,labrax,cuttlefish,dannularis',
 'capblanc' => 'dsargus,sparus,grouper,amberjack,dentex,labrax,conger,octopus',
 'bizerte' => 'mugil,sparus,labrax,dvulgaris,conger,squid',
 'tabarka' => 'dsargus,dvulgaris,grouper,dentex,oblada,octopus,barracuda',
 'kelibia' => 'squid,sparus,mugil,mackerel,horsemackerel,dorado,cuttlefish',
 'haouaria' => 'dsargus,amberjack,dentex,pagellus,barracuda,bluefish',
 'hammamet' => 'mormyrus,sparus,labrax,sole,bluefish',
 'kantaoui' => 'mugil,sparus,garfish,dvulgaris,salema,barracuda',
 'monastir' => 'salema,oblada,dsargus,dvulgaris,octopus,mullus',
 'mahdia' => 'dsargus,sparus,grouper,dorado,octopus,pagellus',
 'kerkennah' => 'octopus,cuttlefish,sparus,mormyrus,dannularis,mugil',
 'djerba' => 'mormyrus,sparus,labrax,sole,mullus,cuttlefish',
 'zarzis' => 'mugil,sparus,octopus,cuttlefish,horsemackerel,mackerel',
 'korbous' => 'dsargus,sparus,dentex,conger,oblada,barracuda',
 'rafraf' => 'mormyrus,sparus,labrax,bluefish,dsargus',
];
$sf = $pdo->prepare('INSERT INTO spot_fish (spot_id, fish_id) VALUES (?,?)');
foreach ($spotFish as $spot => $list) {
    foreach (explode(',', $list) as $slug) $sf->execute([$spotIds[$spot], $fishIds[$slug]]);
}

// ---------------------------------------------------------------- shops
$shops = [
 [null, 'node/8988158913', 'Le Marin', 'Hammamet', '', '+216 53 333 926', 'https://www.facebook.com/LeMarinHammamet/', 36.3978019, 10.6196579,
  'Fishing shop in Hammamet (OpenStreetMap data).'],
 [null, 'node/3085529033', 'Matériels de pêche – Kirwana', 'Sayada', '', '', '', 35.668883, 10.8901917,
  'Fishing equipment shop in Sayada, Monastir governorate (OpenStreetMap data).'],
 [null, 'node/7619235298', 'Matériels de pêche', 'Nabeul', '', '', '', 36.4602701, 10.7473843,
  'Fishing equipment shop in Nabeul governorate (OpenStreetMap data).'],
 [null, 'node/8941964377', 'السمكة الذهبية', 'Ariana', '', '', '', 36.8591619, 10.1917027,
  'Fishing shop in the Ariana governorate (OpenStreetMap data).'],
 [null, 'node/8941976855', 'SPACA – matériel de pêche', 'Ariana', '', '', '', 36.8593963, 10.1915656,
  'Sells fishing equipment, seeds, plants and fertiliser (OpenStreetMap data).'],
 [2, '', 'Demo shop (seller account)', 'La Goulette', 'Demo address', '', '', 36.8135, 10.3050,
  'Demo shop managed with the seller test account to show how sellers add and update their products.'],
];
$sh = $pdo->prepare('INSERT INTO shops (owner_id, osm_id, name, city, address, phone, website, lat, lng, description, approved) VALUES (?,?,?,?,?,?,?,?,?,?,1)');
foreach ($shops as $row) $sh->execute($row);
$demoShop = (int)$pdo->lastInsertId();
$p = $pdo->prepare('INSERT INTO products (shop_id, name, category, price, in_stock) VALUES (?,?,?,?,?)');
foreach ([
 ['Live shrimp (100 g)', 'bait', 6.5, 1], ['Sea worms (box)', 'bait', 4.0, 1], ['Frozen sardines (500 g)', 'bait', 3.5, 1],
 ['Telescopic rod 3.6 m', 'rod', 85, 1], ['Spinning reel 4000', 'reel', 95, 1], ['Nylon line 0.30 mm (300 m)', 'line', 12, 1],
 ['Hooks n°6 (pack of 20)', 'hook', 3, 0], ['Squid jig 3.0', 'other', 14, 1],
] as $row) $p->execute(array_merge([$demoShop], $row));

// ---------------------------------------------------------------- tutorials (steps are separated by new lines)
$yt = 'https://www.youtube.com/results?search_query=';
$tutorials = [
 ['kit', 'beginner', 4, 'Your first shore fishing kit', 'Votre premier équipement de pêche du bord', 'عُدّتك الأولى للصيد من الشاطئ',
  "Take a 3 to 4 m rod: a telescopic one is easy to carry.\nAdd a spinning reel of size 3000–4000 filled with 0.25–0.30 mm nylon.\nCarry hooks from n°4 to n°10, a few sinkers and two or three floats.\nBring a bucket, a cloth, pliers and a small knife.\nStart simple: after a few outings you will know what you really need.",
  "Prenez une canne de 3 à 4 m : une télescopique se transporte facilement.\nAjoutez un moulinet taille 3000–4000 garni de nylon 0,25–0,30 mm.\nPrévoyez des hameçons du n°4 au n°10, quelques plombs et deux ou trois flotteurs.\nEmportez un seau, un chiffon, une pince et un petit couteau.\nCommencez simple : après quelques sorties, vous saurez ce qu'il vous faut vraiment.",
  "خذ قصبة طولها من 3 إلى 4 أمتار، والتلسكوبية سهلة الحمل.\nأضف بكرة مقاس 3000–4000 مع خيط نايلون 0.25–0.30 مم.\nاحمل صنانير من رقم 4 إلى رقم 10 وبعض الأثقال وعوّامتين أو ثلاثاً.\nخذ معك دلواً وقطعة قماش وكمّاشة وسكيناً صغيرة.\nابدأ ببساطة: بعد بضع خرجات ستعرف ما تحتاجه فعلاً.",
  'img/tutorials/kit.jpg', $yt . 'shore+fishing+beginner+setup', 1],
 ['knots', 'beginner', 3, 'Three knots every angler should know', 'Trois nœuds que tout pêcheur doit connaître', 'ثلاث عُقد يجب أن يعرفها كل صياد',
  "Clinch knot: ties the hook or swivel to the line.\nPalomar knot: very strong, ideal for braided line.\nDouble uni knot: joins two lines together.\nAlways wet the line before tightening and pull slowly.\nCut the tag end short, but not flush with the knot.",
  "Nœud clinch : pour attacher l'hameçon ou l'émerillon à la ligne.\nNœud palomar : très solide, idéal pour la tresse.\nNœud double uni : pour relier deux fils.\nMouillez toujours le fil avant de serrer et tirez lentement.\nCoupez l'excédent court, mais pas au ras du nœud.",
  "عقدة «كلينش»: لربط الصنارة أو المِدوار بالخيط.\nعقدة «بالومار»: قوية جداً ومثالية للخيط المضفور.\nعقدة «يوني» المزدوجة: لوصل خيطين.\nبلّل الخيط دائماً قبل الشدّ واسحب ببطء.\nاقطع الزائد قصيراً لكن ليس ملاصقاً للعقدة.",
  'img/tutorials/knot.jpg', $yt . 'fishing+knots+clinch+palomar+uni', 2],
 ['bait', 'beginner', 4, 'Choosing the right bait', 'Bien choisir son appât', 'اختيار الطُّعم المناسب',
  "Shrimp catches almost every fish of the Tunisian coast.\nSea worms are the best bait for sea bream and sand steenbras on sandy beaches.\nBread paste is cheap and works very well for mullet and salema in ports.\nSquid and sardine strips stay on the hook and are good at night.\nKeep baits cool and fresh: fish notice the difference.",
  "La crevette prend presque tous les poissons des côtes tunisiennes.\nLes vers marins sont le meilleur appât pour la daurade et le marbré sur les plages de sable.\nLa pâte de pain est économique et marche très bien pour le mulet et la saupe dans les ports.\nLes lanières de calmar et de sardine tiennent à l'hameçon et sont efficaces la nuit.\nGardez vos appâts au frais : le poisson fait la différence.",
  "القمرون يصطاد تقريباً كل أسماك السواحل التونسية.\nدود البحر هو أفضل طُعم للورقة والمنكوس على الشواطئ الرملية.\nعجينة الخبز رخيصة وناجحة جداً مع البوري والشلبة في الموانئ.\nشرائح الكلمار والسردين تثبت على الصنارة وهي فعّالة ليلاً.\nحافظ على الطُّعم بارداً وطازجاً: السمك يلاحظ الفرق.",
  'img/tutorials/bait.jpg', $yt . 'best+bait+sea+fishing+mediterranean', 3],
 ['weather', 'beginner', 5, 'Reading the weather and the sea', 'Lire la météo et la mer', 'قراءة الطقس والبحر',
  "Fish feed most around sunrise and sunset: Sennara calls these the dawn and dusk windows.\nA light breeze and small waves stir up food; a flat calm sea is often slower.\nFalling air pressure before a change of weather usually makes fish more active.\nAround the new moon and the full moon tides are stronger and fish move more.\nNever go on rocks with big waves or strong gusts: no fish is worth the risk.",
  "Les poissons se nourrissent surtout au lever et au coucher du soleil : Sennara les appelle les créneaux de l'aube et du crépuscule.\nUne brise légère et de petites vagues remuent la nourriture ; une mer d'huile est souvent moins productive.\nUne pression qui baisse avant un changement de temps rend souvent les poissons plus actifs.\nAutour de la nouvelle lune et de la pleine lune, les marées sont plus fortes et les poissons bougent davantage.\nN'allez jamais sur les rochers avec de grosses vagues ou de fortes rafales : aucun poisson ne vaut ce risque.",
  "تتغذّى الأسماك أكثر عند شروق الشمس وغروبها، وتسمّي صنّارة هذه الفترات «وقت الفجر» و«وقت الغروب».\nالنسيم الخفيف والأمواج الصغيرة يحرّكان الغذاء، أما البحر الساكن تماماً فغالباً ما يكون أبطأ.\nانخفاض الضغط الجوي قبل تغيّر الطقس يجعل الأسماك عادة أكثر نشاطاً.\nحول المحاق والبدر يكون المدّ أقوى وتتحرّك الأسماك أكثر.\nلا تصعد أبداً على الصخور مع الأمواج العالية أو الهبّات القوية: لا تستحق أي سمكة هذا الخطر.",
  'img/tutorials/sea.jpg', $yt . 'how+weather+moon+pressure+affect+fishing', 4],
 ['float', 'intermediate', 4, 'Float fishing from the rocks', 'La pêche au flotteur depuis les rochers', 'الصيد بالعوّامة من الصخور',
  "Set the float so the bait drifts just above the bottom or the weed.\nThrow a little groundbait (bread, sardine) to bring the fish closer.\nStrike when the float goes under, not at the first tremble.\nFish the foam line after a swell: sea bream and sargo feed there.\nStand back from the edge and never turn your back to the waves.",
  "Réglez le flotteur pour que l'appât dérive juste au-dessus du fond ou des algues.\nLancez un peu d'amorce (pain, sardine) pour attirer le poisson.\nFerrez quand le flotteur plonge, pas au premier frémissement.\nPêchez la ligne d'écume après un coup de mer : daurades et sars s'y nourrissent.\nRestez en retrait du bord et ne tournez jamais le dos aux vagues.",
  "اضبط العوّامة ليسبح الطُّعم فوق القاع أو الأعشاب بقليل.\nارمِ قليلاً من الطُّعم المفروم (خبز، سردين) لجذب السمك.\nاسحب عندما تغطس العوّامة، لا عند أول اهتزاز.\nاصطد عند خط الزبد بعد هيجان البحر، فهناك تتغذّى الورقة والسرغوس.\nابتعد عن الحافة ولا تدر ظهرك للأمواج أبداً.",
  'img/tutorials/float.jpg', $yt . 'float+fishing+from+rocks+sea', 5],
 ['surf', 'intermediate', 5, 'Surfcasting on sandy beaches', 'Le surfcasting sur les plages de sable', 'السيرفكاستينغ على الشواطئ الرملية',
  "Use a 4 to 4.5 m rod and a 80–120 g sinker to reach the second line of waves.\nLook for darker water between sandbanks: it is a channel where fish travel.\nFish the two hours around sunset and the first hours of the night.\nKeep the line tight and watch the rod tip in a rod holder.\nWorms are the number one bait for sand steenbras, sea bream and sole.",
  "Utilisez une canne de 4 à 4,5 m et un plomb de 80 à 120 g pour atteindre la deuxième ligne de vagues.\nCherchez l'eau plus sombre entre les bancs de sable : c'est une baïne où passent les poissons.\nPêchez les deux heures autour du coucher du soleil et le début de la nuit.\nGardez la ligne tendue et surveillez le scion, canne posée sur un pique.\nLes vers sont l'appât n°1 pour le marbré, la daurade et la sole.",
  "استعمل قصبة بطول 4 إلى 4.5 م وثقلاً بين 80 و120 غ لتصل إلى خط الأمواج الثاني.\nابحث عن الماء الأغمق بين الكثبان الرملية: هو مجرى تمرّ منه الأسماك.\nاصطد خلال الساعتين حول الغروب وبداية الليل.\nأبقِ الخيط مشدوداً وراقب رأس القصبة وهي على الحامل.\nالدود هو الطُّعم الأول للمنكوس والورقة والصول.",
  'img/tutorials/surf.jpg', $yt . 'surfcasting+techniques+beach', 6],
 ['squid', 'advanced', 4, 'Squid fishing at night', 'Pêcher le calmar de nuit', 'صيد الكلمار ليلاً',
  "From autumn to winter squid come close to lit quays and harbour lights.\nCast a size 2.5–3.0 squid jig and let it sink for a few seconds.\nLift the rod twice sharply, then let the jig fall again on a tight line.\nMost bites come while the jig is sinking: feel for extra weight.\nReel in steadily without jerking, and watch out for the ink.",
  "De l'automne à l'hiver, les calmars s'approchent des quais éclairés et des lumières du port.\nLancez une turlutte taille 2.5–3.0 et laissez-la couler quelques secondes.\nDonnez deux tirées sèches, puis laissez redescendre la turlutte, fil tendu.\nLa plupart des touches ont lieu à la descente : guettez un poids inhabituel.\nRamenez régulièrement, sans à-coups, et attention à l'encre.",
  "من الخريف إلى الشتاء يقترب الكلمار من الأرصفة المضاءة وأضواء الميناء.\nارمِ طُعماً اصطناعياً للكلمار مقاس 2.5–3.0 واتركه يغوص بضع ثوانٍ.\nارفع القصبة مرتين بقوة ثم اترك الطُّعم ينزل والخيط مشدود.\nأغلب الضربات تأتي أثناء النزول: انتبه لأي ثقل غير عادي.\nاسحب بانتظام دون نتر، واحذر الحبر.",
  'img/fish/loligo_vulgaris.jpg', $yt . 'eging+squid+fishing+night', 7],
 ['release', 'beginner', 3, 'Catch and release, the right way', 'Remettre un poisson à l\'eau correctement', 'إطلاق السمكة بالطريقة الصحيحة',
  "Wet your hands before touching the fish to protect its skin.\nRemove the hook quickly with pliers; cut the line if the hook is swallowed deep.\nKeep the fish in the water or out of it for only a few seconds.\nRelease every fish below the size limit and the protected species.\nTake home only what you will eat, and leave no line or plastic behind.",
  "Mouillez-vous les mains avant de toucher le poisson pour protéger sa peau.\nRetirez l'hameçon rapidement avec une pince ; coupez le fil s'il est avalé profondément.\nGardez le poisson dans l'eau, ou hors de l'eau quelques secondes seulement.\nRelâchez chaque poisson sous la taille minimale et les espèces protégées.\nNe gardez que ce que vous mangerez, et ne laissez ni fil ni plastique.",
  "بلّل يديك قبل لمس السمكة لحماية جلدها.\nانزع الصنارة بسرعة بالكمّاشة، واقطع الخيط إذا كانت الصنارة مبتلعة بعمق.\nأبقِ السمكة في الماء، أو خارجه لبضع ثوانٍ فقط.\nأطلق كل سمكة أصغر من الحجم الأدنى وكل الأنواع المحمية.\nخذ فقط ما ستأكله، ولا تترك خيطاً أو بلاستيكاً وراءك.",
  'img/tutorials/release.jpg', $yt . 'catch+and+release+how+to', 8],
];
$t = $pdo->prepare('INSERT INTO tutorials (slug, level, minutes, title_en, title_fr, title_ar, body_en, body_fr, body_ar, image, video_url, sort_order) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)');
foreach ($tutorials as $row) $t->execute($row);

// ---------------------------------------------------------------- rules (with their sources)
$rules = [
 ['tn', 'Octopus: minimum weight 1 kg', 'Poulpe : poids minimal 1 kg', 'القرنيط: الوزن الأدنى 1 كغ',
  'The Ministry of Agriculture reminds that octopus under 1 kg may not be caught. Octopus fishing is also closed during a yearly closure; in 2026 it was extended until 15 November.',
  "Le ministère de l'Agriculture rappelle que le poulpe de moins de 1 kg ne peut pas être pêché. La pêche est aussi fermée pendant une période annuelle ; en 2026, elle a été prolongée jusqu'au 15 novembre.",
  'تذكّر وزارة الفلاحة بأنه يُمنع صيد القرنيط الذي يقلّ وزنه عن 1 كغ. كما يُغلق صيده خلال فترة سنوية، وقد مُدّدت سنة 2026 إلى غاية 15 نوفمبر.',
  'https://www.webdo.tn/fr/actualite/national/peche-au-poulpe-linterdiction-prolongee-jusquau-15-novembre/405590/', 1],
 ['tn', 'Coryphène (lampuga): opens 15 August, 35 cm minimum', 'Coryphène (lampouka) : ouverture le 15 août, 35 cm minimum', 'اللمبوكة: يُفتح موسمها يوم 15 أوت والحجم الأدنى 35 صم',
  'The coryphène season officially starts on 15 August. Fish under 35 cm must not be caught or sold, so they can reach maturity.',
  "La saison de la coryphène démarre officiellement le 15 août. Les poissons de moins de 35 cm ne doivent être ni pêchés ni vendus, afin qu'ils atteignent la maturité.",
  'يبدأ موسم صيد اللمبوكة رسمياً يوم 15 أوت. يُمنع صيد أو بيع الأسماك التي يقلّ طولها عن 35 صم حتى تبلغ مرحلة النضج.',
  'https://www.tunisienumerique.com/tunisie-la-saison-de-peche-de-la-lampouka-demarre-officiellement-le-15-aout/', 2],
 ['ref', 'Mediterranean reference sizes', 'Tailles de référence en Méditerranée', 'الأحجام المرجعية في المتوسط',
  'For the other species, Sennara shows the minimum sizes of EU Regulation 1967/2006 (Annex III) for the Mediterranean as a guide. They are not Tunisian law, but releasing fish below these sizes lets them breed at least once.',
  "Pour les autres espèces, Sennara affiche à titre indicatif les tailles minimales du règlement UE 1967/2006 (annexe III) pour la Méditerranée. Ce n'est pas la loi tunisienne, mais relâcher les poissons sous ces tailles leur permet de se reproduire au moins une fois.",
  'بالنسبة لبقية الأنواع، تعرض صنّارة على سبيل الإرشاد الأحجام الدنيا الواردة في اللائحة الأوروبية 1967/2006 (الملحق الثالث) للبحر المتوسط. هذه ليست قوانين تونسية، لكن إطلاق الأسماك الأصغر منها يسمح لها بالتكاثر مرة واحدة على الأقل.',
  'https://eur-lex.europa.eu/eli/reg/2006/1967/oj', 3],
 ['tip', 'Check official notices', 'Consultez les avis officiels', 'تابع البلاغات الرسمية',
  'Closures and rules change from year to year. Follow the announcements of the Ministry of Agriculture, Water Resources and Fisheries before you go.',
  "Les fermetures et les règles changent d'une année à l'autre. Suivez les annonces du ministère de l'Agriculture, des Ressources hydrauliques et de la Pêche avant de partir.",
  'تتغيّر فترات الغلق والقواعد من سنة إلى أخرى. تابع بلاغات وزارة الفلاحة والموارد المائية والصيد البحري قبل الخروج.',
  'http://www.agriculture.tn', 4],
];
$r = $pdo->prepare('INSERT INTO rules (kind, title_en, title_fr, title_ar, body_en, body_fr, body_ar, source_url, sort_order) VALUES (?,?,?,?,?,?,?,?,?)');
foreach ($rules as $row) $r->execute($row);

echo "Database '" . DB_NAME . "' created.\n";
foreach (['users', 'spots', 'fish', 'spot_fish', 'shops', 'products', 'tutorials', 'rules'] as $tbl) {
    echo str_pad($tbl, 10) . $pdo->query("SELECT COUNT(*) FROM $tbl")->fetchColumn() . "\n";
}
echo "Accounts: admin@sennara.tn (ADMIN_PASSWORD), seller@sennara.tn and fisher@sennara.tn (DEMO_PASSWORD).\n";
