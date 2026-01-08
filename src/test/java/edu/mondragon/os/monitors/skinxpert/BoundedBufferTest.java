package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoundedBufferTest {

    private BoundedBuffer buffer;
    private static final int CAPACITY = 5;

    @BeforeEach
    void setUp() {
        buffer = new BoundedBuffer(CAPACITY);
    }

    // Método auxiliar para crear tareas sin necesitar el constructor real
    // Usamos Mocks para simular que tienen un ID, sin tocar la clase original
    private PhotoTask createTask(String id) {
        PhotoTask task = mock(PhotoTask.class);
        // Hacemos que el toString devuelva el ID para poder seguir el rastro en los logs
        when(task.toString()).thenReturn("Task-" + id); 
        return task;
    }

    @Test
    @DisplayName("Test Básico: FIFO (El primero en entrar es el primero en salir)")
    void testBasicFifo() throws InterruptedException {
        PhotoTask t1 = createTask("1");
        PhotoTask t2 = createTask("2");

        buffer.add(t1);
        buffer.add(t2);

        // Verificamos que salen en el mismo orden (comparando referencias de objetos)
        assertEquals(t1, buffer.remove(), "El orden debería ser FIFO");
        assertEquals(t2, buffer.remove(), "El orden debería ser FIFO");
    }

    @Test
    @DisplayName("Test Bloqueo: El consumidor debe esperar si el buffer está vacío")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testConsumerBlocksWhenEmpty() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        Thread consumerThread = new Thread(() -> {
            try {
                PhotoTask task = buffer.remove();
                assertNotNull(task);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumerThread.start();

        Thread.sleep(100); 
        assertEquals(1, latch.getCount(), "El consumidor debería seguir bloqueado esperando");

        // Añadimos elemento usando el helper
        buffer.add(createTask("Rescate"));

        boolean finished = latch.await(1, TimeUnit.SECONDS);
        assertTrue(finished, "El consumidor debería haberse desbloqueado tras el add()");
    }

    @Test
    @DisplayName("Test Bloqueo: El productor debe esperar si el buffer está lleno")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testProducerBlocksWhenFull() throws InterruptedException {
        // Llenamos el buffer
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add(createTask("Init-" + i));
        }

        CountDownLatch latch = new CountDownLatch(1);

        Thread producerThread = new Thread(() -> {
            try {
                buffer.add(createTask("Overflow")); // Se bloqueará aquí
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producerThread.start();

        Thread.sleep(100);
        assertEquals(1, latch.getCount(), "El productor debería estar bloqueado por falta de espacio");

        // Liberamos espacio
        buffer.remove();

        boolean finished = latch.await(1, TimeUnit.SECONDS);
        assertTrue(finished, "El productor debería haberse desbloqueado tras el remove()");
    }

    @Test
    @DisplayName("Test de Estrés: Concurrencia masiva")
    void testConcurrencyStress() throws InterruptedException {
        int producerCount = 10;
        int consumerCount = 10;
        int tasksPerThread = 100;
        int totalTasks = producerCount * tasksPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(producerCount + consumerCount);
        CountDownLatch finishLatch = new CountDownLatch(producerCount + consumerCount);
        AtomicInteger consumedCount = new AtomicInteger(0);

        // Productores
        for (int i = 0; i < producerCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < tasksPerThread; j++) {
                        // Aquí usamos el constructor por defecto si Mockito es muy pesado para bucles grandes,
                        // o seguimos usando el mock. Para estrés simple, new PhotoTask() es más rápido si existe,
                        // pero usaremos el mock para consistencia.
                        buffer.add(createTask("Data")); 
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Consumidores
        for (int i = 0; i < consumerCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < tasksPerThread; j++) {
                        buffer.remove();
                        consumedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        boolean allFinished = finishLatch.await(5, TimeUnit.SECONDS);
        assertTrue(allFinished, "El sistema concurrente debería terminar sin deadlocks");
        
        assertEquals(totalTasks, consumedCount.get(), "Se deben consumir tantas tareas como se produjeron");
        
        executor.shutdown();
    }
}