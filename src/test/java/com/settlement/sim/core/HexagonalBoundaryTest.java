package com.settlement.sim.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architectural fitness test enforcing the strict hexagonal boundary:
 * {@code com.settlement.sim.core.*} must remain pure Java with zero imports
 * or inline references to Minecraft, Mojang, NeoForge, or {@code com.settlement.mc.*}.
 */
class HexagonalBoundaryTest {

    private static final Path SIM_CORE_ROOT = Path.of("src/main/java/com/settlement/sim/core");

    private static final List<String> FORBIDDEN_PREFIXES = List.of(
            "net.minecraft.",
            "net.neoforged.",
            "com.mojang.",
            "com.settlement.mc."
    );

    @Test
    void simCoreMustNotDependOnMinecraftOrNeoForge() throws IOException {
        assertTrue(Files.isDirectory(SIM_CORE_ROOT), "Missing sim.core directory: " + SIM_CORE_ROOT);

        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(SIM_CORE_ROOT)) {
            List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
            assertTrue(!javaFiles.isEmpty(), "Expected at least one Java file in sim.core");

            for (Path file : javaFiles) {
                List<String> lines = Files.readAllLines(file);
                boolean inBlockComment = false;
                for (int i = 0; i < lines.size(); i++) {
                    String trimmed = lines.get(i).trim();
                    if (trimmed.startsWith("/*")) {
                        inBlockComment = true;
                    }
                    if (inBlockComment) {
                        if (trimmed.contains("*/")) {
                            inBlockComment = false;
                        }
                        continue;
                    }
                    if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
                        continue;
                    }
                    for (String prefix : FORBIDDEN_PREFIXES) {
                        if (trimmed.contains(prefix)) {
                            violations.add(file + ":" + (i + 1) + " -> " + trimmed);
                        }
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Hexagonal boundary violations in com.settlement.sim.core:\n" + String.join("\n", violations)
        );
    }
}
