package gui;


public class JsonNode {

    private Position position;
    private Data data;

    public JsonNode(String id, int x, int y, String favecolor, String label){
        position = new Position(x, y);
        data = new Data(id, favecolor, label);
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    private class Position{
        private int x;
        private int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }

    public class Data{
        private String id;
        private String favecolor;
        private String label;

        public Data(String id, String favecolor, String label){
            this.id = id;
            this. favecolor = favecolor;
            this. label = label;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFavecolor() {
            return favecolor;
        }

        public void setFavecolor(String favecolor) {
            this.favecolor = favecolor;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

}
