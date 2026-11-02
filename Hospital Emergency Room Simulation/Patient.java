public class Patient implements Runnable {
    private final int id;
    private final WaitingRoom waitingRoom;

    public Patient(int id, WaitingRoom waitingRoom) {
        this.id = id;
        this.waitingRoom = waitingRoom;
    }

    public int getId() {
        return id;
    }

    @Override
    public void run() {
        System.out.println("P" + id + " arrived.");
        waitingRoom.addPatient(this);
    }
}