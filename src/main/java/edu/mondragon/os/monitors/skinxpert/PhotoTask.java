package edu.mondragon.os.monitors.skinxpert;

/**
 * PhotoTask
 * ---------
 * Representa una foto subida por un paciente.
 * Contiene información de paciente, clínica, médico asignado y urgencia.
 * Usada en todo el pipeline concurrente.
 */
public class PhotoTask {
    public enum Urgency { LOW, MEDIUM, HIGH }

    private final int id;
    private final String patientId;
    private final String clinicId;
    private String doctorId;
    private Urgency urgency;
    private final long timestamp;

    public PhotoTask(int id, String patientId, String clinicId) {
        this.id = id;
        this.patientId = patientId;
        this.clinicId = clinicId;
        this.urgency = Urgency.LOW;
        this.timestamp = System.currentTimeMillis();
        this.doctorId = null; // se asignará más tarde
    }

    public int getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getClinicId() { return clinicId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String d) { doctorId = d; }
    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency u) { urgency = u; }

    @Override
    public String toString() {
        return "PhotoTask[id=" + id + ", patient=" + patientId +
               ", doctor=" + doctorId + ", urgency=" + urgency + "]";
    }
}
