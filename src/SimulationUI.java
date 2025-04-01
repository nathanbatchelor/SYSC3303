import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class SimulationUI extends JFrame{
    private SimulationPanel panel;

    public SimulationUI(){
        setTitle("Fire Incident Simulation");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        panel = new SimulationPanel();
        add(panel);
        setVisible(true);
    }

    public void updateState(List<DroneSubsystem> drones, List<FireIncidentSubsystem> fireIncidents){
        panel.updateData(drones, fireIncidents);
    }

    private static class SimulationPanel extends JPanel{
        private List<DroneSubsystem> drones = new CopyOnWriteArrayList<>();
        private List<FireIncidentSubsystem> fireIncidents = new CopyOnWriteArrayList<>();

        public void updateData(List<DroneSubsystem> drones, List<FireIncidentSubsystem> fireIncidents){
            this.drones = drones;
            this.fireIncidents = fireIncidents;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            g.setColor(Color.RED);

            for(DroneSubsystem drone : drones){
                continue;
            }

            for(FireIncidentSubsystem fireIncident : fireIncidents){
                continue;
            }
        }
    }
}
