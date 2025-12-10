package edu.mondragon.os.monitors.skinxpert;

import java.util.*;
import java.util.concurrent.locks.*;

/**
 * AppointmentManager
 * ------------------
 * Gestiona citas médicas con exclusión mutua.
 * Problema de sincronización: acceso concurrente a la lista de citas.
 * Primitiva usada: ReentrantLock para evitar condiciones de carrera.
 */
public class AppointmentManager {
    private final List<String> appointments = new ArrayList<>();
    private final Lock lock = new ReentrantLock();

    // Crear cita (sección crítica protegida por lock)
    public void create(String info) {
        lock.lock();
        try {
            appointments.add(info);
            System.out.println("[APPT] Created: " + info);
        } finally {
            lock.unlock();
        }
    }

    // Eliminar cita
    public void remove(String info) {
        lock.lock();
        try {
            appointments.remove(info);
            System.out.println("[APPT] Removed: " + info);
        } finally {
            lock.unlock();
        }
    }

    // Listar citas (devuelve copia segura)
    public List<String> list() {
        lock.lock();
        try {
            return new ArrayList<>(appointments);
        } finally {
            lock.unlock();
        }
    }
}
