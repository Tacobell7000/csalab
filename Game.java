import processing.core.PApplet;
import processing.core.PImage;
import java.util.ArrayList;

public class Game extends PApplet {

  public static final int APP_WIDTH = 800;
  public static final int APP_HEIGHT = 600;

  PApplet p;

  // Grid dimensions
  static final int ROWS = 8;
  static final int COLS = 8;

  ArrayList<GridLocation> path;
  ArrayList<Enemy> enemies;
  ArrayList<Tower> towers;
  ArrayList<Projectile> projectiles;

  ArrayList<Wave> waves;
  int currentWave = 0;

  int money = 100;
  int lives = 20;
  int towerCost = 75;

  int frameCounter = 0;

  boolean gameOver = false;
  boolean gameWon = false;

  // Tower placement preview
  boolean showPreview = false;
  int previewRow = -1;
  int previewCol = -1;
  boolean previewValid = false;

  // Colors
  int grassColor;
  int grassAltColor;
  int pathColor;
  int towerColor;
  int enemyColor;
  int gridOutlineColor;
  int previewValidColor;
  int previewInvalidColor;
  int hudBgColor;

  int tileW;
  int tileH;

  // ---------------- SETUP ---------------- //

  public void settings() {
    size(APP_WIDTH, APP_HEIGHT, JAVA2D);
    p = this;
  }

  public void setup() {
    tileW = APP_WIDTH / COLS;
    tileH = APP_HEIGHT / ROWS;
    initGame();
    System.out.println("Tower Defense Initialized");
  }

  void initGame() {
    enemies = new ArrayList<>();
    towers = new ArrayList<>();
    path = new ArrayList<>();
    waves = new ArrayList<>();
    projectiles = new ArrayList<>();

    currentWave = 0;
    money = 100;
    lives = 20;
    frameCounter = 0;
    gameOver = false;
    gameWon = false;
    showPreview = false;

    // Define colors
    grassColor = color(50, 160, 50);
    grassAltColor = color(45, 150, 45);
    pathColor = color(140, 120, 80);
    towerColor = color(0, 100, 200);
    enemyColor = color(220, 40, 40);
    gridOutlineColor = color(30, 80, 30);
    previewValidColor = color(0, 255, 0, 100);
    previewInvalidColor = color(255, 0, 0, 100);
    hudBgColor = color(0, 0, 0, 180);

    buildPath();
    setupWaves();
  }

  // ---------------- PATH SYSTEM ---------------- //

  void buildPath() {
    path.clear();
    int row = 2;
    path.add(new GridLocation(row, 0));
    for (int col = 1; col < COLS; col++) {
      path.add(new GridLocation(row, col));
      if (col % 2 == 1) {
        row = (row == 2) ? 3 : 2;
      }
    }
  }

  boolean isPathTile(int r, int c) {
    for (GridLocation loc : path) {
      if (loc.getRow() == r && loc.getCol() == c) return true;
    }
    return false;
  }

  boolean hasTower(int r, int c) {
    for (Tower t : towers) {
      if (t.loc.getRow() == r && t.loc.getCol() == c) return true;
    }
    return false;
  }

  boolean canPlaceTower(int r, int c) {
    if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return false;
    if (isPathTile(r, c)) return false;
    if (hasTower(r, c)) return false;
    return true;
  }

  // ---------------- WAVES (DYNAMIC SCALING) ---------------- //

  class Wave {
    int waveNum;
    int totalEnemies;
    int spawned = 0;
    int spawnDelay;
    int timer = 0;
    float enemyHpMult;    // How much HP enemies in this wave have
    float enemySpeedMult; // How fast they move

    Wave(int waveNum, int totalEnemies, int spawnDelay, float enemyHpMult, float enemySpeedMult) {
      this.waveNum = waveNum;
      this.totalEnemies = totalEnemies;
      this.spawnDelay = spawnDelay;
      this.enemyHpMult = enemyHpMult;
      this.enemySpeedMult = enemySpeedMult;
    }

    boolean update() {
      timer++;
      if (spawned < totalEnemies && timer >= spawnDelay) {
        timer = 0;
        spawned++;
        return true;
      }
      return false;
    }

    boolean isDone() {
      return spawned >= totalEnemies;
    }
  }

  void setupWaves() {
    // Generate 10 waves with progressive difficulty
    // Wave params: (waveNum, totalEnemies, spawnDelay, hpMult, speedMult)
    waves.add(new Wave(1,  5,  80, 1.0f, 1.0f));
    waves.add(new Wave(2,  8,  70, 1.0f, 1.0f));
    waves.add(new Wave(3,  10, 65, 1.2f, 1.1f));
    waves.add(new Wave(4,  12, 60, 1.5f, 1.1f));
    waves.add(new Wave(5,  6,  75, 2.5f, 0.9f));  // Boss wave - fewer but tough
    waves.add(new Wave(6,  14, 55, 1.5f, 1.2f));
    waves.add(new Wave(7,  16, 50, 1.8f, 1.2f));
    waves.add(new Wave(8,  18, 45, 2.0f, 1.3f));
    waves.add(new Wave(9,  20, 40, 2.2f, 1.4f));
    waves.add(new Wave(10, 8,  65, 4.0f, 1.0f)); // Final boss wave
  }

  // ---------------- LOOP ---------------- //

  public void draw() {
    frameCounter++;
    background(grassColor);

    // Draw grid
    drawGrid();

    if (!gameOver && !gameWon) {
      spawnEnemies();
      updateEnemies();
      updateTowers();
      handleHover(); // update tower placement preview
    }

    // Draw game entities
    drawPlacementPreview();
    drawTowers();
    drawEnemies();
    drawProjectiles();

    // HUD overlay
    drawHUD();

    // Game state
    checkGameState();
  }

  // ---------------- GRID RENDERING ---------------- //

  void drawGrid() {
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        int x = c * tileW;
        int y = r * tileH;

        if (isPathTile(r, c)) {
          fill(pathColor);
          stroke(gridOutlineColor);
          strokeWeight(1);
          rect(x, y, tileW, tileH);

          // Path texture
          stroke(120, 100, 60);
          strokeWeight(1);
          line(x + 2, y + 2, x + tileW - 2, y + tileH - 2);
          line(x + 2, y + tileH - 2, x + tileW - 2, y + 2);

          // Direction arrows
          int idx = indexOfPathTile(r, c);
          if (idx >= 0 && idx < path.size() - 1) {
            GridLocation next = path.get(idx + 1);
            int nx = next.getCol() * tileW + tileW / 2;
            int ny = next.getRow() * tileH + tileH / 2;
            int cx = x + tileW / 2;
            int cy = y + tileH / 2;
            stroke(100, 80, 50);
            strokeWeight(2);
            line(cx, cy, nx, ny);
            float angle = atan2(ny - cy, nx - cx);
            line(nx, ny, nx - 8 * cos(angle - 0.4f), ny - 8 * sin(angle - 0.4f));
            line(nx, ny, nx - 8 * cos(angle + 0.4f), ny - 8 * sin(angle + 0.4f));
          }
        } else {
          // Grass with checkerboard pattern
          fill((r + c) % 2 == 0 ? grassColor : grassAltColor);
          stroke(gridOutlineColor);
          strokeWeight(1);
          rect(x, y, tileW, tileH);
        }
      }
    }

    // Grid border
    noFill();
    stroke(20, 60, 20);
    strokeWeight(3);
    rect(0, 0, APP_WIDTH, APP_HEIGHT);
  }

  int indexOfPathTile(int r, int c) {
    for (int i = 0; i < path.size(); i++) {
      GridLocation loc = path.get(i);
      if (loc.getRow() == r && loc.getCol() == c) return i;
    }
    return -1;
  }

  // ---------------- ENEMY ---------------- //

  class Enemy {
    int hp = 3;
    int maxHp = 3;
    boolean dead = false;
    float px, py;
    int targetPathIndex = 1;
    int reward = 10;
    boolean isBoss = false;

    Enemy() {
      GridLocation start = path.get(0);
      px = start.getCol() * tileW + tileW / 2f;
      py = start.getRow() * tileH + tileH / 2f;
    }
  }

  void spawnEnemies() {
    if (currentWave >= waves.size()) return;
    Wave w = waves.get(currentWave);
    if (w.update()) {
      Enemy e = new Enemy();
      // Apply wave's HP multiplier
      e.hp = (int) Math.ceil(3 * w.enemyHpMult);
      e.maxHp = e.hp;
      // Scale reward with HP
      e.reward = 10 + (e.hp - 3) * 3;
      // Mark boss enemies
      e.isBoss = w.enemyHpMult >= 2.5f;
      enemies.add(e);
    }
    if (w.isDone() && enemies.isEmpty()) {
      currentWave++;
      if (currentWave < waves.size()) {
        money += 30 + currentWave * 10;
      }
    }
  }

  void updateEnemies() {
    // Base speed, multiplied by current wave's speed factor
    float baseSpeed = 1.5f;
    float speedMult = 1.0f;
    if (currentWave < waves.size()) {
      speedMult = waves.get(currentWave).enemySpeedMult;
    }

    for (int i = enemies.size() - 1; i >= 0; i--) {
      Enemy e = enemies.get(i);
      float speed = baseSpeed * speedMult;

      if (e.dead) {
        enemies.remove(i);
        continue;
      }

      if (e.targetPathIndex >= path.size()) {
        enemies.remove(i);
        lives--;
        continue;
      }

      GridLocation targetLoc = path.get(e.targetPathIndex);
      float targetX = targetLoc.getCol() * tileW + tileW / 2f;
      float targetY = targetLoc.getRow() * tileH + tileH / 2f;

      float dx = targetX - e.px;
      float dy = targetY - e.py;
      float dist = sqrt(dx * dx + dy * dy);

      if (dist < speed) {
        e.px = targetX;
        e.py = targetY;
        e.targetPathIndex++;
      } else {
        e.px += (dx / dist) * speed;
        e.py += (dy / dist) * speed;
      }
    }
  }

  // ---------------- ENEMY RENDERING ---------------- //

  void drawEnemies() {
    for (Enemy e : enemies) {
      float healthPercent = (float) e.hp / e.maxHp;

      if (e.isBoss) {
        // Boss rendering - larger, purple, with crown lines
        int bossFill = lerpColor(color(180, 0, 255), color(255, 100, 50), 1 - healthPercent);
        fill(bossFill);
        stroke(120, 0, 200);
        strokeWeight(3);
        ellipse(e.px, e.py, tileW * 0.75f, tileH * 0.75f);

        // Crown spikes
        stroke(255, 200, 0);
        strokeWeight(2);
        float top = e.py - tileH * 0.42f;
        line(e.px - 12, top + 8, e.px - 6, top);
        line(e.px - 6, top, e.px, top + 6);
        line(e.px + 6, top, e.px + 12, top + 8);
      } else {
        // Normal enemy
        int enemyFill = lerpColor(color(255, 0, 0), color(255, 180, 0), healthPercent);
        fill(enemyFill);
        stroke(180, 0, 0);
        strokeWeight(2);
        ellipse(e.px, e.py, tileW * 0.55f, tileH * 0.55f);
      }

      // Eyes
      fill(255);
      noStroke();
      ellipse(e.px - 5, e.py - 3, 6, 6);
      ellipse(e.px + 5, e.py - 3, 6, 6);
      fill(0);
      ellipse(e.px - 5, e.py - 3, 3, 3);
      ellipse(e.px + 5, e.py - 3, 3, 3);

      // Health bar
      int barW = e.isBoss ? 40 : 30;
      int barH = 5;
      int barX = (int) (e.px - barW / 2f);
      int barY = (int) (e.py - tileH * (e.isBoss ? 0.45f : 0.35f));
      fill(200, 0, 0);
      noStroke();
      rect(barX, barY, barW, barH);
      fill(0, 200, 0);
      rect(barX, barY, barW * healthPercent, barH);
      stroke(e.isBoss ? color(255, 200, 0) : color(60));
      strokeWeight(1);
      noFill();
      rect(barX, barY, barW, barH);
    }
  }

  // ---------------- TOWERS ---------------- //

  class Tower {
    GridLocation loc;
    int range = 2;
    int damage = 1;
    int cooldown = 0;
    int cost = 75;

    Tower(GridLocation loc) {
      this.loc = loc;
    }
  }

  void updateTowers() {
    for (Tower t : towers) {
      if (t.cooldown > 0) {
        t.cooldown--;
        continue;
      }
      Enemy target = findBestTarget(t);
      if (target != null) {
        target.hp -= t.damage;
        t.cooldown = 12;
        projectiles.add(new Projectile(t, target));
        if (target.hp <= 0) {
          target.dead = true;
          money += target.reward;
        }
      }
    }
  }

  // ---------------- PROJECTILES ---------------- //

  class Projectile {
    float x, y;
    float targetX, targetY;
    float speed = 5;
    boolean alive = true;

    Projectile(Tower t, Enemy e) {
      this.x = t.loc.getCol() * tileW + tileW / 2f;
      this.y = t.loc.getRow() * tileH + tileH / 2f;
      this.targetX = e.px;
      this.targetY = e.py;
    }

    boolean update() {
      float dx = targetX - x;
      float dy = targetY - y;
      float dist = sqrt(dx * dx + dy * dy);
      if (dist < speed) {
        alive = false;
        return false;
      }
      x += (dx / dist) * speed;
      y += (dy / dist) * speed;
      return true;
    }
  }

  void drawProjectiles() {
    for (int i = projectiles.size() - 1; i >= 0; i--) {
      Projectile proj = projectiles.get(i);
      if (!proj.update() || !proj.alive) {
        projectiles.remove(i);
        continue;
      }
      fill(255, 255, 100);
      noStroke();
      ellipse(proj.x, proj.y, 7, 7);
      // Glow effect
      fill(255, 255, 200, 100);
      ellipse(proj.x, proj.y, 12, 12);
    }
  }

  // ---------------- TOWER RENDERING ---------------- //

  void drawTowers() {
    for (Tower t : towers) {
      int tx = t.loc.getCol() * tileW;
      int ty = t.loc.getRow() * tileH;
      int cx = tx + tileW / 2;
      int cy = ty + tileH / 2;

      // Tower base
      fill(80, 80, 90);
      stroke(140, 140, 160);
      strokeWeight(2);
      rect(tx + 4, ty + 4, tileW - 8, tileH - 8, 4);

      // Tower top
      fill(towerColor);
      stroke(160, 200, 255);
      strokeWeight(1);
      ellipse(cx, cy, tileW * 0.45f, tileH * 0.45f);

      // Gun barrel
      float angle = frameCounter * 0.03f + t.loc.hashCode() * 0.5f;
      int barrelLen = tileW / 3;
      int bx = (int) (cx + cos(angle) * barrelLen);
      int by = (int) (cy + sin(angle) * barrelLen);
      stroke(180, 200, 255);
      strokeWeight(3);
      line(cx, cy, bx, by);

      // Center
      fill(255);
      noStroke();
      ellipse(cx, cy, 7, 7);
    }
  }

  // ---------------- PLACEMENT PREVIEW ---------------- //

  void handleHover() {
    if (gameOver || gameWon) {
      showPreview = false;
      return;
    }
    if (mouseY < 45 || mouseY > APP_HEIGHT - 35) {
      showPreview = false;
      return;
    }

    previewCol = mouseX / tileW;
    previewRow = mouseY / tileH;
    previewValid = canPlaceTower(previewRow, previewCol) && money >= towerCost;
    showPreview = true;
  }

  void drawPlacementPreview() {
    if (!showPreview || gameOver || gameWon) return;

    int px = previewCol * tileW;
    int py = previewRow * tileH;

    if (previewValid) {
      fill(previewValidColor);
    } else {
      fill(previewInvalidColor);
    }
    noStroke();
    rect(px, py, tileW, tileH);

    // Range preview
    if (previewValid) {
      int cx = px + tileW / 2;
      int cy = py + tileH / 2;
      int rangePx = 2 * Math.min(tileW, tileH) / 2;
      noFill();
      stroke(255, 255, 255, 50);
      strokeWeight(1);
      ellipse(cx, cy, rangePx * 2, rangePx * 2);
    }
  }

  // ---------------- TARGETING ---------------- //

  Enemy findBestTarget(Tower t) {
    Enemy best = null;
    float bestProgress = -1;

    for (Enemy e : enemies) {
      float towerCX = t.loc.getCol() * tileW + tileW / 2f;
      float towerCY = t.loc.getRow() * tileH + tileH / 2f;
      float dist = sqrt(sq(e.px - towerCX) + sq(e.py - towerCY));
      float distInTiles = dist / Math.min(tileW, tileH);

      if (distInTiles > t.range) continue;

      if (e.targetPathIndex > bestProgress) {
        bestProgress = e.targetPathIndex;
        best = e;
      }
    }
    return best;
  }

  // ---------------- HUD ---------------- //

  void drawHUD() {
    // === TOP BAR ===
    noStroke();
    fill(hudBgColor);
    rect(0, 0, APP_WIDTH, 45);

    textSize(15);
    textAlign(LEFT, CENTER);

    // Lives
    fill(255, 60, 60);
    text("♥ " + lives, 15, 22);

    // Money
    fill(255, 220, 0);
    text("$" + money, 110, 22);

    // Wave
    fill(100, 200, 255);
    text("Wave " + (currentWave + 1) + "/" + waves.size(), 200, 22);

    // Tower cost info
    fill(180, 180, 255);
    text("Tower: $" + towerCost, 350, 22);

    // Enemy count
    if (currentWave < waves.size()) {
      Wave w = waves.get(currentWave);
      fill(255, 150, 150);
      text("Enemies: " + enemies.size() + " (" + w.spawned + "/" + w.totalEnemies + ")", 480, 22);
    }

    // === BOTTOM BAR ===
    fill(hudBgColor);
    rect(0, APP_HEIGHT - 35, APP_WIDTH, 35);

    textSize(12);
    textAlign(CENTER, CENTER);
    fill(180);
    if (!gameOver && !gameWon) {
      text("Click on green tiles to place towers ($" + towerCost + " each)  |  Cannot place on the brown path", APP_WIDTH / 2, APP_HEIGHT - 17);
    } else {
      text("Click PLAY AGAIN to restart", APP_WIDTH / 2, APP_HEIGHT - 17);
    }
  }

  // ---------------- REPLAY BUTTON ---------------- //

  boolean isMouseOverReplay() {
    int btnX = APP_WIDTH / 2 - 100;
    int btnY = APP_HEIGHT / 2 + 50;
    int btnW = 200;
    int btnH = 50;
    return mouseX >= btnX && mouseX <= btnX + btnW &&
           mouseY >= btnY && mouseY <= btnY + btnH;
  }

  void drawReplayButton() {
    int btnX = APP_WIDTH / 2 - 100;
    int btnY = APP_HEIGHT / 2 + 50;
    int btnW = 200;
    int btnH = 50;

    boolean hovered = isMouseOverReplay();

    if (hovered) {
      fill(60, 180, 60);
    } else {
      fill(40, 140, 40);
    }
    stroke(100, 255, 100);
    strokeWeight(2);
    rect(btnX, btnY, btnW, btnH, 8);

    fill(255);
    textSize(18);
    textAlign(CENTER, CENTER);
    noStroke();
    text("PLAY AGAIN", APP_WIDTH / 2, btnY + btnH / 2);
  }

  // ---------------- MOUSE INPUT ---------------- //

  public void mouseClicked() {
    // Replay button
    if ((gameOver || gameWon) && isMouseOverReplay()) {
      initGame();
      loop();
      return;
    }

    // Tower placement
    if (!gameOver && !gameWon) {
      int col = mouseX / tileW;
      int row = mouseY / tileH;

      if (canPlaceTower(row, col) && money >= towerCost) {
        money -= towerCost;
        towers.add(new Tower(new GridLocation(row, col)));
        towerCost += 15; // Each tower costs more
      }
    }
  }

  // ---------------- GAME STATE ---------------- //

  void checkGameState() {
    if (lives <= 0 && !gameOver) {
      gameOver = true;
    }
    if (currentWave >= waves.size() && enemies.isEmpty() && lives > 0 && !gameWon && !gameOver) {
      gameWon = true;
    }

    if (gameOver) {
      drawEndScreen("GAME OVER", color(255, 60, 60), "You lost all your lives!");
      drawReplayButton();
    } else if (gameWon) {
      drawEndScreen("VICTORY!", color(80, 255, 80), "Final Score: $" + money);
      drawReplayButton();
    }
  }

  void drawEndScreen(String title, int titleColor, String subtitle) {
    fill(0, 0, 0, 190);
    noStroke();
    rect(0, 0, APP_WIDTH, APP_HEIGHT);
    fill(titleColor);
    textSize(48);
    textAlign(CENTER, CENTER);
    text(title, APP_WIDTH / 2, APP_HEIGHT / 2 - 30);
    fill(200);
    textSize(20);
    text(subtitle, APP_WIDTH / 2, APP_HEIGHT / 2 + 10);
  }
}