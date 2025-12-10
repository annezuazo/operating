package edu.mondragon.os.monitors.skinxpert;

import java.util.*;
import java.util.concurrent.locks.*;

/**
 * UserStore
 * ---------
 * Problema de sincronización: múltiples lectores concurrentes y escritor exclusivo.
 * Primitiva usada: ReadWriteLock.
 * Admin escribe (alta de usuarios), médicos/pacientes leen en paralelo.
 */
public class UserStore {
    private final Map<String,String> patients = new HashMap<>();
    private final Map<String,String> doctors = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // ADMIN: alta paciente
    public void addPatient(String id, String info) {
        lock.writeLock().lock();
        try {
            patients.put(id, info);
            System.out.println("[ADMIN] Added patient: " + id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ADMIN: alta médico
    public void addDoctor(String id, String info) {
        lock.writeLock().lock();
        try {
            doctors.put(id, info);
            System.out.println("[ADMIN] Added doctor: " + id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Lecturas paralelas
    public String getPatient(String id) {
        lock.readLock().lock();
        try {
            return patients.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getDoctor(String id) {
        lock.readLock().lock();
        try {
            return doctors.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }
}
