package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientUploaderTest {

    private BoundedBuffer mockBuffer;
    private PatientUploader uploader;

    @BeforeEach
    void setUp() {
        mockBuffer = mock(BoundedBuffer.class);
        // Creamos una instancia básica para tests simples
        uploader = new PatientUploader("TestPatient", mockBuffer);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (uploader.isAlive()) {
            uploader.interrupt();
            uploader.join(1000);
        }
    }

    @Test
    @DisplayName("Test Funcional: Genera tarea y la añade al buffer")
    void testUploadsPhotosToBuffer() throws InterruptedException {
        // Hacemos que el buffer acepte cualquier cosa
        doNothing().when(mockBuffer).add(any(PhotoTask.class));

        uploader.start();

        // Esperamos suficiente tiempo para que el bucle corra al menos una vez (sleep 400ms + random)
        Thread.sleep(1200);

        uploader.interrupt();
        uploader.join(1000);

        // Verificamos que se llamó al método add del buffer al menos una vez
        verify(mockBuffer, atLeast(1)).add(any(PhotoTask.class));
    }

    @Test
    @DisplayName("Test Concurrencia: IDs únicos con múltiples hilos (Global Counter)")
    void testCounterThreadSafety() throws InterruptedException {
        // --- ESCENARIO ---
        // Lanzamos muchos hilos de pacientes a la vez.
        // Si el 'synchronized' funciona mal, habrá IDs duplicados.
        
        int numberOfPatients = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfPatients);
        
        // Usamos un Set sincronizado para guardar los IDs generados
        // Set no permite duplicados, así que si intentamos meter uno repetido, lo sabremos.
        Set<Integer> generatedIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        
        // Mock del buffer que guarda el ID de la tarea recibida
        BoundedBuffer sharedMockBuffer = mock(BoundedBuffer.class);
        
        // Cuando alguien haga buffer.add(task), guardamos el ID de esa task en nuestro Set
        doAnswer(invocation -> {
            PhotoTask task = invocation.getArgument(0);
            generatedIds.add(task.getId()); 
            return null;
        }).when(sharedMockBuffer).add(any(PhotoTask.class));

        // --- EJECUCIÓN ---
        List<PatientUploader> patients = new ArrayList<>();
        
        for (int i = 0; i < numberOfPatients; i++) {
            PatientUploader p = new PatientUploader("P" + i, sharedMockBuffer);
            patients.add(p);
            executor.submit(p);
        }

        // Dejamos que corran un rato para generar muchas fotos
        Thread.sleep(2000);

        // Paramos todo
        for (PatientUploader p : patients) {
            p.interrupt();
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // --- VERIFICACIÓN ---
        
        // 1. Aseguramos que se han generado IDs
        assertFalse(generatedIds.isEmpty(), "Se deberían haber generado IDs de fotos");
        
        System.out.println("Total IDs generados: " + generatedIds.size());
        
        // 2. Comprobamos la unicidad
        // La lógica es: Cada vez que 'add' se llamó, metimos el ID en el Set.
        // Si hubo duplicados, el tamaño del Set sería menor que el número total de llamadas a 'add'.
        // Pero como usamos un Set concurrente y lo llenamos en tiempo real, 
        // la mejor prueba es verificar que no hubo colisiones en la lógica interna del contador.
        
        // Para verificar estrictamente duplicados, capturamos TODAS las llamadas:
        ArgumentCaptor<PhotoTask> captor = ArgumentCaptor.forClass(PhotoTask.class);
        verify(sharedMockBuffer, atLeast(1)).add(captor.capture());
        
        List<PhotoTask> allTasks = captor.getAllValues();
        long uniqueIdsCount = allTasks.stream().map(PhotoTask::getId).distinct().count();
        
        assertEquals(allTasks.size(), uniqueIdsCount, 
            "El número de IDs únicos debe ser igual al total de tareas. ¡Hay duplicados! Falló el synchronized.");
    }

    @Test
    @DisplayName("Test Interrupción: Termina correctamente")
    void testInterruption() throws InterruptedException {
        // Simulamos un buffer que tarda mucho si se le llama, para forzar espera
        doAnswer(inv -> { Thread.sleep(5000); return null; }).when(mockBuffer).add(any());

        uploader.start();
        assertTrue(uploader.isAlive());

        uploader.interrupt();
        uploader.join(1000);

        assertFalse(uploader.isAlive(), "El hilo debería morir al ser interrumpido");
    }
}