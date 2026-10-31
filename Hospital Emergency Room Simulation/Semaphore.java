//Semaphore
public class Semaphore{
    protected int permits = 0 ;
    private int wakeups = 0;
    public Semaphore(int val){
        if(val<0){
            throw new IllegalArgumentException("Initial value must be >= 0");
        }
        this.permits = val ;
    }
    public synchronized void acquire() {
        permits--;
        if (permits < 0) {
            do {
                try {
                    wait();
                } catch (InterruptedException e) { }
            } while (wakeups == 0);
            wakeups--;
        }
    }
    public synchronized void release(){
        permits++;
        if(permits<=0){
        wakeups++;    
        notify();
        }
    }
    public synchronized boolean tryAcquire(long timeoutMillis) {
        permits--;
        if (permits >= 0) {
            return true;   
        }        
 
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (wakeups == 0) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                value++;            
                return false;       
            }
            try {
                wait(remaining);
            } catch (InterruptedException e) { }
        }
        wakeups--;
        return true;
        
    }
}