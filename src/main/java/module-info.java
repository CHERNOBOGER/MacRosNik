module macrosnik {
    requires javafx.controls;
    requires java.desktop;
    requires java.logging;

    requires com.github.kwhat.jnativehook;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    exports macrosnik.app;

    opens macrosnik.domain to com.fasterxml.jackson.databind;
    opens macrosnik.domain.enums to com.fasterxml.jackson.databind;
    opens macrosnik.settings to com.fasterxml.jackson.databind;
    opens macrosnik.storage to com.fasterxml.jackson.databind;
}
