package gui.elements;

public class ServerJson extends NodeJson{

    public ServerJson(String id, int x, int y, String favecolor, String label) {
        int xS, yS;
        if (Integer.valueOf(id.split("_")[1]) % 2 == 0) {
            xS = x - 11;
            yS = y + 26;
        } else {
            xS = x + 11;
            yS = y + 26;
        }
        position = new Position(xS, yS);
        data = new Data(id, favecolor, label, "rectangle", 20, 25);
    }
}
