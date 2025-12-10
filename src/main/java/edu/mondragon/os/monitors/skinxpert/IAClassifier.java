package edu.mondragon.os.monitors.skinxpert;

import java.util.*;

/**
 * IAClassifier
 * ------------
 * Consume fotos del buffer, asigna urgencia y las envía a la cola del médico.
 * Problema de sincronización: coordinación multi-stage (buffer → IA → cola de prioridad).
 * Primitivas usadas: semáforos en buffer, locks en cola de prioridad.
 */
public class IAClassifier extends Thread {
    private final BoundedBuffer buffer;
    private final Map<String, PriorityQueueSync> doctorQueues;
    private final Random rand = new Random();

    public IAClassifier(BoundedBuffer buffer, Map<String, PriorityQueueSync> doctorQueues) {
        super("IAClassifier");
        this.buffer = buffer;
        this.doctorQueues = doctorQueues;
    }

    private PhotoTask.Urgency classify() {
        int r = rand.nextInt(100);
        if (r < 20) return PhotoTask.Urgency.HIGH;
        else if (r < 60) return PhotoTask.Urgency.MEDIUM;
        else return PhotoTask.Urgency.LOW;
    }

    @Override
    public void run() {
        try {
            while (!interrupted()) {
                PhotoTask t = buffer.remove();

                // Asignación simplificada de médico
                if (t.getDoctorId() == null) {
                    t.setDoctorId("doc1");
                }

                // Clasificación IA
                t.setUrgency(classify());
                System.out.println("[IA] Classified: " + t);

                // Enviar a cola del médico
                doctorQueues.get(t.getDoctorId()).add(t);

                Thread.sleep(300);
            }
        } catch (InterruptedException e) {
            // salir silenciosamente
        }
    }
}
