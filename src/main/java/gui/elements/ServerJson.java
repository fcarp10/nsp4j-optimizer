package gui.elements;

public class ServerJson extends NodeJson{

    public ServerJson(String id, int x, int y, String favecolor, String label) {
        int xS, yS;
        if (Integer.valueOf(id.split("_")[1]) % 2 == 0) {
            xS = x - 10;
            yS = y + 21;
        } else {
            xS = x + 10;
            yS = y + 21;
        }
        position = new Position(xS, yS);
        data = new Data(id, favecolor, label, "rectangle", 15, 20);
    }
}
