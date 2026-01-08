package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UserStoreTest {

    private UserStore userStore;

    @BeforeEach
    void setUp() {
        userStore = new UserStore();
    }

    @Test
    @DisplayName("Test Funcional: Añadir y recuperar Pacientes y Médicos")
    void testBasicAddAndGet() {
        // 1. Pacientes
        userStore.addPatient("P01", "Mikel - Dermatitis");
        String pInfo = userStore.getPatient("P01");
        
        assertEquals("Mikel - Dermatitis", pInfo, "La info del paciente debe coincidir");
        assertNull(userStore.getPatient("P99"), "Debe devolver null si no existe");

        // 2. Médicos
        userStore.addDoctor("D01", "Dr. House - Diagnóstico");
        String dInfo = userStore.getDoctor("D01");
        
        assertEquals("Dr. House - Diagnóstico", dInfo, "La info del médico debe coincidir");
    }

    @Test
    @DisplayName("Test Concurrencia: Escritor único vs Múltiples Lectores")
    void testReadWriteConcurrency() throws InterruptedException {
        // --- ESCENARIO ---
        // El ReadWriteLock brilla cuando hay muchas lecturas y pocas escrituras.
        // Simularemos 1 hilo Admin escribiendo y 10 hilos usuarios leyendo a la vez.
        // Si el lock fallara, el HashMap podría lanzar ConcurrentModificationException o dar datos corruptos.

        int readersCount = 10;
        int totalWrites = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(readersCount + 1);
        CountDownLatch latch = new CountDownLatch(readersCount + 1);
        AtomicInteger successfulReads = new AtomicInteger(0);

        // 1. Hilo Escritor (Admin)
        executor.submit(() -> {
            try {
                for (int i = 0; i < totalWrites; i++) {
                    userStore.addPatient("P" + i, "Info-" + i);
                    // Pequeña pausa para mezclar operaciones con los lectores
                    if (i % 100 == 0) Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        // 2. Hilos Lectores (Consultas constantes)
        for (int i = 0; i < readersCount; i++) {
            executor.submit(() -> {
                try {
                    // Cada lector intenta leer aleatoriamente mientras el escritor trabaja
                    for (int k = 0; k < 500; k++) {
                        // Leemos un ID aleatorio dentro del rango de escritura
                        int idToRead = (int) (Math.random() * totalWrites);
                        String info = userStore.getPatient("P" + idToRead);
                        
                        // Si leemos algo que no es null, verificamos que no esté corrupto
                        if (info != null) {
                            if (info.equals("Info-" + idToRead)) {
                                successfulReads.incrementAndGet();
                            } else {
                                fail("Lectura corrupta: Se esperaba Info-" + idToRead + " pero se leyó " + info);
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Esperar a que todos terminen
        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertTrue(finished, "El test de concurrencia debería terminar a tiempo");

        // Verificación final: aseguramos que el último dato escrito está ahí
        assertEquals("Info-" + (totalWrites - 1), userStore.getPatient("P" + (totalWrites - 1)));
        
        System.out.println("Lecturas exitosas concurrentes: " + successfulReads.get());
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("Test Aislamiento: Escribir médico no bloquea lectura de paciente (Teórico)")
    void testSeparateMaps() {
        // Aunque usen el mismo lock, verificamos que la lógica de mapas sea independiente
        userStore.addDoctor("D1", "Doctor 1");
        userStore.addPatient("P1", "Patient 1");
        
        assertEquals("Doctor 1", userStore.getDoctor("D1"));
        assertEquals("Patient 1", userStore.getPatient("P1"));
    }
}