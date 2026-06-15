# TODO

- [x] Inspect `Game.java` and `TDMain.java` to identify why nothing renders.
- [x] Determine that `TDMain` manually drives `PApplet` lifecycle incorrectly.
- [x] Update `TDMain.java` to start the Processing sketch with `PApplet.runSketch(...)`.
- [x] Stop manually blitting Processing backbuffer in `paintComponent`.
- [ ] Build/run locally and verify the start screen renders.
- [ ] If the embedded mode still fails, switch to a pure Processing window by launching `Game` directly (alternate fallback).

