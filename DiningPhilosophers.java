import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DiningPhilosophers {
    // Constants
    private static final int NUM_PHILOSOPHERS = 5;
    private static final int THINKING_TIME = 10; // Max thinking time in seconds
    private static final int EATING_TIME = 5;    // Max eating time in seconds
    private static final int WAIT_TIME_FOR_FORK = 4; // Waiting time for the fork in seconds

    public static void main(String[] args) {
        Fork[][] forks = new Fork[6][NUM_PHILOSOPHERS];
        Philosopher[][] philosophers = new Philosopher[6][NUM_PHILOSOPHERS];

        // Initialize forks for each table (including the sixth table)
        for (int table = 0; table < 6; table++) {
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                forks[table][i] = new Fork();
            }
        }

        // Create philosophers for 5 tables
        for (int table = 0; table < 5; table++) {
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                int leftFork = i;
                int rightFork = (i + 1) % NUM_PHILOSOPHERS;
                philosophers[table][i] = new Philosopher(
                        "Philosopher " + (char) (table * NUM_PHILOSOPHERS + 'A' + i),
                        forks[table][leftFork], forks[table][rightFork], table);
                new Thread(philosophers[table][i]).start();
            }
        }

        // Monitoring thread to detect deadlocks
        //new Thread(() -> {
            while(true){
                for (int table = 0; table < 5; table++) {
                    if (checkDeadlock(philosophers[table])) {
                        System.out.println("Deadlock detected at table " + table + ". Moving a philosopher to the sixth table.");
                        movePhilosopherToSixthTable(philosophers, table, forks);
                        if (checkDeadlock(philosophers[5])) {
                            System.out.println("Sixth table has deadlocked! Simulation ends.");
                            System.exit(0);
                        }
                    }
                }
                try {
                    Thread.sleep(500); // Check for deadlocks periodically
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
       // }).start();
    }

    // Philosopher class
    static class Philosopher implements Runnable {
        private String name;
        private Fork leftFork;
        private Fork rightFork;
        private int tableId;
        private boolean isEating = false;

        public Philosopher(String name, Fork leftFork, Fork rightFork, int tableId) {
            this.name = name;
            this.leftFork = leftFork;
            this.rightFork = rightFork;
            this.tableId = tableId;
        }

        @Override
        public void run() {
            Random rand = new Random();
            try {
                int philosophers=0;
                while (true) {
                    // Thinking
                    philosophers++;
                    if(philosophers>=25) break;
                    System.out.println(name + " is thinking.");
                    Thread.sleep(rand.nextInt(THINKING_TIME * 1000));

                    // Hungry and trying to pick up forks
                    System.out.println(name + " is hungry and trying to pick up the left fork.");
                    if (leftFork.pickUp()) {
                        System.out.println(name + " picked up the left fork.");
                        Thread.sleep(WAIT_TIME_FOR_FORK * 1000);

                        // Try to pick up right fork
                        if (rightFork.pickUp()) {
                            System.out.println(name + " picked up the right fork. Now eating.");
                            isEating = true;
                            Thread.sleep(rand.nextInt(EATING_TIME * 1000));

                            // Finished eating, put down forks
                            rightFork.putDown();
                            leftFork.putDown();
                            System.out.println(name + " finished eating and put down both forks.");
                            isEating = false;
                        } else {
                            // Failed to pick up right fork, put down left fork
                            System.out.println(name + " could not pick up the right fork. Putting down left fork.");
                            leftFork.putDown();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean isEating() {
            return isEating;
        }
    }

    // Fork class using ReentrantLock
    static class Fork {
        private final Lock lock = new ReentrantLock();

        public boolean pickUp() {
            return lock.tryLock();
        }

        public void putDown() {
            lock.unlock();
        }
    }

    // Check if a table is in deadlock (i.e., all philosophers are waiting for forks)
    private static boolean checkDeadlock(Philosopher[] philosophers) {
        for (Philosopher philosopher : philosophers) {
            if (philosopher.isEating()) {
                return false; // Not in deadlock if any philosopher is eating
            }
        }
        return true; // All philosophers are not eating, so deadlock
    }

    // Move a random philosopher to the sixth table
    private static void movePhilosopherToSixthTable(Philosopher[][] philosophers, int deadlockedTable, Fork[][] forks) {
        Random rand = new Random();
        int philosopherIndex = rand.nextInt(NUM_PHILOSOPHERS);
        Philosopher philosopherToMove = philosophers[deadlockedTable][philosopherIndex];

        // Find an empty spot at the sixth table
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            if (philosophers[5][i] == null) {
                philosophers[5][i] = new Philosopher(
                        philosopherToMove.name, forks[5][i], forks[5][(i + 1) % NUM_PHILOSOPHERS], 5);
                new Thread(philosophers[5][i]).start();
                philosophers[deadlockedTable][philosopherIndex] = null;
                System.out.println(philosopherToMove.name + " moved to the sixth table.");
                break;
            }
        }
    }
}