import java.util.List;

public class Zone {
    private final int id;
    private final int x1, x2, y1, y2;

    public Zone(int id, int x1, int y1, int x2, int y2) {
        this.id = id;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public int getId() {
        return id;
    }
    public List<List<Integer>> getCoords() {
        return List.of(List.of(x1, y1), List.of(x2, y2));
    }
}
