package macrosnik.dsl;

import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

sealed interface StructuredDslCommand permits
        StructuredDslCommand.WaitCommand,
        StructuredDslCommand.TextCommand,
        StructuredDslCommand.MouseMoveCommand,
        StructuredDslCommand.MousePathCommand,
        StructuredDslCommand.MouseButtonCommand,
        StructuredDslCommand.KeyCommand,
        StructuredDslCommand.KeyComboCommand {

    String originalHead();

    String normalizedHead();

    String data();

    record WaitCommand(String originalHead,
                       String normalizedHead,
                       String data,
                       DelayUnit defaultUnit) implements StructuredDslCommand {
    }

    record TextCommand(String originalHead,
                       String normalizedHead,
                       String data) implements StructuredDslCommand {
    }

    record MouseMoveCommand(String originalHead,
                            String normalizedHead,
                            String data) implements StructuredDslCommand {
    }

    record MousePathCommand(String originalHead,
                            String normalizedHead,
                            String data) implements StructuredDslCommand {
    }

    record MouseButtonCommand(String originalHead,
                              String normalizedHead,
                              String data,
                              MouseButton button,
                              MouseButtonActionType actionType) implements StructuredDslCommand {
    }

    record KeyCommand(String originalHead,
                      String normalizedHead,
                      String data,
                      String keyToken,
                      KeyActionType actionType) implements StructuredDslCommand {
    }

    record KeyComboCommand(String originalHead,
                           String normalizedHead,
                           String data,
                           String comboText) implements StructuredDslCommand {
    }

    enum DelayUnit {
        MILLISECONDS(1),
        SECONDS(1_000),
        MINUTES(60_000);

        final long multiplierMs;

        DelayUnit(long multiplierMs) {
            this.multiplierMs = multiplierMs;
        }
    }
}
