/**
 * GridLocation - immutable row/col coordinate for grid-based games
 */
public class GridLocation {

  private final int row;
  private final int col;

  public GridLocation(int row, int col) {
    this.row = row;
    this.col = col;
  }

  // ---------------- ACCESSORS ---------------- //

  public int getRow() {
    return row;
  }

  public int getCol() {
    return col;
  }

  // optional aliases (kept for compatibility)
  public int getYCoord() {
    return row;
  }

  public int getXCoord() {
    return col;
  }

  // ---------------- UTILITY ---------------- //

  @Override
  public String toString() {
    return "[" + row + "," + col + "]";
  }

  // ---------------- FIXED EQUALITY ---------------- //

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    GridLocation other = (GridLocation) obj;
    return row == other.row && col == other.col;
  }

  @Override
  public int hashCode() {
    return 31 * row + col;
  }
}