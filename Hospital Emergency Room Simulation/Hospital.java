import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class Hospital {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter waiting room capacity: ");
        int capacity = input.nextInt();
        System.out.print("Enter number of doctors: ");
        int numberOfDoctors = input.nextInt();
        System.out.print("Enter number of patients: ");
        int numberOfPatients = input.nextInt();
        if (capacity <= 0 || numberOfDoctors <= 0 || numberOfPatients <= 0) {
            System.out.println("All values must be greater than zero.");
            input.close();
            return;
        }

        System.out.println("Patients arriving:");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= numberOfPatients; i++)
            sb.append("P").append(i).append(" ");
        System.out.println(sb.toString().trim());
        System.out.print("\n");

        WaitingRoom waitingRoom = new WaitingRoom(capacity);
        Semaphore treatmentRooms = new Semaphore(2);
        AtomicInteger treatedCount = new AtomicInteger(0);
        Thread[] patientThreads = new Thread[numberOfPatients];
        Thread[] doctorThreads = new Thread[numberOfDoctors];

        for (int i = 0; i < numberOfDoctors; i++) {
            Doctor doctor = new Doctor(
                    i + 1,
                    waitingRoom,
                    treatmentRooms,
                    treatedCount,
                    numberOfPatients,
                    numberOfDoctors
            );

            doctorThreads[i] = new Thread(doctor);
            doctorThreads[i].start();
        }
        for (int i = 0; i < numberOfPatients; i++) {
            Patient patient = new Patient(i + 1, waitingRoom);
            patientThreads[i] = new Thread(patient);
            patientThreads[i].start();
        }

        try {
            for (Thread patientThread : patientThreads) {
                patientThread.join();
            }
            while (treatedCount.get() < numberOfPatients) {
                Thread.sleep(100);
            }
            waitingRoom.close(numberOfDoctors);
            for (Thread doctorThread : doctorThreads) {
                doctorThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Hospital simulation interrupted.");
        }
        System.out.println("\nAll patients have been treated.");
        System.out.println("Simulation completed.");
        input.close();
    }
}
