package edu.mondragon.os.monitors.skinxpert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhotoTaskTest {

    @Test
    @DisplayName("Test de Inicialización: El constructor asigna valores y valores por defecto")
    void testConstructorAndDefaults() {
        int id = 101;
        String patient = "Patient_X";
        String clinic = "Clinic_Central";

        PhotoTask task = new PhotoTask(id, patient, clinic);

        // Verificamos asignaciones directas
        assertEquals(id, task.getId());
        assertEquals(patient, task.getPatientId());
        assertEquals(clinic, task.getClinicId());

        // Verificamos valores por defecto críticos
        assertEquals(PhotoTask.Urgency.LOW, task.getUrgency(), 
            "La urgencia por defecto debería ser LOW hasta que la IA diga lo contrario");
        
        assertNull(task.getDoctorId(), 
            "El doctor debería ser nulo al principio (se asigna en el buffer)");
    }

    @Test
    @DisplayName("Test de Mutación: Los setters modifican el estado correctamente")
    void testSetters() {
        PhotoTask task = new PhotoTask(1, "P1", "C1");

        // 1. Cambiar Doctor
        task.setDoctorId("Dr. House");
        assertEquals("Dr. House", task.getDoctorId());

        // 2. Cambiar Urgencia
        task.setUrgency(PhotoTask.Urgency.HIGH);
        assertEquals(PhotoTask.Urgency.HIGH, task.getUrgency());
        
        // Cambiar a otra urgencia
        task.setUrgency(PhotoTask.Urgency.MEDIUM);
        assertEquals(PhotoTask.Urgency.MEDIUM, task.getUrgency());
    }

    @Test
    @DisplayName("Test toString: Formato correcto para logs")
    void testToString() {
        PhotoTask task = new PhotoTask(55, "Juan", "ClinicaA");
        task.setDoctorId("Dra. Mendez");
        
        // Nota: Como no vi tu Enum completo, uso uno seguro del código que pasaste:
        task.setUrgency(PhotoTask.Urgency.HIGH);

        String result = task.toString();

        // Verificamos que la cadena contenga la info esencial
        assertTrue(result.contains("id=55"));
        assertTrue(result.contains("patient=Juan"));
        assertTrue(result.contains("doctor=Dra. Mendez"));
        assertTrue(result.contains("urgency=HIGH"));
    }
    
    @Test
    @DisplayName("Test Enum: Verificar valores disponibles")
    void testUrgencyEnum() {
        // Aseguramos que el Enum tiene los valores esperados por la IA
        PhotoTask.Urgency[] values = PhotoTask.Urgency.values();
        
        assertTrue(values.length >= 3, "Debería haber al menos LOW, MEDIUM, HIGH");
        
        assertNotNull(PhotoTask.Urgency.valueOf("LOW"));
        assertNotNull(PhotoTask.Urgency.valueOf("MEDIUM"));
        assertNotNull(PhotoTask.Urgency.valueOf("HIGH"));
    }
}