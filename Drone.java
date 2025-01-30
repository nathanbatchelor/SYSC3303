public class Drone implements Runnable {
    boolean available;
    Scheduler scheduler;
    FireEvent currentJob;

    public Drone(Scheduler scheduler){
        available=false;
        this.scheduler=scheduler;
    }
    @Override
    public void run() {
        setAvailable();
        while (true) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void setAvailable(){
        scheduler.droneRequestWork(this);
    }
    public void setJob(FireEvent fireEvent){
        currentJob = fireEvent;
        System.out.println(fireEvent);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        currentJob = null;
        System.out.println("completed fire event");
        setAvailable();
    }

}
