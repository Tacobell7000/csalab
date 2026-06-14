import processing.core.PApplet;
import processing.core.PImage;
import java.util.ArrayList;

public class Game extends PApplet {

  public static final int APP_WIDTH = 960;
  public static final int APP_HEIGHT = 640;

  PApplet p;

  static final int ROWS = 10;
  static final int COLS = 10;

  // Screen state
  final int SCREEN_START = 0, SCREEN_GAME = 1;
  int screen = SCREEN_START;

  // Difficulty
  int difficulty = 1; // 0=easy, 1=medium, 2=hard
  float[] diffStartMoney = {225, 150, 90};
  int[] diffStartLives = {30, 25, 18};
  float[] diffHpScale = {0.7f, 1.0f, 1.4f};
  float[] diffSpeedScale = {0.85f, 1.0f, 1.2f};
  int[] diffWaveCount = {12, 15, 18};

  // Two separate paths for upper and lower lanes
  ArrayList<GridLocation> pathUpper;
  ArrayList<GridLocation> pathLower;
  ArrayList<GridLocation> allPathTiles; // combined for isPathTile check

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

  int selectedTowerType = 0;
  int[] towerCosts = {75, 120, 90, 150};

  boolean showPreview = false;
  int previewRow = -1, previewCol = -1;
  boolean previewValid = false;

  int grassColor, grassAltColor, pathColor, gridOutlineColor;
  int previewValidColor, previewInvalidColor, hudBgColor;
  int startBgColor;

  int tileW, tileH;

  // Start screen button states
  int startBtnX, startBtnY, startBtnW, startBtnH;

  // ---------------- SETUP ---------------- //

  public void settings() {
    size(APP_WIDTH, APP_HEIGHT, JAVA2D);
    p = this;
  }

  public void setup() {
    tileW = APP_WIDTH / COLS;
    tileH = APP_HEIGHT / ROWS;

    grassColor = color(50, 160, 50);
    grassAltColor = color(45, 150, 45);
    pathColor = color(140, 120, 80);
    gridOutlineColor = color(30, 80, 30);
    previewValidColor = color(0, 255, 0, 100);
    previewInvalidColor = color(255, 0, 0, 100);
    hudBgColor = color(0, 0, 0, 190);
    startBgColor = color(20, 40, 20);

    allPathTiles = new ArrayList<>();
    pathUpper = new ArrayList<>();
    pathLower = new ArrayList<>();

    startBtnX = APP_WIDTH / 2 - 120;
    startBtnY = 0; // set per button
    startBtnW = 240;
    startBtnH = 50;

    initGameData();
    System.out.println("Tower Defense Initialized - Map: " + COLS + "x" + ROWS);
  }

  void initGameData() {
    enemies = new ArrayList<>();
    towers = new ArrayList<>();
    projectiles = new ArrayList<>();
    waves = new ArrayList<>();
    allPathTiles.clear();
    pathUpper.clear();
    pathLower.clear();

    currentWave = 0;
    money = (int) diffStartMoney[difficulty];
    lives = diffStartLives[difficulty];
    frameCounter = 0;
    gameOver = false;
    gameWon = false;
    showPreview = false;
    selectedTowerType = 0;

    buildPaths();
    setupWaves();
    screen = SCREEN_GAME;
  }

  // ---------------- DUAL PATH SYSTEM ---------------- //

  void buildPaths() {
    pathUpper.clear();
    pathLower.clear();
    allPathTiles.clear();

    // Upper lane: row 2 left to right
    for (int c = 0; c < COLS; c++) {
      pathUpper.add(new GridLocation(2, c));
    }

    // Lower lane: row 6 left to right, then row 7 right to left
    for (int c = 0; c < COLS; c++) {
      pathLower.add(new GridLocation(6, c));
    }
    for (int c = COLS - 1; c >= 0; c--) {
      pathLower.add(new GridLocation(7, c));
    }

    // Combine for checking if a tile is any path
    allPathTiles.addAll(pathUpper);
    allPathTiles.addAll(pathLower);
  }

  boolean isPathTile(int r, int c) {
    for (GridLocation loc : allPathTiles) {
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
    float hs = diffHpScale[difficulty];
    float ss = diffSpeedScale[difficulty];
    int total = diffWaveCount[difficulty];

    // Generate waves dynamically based on difficulty
    int[] enemyCounts = new int[total];
    float[] hpScalars = new float[total];
    float[] spScalars = new float[total];
    int[] delays = new int[total];

    for (int w = 0; w < total; w++) {
      float prog = (float) w / (total - 1);
      // EXPONENTIAL scaling - early waves easy, late waves brutal
      float expFactor = (float) Math.pow(prog * 2.0 + 0.3, 2.8);
      
      enemyCounts[w] = (int)(5 + expFactor * (total + 8));
      delays[w] = Math.max(20, 80 - (int)(prog * 60));
      
      // HP scales exponentially - final wave ~10x base HP
      hpScalars[w] = hs * (0.8f + expFactor * 3.5f);
      // Speed scales exponentially - final wave ~2.5x speed
      spScalars[w] = ss * (0.9f + expFactor * 0.7f);

      // Boss waves at wave 5, 10, 15 - EXTREME
      if (w > 0 && w % 5 == 0) {
        enemyCounts[w] = Math.max(4, enemyCounts[w] / 3);
        hpScalars[w] *= 3.5f;   // Boss hp is insane
        spScalars[w] *= 0.55f;  // But bosses move slower
      }
    }

    for (int w = 0; w < total; w++) {
      waves.add(new Wave(w + 1, enemyCounts[w], delays[w], hpScalars[w], spScalars[w]));
    }
  }

  // ---------------- LOOP ---------------- //

  public void draw() {
    frameCounter++;

    if (screen == SCREEN_START) {
      drawStartScreen();
      return;
    }

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

  // ---------------- START SCREEN ---------------- //

  void drawStartScreen() {
    background(startBgColor);
    fill(grassColor);
    noStroke();
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        fill((r + c) % 2 == 0 ? grassColor : grassAltColor);
        rect(c * tileW, r * tileH, tileW, tileH);
      }
    }

    // Dark overlay
    fill(0, 0, 0, 180);
    noStroke();
    rect(0, 0, APP_WIDTH, APP_HEIGHT);

    // Title
    fill(255, 220, 0);
    textSize(52);
    textAlign(CENTER, CENTER);
    text("TOWER DEFENSE", APP_WIDTH / 2, APP_HEIGHT / 4 - 20);

    fill(180);
    textSize(18);
    text("Choose Your Difficulty", APP_WIDTH / 2, APP_HEIGHT / 4 + 40);

    // Draw difficulty buttons
    int btnStartY = APP_HEIGHT / 2 - 60;
    drawDiffButton(btnStartY, "EASY", "Start: $225  Lives: 30  Slower enemies", color(60, 200, 60), 0);
    drawDiffButton(btnStartY + 70, "MEDIUM", "Start: $150  Lives: 25  Balanced", color(200, 180, 40), 1);
    drawDiffButton(btnStartY + 140, "HARD", "Start: $90  Lives: 18  Faster/tougher enemies", color(220, 60, 60), 2);

    // Footer
    fill(140);
    textSize(12);
    text("Build towers to stop enemies from reaching the end!  4 tower types available.", APP_WIDTH / 2, APP_HEIGHT - 30);
  }

  void drawDiffButton(int y, String label, String detail, int col, int diff) {
    boolean hovered = mouseX >= startBtnX && mouseX <= startBtnX + startBtnW &&
                      mouseY >= y && mouseY <= y + startBtnH;

    fill(hovered ? lerpColor(col, color(255), 0.3f) : col);
    stroke(hovered ? 255 : lerpColor(col, color(0), 0.3f));
    strokeWeight(2);
    rect(startBtnX, y, startBtnW, startBtnH, 8);

    fill(255);
    textSize(22);
    textAlign(CENTER, CENTER);
    noStroke();
    text(label, APP_WIDTH / 2, y + 20);

    textSize(11);
    fill(220);
    text(detail, APP_WIDTH / 2, y + 41);
  }

  // ---------------- GRID ---------------- //

  void drawGrid() {
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        int x = c * tileW, y = r * tileH;
        if (isPathTile(r, c)) {
          fill(pathColor); stroke(gridOutlineColor); strokeWeight(1);
          rect(x, y, tileW, tileH);
          stroke(120, 100, 60); strokeWeight(1);
          for (int bx = x; bx < x + tileW; bx += tileW / 3) line(bx, y, bx, y + tileH);
          line(x, y + tileH / 2, x + tileW, y + tileH / 2);
        } else {
          // Highlight the middle "no man's land" row 4-5 area
          if (r >= 3 && r <= 5 && !isPathTile(r, c)) {
            fill(35, 105, 35);
          } else {
            fill((r + c) % 2 == 0 ? grassColor : grassAltColor);
          }
          stroke(gridOutlineColor); strokeWeight(1);
          rect(x, y, tileW, tileH);
        }
      }
    }
    noFill(); stroke(20, 60, 20); strokeWeight(3);
    rect(0, 0, APP_WIDTH, APP_HEIGHT);
  }

  int indexInPath(ArrayList<GridLocation> path, int r, int c) {
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
    boolean isUpperPath; // true = upper, false = lower

    Enemy() {
      // Randomly assign to upper or lower path
      isUpperPath = random(1) < 0.5f;
      ArrayList<GridLocation> chosenPath = isUpperPath ? pathUpper : pathLower;
      GridLocation start = chosenPath.get(0);
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

      ArrayList<GridLocation> chosenPath = e.isUpperPath ? pathUpper : pathLower;
      if (e.targetPathIndex >= chosenPath.size()) { enemies.remove(i); lives--; continue; }

      GridLocation targetLoc = chosenPath.get(e.targetPathIndex);
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
    int type, range, damage, cooldown, maxCooldown, cost;

    Tower(GridLocation loc, int type) {
      this.loc = loc; this.type = type;
      if (type == TYPE_BASIC)      { range = 2; damage = 1; maxCooldown = 10; cost = 75; }
      else if (type == TYPE_SPLASH) { range = 1; damage = 1; maxCooldown = 25; cost = 120; }
      else if (type == TYPE_SNIPER) { range = 4; damage = 3; maxCooldown = 30; cost = 90; }
      else                          { range = 0; damage = 0; maxCooldown = 120; cost = 150; }
      cooldown = 0;
    }
  }

  void updateTowers() {
    for (Tower t : towers) {
      if (t.type == TYPE_GENERATOR) continue;
      if (t.cooldown > 0) { t.cooldown--; continue; }
      if (t.type == TYPE_SPLASH) {
        // Splash fires a bouncing projectile at nearest enemy
        Enemy target = findBestTarget(t);
        if (target != null) {
          float baseDist = distTowerToEnemy(t, target);
          if (baseDist <= t.range) {
            t.cooldown = t.maxCooldown;
            Projectile pj = new Projectile(t, target);
            pj.bouncesLeft = 3; // Will bounce up to 3 times
            projectiles.add(pj);
          }
        }
      } else {
        Enemy target = findBestTarget(t);
        if (target != null) {
          target.hp -= t.damage;
          t.cooldown = t.maxCooldown;
          projectiles.add(new Projectile(t, target));
          if (target.hp <= 0) { target.dead = true; addMoney(target.reward); }
        }
      }
    }
  }

  // Bank: each banker gives +1% to all money gains
  int countBankers() {
    int count = 0;
    for (Tower t : towers) if (t.type == TYPE_GENERATOR) count++;
    return count;
  }

  float moneyMultiplier() {
    return 1.0f + countBankers() * 0.01f;
  }

  void addMoney(int base) {
    money += (int)(base * moneyMultiplier());
  }

  void handleGenerators() {
    for (Tower t : towers) {
      if (t.type == TYPE_GENERATOR) {
        if (t.cooldown > 0) t.cooldown--;
        if (t.cooldown <= 0) { addMoney(5); t.cooldown = t.maxCooldown; }
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
    int bouncesLeft = 0;

    Projectile(Tower t, Enemy e) {
      this.type = t.type;
      this.x = t.loc.getCol() * tileW + tileW / 2f;
      this.y = t.loc.getRow() * tileH + tileH / 2f;
      if (e != null) { this.targetX = e.px; this.targetY = e.py; }
      else { this.targetX = x; this.targetY = y; alive = false; }
      if (type == TYPE_SNIPER) speed = 12;
      if (type == TYPE_SPLASH) speed = 5;
    }

    boolean update() {
      if (!alive) return false;
      float dx = targetX - x, dy = targetY - y;
      float dist = sqrt(dx * dx + dy * dy);
      if (dist < speed) {
        if (type == TYPE_SPLASH && bouncesLeft > 0) {
          // Bounce: deal damage to current target and find another
          Enemy hit = findNearestEnemy(x, y, null);
          if (hit != null) {
            hit.hp--;
            if (hit.hp <= 0) { hit.dead = true; addMoney(hit.reward); }
            // Find new bounce target
            Enemy next = findNearestEnemy(x, y, hit);
            if (next != null) {
              this.targetX = next.px;
              this.targetY = next.py;
              bouncesLeft--;
              return true;
            }
          }
        }
        alive = false; return false;
      }
      x += dx / dist * speed; y += dy / dist * speed;
      return true;
    }
  }

  Enemy findNearestEnemy(float sx, float sy, Enemy exclude) {
    Enemy best = null;
    float bestDist = 9999;
    for (Enemy e : enemies) {
      if (e == exclude || e.dead) continue;
      float d = sqrt(sq(e.px - sx) + sq(e.py - sy));
      if (d < bestDist) { bestDist = d; best = e; }
    }
    return best;
  }

  void drawProjectiles() {
    for (int i = projectiles.size() - 1; i >= 0; i--) {
      Projectile pj = projectiles.get(i);
      if (!pj.update()) { projectiles.remove(i); continue; }
      if (pj.type == TYPE_SNIPER) {
        fill(255, 80, 80); noStroke(); ellipse(pj.x, pj.y, 4, 10);
      } else if (pj.type == TYPE_SPLASH) {
        fill(100, 255, 100, 150); noStroke(); ellipse(pj.x, pj.y, 14, 14);
        // Bounce trail
        fill(100, 255, 100, 60); ellipse(pj.x, pj.y, 20, 20);
      } else {
        fill(255, 255, 100); noStroke(); ellipse(pj.x, pj.y, 7, 7);
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
    fill(40, 150, 40); noStroke();
    rect(cx - tileW / 6, cy - tileH / 5, tileW / 3, tileH / 4, 3);
    float pulse = sin(frameCounter * 0.1f) * 3 + 7;
    fill(100, 255, 100, 100); ellipse(cx, cy, pulse, pulse);
  }

  void drawTowerSniper(int tx, int ty, int cx, int cy, Tower t) {
    fill(60, 60, 80); stroke(150, 150, 200); strokeWeight(2);
    rect(tx + 2, ty + 8, tileW - 4, tileH - 16, 2);
    fill(200, 60, 60); stroke(255, 100, 100); strokeWeight(1);
    ellipse(cx, cy, tileW * 0.3f, tileH * 0.3f);
    float ang = frameCounter * 0.02f + t.loc.hashCode() * 0.3f;
    int bx = (int)(cx + cos(ang) * tileW * 0.7f), by = (int)(cy + sin(ang) * tileW * 0.7f);
    stroke(180, 60, 60); strokeWeight(4); line(cx, cy, bx, by);
    fill(255); noStroke(); ellipse(cx, cy, 6, 6);
    stroke(255, 100, 100); strokeWeight(1); noFill(); ellipse(cx, cy, tileW, tileH * 0.6f);
  }

  void drawTowerGenerator(int tx, int ty, int cx, int cy, Tower t) {
    fill(80, 70, 30); stroke(200, 180, 80); strokeWeight(2);
    rect(tx + 4, ty + 4, tileW - 8, tileH - 8, 4);
    fill(255, 220, 0); stroke(255, 240, 100); strokeWeight(1);
    ellipse(cx, cy, tileW * 0.4f, tileH * 0.4f);
    fill(40, 40, 0); noStroke();
    textSize(tileW * 0.35f); textAlign(CENTER, CENTER); text("$", cx, cy + 1);
    float spark = sin(frameCounter * 0.12f);
    fill(255, 255, 100, 100 + (int)(spark * 80)); ellipse(cx, cy - tileH * 0.25f, 8, 8);
  }

  // ---------------- PREVIEW ---------------- //

  void handleHover() {
    if (gameOver || gameWon || mouseY < 54 || mouseY > APP_HEIGHT - 35) { showPreview = false; return; }
    previewCol = mouseX / tileW; previewRow = mouseY / tileH;
    previewValid = canPlaceTower(previewRow, previewCol) && money >= towerCosts[selectedTowerType];
    showPreview = true;
  }

  void drawPlacementPreview() {
    if (!showPreview || gameOver || gameWon) return;
    int px = previewCol * tileW, py = previewRow * tileH;
    fill(previewValid ? previewValidColor : previewInvalidColor);
    noStroke(); rect(px, py, tileW, tileH);
    if (previewValid && getPreviewRange() > 0) {
      int cx = px + tileW / 2, cy = py + tileH / 2;
      int rangePx = getPreviewRange() * Math.min(tileW, tileH) / 2;
      noFill(); stroke(255, 255, 255, 50); strokeWeight(1);
      ellipse(cx, cy, rangePx * 2, rangePx * 2);
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
    noStroke(); fill(hudBgColor); rect(0, 0, APP_WIDTH, 54);
    textSize(14); textAlign(LEFT, CENTER);
    fill(255, 60, 60); text("♥ " + lives, 15, 20);
    fill(255, 220, 0); text("$" + money, 100, 20);
    fill(100, 200, 255); text("Wave " + (currentWave + 1) + "/" + waves.size(), 195, 20);
    if (currentWave < waves.size()) {
      Wave w = waves.get(currentWave);
      fill(255, 150, 150);
      text("Enemies: " + enemies.size() + " (" + w.spawned + "/" + w.totalEnemies + ")", 370, 20);
    }
    textSize(12);
    drawTowerButton(480, 30, TYPE_BASIC, "1: Gunner $75");
    drawTowerButton(610, 30, TYPE_SPLASH, "2: Splash $120");
    drawTowerButton(740, 30, TYPE_SNIPER, "3: Sniper $90");
    drawTowerButton(870, 30, TYPE_GENERATOR, "4: Bank $150");

    fill(hudBgColor); rect(0, APP_HEIGHT - 35, APP_WIDTH, 35);
    textSize(12); textAlign(CENTER, CENTER); fill(180);
    String tn = selectedTowerType == TYPE_BASIC ? "Gunner" :
                selectedTowerType == TYPE_SPLASH ? "Splash" :
                selectedTowerType == TYPE_SNIPER ? "Sniper" : "Bank";
    if (!gameOver && !gameWon)
      text("Press 1-4 to select tower  |  Selected: " + tn + " ($" + towerCosts[selectedTowerType] + ")  |  Click green tile", APP_WIDTH / 2, APP_HEIGHT - 17);
    else text("Click PLAY AGAIN to restart", APP_WIDTH / 2, APP_HEIGHT - 17);
  }

  void drawTowerButton(int bx, int by, int type, String label) {
    boolean sel = selectedTowerType == type;
    boolean canAfford = money >= towerCosts[type];
    fill(sel ? color(255, 255, 255, 200) : canAfford ? color(80, 80, 120, 200) : color(60, 20, 20, 200));
    stroke(sel ? color(255, 255, 0) : color(100)); strokeWeight(sel ? 2 : 1);
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
    // Start screen difficulty selection
    if (screen == SCREEN_START) {
      int yStart = APP_HEIGHT / 2 - 60;
      for (int d = 0; d < 3; d++) {
        int by = yStart + d * 70;
        if (mouseX >= startBtnX && mouseX <= startBtnX + startBtnW && mouseY >= by && mouseY <= by + startBtnH) {
          difficulty = d;
          initGameData();
          return;
        }
      }
      return;
    }

    if ((gameOver || gameWon) && isMouseOverReplay()) { initGameData(); loop(); return; }
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