package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentManagerTest {

    private AppointmentManager manager;

    @BeforeEach
    void setUp() {
        manager = new AppointmentManager();
    }

    @Test
    @DisplayName("Test básico: Crear y listar citas")
    void testCreateAndList() {
        manager.create("Paciente A - 10:00");
        manager.create("Paciente B - 11:00");

        List<String> list = manager.list();

        assertEquals(2, list.size(), "Debería haber 2 citas");
        assertTrue(list.contains("Paciente A - 10:00"));
        assertTrue(list.contains("Paciente B - 11:00"));
    }

    @Test
    @DisplayName("Test básico: Eliminar citas")
    void testRemove() {
        String info = "Paciente C - 12:00";
        manager.create(info);
        assertEquals(1, manager.list().size());

        manager.remove(info);
        assertEquals(0, manager.list().size(), "La lista debería estar vacía tras eliminar");
    }

    @Test
    @DisplayName("Test de Concurrencia: Acceso simultáneo con múltiples hilos")
    void testConcurrency() throws InterruptedException {
        int numberOfThreads = 10;
        int appointmentsPerThread = 100;
        
        // Simulamos 10 hilos intentando añadir 100 citas cada uno a la vez
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            service.submit(() -> {
                try {
                    for (int j = 0; j < appointmentsPerThread; j++) {
                        manager.create("Hilo " + threadNum + " - Cita " + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Esperamos a que todos los hilos terminen
        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertTrue(finished, "Los hilos deberían terminar a tiempo");

        // Verificación crítica:
        // Si el 'lock' no funcionara, el tamaño sería menor a 1000 debido a condiciones de carrera.
        assertEquals(numberOfThreads * appointmentsPerThread, manager.list().size(), 
            "El tamaño total debe coincidir, garantizando la exclusión mutua");
    }
}