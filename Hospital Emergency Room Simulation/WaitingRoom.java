import java.util.LinkedList;
import java.util.Queue;

public class WaitingRoom {
    private final Queue<Patient> queue = new LinkedList<>();
    private final Semaphore empty;                      
    private final Semaphore full = new Semaphore(0);    
    private final Semaphore mutex = new Semaphore(1);   

    public WaitingRoom(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        this.empty = new Semaphore(capacity);
    }

    public void addPatient(Patient patient) {
        if (!empty.tryAcquire(0)) {
            System.out.println("The waiting room is full.\n"
                    + "P" + patient.getId() + " is waiting outside for an available seat.");
            empty.acquire();   
        }

        mutex.acquire();
        try {
            queue.add(patient);
            System.out.println("P" + patient.getId() + " entered the waiting room.");
        } finally {
            mutex.release();
        }
        full.release();
    }

    public Patient takePatient() {
        full.acquire();
        Patient p;
        mutex.acquire();
        try {
            p = queue.poll();
        } finally {
            mutex.release();
        }
        if (p != null) {
            empty.release();
        }
        return p;
    }

    public void close(int numberOfDoctors) {
        for (int i = 0; i < numberOfDoctors; i++) {
            full.release();
        }
    }
}