package edu.mondragon.os.monitors.skinxpert;

import java.util.concurrent.BlockingQueue;
import java.util.Random;

/**
 * DoctorService
 * -------------
 * Simula la atención de un médico.
 * Problema de sincronización: consumo ordenado de tareas con prioridad.
 * Primitivas usadas: PriorityQueueSync (Lock + Condition) y BlockingQueue para paso seguro de diagnósticos.
 */
public class DoctorService extends Thread {
    private final String doctorId;
    private final PriorityQueueSync queue;
    private final BlockingQueue<String> dbQueue;
    private final Random rand = new Random();

    public DoctorService(String id, PriorityQueueSync q, BlockingQueue<String> db) {
        super("Doctor-" + id);
        this.doctorId = id;
        this.queue = q;
        this.dbQueue = db;
    }

    @Override
    public void run() {
        try {
            while (!interrupted()) {
                // Consumir tarea en orden de urgencia
                PhotoTask t = queue.take();
                System.out.println(getName() + " attending " + t);

                // Simular tiempo de diagnóstico
                Thread.sleep(400 + rand.nextInt(300));

                // Generar diagnóstico y enviarlo a la cola de DB
                String diag = "[DIAG] " + doctorId + " -> photo " +
                              t.getId() + " urgency=" + t.getUrgency();

                dbQueue.put(diag);
            }
        } catch (InterruptedException e) {
            // salir silenciosamente
        }
    }
}
