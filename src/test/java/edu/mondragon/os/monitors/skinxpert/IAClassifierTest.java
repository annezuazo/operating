package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IAClassifierTest {

    private IAClassifier iaThread;
    private BoundedBuffer mockBuffer;
    private Map<String, PriorityQueueSync> doctorQueues;
    private PriorityQueueSync mockDoc1Queue;

    @BeforeEach
    void setUp() {
        // 1. Mock del Buffer de entrada
        mockBuffer = mock(BoundedBuffer.class);

        // 2. Preparar el mapa de colas de salida
        doctorQueues = new HashMap<>();
        mockDoc1Queue = mock(PriorityQueueSync.class);
        
        // Asumimos que existe un médico "doc1"
        doctorQueues.put("doc1", mockDoc1Queue);

        // 3. Crear el hilo IA
        iaThread = new IAClassifier(mockBuffer, doctorQueues);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (iaThread.isAlive()) {
            iaThread.interrupt();
            iaThread.join(1000);
        }
    }

    @Test
    @DisplayName("Test Flujo: Recoge foto, asigna ID, clasifica y envía a cola")
    void testClassifyAndRouteTask() throws InterruptedException {
        // --- PREPARACIÓN DEL ESCENARIO ---
        PhotoTask mockTask = mock(PhotoTask.class);

        // Configuración CLAVE para simular el cambio de ID:
        // 1ª llamada (en el if): devuelve null -> entra al if y hace setDoctorId
        // 2ª llamada (en el get del mapa): devuelve "doc1" -> encuentra la cola
        when(mockTask.getDoctorId())
                .thenReturn(null)
                .thenReturn("doc1");
        
        // El toString para depuración
        when(mockTask.toString()).thenReturn("Task-Unknown");

        // Comportamiento del buffer:
        // 1. Devuelve la tarea.
        // 2. Lanza interrupción para terminar el test (salir del while).
        when(mockBuffer.remove())
                .thenReturn(mockTask)
                .thenThrow(new InterruptedException("End of test"));

        // --- EJECUCIÓN ---
        iaThread.start();
        
        // Esperamos a que el hilo muera (debido a la interrupción programada)
        iaThread.join(2000);

        // --- VERIFICACIÓN ---
        
        // 1. Verificar que sacó algo del buffer
        verify(mockBuffer, atLeast(1)).remove();
        // 2. Verificar que asignó el ID por defecto "doc1" (porque venía null)
        verify(mockTask).setDoctorId("doc1");

        // 3. Verificar que la IA clasificó la urgencia (cualquiera de las 3 opciones)
        // Usamos 'any()' porque el random es interno y no podemos predecirlo fácilmente
        verify(mockTask).setUrgency(any(PhotoTask.Urgency.class));

        // 4. Verificar que se añadió a la cola del médico correcto
        verify(mockDoc1Queue).add(mockTask);
    }

    @Test
    @DisplayName("Test Routing: Respeta si la tarea ya tiene médico asignado")
    void testPreservesExistingDoctorId() throws InterruptedException {
        // Escenario: La tarea ya viene con "doc2" asignado
        PhotoTask mockTask = mock(PhotoTask.class);
        when(mockTask.getDoctorId()).thenReturn("doc2");

        // Preparamos la cola para el doc2
        PriorityQueueSync mockDoc2Queue = mock(PriorityQueueSync.class);
        doctorQueues.put("doc2", mockDoc2Queue);

        // Buffer devuelve tarea y luego corta
        when(mockBuffer.remove())
                .thenReturn(mockTask)
                .thenThrow(new InterruptedException());

        iaThread.start();
        iaThread.join(2000);

        // VERIFICACIONES
        // NO debe haber llamado a setDoctorId("doc1") porque ya tenía "doc2"
        verify(mockTask, never()).setDoctorId("doc1");

        // Debe haber ido a la cola del doc2
        verify(mockDoc2Queue).add(mockTask);
        
        // Y no a la del doc1
        verify(mockDoc1Queue, never()).add(mockTask);
    }

    @Test
    @DisplayName("Test Interrupción: Termina suavemente")
    void testInterruption() throws InterruptedException {
        // Hacemos que el buffer se bloquee simulando espera
        when(mockBuffer.remove()).thenAnswer(inv -> {
            Thread.sleep(5000);
            return null;
        });

        iaThread.start();
        assertTrue(iaThread.isAlive());

        // Interrumpimos
        iaThread.interrupt();
        iaThread.join(1000);

        assertFalse(iaThread.isAlive(), "El hilo debería terminar al ser interrumpido");
    }
}