import java.io.*;

/**
 * The FireIncidentSubsystem class will help read input file, create FireEvent and parse information from the file line by line and add teh FireEvent to the
 * scheduler queue.
 *
 * @author Anique
 * @author Jasjot
 *
 * @version 1.0 (Iteration 1), 29th January 2025
 *
 */

public class FireIncidentSubsystem implements Runnable {

    private final Scheduler sch;
    private final String input;

    public FireIncidentSubsystem(Scheduler sch, String input) {
        this.sch = sch;
        this.input = input;
    }

    /**
     * This method reads the input file and adds the event to the scheduler.
     *
     */
    @Override
    public void run() {
        // temp variable used to skip header of csv file
        int temp = 0;

        try{
            //read in the input file using BufferReader
            BufferedReader reader= new BufferedReader(new FileReader(input));
            String line;

            //while next line of csv is not empty continue parsing
            while((line = reader.readLine()) != null) {
                if (temp != 0) {
                    //create fire event data structure and use scheduler addEvent to add the event
                    FireEvent fireEvent = parseEvent(line);
                    System.out.println("FireIncidentSubsystem new Event: " + fireEvent);
                    sch.addFireEvent(fireEvent);
                    Thread.sleep(800);
                }
                temp++;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     *
     * @param line of the csv file being parsed
     * @return a new FireEvent data structure
     */
    private FireEvent parseEvent(String line) {
        //split line of csv file and create attributes of FireEvent data structure
        String[] slices = line.split(",");
        String time = slices[0];
        int zoneId = Integer.parseInt(slices[1]);
        String eventType = slices[2];
        String severity = slices[3];
        return new FireEvent(time, zoneId, eventType, severity);
    }
}
