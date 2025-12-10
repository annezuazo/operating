package edu.mondragon.os.monitors.skinxpert;

import java.util.*;
import java.util.concurrent.locks.*;

/**
 * PriorityQueueSync
 * -----------------
 * Problema de sincronización: coordinación de hilos con prioridad.
 * Primitiva usada: Lock + Condition.
 * Garantiza que los médicos atienden primero las fotos más urgentes.
 */
public class PriorityQueueSync {
    private final PriorityQueue<PhotoTask> pq;
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public PriorityQueueSync() {
        pq = new PriorityQueue<>((a, b) ->
            b.getUrgency().ordinal() - a.getUrgency().ordinal()
        );
    }

    // Añadir tarea a la cola
    public void add(PhotoTask t) {
        lock.lock();
        try {
            pq.offer(t);
            System.out.println("[PRIO ADD] " + t);
            notEmpty.signal(); // despierta consumidores
        } finally {
            lock.unlock();
        }
    }

    // Tomar tarea (bloquea si cola vacía)
    public PhotoTask take() throws InterruptedException {
        lock.lock();
        try {
            while (pq.isEmpty()) notEmpty.await();
            PhotoTask t = pq.poll();
            System.out.println("[PRIO REM] " + t);
            return t;
        } finally {
            lock.unlock();
        }
    }

    // Snapshot de la cola (lectura segura)
    public List<PhotoTask> snapshot() {
        lock.lock();
        try {
            return new ArrayList<>(pq);
        } finally {
            lock.unlock();
        }
    }
}
