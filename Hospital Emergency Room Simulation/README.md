# Hospital Emergency Room Simulation

**Course:** CS241/SCS241 – Operating Systems
**Assignment 1:** Hospital Emergency Room Simulation
**Faculty of Computing and Artificial Intelligence, Cairo University**


| Name | ID |
|------|------|
| Omar Ahmed | 20240360 |
| Adham Amr | 20240070 |
| Mohamed Tarek | 20240508 |
| Mohamed Atef | 20240513 |
| Mariam Ashraf | 20240572 |

---

## 1. Overview

This project simulates a busy hospital emergency room using Java threads and a **custom Semaphore class**. It is a classic **Producer–Consumer (Bounded Buffer)** problem:

- **Patients** arrive and try to enter a waiting room with limited capacity.
- **Doctors** wait until a patient is available, take one, and try to acquire one of **two treatment rooms**.
- If a doctor cannot get a treatment room within **3 seconds**, the patient is sent back to the waiting room.
- Otherwise, the treatment takes a random amount of time, after which the room is released and the patient leaves.

The waiting room and the treatment rooms are **separate shared resources**, each synchronized independently.

---

## 2. How to Compile and Run

```bash
javac *.java
java Hospital
```

The program asks for three values:

```
Enter waiting room capacity: 2
Enter number of doctors: 3
Enter number of patients: 5
```

All values must be greater than zero.

---

## 3. Project Structure

| File | Responsibility |
|------|----------------|
| `Hospital.java` | Reads input, creates the shared resources, doctors and patients, starts the threads, and waits for the simulation to finish. |
| `Semaphore.java` | Custom counting semaphore (blocking, no busy-waiting) with `acquire()`, `release()` and the timed `tryAcquire(timeout)`. |
| `Patient.java` | Patient thread: arrives, then tries to enter the waiting room. |
| `Doctor.java` | Doctor thread: takes a patient, tries to acquire a treatment room, treats the patient, releases the room. |
| `WaitingRoom.java` | Additional shared-resource class: the bounded buffer that holds waiting patients. |

---

## 4. Synchronization Design

### 4.1 Semaphore (`Semaphore.java`)

A counting semaphore implemented with `synchronized`, `wait()` and `notify()`, following the lab design:

- `acquire()` decrements the counter and blocks if it becomes negative.
- `release()` increments the counter and wakes one waiting thread if the counter is `<= 0`.
- A `wakeups` counter ensures a thread leaves the waiting state only when a real `release()` signaled it (protection against spurious wakeups).
- `tryAcquire(timeoutMillis)` is the custom method required by the assignment. It returns `true` if a permit is obtained within the time limit, and `false` otherwise (restoring the counter on timeout so no permit is lost).

### 4.2 Waiting Room = Bounded Buffer (`WaitingRoom.java`)

The waiting room is a bounded buffer (a queue) protected by three semaphores:

| Semaphore | Initial value | Purpose |
|-----------|---------------|---------|
| `empty` | `capacity` | Counts free seats |
| `full` | `0` | Counts patients currently waiting |
| `mutex` | `1` | Mutual exclusion on the queue |

**Producer side (`addPatient`)** – used by patients, and by doctors when they send a patient back:

```
empty.acquire()   // wait for a free seat (patient waits outside if none)
mutex.acquire()
queue.add(patient)
mutex.release()
full.release()    // signal that a patient is available
```

If no seat is free (`empty.tryAcquire(0)` fails), the patient prints that the waiting room is full and waits outside until a seat is released.

**Consumer side (`takePatient`)** – used by doctors:

```
full.acquire()    // doctor stays idle until a patient is available
mutex.acquire()
patient = queue.poll()
mutex.release()
empty.release()   // a seat became free
```

### 4.3 Treatment Rooms

The treatment rooms are an independent shared resource: a counting `Semaphore(2)` in `Hospital`, shared by all doctors.

- A doctor calls `tryAcquire(3000)` before treating.
- **Success:** the doctor treats the patient, then calls `release()` after treatment.
- **Timeout:** the doctor sends the patient back to the waiting room (`addPatient`) and becomes available again.

### 4.4 Termination

- Each doctor increments a shared `AtomicInteger treatedCount` after finishing a patient (the counter is thread-safe, so no race condition on it).
- `Hospital` first joins all patient threads, then checks `treatedCount` every 100 ms until it equals the number of patients. This check is used only to detect the end of the simulation; all access to the shared resources (waiting room and treatment rooms) is synchronized with semaphores.
- Then `WaitingRoom.close(numberOfDoctors)` releases `full` once per doctor. Each idle doctor wakes up, receives `null` from `takePatient()`, and exits its loop.
- Finally `Hospital` joins all doctor threads and prints the final messages.

---

## 5. Why There Is No Race Condition or Deadlock

**Race conditions**

- The waiting-room queue is accessed only inside `mutex.acquire()` / `mutex.release()` (released in a `finally` block).
- The treatment-room counter is managed only by the semaphore's `synchronized` methods.

**Deadlock**

- A doctor holding a treatment room never waits for anything except `Thread.sleep()`, so it always finishes and releases the room.
- A doctor waiting for a room gives up after 3 seconds, so no doctor waits forever.
- When a doctor sends a patient back and the waiting room is full, it may wait for a seat. This is safe: other doctors keep treating and taking patients from the queue, which frees seats.
- The `mutex` is never held while waiting on `empty` or `full`, so there is no circular wait between the semaphores.

---

## 6. Simulation Details

- Treatment time is random (between 2 and 5 seconds) using `ThreadLocalRandom`.
- The treatment-room timeout is fixed at 3 seconds (`ROOM_TIMEOUT_MS` in `Doctor.java`).
- Output messages cover every required event: arrival, entering the waiting room, waiting outside, doctor assignment, trying/acquiring a treatment room, beginning treatment, going back to the waiting room, finishing treatment, and leaving the hospital.
- Because the program is multithreaded, lines from different threads may interleave differently from run to run. This is expected.

---

## 7. Sample Run

**Input**

```
Enter waiting room capacity: 2
Enter number of doctors: 3
Enter number of patients: 5
```

**Output (one possible scenario)**

```
Patients arriving:
P1 P2 P3 P4 P5

P3 arrived.
P2 arrived.
P5 arrived.
P1 arrived.
P4 arrived.
The waiting room is full.
P4 is waiting outside for an available seat.
P3 entered the waiting room.
The waiting room is full.
P1 is waiting outside for an available seat.
P2 entered the waiting room.
The waiting room is full.
P5 is waiting outside for an available seat.
P4 entered the waiting room.
P1 entered the waiting room.
P5 entered the waiting room.

Doctor 1 is assigned to P3.

Doctor 3 is assigned to P4.
Doctor 1 is trying to acquire a treatment room.
Doctor 3 is trying to acquire a treatment room.
Doctor 1 acquired a treatment room.

Doctor 2 is assigned to P2.
Doctor 3 acquired a treatment room.
Doctor 1 begins treating P3.
Doctor 2 is trying to acquire a treatment room.
Doctor 3 begins treating P4.
Doctor 2 could not acquire a treatment room.
Doctor 2 is sending P2 back to the waiting room.
The waiting room is full.
P2 is waiting outside for an available seat.
Doctor 1 finished treating P3.
Doctor 1 released the treatment room.
Doctor 1 is now available.
P3 left.

Doctor 1 is assigned to P1.
Doctor 1 is trying to acquire a treatment room.
Doctor 1 acquired a treatment room.
P2 entered the waiting room.
Doctor 1 begins treating P1.
Doctor 2 is now available.

Doctor 2 is assigned to P5.
Doctor 2 is trying to acquire a treatment room.
Doctor 3 finished treating P4.
Doctor 3 released the treatment room.
Doctor 3 is now available.
P4 left.
Doctor 2 acquired a treatment room.
Doctor 2 begins treating P5.

Doctor 3 is assigned to P2.
Doctor 3 is trying to acquire a treatment room.
Doctor 1 finished treating P1.
Doctor 1 released the treatment room.
Doctor 1 is now available.
Doctor 3 acquired a treatment room.
Doctor 3 begins treating P2.
P1 left.
Doctor 2 finished treating P5.
Doctor 2 released the treatment room.
Doctor 2 is now available.
P5 left.
Doctor 3 finished treating P2.
Doctor 3 released the treatment room.
Doctor 3 is now available.
P2 left.

All patients have been treated.
Simulation completed.
```

---

## 8. Testing

The following cases were tested:

| Capacity | Doctors | Patients | Purpose |
|----------|---------|----------|---------|
| 2 | 3 | 5 | The sample from the assignment |
| 3 | 1 | 5 | A single doctor, patients waiting outside |
| 1 | 1 | 1 | Minimum values |
| 1 | 3 | 10 | Small waiting room, heavy contention |
| 2 | 5 | 10 | Many doctors competing for two rooms (treatment-room timeout and patient return) |

In all cases the simulation terminates with every patient treated and no deadlock.