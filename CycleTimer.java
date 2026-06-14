import processing.core.PApplet;

public class CycleTimer {

    PApplet p;

    // ---------------- FIELDS ---------------- //
    private long startTime;
    private long lastReset;
    private long frequencyMs;

    // ---------------- CONSTRUCTOR ---------------- //

    public CycleTimer(PApplet p, long frequencyMs) {
        this.p = p;
        this.frequencyMs = frequencyMs;

        this.startTime = p.millis();
        this.lastReset = p.millis();
    }

    // ---------------- TIME METHODS ---------------- //

    public long getGameTime() {
        return p.millis();
    }

    public long getTimerTime() {
        return getGameTime() - startTime;
    }

    public long getTime() {
        return getGameTime() - lastReset;
    }

    public long getCountdown() {
        return frequencyMs - getTime();
    }

    // ---------------- CORE FIXED LOGIC ---------------- //

    public boolean isDone() {

        if (getTime() >= frequencyMs) {

            // FIX: prevents drift over time
            lastReset += frequencyMs;

            return true;
        }

        return false;
    }

    // ---------------- UTIL ---------------- //

    public float getSeconds(long msTime) {
        return msTime / 1000.0f;
    }

    public void resetCycleTime() {
        lastReset = p.millis();
    }

    public void pause(final int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public String toString() {
        return "CycleTimer: time=" + getTime() +
               " freq=" + frequencyMs;
    }
}