package edu.mondragon.os.monitors.skinxpert;

import java.util.*;
import java.util.concurrent.*;


/**
 * Main
 * ----
 * Orquesta la simulación completa:
 * Pacientes -> Buffer -> IA -> Médicos -> DB.
 * Demuestra comportamiento concurrente con trazas de ejecución.
 */
public class Main {
    public static void main(String[] args) throws Exception {

        // Buffer compartido con capacidad limitada
        BoundedBuffer clinicBuffer = new BoundedBuffer(20);

        // Cola de prioridad para médicos
        Map<String, PriorityQueueSync> doctorQueues = new HashMap<>();
        doctorQueues.put("doc1", new PriorityQueueSync());

        // Cola para escritura en DB
        BlockingQueue<String> dbQueue = new ArrayBlockingQueue<>(500);
        DatabaseWriter db = new DatabaseWriter(dbQueue, "log.txt");
        db.start();

        // IA clasifica fotos
        IAClassifier ia = new IAClassifier(clinicBuffer, doctorQueues);
        ia.start();

        // Médico atiende fotos
        DoctorService doctor = new DoctorService("doc1", doctorQueues.get("doc1"), dbQueue);
        doctor.start();

        // Pacientes suben fotos
        PatientUploader p1 = new PatientUploader("P001", clinicBuffer);
        PatientUploader p2 = new PatientUploader("P002", clinicBuffer);
        p1.start();
        p2.start();

        // Ejecutar simulación durante 20 segundos
        Thread.sleep(20000);

        // Interrumpir hilos ordenadamente
        p1.interrupt();
        p2.interrupt();
        ia.interrupt();
        doctor.interrupt();
        db.interrupt();

        // Esperar finalización
        p1.join();
        p2.join();
        ia.join();
        doctor.join();
        db.join();

        System.out.println("Simulation finished.");
    }
}
