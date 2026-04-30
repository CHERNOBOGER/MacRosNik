package macrosnik.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenProbeSnapshotTest {

    @Test
    void formatsCoordinatesAndColorValues() {
        ScreenProbeSnapshot snapshot = new ScreenProbeSnapshot(1280, 720, 74, 175, 80);

        assertEquals("1280 720", snapshot.coordinatesText());
        assertEquals("(1280, 720)", snapshot.positionText());
        assertEquals("#4AAF50", snapshot.hexColor());
        assertEquals("74, 175, 80", snapshot.rgbText());
    }

    @Test
    void convertsFromAndToFxColor() {
        javafx.scene.paint.Color color = new javafx.scene.paint.Color(0.501, 0.25, 0.999, 1.0);

        ScreenProbeSnapshot snapshot = ScreenProbeSnapshot.fromFx(10, 20, color);

        assertEquals(128, snapshot.red());
        assertEquals(64, snapshot.green());
        assertEquals(255, snapshot.blue());
        assertEquals(javafx.scene.paint.Color.rgb(128, 64, 255), snapshot.toFxColor());
    }
}
