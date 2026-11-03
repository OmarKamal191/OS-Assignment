import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Doctor implements Runnable {
    private static final long ROOM_TIMEOUT_MS = 3000;

    private final int id;
    private final WaitingRoom waitingRoom;
    private final Semaphore treatmentRooms;
    private final AtomicInteger treatedCount;
    private final int totalPatients;
    private final int numberOfDoctors;

    public Doctor(int id, WaitingRoom waitingRoom, Semaphore treatmentRooms,
                  AtomicInteger treatedCount, int totalPatients, int numberOfDoctors) {
        this.id = id;
        this.waitingRoom = waitingRoom;
        this.treatmentRooms = treatmentRooms;
        this.treatedCount = treatedCount;
        this.totalPatients = totalPatients;
        this.numberOfDoctors = numberOfDoctors;
    }

    @Override
    public void run() {
        while (true) {
            Patient patient = waitingRoom.takePatient();
            if (patient == null) {
                return;
            }

            System.out.println("Doctor " + id + " is assigned to P" + patient.getId() + ".");
            System.out.println("Doctor " + id + " is trying to acquire a treatment room.");
            boolean acquired = treatmentRooms.tryAcquire(ROOM_TIMEOUT_MS);

            if (!acquired) {
                System.out.println("Doctor " + id + " could not acquire a treatment room.");
                System.out.println("Doctor " + id + " is sending P" + patient.getId()
                        + " back to the waiting room.");

                Thread t = new Thread(() -> waitingRoom.addPatient(patient));
                t.start();

                System.out.println("Doctor " + id + " is now available.");
                continue;
            }

            System.out.println("Doctor " + id + " acquired a treatment room.");
            System.out.println("Doctor " + id + " begins treating P" + patient.getId() + ".");

            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3001));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("Doctor " + id + " finished treating P" + patient.getId() + ".");
            treatmentRooms.release();
            System.out.println("Doctor " + id + " released the treatment room.");

            System.out.println("Doctor " + id + " is now available.");
            System.out.println("P" + patient.getId() + " left.");

            if (treatedCount.incrementAndGet() == totalPatients) {
                waitingRoom.close(numberOfDoctors);
            }
        }
    }
}