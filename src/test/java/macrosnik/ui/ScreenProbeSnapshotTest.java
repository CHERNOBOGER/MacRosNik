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
}
