package com.settlement.sim.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architectural fitness test enforcing the Hexagonal Boundary rule:
 * Pure simulation core classes in {@code com.settlement.sim.core.*} must never
 * depend on Minecraft or NeoForge engine classes.
 */
class HexagonalBoundaryTest {

    private static final String FORBIDDEN_MINECRAFT = "import net.minecraft.";
    private static final String FORBIDDEN_NEOFORGED = "import net.neoforged.";

    @Test
    @DisplayName("Verify com.settlement.sim.core contains zero Minecraft or NeoForge imports")
    void testHexagonalBoundaryPurity() throws IOException {
        Path coreDir = Path.of("src/main/java/com/settlement/sim/core");
        assertTrue(Files.exists(coreDir), "Core directory must exist: " + coreDir);

        List<String> violations = new ArrayList<>();
        int scannedFiles = 0;

        try (Stream<Path> stream = Files.walk(coreDir)) {
            List<Path> javaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            scannedFiles = javaFiles.size();

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.startsWith(FORBIDDEN_MINECRAFT) || line.startsWith("import static net.minecraft.")
                            || line.startsWith(FORBIDDEN_NEOFORGED) || line.startsWith("import static net.neoforged.")) {
                        violations.add(String.format("%s:%d -> %s", javaFile, i + 1, line));
                    }
                }
            }
        }

        assertTrue(scannedFiles > 0, "Expected at least one Java file in sim.core, found 0");
        assertTrue(violations.isEmpty(),
                () -> "Hexagonal boundary violations found in sim.core:\n" + String.join("\n", violations));
    }
}
