package edu.mondragon.os.monitors.skinxpert;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * BoundedBuffer
 * -------------
 * Problema de sincronización: productor–consumidor con capacidad limitada.
 * Primitivas usadas: semáforos (spaces, items, mutex).
 * Garantiza ausencia de condiciones de carrera y saturación.
 */
public class BoundedBuffer {
    private final List<PhotoTask> list = new LinkedList<>();
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore items = new Semaphore(0);
    private final Semaphore spaces;

    public BoundedBuffer(int capacity) {
        this.spaces = new Semaphore(capacity);
    }

    // Productor: paciente sube foto
    public void add(PhotoTask t2) throws InterruptedException {
        spaces.acquire();        // bloquea si buffer lleno
        mutex.acquire();         // sección crítica
        try {
            list.add(t2);
            System.out.println("[BUFFER ADD] " + t2);
        } finally {
            mutex.release();
        }
        items.release();         // notifica que hay un elemento disponible
    }

    // Consumidor: IA procesa foto
    public PhotoTask remove() throws InterruptedException {
        items.acquire();         // bloquea si buffer vacío
        mutex.acquire();
        try {
            PhotoTask t = list.remove(0);
            System.out.println("[BUFFER REM] " + t);
            return t;
        } finally {
            mutex.release();
            spaces.release();    // libera espacio
        }
    }
}
