package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PriorityQueueSyncTest {

    private PriorityQueueSync queue;

    @BeforeEach
    void setUp() {
        queue = new PriorityQueueSync();
    }

    // Helper para crear tareas rápido con una urgencia específica
    private PhotoTask createTask(int id, PhotoTask.Urgency urgency) {
        PhotoTask t = new PhotoTask(id, "P" + id, "Clinic");
        t.setUrgency(urgency);
        return t;
    }

    @Test
    @DisplayName("Test Prioridad: HIGH debe salir antes que LOW aunque entre más tarde")
    void testPriorityOrdering() throws InterruptedException {
        // 1. Insertamos en orden "incorrecto" (primero lo menos urgente)
        PhotoTask lowTask = createTask(1, PhotoTask.Urgency.LOW);
        PhotoTask mediumTask = createTask(2, PhotoTask.Urgency.MEDIUM);
        PhotoTask highTask = createTask(3, PhotoTask.Urgency.HIGH);

        queue.add(lowTask);
        queue.add(mediumTask);
        queue.add(highTask);

        // 2. Extraemos y verificamos el orden
        // Debería ser: HIGH -> MEDIUM -> LOW
        
        PhotoTask first = queue.take();
        assertEquals(PhotoTask.Urgency.HIGH, first.getUrgency(), "El primero debería ser HIGH");
        assertEquals(3, first.getId());

        PhotoTask second = queue.take();
        assertEquals(PhotoTask.Urgency.MEDIUM, second.getUrgency(), "El segundo debería ser MEDIUM");
        
        PhotoTask third = queue.take();
        assertEquals(PhotoTask.Urgency.LOW, third.getUrgency(), "El tercero debería ser LOW");
    }

    @Test
    @DisplayName("Test Bloqueo: take() espera si la cola está vacía")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testBlockingWhenEmpty() throws InterruptedException {
        // Usamos AtomicReference para sacar el resultado del hilo
        AtomicReference<PhotoTask> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Hilo consumidor
        Thread consumer = new Thread(() -> {
            try {
                // Esto se bloqueará hasta que alguien añada algo
                PhotoTask t = queue.take();
                result.set(t);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();

        // Aseguramos que el hilo ha tenido tiempo de bloquearse
        Thread.sleep(100);
        assertTrue(consumer.isAlive(), "El consumidor debería estar esperando (WAITING)");
        assertNull(result.get(), "No debería haber resultado todavía");

        // Ahora añadimos una tarea (Producer)
        PhotoTask task = createTask(99, PhotoTask.Urgency.HIGH);
        queue.add(task);

        // Esperamos a que el consumidor termine
        boolean finished = latch.await(1, TimeUnit.SECONDS);
        
        assertTrue(finished, "El consumidor debería haber despertado tras el add()");
        assertEquals(task, result.get(), "El consumidor debería haber recuperado la tarea añadida");
    }

    @Test
    @DisplayName("Test Snapshot: Devuelve copia segura sin vaciar la cola")
    void testSnapshot() {
        queue.add(createTask(1, PhotoTask.Urgency.LOW));
        queue.add(createTask(2, PhotoTask.Urgency.HIGH));

        var list = queue.snapshot();

        assertEquals(2, list.size(), "El snapshot debe tener todos los elementos");
        
        // Verificar que el snapshot NO afecta a la cola real
        // Si fuera la cola real, al recorrerla a veces se vacía o se altera
        queue.add(createTask(3, PhotoTask.Urgency.MEDIUM));
        
        assertEquals(2, list.size(), "El snapshot previo no debe cambiar al añadir nuevos elementos");
        assertEquals(3, queue.snapshot().size(), "La cola real debe tener 3 elementos");
    }
}