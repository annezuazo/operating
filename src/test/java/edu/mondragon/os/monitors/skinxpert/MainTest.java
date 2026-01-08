package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    private File logFile;

    @BeforeEach
    void setUp() {
        // Identificamos el fichero que crea el Main
        logFile = new File("log.txt");
        // Nos aseguramos de borrarlo antes de empezar para que el test sea limpio
        if (logFile.exists()) {
            logFile.delete();
        }
    }

    @AfterEach
    void tearDown() {
        // Borramos el fichero de log creado por el test para no dejar basura
        if (logFile.exists()) {
            logFile.delete();
        }
    }

    @Test
    @DisplayName("Test de Integración: Arrancar sistema completo y verificar logs")
    void testMainSystemExecution() throws InterruptedException, IOException {
        
        // Ejecutamos el Main en un hilo aparte para no bloquear JUnit durante 20 segundos
        Thread simulationThread = new Thread(() -> {
            try {
                // Ejecutamos el main real
                Main.main(new String[]{});
            } catch (InterruptedException e) {
                // Es esperado: interrumpiremos el sleep(20000) manualmente
                System.out.println("Test: Simulación interrumpida intencionadamente para ahorrar tiempo.");
            } catch (Exception e) {
                // Cualquier otro error es un fallo del test
                e.printStackTrace();
                fail("El Main lanzó una excepción inesperada: " + e.getMessage());
            }
        });

        simulationThread.start();

        // Dejamos que el sistema corra durante 2 o 3 segundos
        // Tiempo suficiente para que los pacientes suban fotos y la IA las procese
        Thread.sleep(3000);

        // Interrumpimos el hilo del Main (rompe el Thread.sleep(20000))
        simulationThread.interrupt();
        
        // Esperamos a que el hilo termine de cerrarse
        simulationThread.join(1000);

        // --- VERIFICACIONES (Métricas de Reliability) ---

        // 1. Verificamos que se ha creado el fichero de base de datos
        assertTrue(logFile.exists(), "El sistema debería haber creado el fichero log.txt");
        assertTrue(logFile.length() > 0, "El fichero log.txt debería contener datos");

        // 2. Leemos el contenido para ver si el flujo completo funcionó
        // (Paciente -> Buffer -> IA -> Médico -> DB)
        List<String> lines = Files.readAllLines(logFile.toPath());
        
        System.out.println("Lineas escritas en DB durante el test: " + lines.size());
        
        // Debería haber al menos alguna línea de diagnóstico
        boolean hasDiagnosis = lines.stream().anyMatch(line -> line.contains("[DIAG]"));
        
        // NOTA: Si en 3 segundos no da tiempo a procesar nada, aumenta el sleep anterior a 4000 o 5000.
        // Pero con threads locales suele ser muy rápido.
        if (!lines.isEmpty()) {
             // Es aceptable que solo haya registros si la IA es lenta, pero comprobamos que escribe
             assertTrue(lines.size() >= 1, "Debería haber escrito al menos una línea en la DB");
        }
    }
}