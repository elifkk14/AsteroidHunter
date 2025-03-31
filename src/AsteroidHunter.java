import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sound.sampled.*;
import javax.swing.*;

public class AsteroidHunter extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AsteroidHunter(); // Bu haliyle bırakılabilir
        });
    }
    private Difficulty difficulty = Difficulty.MEDIUM;

    static final int WORLD_WIDTH = 2000;  // Oyun dünyasının genişliği
    static final int WORLD_HEIGHT = 2000; // Oyun dünyasının yüksekliği

    // Kamera pozisyonu
    private double cameraX = 0;
    private double cameraY = 0;

    public double getCameraX() {
        return cameraX;
    }

    public double getCameraY() {
        return cameraY;
    }

    public void setCameraX(double cameraX) {
        this.cameraX = cameraX;
    }

    public void setCameraY(double cameraY) {
        this.cameraY = cameraY;    
    }


     // Oyun sabitleri
     // Oyun sabitleri
    private static int GAME_WIDTH = 1280;  // Daha büyük bir genişlik
    private static int GAME_HEIGHT = 720;  // Daha büyük bir yükseklik
    private static final int FPS = 60;
    private static final int DELAY = 1000 / FPS;
 
     public static int getGAME_WIDTH() {
         return GAME_WIDTH;
     }
 
     public static int getGAME_HEIGHT() {
         return GAME_HEIGHT;
     }
    
    // Oyun durumu
    private boolean running = false;
    private boolean paused = false;
    private int score = 0;
    private int level = 1;
    private int lives = 3;
    
    // Oyun nesneleri
    private Player player;
    private final List<Asteroid> asteroids = new CopyOnWriteArrayList<>();
    private final List<Bullet> bullets = new CopyOnWriteArrayList<>();
    private final List<Explosion> explosions = new CopyOnWriteArrayList<>();
    private final List<PowerUp> powerUps = new CopyOnWriteArrayList<>();
    private final List<EnemyShip> enemies = new CopyOnWriteArrayList<>();
    private final List<Star> stars = new CopyOnWriteArrayList<>();
    
    // Rastgele sayı üreteci
    private final Random random = new Random();
    
    // Oyun paneli
    private GamePanel gamePanel;
    
    // Zamanlayıcılar ve sayaçlar
    private javax.swing.Timer gameTimer;
    private int asteroidSpawnCounter = 0;
    private int enemySpawnCounter = 0;
  
    
    // Ses efektleri
    private Clip laserSound;
    private Clip explosionSound;
    private Clip powerUpSound;

    
    
    public AsteroidHunter() {
        
        setTitle("Asteroid Avcısı");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
    
        // Ekran boyutuna göre ayarlama
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        GAME_WIDTH = (int) (screenSize.getWidth() * 0.8);
        GAME_HEIGHT = (int) (screenSize.getHeight() * 0.8);
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        
        initSounds();
        initUI();
        initGame();
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void initSounds() {
        try {
            // Ses dosyaları olduğunu varsayıyoruz, gerçek uygulamada kaynak dosyaları gerekir
            // laserSound = AudioSystem.getClip();
            // laserSound.open(AudioSystem.getAudioInputStream(new File("laser.wav")));
            
            // explosionSound = AudioSystem.getClip();
            // explosionSound.open(AudioSystem.getAudioInputStream(new File("explosion.wav")));
            
            // powerUpSound = AudioSystem.getClip();
            // powerUpSound.open(AudioSystem.getAudioInputStream(new File("powerup.wav")));
        } catch (Exception e) {
            System.out.println("Ses dosyaları yüklenemedi: " + e.getMessage());
        }
    }
    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }
    
    
    private void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    private void initUI() {
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        gamePanel.setFocusable(true);
        add(gamePanel);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher((KeyEvent e) -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                keyPressed(e);
            } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                keyReleased(e);
            }
            return false;
        });
    }
    
    private void initGame() {
        player = new Player(WORLD_WIDTH / 2, WORLD_HEIGHT / 2); // Oyuncuyu dünya merkezine yerleştir
        for (int i = 0; i < 100; i++) {
            stars.add(new Star(random.nextInt(WORLD_WIDTH), random.nextInt(WORLD_HEIGHT),
                    0.5f + random.nextFloat() * 2.0f));
        }
        // Başlangıçta 5 asteroid ve 2 düşman gemisi ekle
        for (int i = 0; i < 5; i++) {
            spawnAsteroid();
        }
        for (int i = 0; i < 2; i++) {
            spawnEnemyShip();
        }
        gameTimer = new javax.swing.Timer(DELAY, (ActionEvent e) -> {
            if (!paused) {
                update();
            }
            gamePanel.repaint();
        });
        showMainMenu();
    }

    private void showMainMenu() {
        // Modern arka planlı panel
        JPanel menuPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Koyu mavi/çok koyu gri degrade
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 30),
                                                       0, getHeight(), new Color(10, 10, 10));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        // Başlık
        JLabel titleLabel = new JLabel("ASTEROID AVCISI");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        
        // Modern butonlar
        JButton startButton = createModernButton("Start Game");
        JButton difficultyButton = createModernButton("Difficulty: " + difficulty);
        JButton exitButton = createModernButton("Exit");
        
        // Menü öğelerini dikey boşluklarla merkezde yerleştir
        menuPanel.add(Box.createVerticalGlue());
        menuPanel.add(titleLabel);
        menuPanel.add(Box.createVerticalStrut(30));
        menuPanel.add(startButton);
        menuPanel.add(Box.createVerticalStrut(15));
        menuPanel.add(difficultyButton);
        menuPanel.add(Box.createVerticalStrut(15));
        menuPanel.add(exitButton);
        menuPanel.add(Box.createVerticalGlue());
        
        // Buton aksiyonları
        startButton.addActionListener(e -> {
            setContentPane(gamePanel);
            revalidate();
            repaint();
            startGame();
        });
        
        difficultyButton.addActionListener(e -> {
            switch (difficulty) {
                case EASY -> difficulty = Difficulty.MEDIUM;
                case MEDIUM -> difficulty = Difficulty.HARD;
                case HARD -> difficulty = Difficulty.EASY;
            }
            difficultyButton.setText("Difficulty: " + difficulty);
        });
        
        exitButton.addActionListener(e -> System.exit(0));
        
        setContentPane(menuPanel);
        revalidate();
        repaint();
    }
    
    
    
    // Şık buton oluşturma yardımcı metodu
    private JButton createModernButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("SansSerif", Font.PLAIN, 24));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(50, 50, 50));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(300, 50));
        
        // Basit hover efekti
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(70, 70, 70));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(50, 50, 50));
            }
        });
        return button;
    }
    
    
    
    
    private void startGame() {
        running = true;
        gameTimer.start();
    }
    
    private void pauseGame() {
        paused = !paused;
    }
    
    private void endGame() {
        running = false;
        gameTimer.stop();
        
        JOptionPane.showMessageDialog(this, 
                """
                Oyun Bitti!
                
                Puan\u0131n\u0131z: """ + score + "\n" +
                "Seviye: " + level + "\n\n" +
                "Tekrar oynamak için Tamam'a tıklayın.",
                "Asteroid Avcısı", JOptionPane.INFORMATION_MESSAGE);
        
        // Oyunu sıfırla ve yeniden başlat
        resetGame();
        startGame();
    }
    
    private void resetGame() {
        score = 0;
        level = 1;
        lives = 3;
        asteroids.clear();
        bullets.clear();
        explosions.clear();
        powerUps.clear();
        enemies.clear();
        player = new Player(WORLD_WIDTH / 2, WORLD_HEIGHT / 2); // Sıfırlamada da dünya merkezine
        asteroidSpawnCounter = 0;
        enemySpawnCounter = 0;
    
    }
    
    private void update() {
        if (!running) {
            return;
        }
    
        cameraX = player.getX() - GAME_WIDTH / 2;
        cameraY = player.getY() - GAME_HEIGHT / 2;
        cameraX = Math.max(0, Math.min(cameraX, WORLD_WIDTH - GAME_WIDTH));
        cameraY = Math.max(0, Math.min(cameraY, WORLD_HEIGHT - GAME_HEIGHT));
    
        for (Star star : stars) {
            star.update();
        }
        player.update();
        for (Bullet bullet : bullets) {
            bullet.update();
            if (!bullet.isActive()) {
                bullets.remove(bullet);
            }
        }
    
        // Ekrandan çıkan veya yok edilen asteroidleri geçici bir listeye ekle
        List<Asteroid> toRemove = new ArrayList<>();
        for (Asteroid asteroid : asteroids) {
            asteroid.update();
            if (asteroid.isOffScreen(cameraX, cameraY)) {
                toRemove.add(asteroid);
                spawnAsteroid();
                continue;
            }
            if (player.isActive() && asteroid.isActive() && player.collidesWith(asteroid)) {
                asteroid.setActive(false);
                player.hit();
                explosions.add(new Explosion(asteroid.getX(), asteroid.getY(), 2.0));
                playSound(explosionSound);
                lives--;
                if (lives <= 0) {
                    player.setActive(false);
                    endGame();
                }
                toRemove.add(asteroid); // Çarpışmada yok edilen asteroidi de kaldır
                continue;
            }
    
            for (Bullet bullet : bullets) {
                if (bullet.isActive() && asteroid.isActive() && bullet.collidesWith(asteroid)) {
                    asteroid.hit();
                    bullet.setActive(false);
                    if (!asteroid.isActive()) {
                        score += asteroid.getPoints();
                        explosions.add(new Explosion(asteroid.getX(), asteroid.getY(), 1.5));
                        playSound(explosionSound);
                        if (random.nextDouble() < 0.2) {
                            spawnPowerUp(asteroid.getX(), asteroid.getY());
                        }
                        toRemove.add(asteroid); // Mermiyle yok edilen asteroidi kaldır
                    }
                }
            }
        }
        // Döngü bittikten sonra asteroidleri ana listeden kaldır
        asteroids.removeAll(toRemove);
    
        // Düşman gemilerini güncelle
        List<EnemyShip> enemiesToRemove = new ArrayList<>();
        for (EnemyShip enemy : enemies) {
            enemy.update();
            if (enemy.isOffScreen(cameraX, cameraY)) {
                enemiesToRemove.add(enemy);
                continue;
            }
            if (player.isActive() && enemy.isActive() && player.collidesWith(enemy)) {
                enemy.setActive(false);
                player.hit();
                explosions.add(new Explosion(enemy.getX(), enemy.getY(), 2.0));
                playSound(explosionSound);
                lives--;
                if (lives <= 0) {
                    player.setActive(false);
                    endGame();
                }
                enemiesToRemove.add(enemy);
                continue;
            }
            for (Bullet bullet : bullets) {
                if (bullet.isActive() && enemy.isActive() && bullet.collidesWith(enemy)) {
                    enemy.hit();
                    bullet.setActive(false);
                    if (!enemy.isActive()) {
                        score += enemy.getPoints();
                        explosions.add(new Explosion(enemy.getX(), enemy.getY(), 2.0));
                        playSound(explosionSound);
                        if (random.nextDouble() < 0.4) {
                            spawnPowerUp(enemy.getX(), enemy.getY());
                        }
                        enemiesToRemove.add(enemy);
                    }
                }
            }
        }
        enemies.removeAll(enemiesToRemove);
    
        // Güçlendirmeleri güncelle
        for (PowerUp powerUp : powerUps) {
            powerUp.update();
            if (!powerUp.isActive()) {
                powerUps.remove(powerUp);
                continue;
            }
            if (player.isActive() && powerUp.isActive() && player.collidesWith(powerUp)) {
                powerUp.setActive(false);
                applyPowerUp(powerUp.getType());
                playSound(powerUpSound);
            }
        }
    
        // Patlamaları güncelle
        for (Explosion explosion : explosions) {
            explosion.update();
            if (!explosion.isActive()) {
                explosions.remove(explosion);
            }
        }
    
        // Seviye ilerlemesini kontrol et
        if (score >= level * 1000) {
            level++;
        }
    
        // Yeni nesneleri üret
        spawnGameObjects();
    
        // Kullanıcı olmayan nesneleri temizle
        cleanupInactiveObjects();
    }
    
    private void spawnGameObjects() {
        // Asteroid üretim hızı
int asteroidThreshold = switch (difficulty) {
    case EASY -> 80;
    case MEDIUM -> 60;
    case HARD -> 40;
};
if (++asteroidSpawnCounter >= asteroidThreshold / (1 + level * 0.2)) {
    asteroidSpawnCounter = 0;
    spawnAsteroid();
}

// Düşman üretim hızı
int enemyThreshold = switch (difficulty) {
    case EASY -> 220;
    case MEDIUM -> 180;
    case HARD -> 130;
};
if (++enemySpawnCounter >= enemyThreshold / (1 + level * 0.1)) {
    enemySpawnCounter = 0;
    spawnEnemyShip();
}

    }
    
    private void spawnAsteroid() {
        int size = 20 + random.nextInt(30);
        int health = 1 + random.nextInt(level);
        double x = 0, y = 0, angle = 0;
    
        // Kameraya göre spawn pozisyonu
        int edge = random.nextInt(4);
        switch (edge) {
            case 0 -> { // ÜST
                x = cameraX + random.nextInt(GAME_WIDTH);
                y = cameraY - size;
                angle = 90 + random.nextDouble() * 90 - 45; // Daha çok aşağıya doğru
            }
            case 1 -> { // ALT
                x = cameraX + random.nextInt(GAME_WIDTH);
                y = cameraY + GAME_HEIGHT + size;
                angle = 270 + random.nextDouble() * 90 - 45; // Daha çok yukarıya doğru
            }
            case 2 -> { // SOL
                x = cameraX - size;
                y = cameraY + random.nextInt(GAME_HEIGHT);
                angle = 0 + random.nextDouble() * 90 - 45; // Daha çok sağa doğru
            }
            case 3 -> { // SAĞ
                x = cameraX + GAME_WIDTH + size;
                y = cameraY + random.nextInt(GAME_HEIGHT);
                angle = 180 + random.nextDouble() * 90 - 45; // Daha çok sola doğru
            }
        }
    
        // Dünya sınırları içinde tut
        x = Math.max(0, Math.min(x, WORLD_WIDTH - size));
        y = Math.max(0, Math.min(y, WORLD_HEIGHT - size));
    
        double speed = 1 + random.nextDouble() * (level * 0.5);
        asteroids.add(new Asteroid(x, y, size, health, speed, angle));
    }
    
    private void spawnEnemyShip() {
        double x, y;
        double angle;
    
        int edge = random.nextInt(4);
        switch (edge) {
            case 0 -> { // ÜST
                x = cameraX + random.nextInt(GAME_WIDTH);
                y = cameraY - 30;
                angle = 180; // Aşağıya doğru
            }
            case 1 -> { // ALT
                x = cameraX + random.nextInt(GAME_WIDTH);
                y = cameraY + GAME_HEIGHT + 30;
                angle = 0; // Yukarıya doğru
            }
            case 2 -> { // SOL
                x = cameraX - 30;
                y = cameraY + random.nextInt(GAME_HEIGHT);
                angle = 90; // Sağa doğru
            }
            case 3 -> { // SAĞ
                x = cameraX + GAME_WIDTH + 30;
                y = cameraY + random.nextInt(GAME_HEIGHT);
                angle = 270; // Sola doğru
            }
            default -> throw new IllegalStateException("Unexpected value: " + edge);
        }
    
        // Dünya sınırları içinde tut
        x = Math.max(0, Math.min(x, WORLD_WIDTH - 30));
        y = Math.max(0, Math.min(y, WORLD_HEIGHT - 30));
    
        int baseHealth = switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };
        int health = baseHealth + level / 2;
    
        double speed = 1.5 + random.nextDouble() * level * 0.3;
        enemies.add(new EnemyShip(x, y, speed, angle, health));
    }
    
    private void spawnPowerUp(double x, double y) {
        PowerUp.Type type = PowerUp.Type.values()[random.nextInt(PowerUp.Type.values().length)];
        powerUps.add(new PowerUp(x, y, type));
    }
    
    private void applyPowerUp(PowerUp.Type type) {
        switch (type) {
            case SHIELD -> player.setShieldActive(true);
            case TRIPLE_SHOT -> player.setTripleShotActive(true);
            case SPEED_BOOST -> player.setSpeedBoost(true);
            case EXTRA_LIFE -> lives = Math.min(lives + 1, 5);
            case SCORE_BONUS -> score += 500;
        }
    }
    
    private void cleanupInactiveObjects() {
        // Aktif olmayan nesneleri temizle
        bullets.removeIf(bullet -> !bullet.isActive());
        asteroids.removeIf(asteroid -> !asteroid.isActive());
        explosions.removeIf(explosion -> !explosion.isActive());
        powerUps.removeIf(powerUp -> !powerUp.isActive());
        enemies.removeIf(enemy -> !enemy.isActive());
    }
    
    private void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        // Menü tuşları
        if (key == KeyEvent.VK_P) {
            pauseGame();
        } else if (key == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
        
        // Hareket tuşları
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            player.setMovingUp(true);
        }
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            player.setMovingDown(true);
        }
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            player.setMovingLeft(true);
        }
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            player.setMovingRight(true);
        }
        
        // Ateş tuşu
        if (key == KeyEvent.VK_SPACE) {
            fireBullet();
        }
    }
    
    private void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        
        // Hareket tuşları
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            player.setMovingUp(false);
        }
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            player.setMovingDown(false);
        }
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            player.setMovingLeft(false);
        }
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            player.setMovingRight(false);
        }
    }
    
    private void fireBullet() {
        if (player.isActive() && player.canFire()) {
            // Geminin açısına göre mermi pozisyonunu hesapla
            double radians = Math.toRadians(player.getAngle());
            double bulletOffset = Player.SIZE / 2 + Bullet.SIZE; // Geminin ucundan biraz öteye
            double bulletX = player.getX() + Math.cos(radians) * bulletOffset;
            double bulletY = player.getY() + Math.sin(radians) * bulletOffset;
    
            if (player.hasTripleShot()) {
                // Üçlü atış
                bullets.add(new Bullet(bulletX, bulletY, player.getAngle()));
                bullets.add(new Bullet(bulletX, bulletY, player.getAngle() - 15));
                bullets.add(new Bullet(bulletX, bulletY, player.getAngle() + 15));
            } else {
                // Tekli atış
                bullets.add(new Bullet(bulletX, bulletY, player.getAngle()));
            }
            player.resetFireCooldown();
            playSound(laserSound);
        }
    }
    
    // İç sınıf: Oyun Paneli
    // GamePanel'de çizimi kameraya göre ayarlama
    private class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Modern arka plan: Hafif grid efekti ve degrade
            drawModernBackground(g2d);

            // Kamerayı uygula
            g2d.translate(-cameraX, -cameraY);

            // Yıldızları çiz
            for (Star star : stars) {
                star.draw(g2d);
            }

            // Oyuncuyu çiz
            if (player.isActive()) {
                player.draw(g2d);
            }

            // Mermileri çiz
            for (Bullet bullet : bullets) {
                bullet.draw(g2d);
            }

            // Asteroidleri çiz
            for (Asteroid asteroid : asteroids) {
                asteroid.draw(g2d);
            }

            // Güçlendirmeleri çiz
            for (PowerUp powerUp : powerUps) {
                powerUp.draw(g2d);
            }

            // Düşmanları çiz
            for (EnemyShip enemy : enemies) {
                enemy.draw(g2d);
            }

            // Patlamaları çiz
            for (Explosion explosion : explosions) {
                explosion.draw(g2d);
            }

            // Kamerayı geri çevir ve HUD'u sabit çiz
            g2d.translate(cameraX, cameraY);
            drawModernHUD(g2d);

            if (paused) {
                drawModernPauseScreen(g2d);
            }
        }

        private void drawModernBackground(Graphics2D g2d) {
            // Degrade arka plan
            GradientPaint bgGradient = new GradientPaint(
                0, 0, new Color(10, 10, 30),
                0, getHeight(), new Color(5, 5, 15)
            );
            g2d.setPaint(bgGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Hafif grid efekti
            g2d.setColor(new Color(255, 255, 255, 20));
            g2d.setStroke(new BasicStroke(1f));
            int gridSize = 50;
            for (int x = (int)cameraX % gridSize; x < getWidth(); x += gridSize) {
                g2d.drawLine(x, 0, x, getHeight());
            }
            for (int y = (int)cameraY % gridSize; y < getHeight(); y += gridSize) {
                g2d.drawLine(0, y, getWidth(), y);
            }
        }

        private void drawModernHUD(Graphics2D g2d) {
            // Font ayarları (Orbitron yoksa SansSerif kullanıyoruz)
            Font hudFont = new Font("SansSerif", Font.BOLD, 18);
            g2d.setFont(hudFont);

            // HUD kutusu arka planı
            int padding = 10;
            int boxWidth = 250;
            int boxHeight = 120;
            GradientPaint hudGradient = new GradientPaint(
                0, 0, new Color(0, 0, 0, 150),
                0, boxHeight, new Color(0, 0, 50, 150)
            );
            g2d.setPaint(hudGradient);
            g2d.fillRoundRect(padding, padding, boxWidth, boxHeight, 15, 15);
            g2d.setColor(new Color(0, 200, 255, 200));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(padding, padding, boxWidth, boxHeight, 15, 15);

            // Puan ve Seviye
            g2d.setColor(Color.WHITE);
            g2d.drawString("PUAN: " + score, padding + 20, padding + 25);
            g2d.drawString("SEVİYE: " + level, padding + 20, padding + 50);

            // Canlar (küçük gemi ikonlarıyla)
            g2d.drawString("CAN: ", padding + 20, padding + 75);
            for (int i = 0; i < lives; i++) {
                AffineTransform oldTransform = g2d.getTransform();
                g2d.translate(padding + 70 + i * 25, padding + 70);
                g2d.scale(0.5, 0.5);
                g2d.setColor(new Color(255, 50, 50));
                g2d.fillPolygon(
                    new int[] {0, -10, 0, 10},
                    new int[] {-10, 0, 10, 0},
                    4
                );
                g2d.setColor(Color.WHITE);
                g2d.drawPolygon(
                    new int[] {0, -10, 0, 10},
                    new int[] {-10, 0, 10, 0},
                    4
                );
                g2d.setTransform(oldTransform);
            }

            // Güçlendirmeler (ikonlar ve zaman çubukları)
            int yOffset = padding + boxHeight + 10;
            if (player.hasShield()) {
                drawPowerUpBar(g2d, "KALKAN", Color.CYAN, player.shieldDuration, Player.MAX_SHIELD_DURATION, padding + 20, yOffset);
                yOffset += 30;
            }
            if (player.hasTripleShot()) {
                drawPowerUpBar(g2d, "ÜÇLÜ ATIŞ", Color.RED, player.tripleShotDuration, Player.MAX_TRIPLE_SHOT_DURATION, padding + 20, yOffset);
                yOffset += 30;
            }
            if (player.hasSpeedBoost()) {
                drawPowerUpBar(g2d, "HIZ", Color.YELLOW, player.speedBoostDuration, Player.MAX_SPEED_BOOST_DURATION, padding + 20, yOffset);
            }
        }

        private void drawPowerUpBar(Graphics2D g2d, String label, Color color, int duration, int maxDuration, int x, int y) {
            // Güçlendirme ikonu
            g2d.setColor(color);
            g2d.fillOval(x - 15, y - 10, 10, 10);

            // Etiket
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, x, y);

            // Zaman çubuğu
            int barWidth = 150;
            int barHeight = 5;
            float progress = (float)duration / maxDuration;
            g2d.setColor(new Color(50, 50, 50, 150));
            g2d.fillRect(x, y + 5, barWidth, barHeight);
            g2d.setColor(color);
            g2d.fillRect(x, y + 5, (int)(barWidth * (1 - progress)), barHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y + 5, barWidth, barHeight);
        }

        private void drawModernPauseScreen(Graphics2D g2d) {
            // Yarı saydam koyu arka plan
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Modern pause kutusu
            int boxWidth = 400;
            int boxHeight = 200;
            int boxX = (getWidth() - boxWidth) / 2;
            int boxY = (getHeight() - boxHeight) / 2;
            GradientPaint pauseGradient = new GradientPaint(
                boxX, boxY, new Color(0, 100, 150, 200),
                boxX, boxY + boxHeight, new Color(0, 50, 100, 200)
            );
            g2d.setPaint(pauseGradient);
            g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 20, 20);
            g2d.setColor(new Color(0, 200, 255));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 20, 20);

            // Başlık
            g2d.setFont(new Font("SansSerif", Font.BOLD, 36));
            g2d.setColor(Color.WHITE);
            String message = "OYUN DURAKLATILDI";
            int width = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (getWidth() - width) / 2, boxY + 80);

            // Talimat
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
            message = "Devam etmek için P tuşuna basın";
            width = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (getWidth() - width) / 2, boxY + 120);
        }
    }



// Oyuncu sınıfı
class Player extends GameObject {
    static final int SIZE = 30;
    private static final double ACCELERATION = 0.5;
    private static final double MAX_SPEED = 5.0;
    private static final double FRICTION = 0.98;
    
    private double angle = 0;
    private double vx = 0;
    private double vy = 0;
    
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    
    private int fireCooldown = 0;
    private static final int FIRE_RATE = 10;  // Kaç frame'de bir ateş edilebilir
    
    private boolean shieldActive = false;
    private int shieldDuration = 0;
    private static final int MAX_SHIELD_DURATION = 300;  // 5 saniye
    
    private boolean tripleShotActive = false;
    private int tripleShotDuration = 0;
    private static final int MAX_TRIPLE_SHOT_DURATION = 300;  // 5 saniye
    
    private boolean speedBoost = false;
    private int speedBoostDuration = 0;
    private static final int MAX_SPEED_BOOST_DURATION = 300;  // 5 saniye
    
    private int invulnerabilityTime = 0;

    // Mevcut kodun çoğu aynı kalıyor, sadece getter'ları ekliyorum
    public int getShieldDuration() { return shieldDuration; }
    public int getTripleShotDuration() { return tripleShotDuration; }
    public int getSpeedBoostDuration() { return speedBoostDuration; }
    
    public Player(double x, double y) {
        super(x, y);
        initializeCollisionRadius();
    }
    
    private void initializeCollisionRadius() {
        this.collisionRadius = SIZE / 2;
    }

    // Oyuncu sınıfının update metodu

    @Override
    public void update() {
        // Hareketi güncelle
        if (movingUp) {
            vy -= ACCELERATION * (speedBoost ? 1.5 : 1.0);
        }
        if (movingDown) {
            vy += ACCELERATION * (speedBoost ? 1.5 : 1.0);
        }
        if (movingLeft) {
            vx -= ACCELERATION * (speedBoost ? 1.5 : 1.0);
        }
        if (movingRight) {
            vx += ACCELERATION * (speedBoost ? 1.5 : 1.0);
        }
        
        // Hızı limitle
        double maxSpeed = MAX_SPEED * (speedBoost ? 1.5 : 1.0);
        double currentSpeed = Math.sqrt(vx * vx + vy * vy);
        if (currentSpeed > maxSpeed) {
            vx = vx / currentSpeed * maxSpeed;
            vy = vy / currentSpeed * maxSpeed;
        }
        
        // Sürtünme uygula
        vx *= FRICTION;
        vy *= FRICTION;
        
        // Yeni pozisyonu hesapla
        double newX = x + vx;
        double newY = y + vy;
        
        // Dünya sınırlarından çıkmasını engelle
        int halfSize = SIZE / 2;
        newX = Math.max(halfSize, Math.min(newX, AsteroidHunter.WORLD_WIDTH - halfSize));
        newY = Math.max(halfSize, Math.min(newY, AsteroidHunter.WORLD_HEIGHT - halfSize));
        
        // Pozisyonu güncelle
        x = newX;
        y = newY;
        
        // Uçuş açısını güncelle
        if (vx != 0 || vy != 0) {
            angle = Math.toDegrees(Math.atan2(vy, vx));
        }
    
    // Ateş etme hızını güncelle
    if (fireCooldown > 0) {
        fireCooldown--;
    }
    
    // Güçlendirmelerin sürelerini kontrol et
    if (shieldActive) {
        shieldDuration++;
        if (shieldDuration >= MAX_SHIELD_DURATION) {
            shieldActive = false;
            shieldDuration = 0;
        }
    }
    
    if (tripleShotActive) {
        tripleShotDuration++;
        if (tripleShotDuration >= MAX_TRIPLE_SHOT_DURATION) {
            tripleShotActive = false;
            tripleShotDuration = 0;
        }
    }
    
    if (speedBoost) {
        speedBoostDuration++;
        if (speedBoostDuration >= MAX_SPEED_BOOST_DURATION) {
            speedBoost = false;
            speedBoostDuration = 0;
        }
    }
    
    // Hasar aldıktan sonra dokunulmazlık süresini güncelle
    if (invulnerabilityTime > 0) {
        invulnerabilityTime--;
    }

    
}
// `WORLD_WIDTH` ve `WORLD_HEIGHT` erişimi için AsteroidHunter'dan statik metodlar ekleyin
public static int getWORLD_WIDTH() {
    return AsteroidHunter.WORLD_WIDTH;
}

public static int getWORLD_HEIGHT() {
    return AsteroidHunter.WORLD_HEIGHT;
}

    // Player sınıfının draw metodunu göster
    @Override
    public void draw(Graphics2D g2d) {
    // İlk olarak kalkanı göster
    if (shieldActive) {
        GradientPaint shieldGradient = new GradientPaint(
            (float)(x - SIZE), (float)y, new Color(0, 255, 255, 100),
            (float)(x + SIZE), (float)y, new Color(0, 100, 255, 100),
            true
        );
        g2d.setPaint(shieldGradient);
        g2d.fillOval((int)(x - SIZE*1.2), (int)(y - SIZE*1.2), (int)(SIZE*2.4), (int)(SIZE*2.4));
    }

    if (invulnerabilityTime > 0 && invulnerabilityTime % 6 >= 3) {
        return;
    }

    AffineTransform oldTransform = g2d.getTransform();
    AffineTransform transform = new AffineTransform();
    transform.translate(x, y);
    transform.rotate(Math.toRadians(angle));
    g2d.setTransform(transform);

    // Geminin gövdesi (daha detaylı)
    GradientPaint bodyGradient = new GradientPaint(
        -SIZE/2, 0, new Color(200, 200, 255),
        SIZE/2, 0, new Color(100, 100, 200)
    );
    g2d.setPaint(bodyGradient);
    
    // Ana gövde
    int[] xBody = {SIZE/2, -SIZE/2, -SIZE/4, -SIZE/2};
    int[] yBody = {0, SIZE/3, 0, -SIZE/3};
    g2d.fillPolygon(xBody, yBody, 4);
    
    // Detay çizgileri
    g2d.setColor(new Color(50, 50, 100));
    g2d.drawPolygon(xBody, yBody, 4);
    g2d.drawLine(SIZE/4, 0, -SIZE/4, SIZE/6);
    g2d.drawLine(SIZE/4, 0, -SIZE/4, -SIZE/6);

    // Motor ateşi efekti (daha gerçekçi)
    if (movingUp || movingDown || movingLeft || movingRight) {
        int flameSize = SIZE/2 + (int)(Math.random() * SIZE/4);
        GradientPaint flameGradient = new GradientPaint(
            -SIZE/2, -SIZE/6, Color.YELLOW,
            -SIZE, 0, Color.RED,
            true
        );
        g2d.setPaint(flameGradient);
        
        int[] xFlame = {-SIZE/2, -SIZE/2 - flameSize, -SIZE/2};
        int[] yFlame = {SIZE/6, 0, -SIZE/6};
        g2d.fillPolygon(xFlame, yFlame, 3);
        
        // Ateş partikülleri
        g2d.setColor(new Color(255, 150, 0, 150));
        for (int i = 0; i < 5; i++) {
            int px = -SIZE/2 - flameSize - (int)(Math.random() * 5);
            int py = -SIZE/6 + (int)(Math.random() * SIZE/3);
            g2d.fillOval(px, py, 2, 2);
        }
    }

    g2d.setTransform(oldTransform);
}
    
    public boolean canFire() {
        return fireCooldown <= 0;
    }
    
    public void resetFireCooldown() {
        fireCooldown = FIRE_RATE;
    }
    
    public double getAngle() {
        return angle;
    }
    
    public void hit() {
        if (shieldActive) {
            shieldActive = false;
            shieldDuration = 0;
        } else if (invulnerabilityTime <= 0) {
            invulnerabilityTime = 90; // 1.5 saniye dokunulmazlık
        }
    }
    
    // Getter ve setter'lar
    public void setMovingUp(boolean movingUp) {
        this.movingUp = movingUp;
    }
    
    public void setMovingDown(boolean movingDown) {
        this.movingDown = movingDown;
    }
    
    public void setMovingLeft(boolean movingLeft) {
        this.movingLeft = movingLeft;
    }
    
    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
    }
    
    public void setShieldActive(boolean shieldActive) {
        this.shieldActive = shieldActive;
        this.shieldDuration = 0;
    }
    
    public void setTripleShotActive(boolean tripleShotActive) {
        this.tripleShotActive = tripleShotActive;
        this.tripleShotDuration = 0;
    }
    
    public void setSpeedBoost(boolean speedBoost) {
        this.speedBoost = speedBoost;
        this.speedBoostDuration = 0;
    }
    
    public boolean hasShield() {
        return shieldActive;
    }
    
    public boolean hasTripleShot() {
        return tripleShotActive;
    }
    
    public boolean hasSpeedBoost() {
        return speedBoost;
    }
}

// Mermi sınıfı
class Bullet extends GameObject {
    static final int SIZE = 4;
    private static final double SPEED = 10.0;
    
    private final double vx;
    private final double vy;
    private int lifetime = 0;
    private static final int MAX_LIFETIME = 60;  // 1 saniye
    
    public Bullet(double x, double y, double angle) {
        super(x, y);
        this.collisionRadius = SIZE / 2;
        
        double radians = Math.toRadians(angle);
        vx = Math.cos(radians) * SPEED;
        vy = Math.sin(radians) * SPEED;
    }
    
    @Override
    public void update() {
        x += vx;
        y += vy;
        
        lifetime++;
        if (lifetime >= MAX_LIFETIME || 
            x < 0 || x > AsteroidHunter.WORLD_WIDTH || 
            y < 0 || y > AsteroidHunter.WORLD_HEIGHT) {
            setActive(false);
        }
    }

    // Mermi sınıfının draw metodunu göster
    @Override
    public void draw(Graphics2D g2d) {
    AffineTransform oldTransform = g2d.getTransform();
    AffineTransform transform = new AffineTransform();
    transform.translate(x, y);
    transform.rotate(Math.atan2(vy, vx)); // Merminin hareket yönüne göre dön
    g2d.setTransform(transform);

    // Lazer efekti için gradient
    GradientPaint laserGradient = new GradientPaint(
        -SIZE * 2, 0, new Color(255, 0, 0, 255),
        SIZE, 0, new Color(255, 100, 0, 0)
    );
    g2d.setPaint(laserGradient);
    g2d.fillRect(-SIZE * 2, -SIZE / 2, SIZE * 3, SIZE);

    g2d.setTransform(oldTransform);
}
}

// Asteroid sınıfı
class Asteroid extends GameObject {
    private final int size;
    private int health;
    @SuppressWarnings("FieldMayBeFinal")
    private double speed;
    private double angle;
    private final double rotationSpeed;
    private final int[] xPoints;
    private final int[] yPoints;
    private final int numPoints;
    
    public Asteroid(double x, double y, int size, int health, double speed, double angle) {
        super(x, y);
        this.size = size;
        this.health = health;
        this.speed = speed;
        this.angle = angle;
        this.rotationSpeed = Math.random() * 2 - 1; // -1 ile 1 arası, daha yavaş dönme
        
        this.collisionRadius = size / 2;
        
        // Asteroid şeklini rastgele oluştur
        numPoints = 10 + (int)(Math.random() * 6);
        xPoints = new int[numPoints];
        yPoints = new int[numPoints];
        
        for (int i = 0; i < numPoints; i++) {
            double pointAngle = Math.PI * 2 * i / numPoints;
            double radius = size / 2 * (0.8 + Math.random() * 0.4);
            xPoints[i] = (int) (Math.cos(pointAngle) * radius);
            yPoints[i] = (int) (Math.sin(pointAngle) * radius);
        }
    }
    
    @Override
    public void update() {
        // Açıyı radyana çevir
        double radians = Math.toRadians(angle);
        
        // Pozisyonu güncelle
        x += Math.cos(radians) * speed;
        y += Math.sin(radians) * speed;
        
        // Asteroid'i döndür
        angle += rotationSpeed;
    }

    // Asteroid sınıfının draw metodunu göster
    @Override
    public void draw(Graphics2D g2d) {
    AffineTransform oldTransform = g2d.getTransform();
    AffineTransform transform = new AffineTransform();
    transform.translate(x, y);
    transform.rotate(Math.toRadians(angle));
    g2d.setTransform(transform);

    // Gölgeli doku efekti
    Color baseColor;
    if (health >= 3) {
        baseColor = new Color(101, 67, 33); // Koyu kahverengi
    } else if (health == 2) {
        baseColor = new Color(120, 120, 120); // Gri
    } else {
        baseColor = new Color(180, 180, 180); // Açık gri
    }

    // Ana gövde
    GradientPaint asteroidGradient = new GradientPaint(
        -size/2, -size/2, baseColor.darker(),
        size/2, size/2, baseColor.brighter(),
        true
    );
    g2d.setPaint(asteroidGradient);
    g2d.fillPolygon(xPoints, yPoints, numPoints);

    // Krater efektleri
    g2d.setColor(baseColor.darker().darker());
    for (int i = 0; i < 3; i++) {
        int craterSize = size/8 + (int)(Math.random() * size/6);
        int craterX = -size/3 + (int)(Math.random() * size/1.5);
        int craterY = -size/3 + (int)(Math.random() * size/1.5);
        g2d.fillOval(craterX, craterY, craterSize, craterSize);
    }

    // Kenar vurgusu
    g2d.setColor(new Color(255, 255, 255, 50));
    g2d.drawPolygon(xPoints, yPoints, numPoints);

    g2d.setTransform(oldTransform);
}
    
    public void hit() {
        health--;
        if (health <= 0) {
            setActive(false);
        }
    }
    
    public boolean isOffScreen(double cameraX, double cameraY) {
        double relativeX = x - cameraX;
        double relativeY = y - cameraY;
        return relativeY < -size || // Üstten çıkma kontrolü eklendi
               relativeY > AsteroidHunter.getGAME_HEIGHT() + size ||
               relativeX < -size ||
               relativeX > AsteroidHunter.getGAME_WIDTH() + size;
    }

    
    
    public int getPoints() {
        return 100 + (size / 5);
    }

    // Removed duplicate method definition
}

// Patlama efekti sınıfı
class Explosion extends GameObject {
    private int frame = 0;
    private static final int MAX_FRAMES = 20;
    private final double size;
    
    public Explosion(double x, double y, double size) {
        super(x, y);
        this.size = size;
    }
    
    @Override
    public void update() {
        frame++;
        if (frame >= MAX_FRAMES) {
            setActive(false);
        }
    }

    // Patlama efekti sınıfının draw metodunu göster

    @Override
    public void draw(Graphics2D g2d) {
        double progress = (double) frame / MAX_FRAMES;
        int alpha = (int) (255 * (1 - progress));
        double radius = size * 20 * progress;
        
        // İç patlama (daha parlak)
        g2d.setColor(new Color(255, 200, 0, alpha));
        g2d.fillOval((int) (x - radius/2), (int) (y - radius/2), (int) radius, (int) radius);
        
        // Dış patlama halkası
        g2d.setColor(new Color(255, 50, 0, alpha));
        g2d.drawOval((int) (x - radius/1.5), (int) (y - radius/1.5), (int) (radius/0.75), (int) (radius/0.75));
    }
}

// Güçlendirme sınıfı
class PowerUp extends GameObject {
    public enum Type {
        SHIELD, TRIPLE_SHOT, SPEED_BOOST, EXTRA_LIFE, SCORE_BONUS
    }
    
    private static final int SIZE = 20;
    private double angle = 0;
    private final Type type;
    private int lifetime = 0;
    private static final int MAX_LIFETIME = 300;  // 5 saniye
    
    public PowerUp(double x, double y, Type type) {
        super(x, y);
        this.type = type;
        this.collisionRadius = SIZE / 2;
    }
    
    @Override
    public void update() {
        // Döndür
        angle += 2;
        if (angle >= 360) {
            angle = 0;
        }
        
        // Süre sınırı
        lifetime++;
        if (lifetime >= MAX_LIFETIME) {
            setActive(false);
        }
    }

    // Güçlendirme sınıfının draw metodunu göster
    
    @Override
    public void draw(Graphics2D g2d) {
        // Yanıp sönme efekti
        if (lifetime > MAX_LIFETIME - 60 && lifetime % 10 >= 5) {
            return; // Son 1 saniyede yanıp sön
        }
        
        AffineTransform oldTransform = g2d.getTransform();
        
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.rotate(Math.toRadians(angle));
        g2d.setTransform(transform);
        
        // Türe göre renk ve şekil
        switch (type) {
            case SHIELD -> {
                g2d.setColor(Color.CYAN);
                g2d.fillOval(-SIZE/2, -SIZE/2, SIZE, SIZE);
            }
                
            case TRIPLE_SHOT -> {
                g2d.setColor(Color.RED);
                int[] xPoints = {0, -SIZE/2, 0, SIZE/2};
                int[] yPoints = {-SIZE/2, 0, SIZE/2, 0};
                g2d.fillPolygon(xPoints, yPoints, 4);
            }
                
            case SPEED_BOOST -> {
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(-SIZE/2, -SIZE/4, SIZE, SIZE/2);
                g2d.fillPolygon(
                        new int[] {SIZE/2, SIZE, SIZE/2},
                        new int[] {-SIZE/2, 0, SIZE/2},
                        3
                );
            }
                
            case EXTRA_LIFE -> {
                g2d.setColor(Color.GREEN);
                g2d.fillPolygon(
                        new int[] {0, -SIZE/2, 0, SIZE/2},
                        new int[] {-SIZE/2, 0, SIZE/2, 0},
                        4
                );
                g2d.fillPolygon(
                        new int[] {-SIZE/2, 0, SIZE/2},
                        new int[] {-SIZE/4, -SIZE, -SIZE/4},
                        3
                );
            }
                
            case SCORE_BONUS -> {
                g2d.setColor(Color.ORANGE);
                int sides = 5;
                int[] xStarPoints = new int[sides * 2];
                int[] yStarPoints = new int[sides * 2];
                
                for (int i = 0; i < sides * 2; i++) {
                    double starAngle = Math.PI * i / sides;
                    double radius = (i % 2 == 0) ? SIZE/2 : SIZE/4;
                    xStarPoints[i] = (int) (Math.cos(starAngle) * radius);
                    yStarPoints[i] = (int) (Math.sin(starAngle) * radius);
                }
                g2d.fillPolygon(xStarPoints, yStarPoints, sides * 2);
            }
        }
        
        g2d.setTransform(oldTransform);
    }
    
    public Type getType() {
        return type;
    }
}

// Düşman gemi sınıfı
class EnemyShip extends GameObject {
    private static final int SIZE = 25;
    private final double speed;
    private final double angle;
    private int health;
    private int shootTimer = 0;
    private final int shootInterval;
    
    public EnemyShip(double x, double y, double speed, double angle, int health) {
        super(x, y);
        this.speed = speed;
        this.angle = angle;
        this.health = health;
        this.shootInterval = 30 + (int)(Math.random() * 60);  // 0.5-1.5 saniye arası
        this.collisionRadius = SIZE / 2;
    }
    
    @Override
    public void update() {
        double radians = Math.toRadians(angle);
        x += Math.cos(radians) * speed;
        y += Math.sin(radians) * speed;
        shootTimer++;
    }
    
    @Override
    public void draw(Graphics2D g2d) {
        // Mevcut draw metodu aynı kalabilir
        AffineTransform oldTransform = g2d.getTransform();
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.rotate(Math.toRadians(angle + 90));
        g2d.setTransform(transform);

        GradientPaint bodyGradient = new GradientPaint(
            -SIZE/3, 0, new Color(150, 0, 0),
            SIZE/3, 0, new Color(80, 0, 0)
        );
        g2d.setPaint(bodyGradient);
        g2d.fillRect(-SIZE/3, -SIZE/2, 2*SIZE/3, SIZE);

        int[] xWing = {-SIZE/3, -SIZE, -SIZE/3};
        int[] yWing = {-SIZE/3, 0, SIZE/3};
        g2d.fillPolygon(xWing, yWing, 3);
        
        g2d.setColor(new Color(200, 0, 0));
        g2d.drawPolygon(xWing, yWing, 3);
        g2d.drawLine(-SIZE/2, -SIZE/6, -SIZE*3/4, 0);
        g2d.drawLine(-SIZE/2, SIZE/6, -SIZE*3/4, 0);

        if (shootTimer > shootInterval - 20) {
            int flameWidth = SIZE/4 + (int)(Math.random() * SIZE/4);
            GradientPaint flameGradient = new GradientPaint(
                -SIZE/3, -SIZE/6, Color.YELLOW,
                -SIZE/3 - flameWidth, 0, Color.RED
            );
            g2d.setPaint(flameGradient);
            g2d.fillRect(-SIZE/3 - 1, -SIZE/6, flameWidth, SIZE/3);
        }

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawLine(-SIZE/6, -SIZE/6, SIZE/6, SIZE/6);
        g2d.drawLine(SIZE/6, -SIZE/6, -SIZE/6, SIZE/6);

        g2d.setTransform(oldTransform);
    }
    
    public void hit() {
        health--;
        if (health <= 0) {
            setActive(false);
        }
    }
    
    public boolean canShoot() {
        return shootTimer >= shootInterval;
    }
    
    public void resetShootTimer() {
        shootTimer = 0;
    }
    
    // Parametreli isOffScreen metodu
    public boolean isOffScreen(double cameraX, double cameraY) {
        double relativeX = x - cameraX;
        double relativeY = y - cameraY;
        return relativeY < -SIZE || relativeY > AsteroidHunter.getGAME_HEIGHT() + SIZE ||
               relativeX < -SIZE || relativeX > AsteroidHunter.getGAME_WIDTH() + SIZE;
    }
    
    public int getPoints() {
        return 250;
    }
}

// Arka plan yıldız sınıfı
class Star {
    private double x, y;
    private final float size;
    private float brightness;
    private final double speed;
    
    public Star(double x, double y, float size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.speed = size * 0.2;
        this.brightness = 0.5f + (float) Math.random() * 0.5f;
    }
    
    public void update() {
        y += speed;
        if (y > AsteroidHunter.WORLD_HEIGHT) {
            y = 0;
            x = Math.random() * AsteroidHunter.WORLD_WIDTH;
        }
        brightness = 0.5f + (float) (Math.sin(System.currentTimeMillis() * 0.002 * speed) + 1) * 0.25f;
    }
    
    public void draw(Graphics2D g2d) {
        RadialGradientPaint gradient = new RadialGradientPaint(
            (float) x, (float) y, size * 2,
            new float[] {0.0f, 1.0f},
            new Color[] {new Color(brightness, brightness, brightness, 0.8f), new Color(brightness, brightness, brightness, 0f)}
        );
        g2d.setPaint(gradient);
        g2d.fillOval((int) (x - size), (int) (y - size), (int) (size * 2), (int) (size * 2));
    }
}

// Temel oyun nesnesi sınıfı
abstract class GameObject {
    protected double x, y;
    protected boolean active = true;
    protected double collisionRadius = 0;
    
    public GameObject(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public abstract void update();
    public abstract void draw(Graphics2D g2d);
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void setCollisionRadius(double radius) {
        this.collisionRadius = radius;
    }
    
    public boolean collidesWith(GameObject other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double distance = Math.sqrt(dx*dx + dy*dy);
        
        return distance < (this.collisionRadius + other.collisionRadius);
        }
    }
}

