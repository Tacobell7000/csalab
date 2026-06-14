import processing.core.PApplet;
import processing.core.PImage;
import java.util.ArrayList;

public class Game extends PApplet {

  public static final int APP_WIDTH = 960;
  public static final int APP_HEIGHT = 640;

  PApplet p;

  static final int ROWS = 10;
  static final int COLS = 10;

  final int SCREEN_START = 0, SCREEN_GAME = 1;
  int screen = SCREEN_START;

  int difficulty = 1;
  float[] diffStartMoney = {250, 180, 100};
  int[] diffStartLives = {30, 25, 18};
  float[] diffHpScale = {0.7f, 1.0f, 1.4f};
  float[] diffSpeedScale = {0.85f, 1.0f, 1.2f};
  int[] diffWaveCount = {12, 15, 18};

  int mapType = 0;
  String[] mapNames = {"Dual Lane", "Loop", "S-Shape"};

  ArrayList<ArrayList<GridLocation>> enemyPaths;
  ArrayList<GridLocation> allPathTiles;

  ArrayList<Enemy> enemies;
  ArrayList<Tower> towers;
  ArrayList<Projectile> projectiles;
  ArrayList<Wave> waves;
  int currentWave = 0;

  int money = 180;
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
  int previewValidColor, previewInvalidColor, hudBgColor, startBgColor;
  int tileW, tileH;

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
    enemyPaths = new ArrayList<>();
    initGameData();
    System.out.println("Tower Defense Initialized - Map: " + COLS + "x" + ROWS);
  }

  void initGameData() {
    enemies = new ArrayList<>();
    towers = new ArrayList<>();
    projectiles = new ArrayList<>();
    waves = new ArrayList<>();
    allPathTiles.clear();
    enemyPaths.clear();
    currentWave = 0;
    money = (int) diffStartMoney[difficulty];
    lives = diffStartLives[difficulty];
    frameCounter = 0;
    gameOver = false; gameWon = false;
    showPreview = false;
    selectedTowerType = 0;
    buildPaths();
    setupWaves();
  }

  void startGame() {
    initGameData();
    screen = SCREEN_GAME;
  }

  // ---------------- MAP SYSTEM ---------------- //

  void buildPaths() {
    enemyPaths.clear();
    allPathTiles.clear();
    if (mapType == 0) {
      ArrayList<GridLocation> upper = new ArrayList<>();
      ArrayList<GridLocation> lower = new ArrayList<>();
      for (int c = 0; c < COLS; c++) upper.add(new GridLocation(2, c));
      for (int c = 0; c < COLS; c++) lower.add(new GridLocation(6, c));
      for (int c = COLS - 1; c >= 0; c--) lower.add(new GridLocation(7, c));
      enemyPaths.add(upper); enemyPaths.add(lower);
    } else if (mapType == 1) {
      ArrayList<GridLocation> loop = new ArrayList<>();
      for (int c = 0; c < COLS; c++) loop.add(new GridLocation(1, c));
      for (int r = 2; r < ROWS - 1; r++) loop.add(new GridLocation(r, COLS - 1));
      for (int c = COLS - 2; c >= 0; c--) loop.add(new GridLocation(ROWS - 2, c));
      for (int r = ROWS - 3; r >= 2; r--) loop.add(new GridLocation(r, 0));
      enemyPaths.add(loop);
    } else {
      ArrayList<GridLocation> snake = new ArrayList<>();
      for (int r = 1; r < ROWS - 1; r++) {
        if (r % 2 == 1) for (int c = 0; c < COLS; c++) snake.add(new GridLocation(r, c));
        else for (int c = COLS - 1; c >= 0; c--) snake.add(new GridLocation(r, c));
      }
      enemyPaths.add(snake);
    }
    for (ArrayList<GridLocation> path : enemyPaths) allPathTiles.addAll(path);
  }

  boolean isPathTile(int r, int c) {
    for (GridLocation loc : allPathTiles) if (loc.getRow() == r && loc.getCol() == c) return true;
    return false;
  }

  boolean hasTower(int r, int c) {
    for (Tower t : towers) if (t.loc.getRow() == r && t.loc.getCol() == c) return true;
    return false;
  }

  boolean canPlaceTower(int r, int c) {
    if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return false;
    if (isPathTile(r, c)) return false;
    if (hasTower(r, c)) return false;
    return true;
  }

  Tower getTowerAt(int r, int c) {
    for (Tower t : towers) if (t.loc.getRow() == r && t.loc.getCol() == c) return t;
    return null;
  }

  // ---------------- WAVES ---------------- //

  class Wave {
    int waveNum, totalEnemies, spawned = 0, spawnDelay, timer = 0;
    float enemyHpMult, enemySpeedMult;
    Wave(int waveNum, int totalEnemies, int spawnDelay, float enemyHpMult, float enemySpeedMult) {
      this.waveNum = waveNum; this.totalEnemies = totalEnemies;
      this.spawnDelay = spawnDelay; this.enemyHpMult = enemyHpMult; this.enemySpeedMult = enemySpeedMult;
    }
    boolean update() { timer++; if (spawned < totalEnemies && timer >= spawnDelay) { timer = 0; spawned++; return true; } return false; }
    boolean isDone() { return spawned >= totalEnemies; }
  }

  void setupWaves() {
    float hs = diffHpScale[difficulty], ss = diffSpeedScale[difficulty];
    int total = diffWaveCount[difficulty];
    for (int w = 0; w < total; w++) {
      float prog = (float) w / (total - 1);
      float expFactor = (float) Math.pow(prog * 2.0 + 0.3, 2.8);
      int cnt = (int)(5 + expFactor * (total + 8));
      int del = Math.max(20, 80 - (int)(prog * 60));
      float hp = hs * (0.8f + expFactor * 3.5f);
      float sp = ss * (0.9f + expFactor * 0.7f);
      if (w > 0 && w % 5 == 0) { cnt = Math.max(4, cnt / 3); hp *= 3.5f; sp *= 0.55f; }
      waves.add(new Wave(w + 1, cnt, del, hp, sp));
    }
  }

  // ---------------- LOOP ---------------- //

  public void draw() {
    frameCounter++;
    if (screen == SCREEN_START) { drawStartScreen(); return; }
    background(grassColor);
    drawGrid();
    if (!gameOver && !gameWon) {
      handleGenerators(); spawnEnemies(); updateEnemies(); updateTowers(); handleHover();
    }
    drawPlacementPreview(); drawTowers(); drawEnemies(); drawProjectiles();
    drawHUD(); checkGameState();
  }

  // ---------------- START SCREEN ---------------- //

  void drawStartScreen() {
    // Sky gradient
    for (int y = 0; y < APP_HEIGHT; y++) {
      fill(lerpColor(color(10, 20, 40), color(30, 60, 100), (float) y / APP_HEIGHT));
      noStroke(); rect(0, y, APP_WIDTH, 1);
    }
    // Twinkling stars
    for (int i = 0; i < 40; i++) {
      float sx = (i * 173 + frameCounter * 0.02f) % APP_WIDTH;
      float sy = (i * 97 + sin(frameCounter * 0.03f + i) * 10) % (APP_HEIGHT * 0.65f);
      float tw = sin(frameCounter * 0.05f + i * 1.7f) * 0.5f + 0.5f;
      fill(255, 255, 255, 100 + (int)(tw * 155));
      ellipse(sx, sy, 2 + tw * 3, 2 + tw * 3);
    }
    // Ground
    for (int r = 6; r < ROWS; r++)
      for (int c = 0; c < COLS; c++) {
        fill((r + c) % 2 == 0 ? grassColor : grassAltColor);
        rect(c * tileW, r * tileH, tileW, tileH);
      }
    // Marching silhouettes
    fill(0, 0, 0, 120); noStroke();
    for (int i = 0; i < 6; i++) {
      float ex = (frameCounter * 0.6f + i * 180) % (APP_WIDTH + 100) - 50;
      float ey = APP_HEIGHT * 0.78f + sin(frameCounter * 0.04f + i) * 8;
      ellipse(ex, ey, 24, 24);
    }
    // Panel
    fill(10, 15, 35, 230); noStroke(); rect(80, 40, APP_WIDTH - 160, APP_HEIGHT - 80, 16);
    stroke(255, 220, 0, 80); strokeWeight(2); noFill();
    rect(80, 40, APP_WIDTH - 160, APP_HEIGHT - 80, 16);
    // Title
    fill(255, 220, 0); textSize(44); textAlign(CENTER, CENTER);
    text("TOWER DEFENSE", APP_WIDTH / 2, 85);
    fill(180, 200, 220); textSize(13);
    text("Build towers. Defend your base. Survive the waves.", APP_WIDTH / 2, 120);
    // Left: Difficulty
    int leftX = 120, colW = 320;
    fill(160, 200, 240); textSize(14); textAlign(LEFT, CENTER);
    text("> DIFFICULTY", leftX, 160);
    drawDiffBtn(leftX, 185, colW, "EASY", "$250 | 30 Lives | Slower", color(50, 140, 50), 0);
    drawDiffBtn(leftX, 235, colW, "MEDIUM", "$180 | 25 Lives | Balanced", color(180, 150, 30), 1);
    drawDiffBtn(leftX, 285, colW, "HARD", "$100 | 18 Lives | Tougher", color(180, 40, 40), 2);
    // Right: Map
    int rightX = APP_WIDTH / 2 + 40;
    fill(160, 200, 240); textSize(14); textAlign(LEFT, CENTER);
    text("> MAP SELECTION", rightX, 160);
    drawMapBtn(rightX, 185, colW, "DUAL LANE", "Two paths | Split defense", color(30, 100, 200), 0);
    drawMapBtn(rightX, 235, colW, "LOOP", "Perimeter | Central base", color(180, 130, 30), 1);
    drawMapBtn(rightX, 285, colW, "S-SHAPE", "Long snake | Endless", color(130, 40, 200), 2);
    // Tower previews
    fill(160, 200, 240); textSize(14); textAlign(CENTER, CENTER);
    text("> TOWER TYPES", APP_WIDTH / 2, 355);
    int[] pt = {TYPE_BASIC, TYPE_SPLASH, TYPE_SNIPER, TYPE_GENERATOR};
    String[] pn = {"Gunner", "Splash", "Sniper", "Bank"};
    int[] pc = {75, 120, 90, 150};
    for (int pi = 0; pi < 4; pi++) {
      int px = APP_WIDTH / 2 - 220 + pi * 115, py = 370;
      drawMiniTower(px + 40, py, pt[pi]);
      fill(200, 220, 240); textSize(11); textAlign(CENTER); text(pn[pi], px + 40, py + 38);
      fill(160, 180, 200); textSize(10); text("$" + pc[pi], px + 40, py + 52);
    }
    // START button
    int sby = 448, bw = 300, bh = 55, bx = APP_WIDTH / 2 - bw / 2;
    boolean hs = mouseX >= bx && mouseX <= bx + bw && mouseY >= sby && mouseY <= sby + bh;
    fill(hs ? color(80, 255, 80, 60) : color(50, 200, 50, 30)); noStroke();
    rect(bx - 4, sby - 4, bw + 8, bh + 8, 12);
    fill(hs ? color(60, 200, 60) : color(40, 160, 40));
    stroke(hs ? color(180, 255, 180) : color(80, 200, 80)); strokeWeight(2);
    rect(bx, sby, bw, bh, 8);
    fill(255); textSize(22); textAlign(CENTER, CENTER); noStroke();
    text(">> START GAME <<", APP_WIDTH / 2, sby + bh / 2);
    fill(140, 160, 180); textSize(10);
    text("Press 1-4 in-game to switch towers  |  Click tower to upgrade (max lvl 3)", APP_WIDTH / 2, APP_HEIGHT - 25);
  }

  void drawMiniTower(int cx, int cy, int type) {
    int sz = 12;
    if (type == TYPE_BASIC) { fill(0, 80, 180); stroke(120, 180, 240); strokeWeight(1); rect(cx - sz, cy - sz, sz * 2, sz * 2, 3); fill(255); noStroke(); ellipse(cx, cy, 5, 5); }
    else if (type == TYPE_SPLASH) { fill(40, 150, 40); stroke(100, 220, 100); strokeWeight(1); rect(cx - sz, cy - sz, sz * 2, sz * 2, 4); fill(255); noStroke(); ellipse(cx, cy, 5, 5); }
    else if (type == TYPE_SNIPER) { fill(180, 50, 50); stroke(240, 100, 100); strokeWeight(1); rect(cx - sz + 2, cy - sz + 4, (sz - 1) * 2, (sz - 2) * 2, 2); fill(255); noStroke(); ellipse(cx, cy, 5, 5); }
    else { fill(200, 180, 20); stroke(240, 220, 80); strokeWeight(1); rect(cx - sz, cy - sz, sz * 2, sz * 2, 3); fill(60, 50, 10); noStroke(); textSize(11); textAlign(CENTER, CENTER); text("$", cx, cy); }
  }

  void drawDiffBtn(int bx, int by, int bw, String label, String detail, int col, int diff) {
    boolean hv = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + 42;
    boolean sel = difficulty == diff;
    fill(sel ? lerpColor(col, color(255), 0.35f) : hv ? lerpColor(col, color(255), 0.15f) : col);
    stroke(sel ? color(255, 255, 100) : hv ? color(200, 200, 255) : lerpColor(col, color(0), 0.4f));
    strokeWeight(sel ? 3 : 1); rect(bx, by, bw, 42, 6);
    fill(255); textSize(13); textAlign(LEFT, CENTER); noStroke(); text(label, bx + 12, by + 21);
    fill(200, 200, 220, 150); textSize(9); textAlign(RIGHT, CENTER); text(detail, bx + bw - 10, by + 21);
  }

  void drawMapBtn(int bx, int by, int bw, String label, String detail, int col, int m) {
    boolean hv = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + 42;
    boolean sel = mapType == m;
    fill(sel ? lerpColor(col, color(255), 0.35f) : hv ? lerpColor(col, color(255), 0.15f) : col);
    stroke(sel ? color(255, 255, 100) : hv ? color(200, 200, 255) : lerpColor(col, color(0), 0.4f));
    strokeWeight(sel ? 3 : 1); rect(bx, by, bw, 42, 6);
    fill(255); textSize(13); textAlign(LEFT, CENTER); noStroke(); text(label, bx + 12, by + 21);
    fill(200, 200, 220, 150); textSize(9); textAlign(RIGHT, CENTER); text(detail, bx + bw - 10, by + 21);
  }

  // ---------------- GRID ---------------- //

  void drawGrid() {
    for (int r = 0; r < ROWS; r++) {
      for (int c = 0; c < COLS; c++) {
        int x = c * tileW, y = r * tileH;
        if (isPathTile(r, c)) {
          fill(pathColor); stroke(gridOutlineColor); strokeWeight(1); rect(x, y, tileW, tileH);
          stroke(120, 100, 60); strokeWeight(1);
          for (int bx = x; bx < x + tileW; bx += tileW / 3) line(bx, y, bx, y + tileH);
          line(x, y + tileH / 2, x + tileW, y + tileH / 2);
        } else {
          fill((r + c) % 2 == 0 ? grassColor : grassAltColor);
          stroke(gridOutlineColor); strokeWeight(1); rect(x, y, tileW, tileH);
        }
      }
    }
    noFill(); stroke(20, 60, 20); strokeWeight(3); rect(0, 0, APP_WIDTH, APP_HEIGHT);
  }

  // ---------------- ENEMY ---------------- //

  final int ENEMY_NORMAL = 0, ENEMY_FAST = 1, ENEMY_TANK = 2, ENEMY_SHIELDED = 3;

  class Enemy {
    int hp = 5, maxHp = 5; boolean dead = false; int enemyType = ENEMY_NORMAL;
    boolean shieldActive = false; float px, py; int targetPathIndex = 1;
    int reward = 8; boolean isBoss = false; int pathIndex; float speedMult = 1.0f;

    Enemy(int type, int pathIdx) {
      this.enemyType = type; this.pathIndex = pathIdx;
      GridLocation start = enemyPaths.get(pathIdx).get(0);
      px = start.getCol() * tileW + tileW / 2f; py = start.getRow() * tileH + tileH / 2f;
      if (type == ENEMY_FAST) { hp = 3; speedMult = 2.2f; reward = 3; }
      else if (type == ENEMY_TANK) { hp = 20; speedMult = 0.55f; reward = 10; }
      else if (type == ENEMY_SHIELDED) { hp = 10; shieldActive = true; speedMult = 0.85f; reward = 7; }
      else { hp = 6; speedMult = 1.0f; reward = 5; }
      maxHp = hp;
    }
  }

  void spawnEnemies() {
    if (currentWave >= waves.size()) return;
    Wave w = waves.get(currentWave);
    if (w.update()) {
      int type = ENEMY_NORMAL; float roll = random(1);
      if (w.waveNum >= 3) {
        if (roll < 0.15f) type = ENEMY_FAST;
        else if (roll < 0.30f) type = ENEMY_TANK;
        else if (roll < 0.40f && w.waveNum >= 5) type = ENEMY_SHIELDED;
      }
      int pathIdx = (int) random(enemyPaths.size());
      Enemy e = new Enemy(type, pathIdx);
      e.hp = (int) Math.ceil(e.hp * w.enemyHpMult); e.maxHp = e.hp;
      e.reward = (int)(e.reward + (e.hp - 3) * 1.5f);
      e.isBoss = w.enemyHpMult >= 3.0f;
      enemies.add(e);
    }
    if (w.isDone() && enemies.isEmpty()) { currentWave++; if (currentWave < waves.size()) addMoney(10 + currentWave * 3); }
  }

  void updateEnemies() {
    float baseSpeed = 1.5f;
    float speedMult = (currentWave < waves.size()) ? waves.get(currentWave).enemySpeedMult : 1.0f;
    for (int i = enemies.size() - 1; i >= 0; i--) {
      Enemy e = enemies.get(i); if (e.dead) { enemies.remove(i); continue; }
      ArrayList<GridLocation> chosenPath = enemyPaths.get(e.pathIndex);
      if (e.targetPathIndex >= chosenPath.size()) { enemies.remove(i); lives--; continue; }
      GridLocation targetLoc = chosenPath.get(e.targetPathIndex);
      float tx = targetLoc.getCol() * tileW + tileW / 2f, ty = targetLoc.getRow() * tileH + tileH / 2f;
      float dx = tx - e.px, dy = ty - e.py, dist = sqrt(dx * dx + dy * dy);
      float speed = baseSpeed * speedMult * e.speedMult;
      if (dist < speed) { e.px = tx; e.py = ty; e.targetPathIndex++; }
      else { e.px += dx / dist * speed; e.py += dy / dist * speed; }
    }
  }

  void drawEnemies() {
    for (Enemy e : enemies) {
      float hpPct = (float) e.hp / e.maxHp; float sz = tileW * 0.5f;
      if (e.isBoss) {
        fill(lerpColor(color(180, 0, 255), color(255, 100, 50), 1 - hpPct)); stroke(120, 0, 200); strokeWeight(3);
        sz = tileW * 0.8f; ellipse(e.px, e.py, sz, sz * 0.75f);
        stroke(255, 200, 0); strokeWeight(2); float top = e.py - sz * 0.5f;
        line(e.px - 14, top + 8, e.px - 7, top); line(e.px - 7, top, e.px, top + 7); line(e.px + 7, top, e.px + 14, top + 8);
      } else if (e.enemyType == ENEMY_FAST) {
        fill(lerpColor(color(255, 140, 0), color(255, 255, 0), hpPct)); stroke(200, 100, 0); strokeWeight(2); sz = tileW * 0.4f;
        triangle(e.px, e.py - sz * 0.6f, e.px - sz * 0.35f, e.py + sz * 0.5f, e.px + sz * 0.35f, e.py + sz * 0.5f);
      } else if (e.enemyType == ENEMY_TANK) {
        fill(lerpColor(color(100, 100, 150), color(180, 180, 220), hpPct)); stroke(60, 60, 120); strokeWeight(3); sz = tileW * 0.65f;
        rectMode(CENTER); rect(e.px, e.py, sz, sz, 6); rectMode(CORNER);
      } else if (e.enemyType == ENEMY_SHIELDED) {
        fill(lerpColor(color(0, 180, 220), color(0, 220, 255), hpPct)); stroke(0, 100, 180); strokeWeight(2);
        ellipse(e.px, e.py, sz, sz);
        if (e.shieldActive) { fill(0, 200, 255, 80); noStroke(); ellipse(e.px, e.py, sz * 1.3f, sz * 1.3f); }
      } else {
        fill(lerpColor(color(255, 40, 40), color(255, 180, 40), hpPct)); stroke(180, 20, 20); strokeWeight(2);
        ellipse(e.px, e.py, sz, sz);
      }
      fill(255); noStroke(); ellipse(e.px - 4, e.py - 2, 5, 5); ellipse(e.px + 4, e.py - 2, 5, 5);
      fill(0); ellipse(e.px - 4, e.py - 2, 2.5f, 2.5f); ellipse(e.px + 4, e.py - 2, 2.5f, 2.5f);
      int barW = e.isBoss ? 44 : 30, barH = 5;
      int barX = (int)(e.px - barW / 2f), barY = (int)(e.py - sz * 0.65f);
      fill(200, 0, 0); noStroke(); rect(barX, barY, barW, barH);
      fill(0, 200, 0); rect(barX, barY, barW * hpPct, barH);
      stroke(e.isBoss ? color(255, 200, 0) : e.enemyType == ENEMY_FAST ? color(255, 200, 0) : color(60));
      strokeWeight(1); noFill(); rect(barX, barY, barW, barH);
    }
  }

  // ---------------- TOWERS ---------------- //

  final int TYPE_BASIC = 0, TYPE_SPLASH = 1, TYPE_SNIPER = 2, TYPE_GENERATOR = 3;

  class Tower {
    GridLocation loc; int type, range, damage, cooldown, maxCooldown, cost; int level = 1;
    Tower(GridLocation loc, int type) {
      this.loc = loc; this.type = type;
      if (type == TYPE_BASIC) { range = 2; damage = 1; maxCooldown = 10; cost = 75; }
      else if (type == TYPE_SPLASH) { range = 1; damage = 1; maxCooldown = 25; cost = 120; }
      else if (type == TYPE_SNIPER) { range = 4; damage = 3; maxCooldown = 30; cost = 90; }
      else { range = 0; damage = 0; maxCooldown = 120; cost = 150; }
      cooldown = 0;
    }
    void upgrade() {
      if (level >= 3) return; level++;
      if (type == TYPE_BASIC) { damage += 1; range = Math.min(range + 1, 4); maxCooldown = Math.max(5, maxCooldown - 2); }
      else if (type == TYPE_SPLASH) { damage += 1; maxCooldown = Math.max(12, maxCooldown - 5); }
      else if (type == TYPE_SNIPER) { damage += 2; maxCooldown = Math.max(15, maxCooldown - 5); }
      else { maxCooldown = Math.max(60, maxCooldown - 30); }
    }
    int upgradeCost() { if (level >= 3) return 9999; return cost * (level + 1) / 2; }
  }

  void updateTowers() {
    for (Tower t : towers) {
      if (t.type == TYPE_GENERATOR) continue;
      if (t.cooldown > 0) { t.cooldown--; continue; }
      if (t.type == TYPE_SPLASH) {
        Enemy target = findBestTarget(t);
        if (target != null && distTowerToEnemy(t, target) <= t.range) {
          t.cooldown = t.maxCooldown; Projectile pj = new Projectile(t, target); pj.bouncesLeft = 3; projectiles.add(pj);
        }
      } else {
        Enemy target = findBestTarget(t);
        if (target != null) {
          if (target.shieldActive) { target.shieldActive = false; t.cooldown = t.maxCooldown; projectiles.add(new Projectile(t, target)); }
          else { target.hp -= t.damage; t.cooldown = t.maxCooldown; projectiles.add(new Projectile(t, target)); if (target.hp <= 0) { target.dead = true; addMoney(target.reward); } }
        }
      }
    }
  }

  int countBankers() { int c = 0; for (Tower t : towers) if (t.type == TYPE_GENERATOR) c++; return c; }
  float moneyMultiplier() { return 1.0f + countBankers() * 0.01f; }
  void addMoney(int base) { money += (int)(base * moneyMultiplier()); }

  void handleGenerators() {
    for (Tower t : towers) if (t.type == TYPE_GENERATOR) { if (t.cooldown > 0) t.cooldown--; if (t.cooldown <= 0) { addMoney(5); t.cooldown = t.maxCooldown; } }
  }

  float distTowerToEnemy(Tower t, Enemy e) {
    float cx = t.loc.getCol() * tileW + tileW / 2f, cy = t.loc.getRow() * tileH + tileH / 2f;
    return sqrt(sq(e.px - cx) + sq(e.py - cy)) / Math.min(tileW, tileH);
  }

  // ---------------- PROJECTILES ---------------- //

  class Projectile {
    float x, y, targetX, targetY, speed = 6; boolean alive = true; int type; int bouncesLeft = 0;
    Projectile(Tower t, Enemy e) {
      this.type = t.type; this.x = t.loc.getCol() * tileW + tileW / 2f; this.y = t.loc.getRow() * tileH + tileH / 2f;
      if (e != null) { this.targetX = e.px; this.targetY = e.py; } else { this.targetX = x; this.targetY = y; alive = false; }
      if (type == TYPE_SNIPER) speed = 12; if (type == TYPE_SPLASH) speed = 5;
    }
    boolean update() {
      if (!alive) return false;
      float dx = targetX - x, dy = targetY - y, dist = sqrt(dx * dx + dy * dy);
      if (dist < speed) {
        if (type == TYPE_SPLASH && bouncesLeft > 0) {
          Enemy hit = findNearestEnemy(x, y, null);
          if (hit != null) { hit.hp--; if (hit.hp <= 0) { hit.dead = true; addMoney(hit.reward); }
            Enemy next = findNearestEnemy(x, y, hit);
            if (next != null) { this.targetX = next.px; this.targetY = next.py; bouncesLeft--; return true; } }
        }
        alive = false; return false;
      }
      x += dx / dist * speed; y += dy / dist * speed; return true;
    }
  }

  Enemy findNearestEnemy(float sx, float sy, Enemy exclude) {
    Enemy best = null; float bestDist = 9999;
    for (Enemy e : enemies) { if (e == exclude || e.dead) continue; float d = sqrt(sq(e.px - sx) + sq(e.py - sy)); if (d < bestDist) { bestDist = d; best = e; } }
    return best;
  }

  void drawProjectiles() {
    for (int i = projectiles.size() - 1; i >= 0; i--) {
      Projectile pj = projectiles.get(i); if (!pj.update()) { projectiles.remove(i); continue; }
      if (pj.type == TYPE_SNIPER) { fill(255, 80, 80); noStroke(); ellipse(pj.x, pj.y, 4, 10); }
      else if (pj.type == TYPE_SPLASH) { fill(100, 255, 100, 150); noStroke(); ellipse(pj.x, pj.y, 14, 14); fill(100, 255, 100, 60); ellipse(pj.x, pj.y, 20, 20); }
      else { fill(255, 255, 100); noStroke(); ellipse(pj.x, pj.y, 7, 7); fill(255, 255, 200, 100); ellipse(pj.x, pj.y, 12, 12); }
    }
  }

  // ---------------- TOWER RENDERING ---------------- //

  void drawTowers() {
    for (Tower t : towers) {
      int tx = t.loc.getCol() * tileW, ty = t.loc.getRow() * tileH, cx = tx + tileW / 2, cy = ty + tileH / 2;
      if (t.type == TYPE_BASIC) drawTowerBasic(tx, ty, cx, cy, t);
      else if (t.type == TYPE_SPLASH) drawTowerSplash(tx, ty, cx, cy, t);
      else if (t.type == TYPE_SNIPER) drawTowerSniper(tx, ty, cx, cy, t);
      else drawTowerGenerator(tx, ty, cx, cy, t);
    }
  }

  void drawTowerBasic(int tx, int ty, int cx, int cy, Tower t) {
    int bc = t.level == 1 ? color(0, 100, 200) : t.level == 2 ? color(0, 130, 240) : color(0, 160, 255);
    fill(80, 80, 90); stroke(140, 140, 160); strokeWeight(2); rect(tx + 4, ty + 4, tileW - 8, tileH - 8, 4);
    fill(bc); stroke(160, 200, 255); strokeWeight(1); ellipse(cx, cy, tileW * 0.4f, tileH * 0.4f);
    for (int l = 0; l < t.level; l++) {
      float ang = frameCounter * 0.03f + t.loc.hashCode() * 0.5f + l * TWO_PI / 3;
      int bx = (int)(cx + cos(ang) * (tileW / 3 + l * 5)), by = (int)(cy + sin(ang) * (tileW / 3 + l * 5));
      stroke(180, 200, 255); strokeWeight(3 - l * 0.5f); line(cx, cy, bx, by);
    }
    fill(255); noStroke(); ellipse(cx, cy, 6, 6);
    if (t.level >= 2) { fill(255, 255, 0); textSize(10); textAlign(CENTER); text("*", cx, ty + tileH - 5); }
    if (t.level == 3) { fill(255, 255, 0); textSize(10); textAlign(CENTER); text("**", cx, ty + tileH - 5); }
  }

  void drawTowerSplash(int tx, int ty, int cx, int cy, Tower t) {
    int bc = t.level == 1 ? color(50, 200, 50) : t.level == 2 ? color(50, 230, 80) : color(50, 255, 100);
    fill(60, 90, 60); stroke(100, 200, 100); strokeWeight(2); rect(tx + 4, ty + 4, tileW - 8, tileH - 8, 8);
    fill(bc); stroke(150, 255, 150); strokeWeight(1); ellipse(cx, cy, tileW * 0.45f, tileH * 0.45f);
    fill(40, 150, 40); noStroke(); rect(cx - tileW / 6, cy - tileH / 5, tileW / 3, tileH / 4, 3);
    for (int l = 0; l < t.level; l++) { float pulse = sin(frameCounter * 0.1f + l) * (3 + l * 2) + (7 + l * 3); fill(100, 255, 100, 100 - l * 30); ellipse(cx, cy, pulse, pulse); }
    if (t.level >= 2) { fill(255, 255, 0); textSize(10); textAlign(CENTER); text("*", cx, ty + tileH - 5); }
    if (t.level == 3) { fill(255, 255, 0); textSize(10); textAlign(CENTER); text("**", cx, ty + tileH - 5); }
  }

  void drawTowerSniper(int tx, int ty, int cx, int cy, Tower t) {
    int bc = t.level == 1 ? color(200, 60, 60) : t.level == 2 ? color(240, 40, 40) : color(255, 20, 20);
    fill(60, 60, 80); stroke(150, 150, 200); strokeWeight(2); rect(tx + 2, ty + 8, tileW - 4, tileH - 16, 2);
    fill(bc); stroke(255, 100, 100); strokeWeight(1); ellipse(cx, cy, tileW * 0.3f, tileH * 0.3f);
    for (int l = 0; l < t.level; l++) {
      float ang = frameCounter * 0.02f + t.loc.hashCode() * 0.3f + l * 0.5f;
      int bx = (int)(cx + cos(ang) * tileW * 0.7f), by = (int)(cy + sin(ang) * tileW * 0.7f);
      stroke(180, 60, 60); strokeWeight(4 - l); line(cx, cy, bx, by);
    }
    fill(255); noStroke(); ellipse(cx, cy, 6, 6); stroke(255, 100, 100); strokeWeight(1); noFill(); ellipse(cx, cy, tileW, tileH * 0.6f);
    if (t.level >= 2) { fill(255, 255, 0); textSize(10); textAlign(CENTER); text("*", cx, ty + tileH - 5); }
    if (t.level == 3) { fill(255, 255, 0); textSize(10); textAlign(CENTER); text("**", cx, ty + tileH - 5); }
  }

  void drawTowerGenerator(int tx, int ty, int cx, int cy, Tower t) {
    int bc = t.level == 1 ? color(255, 220, 0) : t.level == 2 ? color(255, 240, 50) : color(255, 255, 100);
    fill(80, 70, 30); stroke(200, 180, 80); strokeWeight(2); rect(tx + 4, ty + 4, tileW - 8, tileH - 8, 4);
    fill(bc); stroke(255, 240, 100); strokeWeight(1); ellipse(cx, cy, tileW * 0.4f, tileH * 0.4f);
    fill(40, 40, 0); noStroke(); textSize(tileW * (0.3f + t.level * 0.05f)); textAlign(CENTER, CENTER); text("$", cx, cy + 1);
    float spark = sin(frameCounter * 0.12f); fill(255, 255, 100, 100 + (int)(spark * 80)); ellipse(cx, cy - tileH * 0.25f, 8, 8);
    if (t.level >= 2) { fill(255, 255, 0); textSize(10); textAlign(CENTER); text("*", cx, ty + tileH - 5); }
    if (t.level == 3) { fill(255, 255, 0); textSize(10); textAlign(CENTER); text("**", cx, ty + tileH - 5); }
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
    fill(previewValid ? previewValidColor : previewInvalidColor); noStroke(); rect(px, py, tileW, tileH);
    if (previewValid && getPreviewRange() > 0) {
      int cx = px + tileW / 2, cy = py + tileH / 2, rp = getPreviewRange() * Math.min(tileW, tileH) / 2;
      noFill(); stroke(255, 255, 255, 50); strokeWeight(1); ellipse(cx, cy, rp * 2, rp * 2);
    }
  }

  int getPreviewRange() { if (selectedTowerType == TYPE_GENERATOR) return 0; if (selectedTowerType == TYPE_SPLASH) return 1; if (selectedTowerType == TYPE_SNIPER) return 4; return 2; }

  // ---------------- TARGETING ---------------- //

  Enemy findBestTarget(Tower t) {
    Enemy best = null; float bestProgress = -1;
    for (Enemy e : enemies) { if (distTowerToEnemy(t, e) > t.range) continue; if (e.targetPathIndex > bestProgress) { bestProgress = e.targetPathIndex; best = e; } }
    return best;
  }

  // ---------------- HUD ---------------- //

  void drawHUD() {
    noStroke(); fill(hudBgColor); rect(0, 0, APP_WIDTH, 54);
    textSize(14); textAlign(LEFT, CENTER);
    fill(255, 60, 60); text("Lives: " + lives, 15, 20);
    fill(255, 220, 0); text("$" + money, 130, 20);
    fill(100, 200, 255); text("Wave " + (currentWave + 1) + "/" + waves.size(), 220, 20);
    if (currentWave < waves.size()) { Wave w = waves.get(currentWave); fill(255, 150, 150); text("Enemies: " + enemies.size() + " (" + w.spawned + "/" + w.totalEnemies + ")", 400, 20); }
    textSize(12);
    drawTowerButton(480, 30, TYPE_BASIC, "1: Gunner $75");
    drawTowerButton(610, 30, TYPE_SPLASH, "2: Splash $120");
    drawTowerButton(740, 30, TYPE_SNIPER, "3: Sniper $90");
    drawTowerButton(870, 30, TYPE_GENERATOR, "4: Bank $150");
    fill(hudBgColor); rect(0, APP_HEIGHT - 35, APP_WIDTH, 35);
    textSize(12); textAlign(CENTER, CENTER); fill(180);
    String tn = selectedTowerType == TYPE_BASIC ? "Gunner" : selectedTowerType == TYPE_SPLASH ? "Splash" : selectedTowerType == TYPE_SNIPER ? "Sniper" : "Bank";
    if (!gameOver && !gameWon) text("Press 1-4 for tower | " + tn + " ($" + towerCosts[selectedTowerType] + ") | Click tile to place, click tower to upgrade", APP_WIDTH / 2, APP_HEIGHT - 17);
    else text("Click PLAY AGAIN to restart", APP_WIDTH / 2, APP_HEIGHT - 17);
  }

  void drawTowerButton(int bx, int by, int type, String label) {
    boolean sel = selectedTowerType == type, can = money >= towerCosts[type];
    fill(sel ? color(255, 255, 255, 200) : can ? color(80, 80, 120, 200) : color(60, 20, 20, 200));
    stroke(sel ? color(255, 255, 0) : color(100)); strokeWeight(sel ? 2 : 1); rect(bx, by, 125, 20, 3);
    fill(can ? (sel ? 0 : 255) : 150); textAlign(LEFT, CENTER); noStroke();
    float tw = textWidth(label); text(label, bx + (125 - tw) / 2, by + 10);
  }

  // ---------------- REPLAY ---------------- //

  boolean isMouseOverReplay() { int bx = APP_WIDTH / 2 - 100, by = APP_HEIGHT / 2 + 50; return mouseX >= bx && mouseX <= bx + 200 && mouseY >= by && mouseY <= by + 50; }

  void drawReplayButton() {
    int bx = APP_WIDTH / 2 - 100, by = APP_HEIGHT / 2 + 50;
    fill(isMouseOverReplay() ? color(60, 180, 60) : color(40, 140, 40));
    stroke(100, 255, 100); strokeWeight(2); rect(bx, by, 200, 50, 8);
    fill(255); textSize(18); textAlign(CENTER, CENTER); noStroke(); text("PLAY AGAIN", APP_WIDTH / 2, by + 25);
  }

  // ---------------- INPUT ---------------- //

  public void keyPressed() {
    if (key == '1') selectedTowerType = TYPE_BASIC; else if (key == '2') selectedTowerType = TYPE_SPLASH;
    else if (key == '3') selectedTowerType = TYPE_SNIPER; else if (key == '4') selectedTowerType = TYPE_GENERATOR;
  }

  public void mouseClicked() {
    if (screen == SCREEN_START) {
      int diffX = 120, diffW = 320;
      for (int d = 0; d < 3; d++) { int by = 185 + d * 50; if (mouseX >= diffX && mouseX <= diffX + diffW && mouseY >= by && mouseY <= by + 42) { difficulty = d; return; } }
      int mapX = APP_WIDTH / 2 + 40, mapW = 320;
      for (int m = 0; m < 3; m++) { int by = 185 + m * 50; if (mouseX >= mapX && mouseX <= mapX + mapW && mouseY >= by && mouseY <= by + 42) { mapType = m; return; } }
      int sbX = APP_WIDTH / 2 - 150, sbY = 448, sbW = 300, sbH = 55;
      if (mouseX >= sbX && mouseX <= sbX + sbW && mouseY >= sbY && mouseY <= sbY + sbH) { startGame(); return; }
      return;
    }
    if ((gameOver || gameWon) && isMouseOverReplay()) { startGame(); loop(); return; }
    if (!gameOver && !gameWon) {
      int col = mouseX / tileW, row = mouseY / tileH;
      Tower existing = getTowerAt(row, col);
      if (existing != null) { if (existing.type == selectedTowerType && existing.level < 3) { int cost = existing.upgradeCost(); if (money >= cost) { money -= cost; existing.upgrade(); } } }
      else if (canPlaceTower(row, col) && money >= towerCosts[selectedTowerType]) { money -= towerCosts[selectedTowerType]; towers.add(new Tower(new GridLocation(row, col), selectedTowerType)); }
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
    fill(titleColor); textSize(48); textAlign(CENTER, CENTER); text(title, APP_WIDTH / 2, APP_HEIGHT / 2 - 30);
    fill(200); textSize(20); text(subtitle, APP_WIDTH / 2, APP_HEIGHT / 2 + 10);
  }
}