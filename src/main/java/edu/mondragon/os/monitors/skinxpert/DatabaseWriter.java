package edu.mondragon.os.monitors.skinxpert;

import java.io.*;
import java.util.concurrent.BlockingQueue;

/**
 * DatabaseWriter
 * --------------
 * Hilo único para escritura en fichero.
 * Problema de sincronización: evitar corrupción concurrente en escritura.
 * Primitiva usada: BlockingQueue (paso de mensajes bloqueante).
 */
public class DatabaseWriter extends Thread {
    private final BlockingQueue<String> q;
    private final String filename;

    public DatabaseWriter(BlockingQueue<String> q, String filename) {
        super("DatabaseWriter");
        this.q = q;
        this.filename = filename;
    }

    @Override
    public void run() {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename, true))) {
            while (!interrupted()) {
                try {
                    // Consumir entrada de la cola (bloquea si vacía)
                    String entry = q.take();
                    out.println(entry);
                    out.flush();
                    System.out.println("[DB] " + entry);
                } catch (InterruptedException e) {
                    break; // salir ordenadamente
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
