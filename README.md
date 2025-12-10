# SkinXpert Operating Systems Simulation

## Descripción
Este proyecto implementa una **simulación multihilo** inspirada en el flujo clínico de la aplicación *SkinXpert*.  
El objetivo es modelar cómo pacientes suben fotos dermatológicas, una IA las clasifica por urgencia, los médicos las atienden en orden de prioridad y los diagnósticos se registran en un sistema seguro.

La simulación está diseñada para cumplir los requisitos del **Nivel 1 de la rúbrica de Operating Systems**:  
- Uso de primitivas de sincronización adecuadas.  
- Resolución de problemas concurrentes clásicos.  
- Solución funcional, libre de *deadlocks*.  
- Evidencia de comportamiento concurrente mediante trazas de ejecución.  

## Flujo de la simulación
1. **Pacientes (`PatientUploader`)**  
   - Generan fotos periódicamente con IDs únicos.  
   - Suben las fotos al buffer compartido.  

2. **Buffer limitado (`BoundedBuffer`)**  
   - Controla la capacidad máxima de fotos.  
   - Usa **semáforos** para implementar el patrón productor–consumidor.  

3. **IA (`IAClassifier`)**  
   - Consume fotos del buffer.  
   - Clasifica cada foto con una urgencia (HIGH, MEDIUM, LOW).  
   - Asigna un médico y envía la foto a su cola de prioridad.  

4. **Cola de prioridad (`PriorityQueueSync`)**  
   - Ordena las fotos según urgencia.  
   - Usa **Lock + Condition** para coordinar médicos y garantizar atención prioritaria.  

5. **Médicos (`DoctorService`)**  
   - Consumen fotos de su cola en orden de urgencia.  
   - Simulan diagnóstico y envían resultados a la cola de base de datos.  

6. **Escritor de base de datos (`DatabaseWriter`)**  
   - Hilo único que consume diagnósticos y los escribe en un fichero (`log.txt`).  
   - Usa **BlockingQueue** para evitar corrupción concurrente.  

7. **Gestión adicional**  
   - `AppointmentManager`: gestiona citas con exclusión mutua (`ReentrantLock`).  
   - `UserStore`: permite múltiples lecturas concurrentes y escrituras exclusivas (`ReadWriteLock`).  


## Problemas de sincronización resueltos
- **Productor–consumidor:** pacientes ↔ buffer ↔ IA.  
- **Exclusión mutua:** citas, contador de fotos, colas.  
- **Coordinación con prioridad:** médicos atienden primero urgencias altas.  
- **Lectores concurrentes/escritor exclusivo:** gestión de usuarios.  
- **Single-writer:** escritura en fichero serializada.  

---

## Primitivas utilizadas
- **Semáforos:** control de capacidad y disponibilidad en el buffer.  
- **Locks y condiciones:** exclusión mutua y coordinación en colas.  
- **ReadWriteLock:** múltiples lectores concurrentes, escritor exclusivo.  
- **BlockingQueue:** paso de mensajes bloqueante y seguro.  
- **Synchronized:** contador global de fotos.  

---

## Garantías
- **Deadlock-free:** cada estructura encapsula su propio lock, sin bloqueos anidados.  
- **No busy-wait:** todos los hilos esperan mediante primitivas bloqueantes.  
- **Correcto comportamiento concurrente:** demostrado con trazas de ejecución.  
