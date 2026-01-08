package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseWriterTest {

    // JUnit 5 crea esta carpeta temporal y la borra al acabar
    @TempDir
    Path tempDir;

    private DatabaseWriter writerThread;
    private BlockingQueue<String> queue;
    private File testFile;

    @BeforeEach
    void setUp() {
        // Creamos una cola real para pasar mensajes
        queue = new LinkedBlockingQueue<>();
        
        // Definimos un fichero dentro de la carpeta temporal
        testFile = tempDir.resolve("test_db_log.txt").toFile();
        
        // Instanciamos el hilo (pero no lo arrancamos todavía en el setUp)
        writerThread = new DatabaseWriter(queue, testFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Nos aseguramos de parar el hilo siempre para no dejar procesos "zombis"
        if (writerThread.isAlive()) {
            writerThread.interrupt();
            writerThread.join(1000); // Esperar a que muera
        }
    }

    @Test
    @DisplayName("Test Funcional: Escribir mensajes en el fichero")
    void testWriteMessagesToFile() throws IOException, InterruptedException {
        writerThread.start();

        String msg1 = "Registro de paciente 001";
        String msg2 = "Registro de paciente 002";

        // Enviamos datos a la cola
        queue.put(msg1);
        queue.put(msg2);

        // Esperamos un poco a que el hilo escriba (operación asíncrona)
        Thread.sleep(200);

        // Verificamos leyendo el fichero real
        List<String> lines = Files.readAllLines(testFile.toPath());

        assertFalse(lines.isEmpty(), "El fichero no debería estar vacío");
        assertEquals(2, lines.size(), "Debería haber 2 líneas escritas");
        assertEquals(msg1, lines.get(0));
        assertEquals(msg2, lines.get(1));
    }

    @Test
    @DisplayName("Test de Fiabilidad: El hilo espera si la cola está vacía (Blocking)")
    void testBlockingWaitForData() throws InterruptedException, IOException {
        writerThread.start();

        // No metemos nada en la cola. El hilo debería estar vivo pero esperando (WAITING)
        Thread.sleep(100);
        assertTrue(writerThread.isAlive());
        
        // El fichero debería existir (se crea al inicio del run) pero estar vacío
        assertTrue(testFile.exists());
        assertEquals(0, testFile.length(), "El fichero debería estar vacío");

        // Ahora metemos datos tarde
        queue.put("Dato tardío");
        
        Thread.sleep(100);
        
        // Verificamos que finalmente se escribió
        List<String> lines = Files.readAllLines(testFile.toPath());
        assertEquals("Dato tardío", lines.get(0));
    }

    @Test
    @DisplayName("Test de Ciclo de Vida: Interrupción correcta")
    void testInterruption() throws InterruptedException {
        writerThread.start();
        
        assertTrue(writerThread.isAlive());

        // Interrumpimos el hilo para simular el apagado del sistema
        writerThread.interrupt();
        
        // Esperamos a que termine
        writerThread.join(2000);
        
        // Verificamos que el hilo ha muerto
        assertFalse(writerThread.isAlive(), "El hilo debería haber terminado tras la interrupción");
    }
}