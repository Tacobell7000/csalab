import processing.core.PApplet;
import processing.core.PImage;

/**
 * Grid Class - Used for rectangular-tiled games
 * Supports images, sprites, and tile marking
 */
public class Grid extends World {

  private int rows;
  private int cols;

  private GridTile[][] board;
  private boolean printingGridMarks = false;

  // ---------------- CONSTRUCTORS ---------------- //

  public Grid(PApplet p) {
    this(p, 3, 3);
  }

  public Grid(PApplet p, int rows, int cols) {
    this(p, "grid", null, rows, cols);
  }

  public Grid(PApplet p, String screenName, String bgFile, int rows, int cols) {
    this(p, screenName, bgFile, null, rows, cols);
  }

  public Grid(PApplet p, String screenName, String bgFile, String[][] tileMarks, int rows, int cols) {
    super(p, screenName, bgFile);

    this.rows = rows;
    this.cols = cols;

    board = new GridTile[rows][cols];

    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        board[r][c] = new GridTile(p, new GridLocation(r, c));
      }
    }

    if (tileMarks != null) {
      setAllMarks(tileMarks);
    }
  }

  // ---------------- SAFETY ---------------- //

  private boolean inBounds(GridLocation loc) {
    return loc != null &&
           loc.getRow() >= 0 && loc.getRow() < rows &&
           loc.getCol() >= 0 && loc.getCol() < cols;
  }

  // ---------------- MARK SYSTEM ---------------- //

  public void setMark(String mark, GridLocation loc) {
    if (!inBounds(loc)) return;
    board[loc.getRow()][loc.getCol()].setMark(mark);

    if (printingGridMarks) printGrid();
  }

  public String getMark(GridLocation loc) {
    if (!inBounds(loc)) return null;
    return board[loc.getRow()][loc.getCol()].getMark();
  }

  public boolean removeMark(GridLocation loc) {
    if (!inBounds(loc)) return false;
    return board[loc.getRow()][loc.getCol()].removeMark();
  }

  public boolean hasMark(GridLocation loc) {
    if (!inBounds(loc)) return false;
    GridTile tile = board[loc.getRow()][loc.getCol()];
    return tile.getMark() != tile.getNoMark();
  }

  public boolean setNewMark(String mark, GridLocation loc) {
    if (!inBounds(loc)) return false;

    boolean ok = board[loc.getRow()][loc.getCol()].setNewMark(mark);

    if (printingGridMarks) printGrid();

    return ok;
  }

  // ---------------- GRID INFO ---------------- //

  public int getNumRows() { return rows; }
  public int getNumCols() { return cols; }

  public int getTileWidth() {
    return p.pixelWidth / cols;
  }

  public int getTileHeight() {
    return p.pixelHeight / rows;
  }

  // ---------------- COORDINATES ---------------- //

  public int getX(GridLocation loc) {
    return loc.getCol() * getTileWidth();
  }

  public int getY(GridLocation loc) {
    return loc.getRow() * getTileHeight();
  }

  public int getCenterX(GridLocation loc) {
    return getX(loc) + getTileWidth() / 2;
  }

  public int getCenterY(GridLocation loc) {
    return getY(loc) + getTileHeight() / 2;
  }

  public GridLocation getGridLocation() {
    int row = p.mouseY / getTileHeight();
    int col = p.mouseX / getTileWidth();
    return new GridLocation(row, col);
  }

  // ---------------- TILE ACCESS ---------------- //

  public GridTile getTile(GridLocation loc) {
    if (!inBounds(loc)) return null;
    return board[loc.getRow()][loc.getCol()];
  }

  // ---------------- IMAGES ---------------- //

  public void setTileImage(GridLocation loc, PImage img) {
    if (!inBounds(loc)) return;
    board[loc.getRow()][loc.getCol()].setImage(img);
  }

  public PImage getTileImage(GridLocation loc) {
    if (!inBounds(loc)) return null;
    return board[loc.getRow()][loc.getCol()].getImage();
  }

  public boolean hasTileImage(GridLocation loc) {
    if (!inBounds(loc)) return false;
    return board[loc.getRow()][loc.getCol()].hasImage();
  }

  public void clearTileImage(GridLocation loc) {
    setTileImage(loc, null);
  }

  public void showTileImage(GridLocation loc) {
    if (!inBounds(loc)) return;

    GridTile tile = board[loc.getRow()][loc.getCol()];
    if (tile.hasImage()) {
      p.image(tile.getImage(), getX(loc), getY(loc));
    }
  }

  public void showGridImages() {
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        GridLocation loc = new GridLocation(r, c);
        if (hasTileImage(loc)) {
          showTileImage(loc);
        }
      }
    }
  }

  // ---------------- SPRITES ---------------- //

  public void setTileSprite(GridLocation loc, Sprite sprite) {
    if (!inBounds(loc)) return;

    GridTile tile = board[loc.getRow()][loc.getCol()];

    if (sprite == null) {
      tile.setSprite(null);
      return;
    }

    sprite.setCenterX(getCenterX(loc));
    sprite.setCenterY(getCenterY(loc));

    tile.setSprite(sprite);
  }

  public Sprite getTileSprite(GridLocation loc) {
    if (!inBounds(loc)) return null;
    return board[loc.getRow()][loc.getCol()].getSprite();
  }

  public boolean hasTileSprite(GridLocation loc) {
    if (!inBounds(loc)) return false;
    return board[loc.getRow()][loc.getCol()].hasSprite();
  }

  public void clearTileSprite(GridLocation loc) {
    setTileSprite(loc, null);
  }

  public void animateTileSprite(GridLocation loc) {
    try {
      AnimatedSprite a = (AnimatedSprite) getTileSprite(loc);
      a.animate();
    } catch (Exception e) {
      // ignore non-animated sprites
    }
  }

  public void showTileSprite(GridLocation loc) {
    if (!inBounds(loc)) return;

    GridTile tile = board[loc.getRow()][loc.getCol()];
    if (tile.hasSprite()) {
      tile.getSprite().show();
    }
  }

  public void showGridSprites() {
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {

        GridLocation loc = new GridLocation(r, c);

        if (hasTileSprite(loc)) {

          Sprite s = getTileSprite(loc);

          if (s != null && s.getIsAnimated()) {
            animateTileSprite(loc);
          } else {
            showTileSprite(loc);
          }
        }
      }
    }
  }

  // ---------------- RENDER ---------------- //

  public void show() {
    super.show();
    showGridImages();
    showGridSprites();
  }

  // ---------------- DEBUG ---------------- //

  public void printGrid() {
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        System.out.print(board[r][c]);
      }
      System.out.println();
    }
  }

  public void startPrintingGridMarks() {
    printingGridMarks = true;
  }

  public void stopPrintingGridMarks() {
    printingGridMarks = false;
  }

  public void setAllMarks(String[][] marks) {
    for (int r = 0; r < Math.min(rows, marks.length); r++) {
      for (int c = 0; c < Math.min(cols, marks[r].length); c++) {
        board[r][c].setMark(marks[r][c]);
      }
    }
  }
}