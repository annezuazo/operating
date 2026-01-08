package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DoctorServiceTest {

    private DoctorService doctorService;
    private PriorityQueueSync mockPriorityQueue;
    private BlockingQueue<String> dbQueue;

    @BeforeEach
    void setUp() {
        mockPriorityQueue = mock(PriorityQueueSync.class);
        dbQueue = new LinkedBlockingQueue<>();
        doctorService = new DoctorService("1", mockPriorityQueue, dbQueue);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (doctorService.isAlive()) {
            doctorService.interrupt();
            doctorService.join(1000);
        }
    }

    @Test
    @DisplayName("Test Diagnóstico: Consume tarea, espera y genera reporte")
    void testDoctorProcessTask() throws InterruptedException {
        // --- PREPARACIÓN ---
        PhotoTask mockTask = mock(PhotoTask.class);
        
        // CORRECCIÓN ID: Asumimos que es int por tu error anterior
        when(mockTask.getId()).thenReturn(999); 
        
        // --- CORRECCIÓN URGENCIA (EL ERROR ACTUAL) ---
        // Obtenemos el primer valor real del Enum Urgency (sea cual sea: LOW, HIGH...)
        // Si sabes el nombre, puedes poner PhotoTask.Urgency.HIGH en su lugar.
        PhotoTask.Urgency realUrgency = PhotoTask.Urgency.values()[0];
        
        when(mockTask.getUrgency()).thenReturn(realUrgency);
        
        // El toString() lo dejamos descriptivo
        when(mockTask.toString()).thenReturn("PhotoTask-999");

        // Configuramos la cola
        when(mockPriorityQueue.take())
            .thenReturn(mockTask)
            .thenThrow(new InterruptedException("Stop testing"));

        // --- EJECUCIÓN ---
        doctorService.start();

        // --- VERIFICACIÓN ---
        String result = dbQueue.poll(2, TimeUnit.SECONDS);

        assertNotNull(result, "El doctor debería haber generado un diagnóstico");
        
        // Construimos el string esperado usando la misma urgencia que recuperamos
        // Formato esperado: "[DIAG] 1 -> photo 999 urgency=NOMBRE_DEL_ENUM"
        String expectedSnippet = "[DIAG] 1 -> photo 999 urgency=" + realUrgency;
        
        assertEquals(expectedSnippet, result, "El mensaje de diagnóstico no coincide");

        verify(mockTask, atLeastOnce()).getId();
        verify(mockTask, atLeastOnce()).getUrgency();
    }

    @Test
    @DisplayName("Test Interrupción: El hilo debe detenerse limpiamente")
    void testInterruption() throws InterruptedException {
        when(mockPriorityQueue.take()).thenAnswer(invocation -> {
            Thread.sleep(5000); 
            return null;
        });

        doctorService.start();
        assertTrue(doctorService.isAlive());

        doctorService.interrupt();
        doctorService.join(1000);
        
        assertFalse(doctorService.isAlive());
    }
}