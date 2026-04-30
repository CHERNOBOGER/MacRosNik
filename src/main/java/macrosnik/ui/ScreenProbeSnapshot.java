package macrosnik.ui;

import java.awt.Color;

public record ScreenProbeSnapshot(int x, int y, int red, int green, int blue) {

    public static ScreenProbeSnapshot from(int x, int y, Color color) {
        return new ScreenProbeSnapshot(x, y, color.getRed(), color.getGreen(), color.getBlue());
    }

    public static ScreenProbeSnapshot fromFx(int x, int y, javafx.scene.paint.Color color) {
        int red = clampColorComponent(color.getRed());
        int green = clampColorComponent(color.getGreen());
        int blue = clampColorComponent(color.getBlue());
        return new ScreenProbeSnapshot(x, y, red, green, blue);
    }

    public String coordinatesText() {
        return x + " " + y;
    }

    public String positionText() {
        return "(" + x + ", " + y + ")";
    }

    public String hexColor() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    public String rgbText() {
        return red + ", " + green + ", " + blue;
    }

    public javafx.scene.paint.Color toFxColor() {
        return javafx.scene.paint.Color.rgb(red, green, blue);
    }

    private static int clampColorComponent(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value * 255.0)));
    }
}
