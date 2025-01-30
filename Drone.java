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
        //while (true) {
            /*try {
                //wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            //notify();
        //}
    }
    public void setAvailable(){
        scheduler.droneRequestWork(this, 0);
    }
    public void setJob(FireEvent fireEvent){
        System.out.println("running setjob");
        currentJob = fireEvent;
        System.out.println(fireEvent);
        try {
            //wait();
            Thread.sleep(2000);
            //notify();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        currentJob = null;
        System.out.println("completed fire event");
        setAvailable();
    }

}
