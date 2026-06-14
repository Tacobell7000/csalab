import processing.core.PApplet;
import processing.core.PImage;
import java.util.ArrayList;

public class Game extends PApplet {

  public static final int APP_WIDTH = 960;
  public static final int APP_HEIGHT = 640;

  PApplet p;

  static final int ROWS = 10;
  static final int COLS = 10;

  ArrayList<GridLocation> path;
  ArrayList<Enemy> enemies;
  ArrayList<Tower> towers;
  ArrayList<Projectile> projectiles;

  ArrayList<Wave> waves;
  int currentWave = 0;

  int money = 150;
  int lives = 25;

  int frameCounter = 0;
  boolean gameOver = false;
  boolean gameWon = false;

  // Tower types (0=basic, 1=splash, 2=sniper, 3=generator)
  int selectedTowerType = 0;
  int[] towerCosts = {75, 120, 90, 150};

  // Preview
  boolean showPreview = false;
  int previewRow = -1;
  int previewCol = -1;
  boolean previewValid = false;

  // Colors
  int grassColor, grassAltColor, pathColor, gridOutlineColor;
  int previewValidColor, previewInvalidColor, hudBgColor;

  int tileW, tileH;

  // ---------------- SETUP ---------------- //

  public void settings() {
    size(APP_WIDTH, APP_HEIGHT, JAVA2D);
    p = this;
  }

  public void setup() {
    tileW = APP_WIDTH / COLS;
    tileH = APP_HEIGHT / ROWS;
    initGame();
    System.out.println("Tower Defense Initialized - Map: " + COLS + "x" + ROWS);
  }

  void initGame() {
    enemies = new ArrayList<>();
    towers = new ArrayList<>();
    path = new ArrayList<>();
    waves = new ArrayList<>();
    projectiles = new ArrayList<>();

    currentWave = 0;
    money = 150;
    lives = 25;
    frameCounter = 0;
    gameOver = false;
    gameWon = false;
    showPreview = false;
    selectedTowerType = 0;

    grassColor = color(50, 160, 50);
    grassAltColor = color(45, 150, 45);
    pathColor = color(140, 120, 80);
    gridOutlineColor = color(30, 80, 30);
    previewValidColor = color(0, 255, 0, 100);
    previewInvalidColor = color(255, 0, 0, 100);
    hudBgColor = color(0, 0, 0, 190);

    buildPath();
    setupWaves();
  }

  // ---------------- PATH - S-SHAPED WITH LOOPS ---------------- //

  void buildPath() {
    path.clear();
    // Snake path through middle rows 3-6, leaving rows 0-2 and 7-9 free for towers
    // Row 3: left to right
    for (int c = 0; c < COLS; c++) path.add(new GridLocation(3, c));
    // Down to row 4, right to left
    for (int c = COLS - 1; c >= 0; c--) path.add(new GridLocation(4, c));
    // Down to row 5, left to right
    for (int c = 0; c < COLS; c++) path.add(new GridLocation(5, c));
    // Down to row 6, right to left
    for (int c = COLS - 1; c >= 0; c--) path.add(new GridLocation(6, c));
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

  // ---------------- WAVES ---------------- //

  class Wave {
    int waveNum, totalEnemies, spawned = 0, spawnDelay, timer = 0;
    float enemyHpMult, enemySpeedMult;

    Wave(int waveNum, int totalEnemies, int spawnDelay, float enemyHpMult, float enemySpeedMult) {
      this.waveNum = waveNum; this.totalEnemies = totalEnemies;
      this.spawnDelay = spawnDelay; this.enemyHpMult = enemyHpMult;
      this.enemySpeedMult = enemySpeedMult;
    }
    boolean update() {
      timer++;
      if (spawned < totalEnemies && timer >= spawnDelay) { timer = 0; spawned++; return true; }
      return false;
    }
    boolean isDone() { return spawned >= totalEnemies; }
  }

  void setupWaves() {
    waves.add(new Wave(1,  6,  80, 1.0f, 1.0f));
    waves.add(new Wave(2,  8,  70, 1.0f, 1.0f));
    waves.add(new Wave(3,  10, 65, 1.2f, 1.1f));
    waves.add(new Wave(4,  12, 60, 1.5f, 1.1f));
    waves.add(new Wave(5,  6,  75, 2.5f, 0.9f));
    waves.add(new Wave(6,  14, 55, 1.5f, 1.2f));
    waves.add(new Wave(7,  16, 50, 1.8f, 1.2f));
    waves.add(new Wave(8,  18, 45, 2.0f, 1.3f));
    waves.add(new Wave(9,  20, 40, 2.2f, 1.4f));
    waves.add(new Wave(10, 8,  65, 4.0f, 1.0f));
  }

  // ---------------- LOOP ---------------- //

  public void draw() {
    frameCounter++;
    background(grassColor);
    drawGrid();

    if (!gameOver && !gameWon) {
      handleGenerators();
      spawnEnemies();
      updateEnemies();
      updateTowers();
      handleHover();
    }

    drawPlacementPreview();
    drawTowers();
    drawEnemies();
    drawProjectiles();
    drawHUD();
    checkGameState();
  }

  // ---------------- GRID ---------------- //

  void drawGrid() {
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        int x = c * tileW, y = r * tileH;
        if (isPathTile(r, c)) {
          fill(pathColor); stroke(gridOutlineColor); strokeWeight(1);
          rect(x, y, tileW, tileH);
          // Brick pattern
          stroke(120, 100, 60); strokeWeight(1);
          for (int bx = x; bx < x + tileW; bx += tileW / 3) {
            line(bx, y, bx, y + tileH);
          }
          line(x, y + tileH / 2, x + tileW, y + tileH / 2);
          // Arrows every other tile
          int idx = indexOfPathTile(r, c);
          if (idx >= 0 && idx < path.size() - 1 && (idx % 3 == 0)) {
            GridLocation next = path.get(idx + 1);
            int nx = next.getCol() * tileW + tileW / 2;
            int ny = next.getRow() * tileH + tileH / 2;
            int cx = x + tileW / 2, cy = y + tileH / 2;
            stroke(80, 60, 30); strokeWeight(2);
            line(cx, cy, nx, ny);
            float ang = atan2(ny - cy, nx - cx);
            line(nx, ny, nx - 8 * cos(ang - 0.4f), ny - 8 * sin(ang - 0.4f));
            line(nx, ny, nx - 8 * cos(ang + 0.4f), ny - 8 * sin(ang + 0.4f));
          }
        } else {
          fill((r + c) % 2 == 0 ? grassColor : grassAltColor);
          stroke(gridOutlineColor); strokeWeight(1);
          rect(x, y, tileW, tileH);
        }
      }
    }
    noFill(); stroke(20, 60, 20); strokeWeight(3);
    rect(0, 0, APP_WIDTH, APP_HEIGHT);
  }

  int indexOfPathTile(int r, int c) {
    for (int i = 0; i < path.size(); i++) {
      if (path.get(i).getRow() == r && path.get(i).getCol() == c) return i;
    }
    return -1;
  }

  // ---------------- ENEMY ---------------- //

  class Enemy {
    int hp = 3, maxHp = 3;
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
      e.hp = (int) Math.ceil(3 * w.enemyHpMult);
      e.maxHp = e.hp;
      e.reward = 10 + (e.hp - 3) * 3;
      e.isBoss = w.enemyHpMult >= 2.5f;
      enemies.add(e);
    }
    if (w.isDone() && enemies.isEmpty()) {
      currentWave++;
      if (currentWave < waves.size()) money += 30 + currentWave * 10;
    }
  }

  void updateEnemies() {
    float baseSpeed = 1.5f;
    float speedMult = (currentWave < waves.size()) ? waves.get(currentWave).enemySpeedMult : 1.0f;
    for (int i = enemies.size() - 1; i >= 0; i--) {
      Enemy e = enemies.get(i);
      if (e.dead) { enemies.remove(i); continue; }
      if (e.targetPathIndex >= path.size()) { enemies.remove(i); lives--; continue; }
      GridLocation targetLoc = path.get(e.targetPathIndex);
      float tx = targetLoc.getCol() * tileW + tileW / 2f;
      float ty = targetLoc.getRow() * tileH + tileH / 2f;
      float dx = tx - e.px, dy = ty - e.py;
      float dist = sqrt(dx * dx + dy * dy);
      float speed = baseSpeed * speedMult;
      if (dist < speed) { e.px = tx; e.py = ty; e.targetPathIndex++; }
      else { e.px += dx / dist * speed; e.py += dy / dist * speed; }
    }
  }

  void drawEnemies() {
    for (Enemy e : enemies) {
      float hpPct = (float) e.hp / e.maxHp;
      if (e.isBoss) {
        fill(lerpColor(color(180, 0, 255), color(255, 100, 50), 1 - hpPct));
        stroke(120, 0, 200); strokeWeight(3);
        ellipse(e.px, e.py, tileW * 0.75f, tileH * 0.75f);
        stroke(255, 200, 0); strokeWeight(2);
        float top = e.py - tileH * 0.42f;
        line(e.px - 12, top + 8, e.px - 6, top);
        line(e.px - 6, top, e.px, top + 6);
        line(e.px + 6, top, e.px + 12, top + 8);
      } else {
        fill(lerpColor(color(255, 0, 0), color(255, 180, 0), hpPct));
        stroke(180, 0, 0); strokeWeight(2);
        ellipse(e.px, e.py, tileW * 0.5f, tileH * 0.5f);
      }
      fill(255); noStroke();
      ellipse(e.px - 4, e.py - 2, 5, 5); ellipse(e.px + 4, e.py - 2, 5, 5);
      fill(0); ellipse(e.px - 4, e.py - 2, 2.5f, 2.5f); ellipse(e.px + 4, e.py - 2, 2.5f, 2.5f);
      int barW = e.isBoss ? 40 : 28, barH = 4;
      int barX = (int)(e.px - barW / 2f), barY = (int)(e.py - tileH * (e.isBoss ? 0.45f : 0.32f));
      fill(200, 0, 0); noStroke(); rect(barX, barY, barW, barH);
      fill(0, 200, 0); rect(barX, barY, barW * hpPct, barH);
      stroke(e.isBoss ? color(255, 200, 0) : color(60)); strokeWeight(1); noFill(); rect(barX, barY, barW, barH);
    }
  }

  // ---------------- TOWERS ---------------- //

  final int TYPE_BASIC = 0, TYPE_SPLASH = 1, TYPE_SNIPER = 2, TYPE_GENERATOR = 3;

  class Tower {
    GridLocation loc;
    int type;
    int range, damage, cooldown, maxCooldown;
    int cost;

    Tower(GridLocation loc, int type) {
      this.loc = loc; this.type = type;
      if (type == TYPE_BASIC) {
        range = 2; damage = 1; maxCooldown = 10; cost = 75;
      } else if (type == TYPE_SPLASH) {
        range = 1; damage = 1; maxCooldown = 25; cost = 120;
      } else if (type == TYPE_SNIPER) {
        range = 4; damage = 3; maxCooldown = 30; cost = 90;
      } else if (type == TYPE_GENERATOR) {
        range = 0; damage = 0; maxCooldown = 120; cost = 150;
      }
      cooldown = 0;
    }
  }

  void updateTowers() {
    for (Tower t : towers) {
      if (t.type == TYPE_GENERATOR) continue;
      if (t.cooldown > 0) { t.cooldown--; continue; }
      if (t.type == TYPE_SPLASH) {
        boolean hit = false;
        for (int i = enemies.size() - 1; i >= 0; i--) {
          Enemy e = enemies.get(i);
          if (distTowerToEnemy(t, e) <= t.range) {
            e.hp -= t.damage;
            hit = true;
            if (e.hp <= 0) { e.dead = true; money += e.reward; }
          }
        }
        if (hit) { t.cooldown = t.maxCooldown; projectiles.add(new Projectile(t, null)); }
      } else {
        Enemy target = findBestTarget(t);
        if (target != null) {
          target.hp -= t.damage;
          t.cooldown = t.maxCooldown;
          projectiles.add(new Projectile(t, target));
          if (target.hp <= 0) { target.dead = true; money += target.reward; }
        }
      }
    }
  }

  void handleGenerators() {
    for (Tower t : towers) {
      if (t.type == TYPE_GENERATOR) {
        if (t.cooldown > 0) t.cooldown--;
        if (t.cooldown <= 0) {
          money += 5;
          t.cooldown = t.maxCooldown;
        }
      }
    }
  }

  float distTowerToEnemy(Tower t, Enemy e) {
    float cx = t.loc.getCol() * tileW + tileW / 2f;
    float cy = t.loc.getRow() * tileH + tileH / 2f;
    return sqrt(sq(e.px - cx) + sq(e.py - cy)) / Math.min(tileW, tileH);
  }

  // ---------------- PROJECTILES ---------------- //

  class Projectile {
    float x, y, targetX, targetY, speed = 6;
    boolean alive = true;
    int type;

    Projectile(Tower t, Enemy e) {
      this.type = t.type;
      this.x = t.loc.getCol() * tileW + tileW / 2f;
      this.y = t.loc.getRow() * tileH + tileH / 2f;
      if (e != null) { this.targetX = e.px; this.targetY = e.py; }
      else { this.targetX = x; this.targetY = y; alive = false; }
      if (type == TYPE_SNIPER) speed = 12;
    }

    boolean update() {
      if (!alive) return false;
      float dx = targetX - x, dy = targetY - y;
      float dist = sqrt(dx * dx + dy * dy);
      if (dist < speed) { alive = false; return false; }
      x += dx / dist * speed; y += dy / dist * speed;
      return true;
    }
  }

  void drawProjectiles() {
    for (int i = projectiles.size() - 1; i >= 0; i--) {
      Projectile pj = projectiles.get(i);
      if (!pj.update()) { projectiles.remove(i); continue; }
      if (pj.type == TYPE_SNIPER) {
        fill(255, 80, 80); noStroke();
        ellipse(pj.x, pj.y, 4, 10);
      } else if (pj.type == TYPE_SPLASH) {
        fill(100, 255, 100, 150); noStroke();
        ellipse(pj.x, pj.y, 18, 18);
      } else {
        fill(255, 255, 100); noStroke();
        ellipse(pj.x, pj.y, 7, 7);
        fill(255, 255, 200, 100); ellipse(pj.x, pj.y, 12, 12);
      }
    }
  }

  // ---------------- TOWER RENDERING ---------------- //

  void drawTowers() {
    for (Tower t : towers) {
      int tx = t.loc.getCol() * tileW, ty = t.loc.getRow() * tileH;
      int cx = tx + tileW / 2, cy = ty + tileH / 2;

      if (t.type == TYPE_BASIC)       drawTowerBasic(tx, ty, cx, cy, t);
      else if (t.type == TYPE_SPLASH) drawTowerSplash(tx, ty, cx, cy, t);
      else if (t.type == TYPE_SNIPER) drawTowerSniper(tx, ty, cx, cy, t);
      else if (t.type == TYPE_GENERATOR) drawTowerGenerator(tx, ty, cx, cy, t);
    }
  }

  void drawTowerBasic(int tx, int ty, int cx, int cy, Tower t) {
    fill(80, 80, 90); stroke(140, 140, 160); strokeWeight(2);
    rect(tx + 4, ty + 4, tileW - 8, tileH - 8, 4);
    fill(0, 100, 200); stroke(160, 200, 255); strokeWeight(1);
    ellipse(cx, cy, tileW * 0.4f, tileH * 0.4f);
    float ang = frameCounter * 0.03f + t.loc.hashCode() * 0.5f;
    int bx = (int)(cx + cos(ang) * tileW / 3), by = (int)(cy + sin(ang) * tileW / 3);
    stroke(180, 200, 255); strokeWeight(3); line(cx, cy, bx, by);
    fill(255); noStroke(); ellipse(cx, cy, 6, 6);
  }

  void drawTowerSplash(int tx, int ty, int cx, int cy, Tower t) {
    fill(60, 90, 60); stroke(100, 200, 100); strokeWeight(2);
    rect(tx + 4, ty + 4, tileW - 8, tileH - 8, 8);
    fill(50, 200, 50); stroke(150, 255, 150); strokeWeight(1);
    ellipse(cx, cy, tileW * 0.45f, tileH * 0.45f);
    // Mortar top
    fill(40, 150, 40); noStroke();
    rect(cx - tileW / 6, cy - tileH / 5, tileW / 3, tileH / 4, 3);
    // Animated pulse
    float pulse = sin(frameCounter * 0.1f) * 3 + 7;
    fill(100, 255, 100, 100); ellipse(cx, cy, pulse, pulse);
  }

  void drawTowerSniper(int tx, int ty, int cx, int cy, Tower t) {
    fill(60, 60, 80); stroke(150, 150, 200); strokeWeight(2);
    rect(tx + 2, ty + 8, tileW - 4, tileH - 16, 2);
    fill(200, 60, 60); stroke(255, 100, 100); strokeWeight(1);
    ellipse(cx, cy, tileW * 0.3f, tileH * 0.3f);
    // Long barrel
    float ang = frameCounter * 0.02f + t.loc.hashCode() * 0.3f;
    int bx = (int)(cx + cos(ang) * tileW * 0.7f), by = (int)(cy + sin(ang) * tileW * 0.7f);
    stroke(180, 60, 60); strokeWeight(4); line(cx, cy, bx, by);
    // Scope
    fill(255); noStroke(); ellipse(cx, cy, 6, 6);
    stroke(255, 100, 100); strokeWeight(1); noFill();
    ellipse(cx, cy, tileW, tileH * 0.6f);
  }

  void drawTowerGenerator(int tx, int ty, int cx, int cy, Tower t) {
    fill(80, 70, 30); stroke(200, 180, 80); strokeWeight(2);
    rect(tx + 4, ty + 4, tileW - 8, tileH - 8, 4);
    fill(255, 220, 0); stroke(255, 240, 100); strokeWeight(1);
    ellipse(cx, cy, tileW * 0.4f, tileH * 0.4f);
    // Dollar sign
    fill(40, 40, 0); noStroke();
    textSize(tileW * 0.35f); textAlign(CENTER, CENTER);
    text("$", cx, cy + 1);
    // Sparkle
    float spark = sin(frameCounter * 0.12f);
    fill(255, 255, 100, 100 + (int)(spark * 80));
    ellipse(cx, cy - tileH * 0.25f, 8, 8);
  }

  // ---------------- PREVIEW ---------------- //

  void handleHover() {
    if (gameOver || gameWon || mouseY < 50 || mouseY > APP_HEIGHT - 35) {
      showPreview = false; return;
    }
    previewCol = mouseX / tileW;
    previewRow = mouseY / tileH;
    previewValid = canPlaceTower(previewRow, previewCol) && money >= towerCosts[selectedTowerType];
    showPreview = true;
  }

  void drawPlacementPreview() {
    if (!showPreview || gameOver || gameWon) return;
    int px = previewCol * tileW, py = previewRow * tileH;
    fill(previewValid ? previewValidColor : previewInvalidColor);
    noStroke(); rect(px, py, tileW, tileH);
    if (previewValid) {
      int cx = px + tileW / 2, cy = py + tileH / 2;
      int rangePx = getPreviewRange();
      if (rangePx > 0) {
        noFill(); stroke(255, 255, 255, 50); strokeWeight(1);
        ellipse(cx, cy, rangePx * 2, rangePx * 2);
      }
    }
  }

  int getPreviewRange() {
    if (selectedTowerType == TYPE_GENERATOR) return 0;
    if (selectedTowerType == TYPE_SPLASH) return 1;
    if (selectedTowerType == TYPE_SNIPER) return 4;
    return 2;
  }

  // ---------------- TARGETING ---------------- //

  Enemy findBestTarget(Tower t) {
    Enemy best = null; float bestProgress = -1;
    for (Enemy e : enemies) {
      if (distTowerToEnemy(t, e) > t.range) continue;
      if (e.targetPathIndex > bestProgress) { bestProgress = e.targetPathIndex; best = e; }
    }
    return best;
  }

  // ---------------- HUD ---------------- //

  void drawHUD() {
    // Top bar
    noStroke(); fill(hudBgColor);
    rect(0, 0, APP_WIDTH, 54);
    textSize(14); textAlign(LEFT, CENTER);
    fill(255, 60, 60); text("♥ " + lives, 15, 20);
    fill(255, 220, 0); text("$" + money, 100, 20);
    fill(100, 200, 255); text("Wave " + (currentWave + 1) + "/" + waves.size(), 195, 20);
    if (currentWave < waves.size()) {
      Wave w = waves.get(currentWave);
      fill(255, 150, 150);
      text("Enemies: " + enemies.size() + " (" + w.spawned + "/" + w.totalEnemies + ")", 370, 20);
    }

    // Tower selection row
    int selY = 36;
    textSize(12);
    drawTowerButton(480, 30, TYPE_BASIC, "1: Gunner $75");
    drawTowerButton(610, 30, TYPE_SPLASH, "2: Splash $120");
    drawTowerButton(740, 30, TYPE_SNIPER, "3: Sniper $90");
    drawTowerButton(870, 30, TYPE_GENERATOR, "4: Bank $150");

    // Bottom bar
    fill(hudBgColor); rect(0, APP_HEIGHT - 35, APP_WIDTH, 35);
    textSize(12); textAlign(CENTER, CENTER); fill(180);
    String towerName = selectedTowerType == TYPE_BASIC ? "Gunner" :
                       selectedTowerType == TYPE_SPLASH ? "Splash" :
                       selectedTowerType == TYPE_SNIPER ? "Sniper" : "Bank";
    if (!gameOver && !gameWon)
      text("Press 1-4 to select tower  |  Selected: " + towerName + " ($" + towerCosts[selectedTowerType] + ")  |  Click green tile to place", APP_WIDTH / 2, APP_HEIGHT - 17);
    else text("Click PLAY AGAIN to restart", APP_WIDTH / 2, APP_HEIGHT - 17);
  }

  void drawTowerButton(int bx, int by, int type, String label) {
    boolean sel = selectedTowerType == type;
    boolean canAfford = money >= towerCosts[type];
    if (sel) {
      fill(255, 255, 255, 200); stroke(255, 255, 0); strokeWeight(2);
    } else {
      fill(canAfford ? color(80, 80, 120, 200) : color(60, 20, 20, 200));
      stroke(sel ? color(255, 255, 0) : color(100)); strokeWeight(1);
    }
    rect(bx, by, 125, 20, 3);
    fill(canAfford ? (sel ? 0 : 255) : 150);
    textAlign(LEFT, CENTER); noStroke();
    float tw = textWidth(label);
    text(label, bx + (125 - tw) / 2, by + 10);
  }

  // ---------------- REPLAY ---------------- //

  boolean isMouseOverReplay() {
    int bx = APP_WIDTH / 2 - 100, by = APP_HEIGHT / 2 + 50;
    return mouseX >= bx && mouseX <= bx + 200 && mouseY >= by && mouseY <= by + 50;
  }

  void drawReplayButton() {
    int bx = APP_WIDTH / 2 - 100, by = APP_HEIGHT / 2 + 50;
    fill(isMouseOverReplay() ? color(60, 180, 60) : color(40, 140, 40));
    stroke(100, 255, 100); strokeWeight(2); rect(bx, by, 200, 50, 8);
    fill(255); textSize(18); textAlign(CENTER, CENTER); noStroke();
    text("PLAY AGAIN", APP_WIDTH / 2, by + 25);
  }

  // ---------------- INPUT ---------------- //

  public void keyPressed() {
    if (key == '1') selectedTowerType = TYPE_BASIC;
    else if (key == '2') selectedTowerType = TYPE_SPLASH;
    else if (key == '3') selectedTowerType = TYPE_SNIPER;
    else if (key == '4') selectedTowerType = TYPE_GENERATOR;
  }

  public void mouseClicked() {
    if ((gameOver || gameWon) && isMouseOverReplay()) { initGame(); loop(); return; }
    if (!gameOver && !gameWon) {
      int col = mouseX / tileW, row = mouseY / tileH;
      if (canPlaceTower(row, col) && money >= towerCosts[selectedTowerType]) {
        money -= towerCosts[selectedTowerType];
        towers.add(new Tower(new GridLocation(row, col), selectedTowerType));
      }
    }
  }

  // ---------------- GAME STATE ---------------- //

  void checkGameState() {
    if (lives <= 0 && !gameOver) gameOver = true;
    if (currentWave >= waves.size() && enemies.isEmpty() && lives > 0 && !gameWon && !gameOver) gameWon = true;
    if (gameOver) { drawEndScreen("GAME OVER", color(255, 60, 60), "You lost all your lives!"); drawReplayButton(); }
    else if (gameWon) { drawEndScreen("VICTORY!", color(80, 255, 80), "Final Score: $" + money); drawReplayButton(); }
  }

  void drawEndScreen(String title, int titleColor, String subtitle) {
    fill(0, 0, 0, 190); noStroke(); rect(0, 0, APP_WIDTH, APP_HEIGHT);
    fill(titleColor); textSize(48); textAlign(CENTER, CENTER);
    text(title, APP_WIDTH / 2, APP_HEIGHT / 2 - 30);
    fill(200); textSize(20);
    text(subtitle, APP_WIDTH / 2, APP_HEIGHT / 2 + 10);
  }
}