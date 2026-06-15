import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PFont;
import processing.data.JSONObject;
import java.io.InputStream;
import java.util.Scanner;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;


/**
 * Resource class - helps manage resources to work inside Docker
 * @author Joel A Bianchi
 * @version 5/18/26
 */
public class Resource {

    /**
     * Loads an interactive 2D image sprite sheet or background from the JAR resources.
     */
    public static PImage loadImage(String path) {
        if (path == null) return null;
        try {
            InputStream is = Resource.class.getClassLoader().getResourceAsStream(path);
            if (is == null) {
                System.err.println("RESOURCE ERROR: File not found at " + path);
                return null;
            }
            BufferedImage bimg = ImageIO.read(is);
            if (bimg == null) {
                System.err.println("RESOURCE ERROR: Could not decode image at " + path);
                return null;
            }
            // Convert to a standard Processing PImage (with alpha/transparency channels)
            PImage img = new PImage(bimg.getWidth(), bimg.getHeight(), PApplet.ARGB);
            bimg.getRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);
            img.updatePixels();
            return img;
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to stream image resource: " + path);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads and registers a custom TrueType vector font (.ttf) from the JAR resources.
     */
    public static PFont loadFont(PApplet p, String fontPath, float size) {
        if (fontPath == null) return p.createFont("SansSerif", size);
        try {
            InputStream is = Resource.class.getClassLoader().getResourceAsStream(fontPath);
            if (is == null) {
                System.err.println("RESOURCE ERROR: Font file not found at " + fontPath);
                return p.createFont("SansSerif", size); // Fallback to safe system font
            }
            java.awt.Font awtFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
            return new PFont(awtFont.deriveFont(size), true);
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to parse font vector data: " + fontPath);
            return p.createFont("SansSerif", size);
        }
    }

    /**
     * Loads a structural JSON data sheet from the JAR resources (useful for physics or animation maps).
     */
    public static JSONObject loadJSONObject(PApplet p, String jsonPath) {
        if (jsonPath == null) return null;
        InputStream is = Resource.class.getClassLoader().getResourceAsStream(jsonPath);
        if (is == null) {
            System.err.println("RESOURCE ERROR: JSON configuration map not found at " + jsonPath);
            return null;
        }
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String jsonString = scanner.hasNext() ? scanner.next() : "{}";
        return p.parseJSONObject(jsonString);
    }
}
