package edu.mondragon.os.monitors.skinxpert;

import java.util.Random;

/**
 * PatientUploader
 * ---------------
 * Simula que un paciente sube fotos periódicamente.
 * Problema de sincronización: contador global de fotos.
 * Primitiva usada: synchronized para incrementar ID de forma segura.
 */
public class PatientUploader extends Thread {
    private final String patientId;
    private final BoundedBuffer buffer;
    private final Random rand = new Random();
    private static int counter = 0;

    public PatientUploader(String id, BoundedBuffer b) {
        super("Patient-" + id);
        this.patientId = id;
        this.buffer = b;
    }

    @Override
    public void run() {
        try {
            while (!interrupted()) {
                Thread.sleep(400 + rand.nextInt(600));
                int photoId;

                // Sección crítica: contador global
                synchronized(PatientUploader.class) {
                    photoId = ++counter;
                }

                PhotoTask t = new PhotoTask(photoId, patientId, "clinicA");
                System.out.println(getName() + " uploading " + t);

                buffer.add(t);
            }
        } catch (InterruptedException e) {
            // salir silenciosamente
        }
    }
}
