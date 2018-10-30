package gui.elements;

public class LinkJson {

    private Data data;

    public LinkJson(String id, String source, String target, String label, String favecolor) {
        this.data = new Data(id, source, target, label, favecolor);
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public class Data {
        private String id;
        private String source;
        private String target;
        private String label;
        private String faveColor;

        public Data(String id, String source, String target, String label, String faveColor) {
            this.id = id;
            this.source = source;
            this.target = target;
            this.label = label;
            this.faveColor = faveColor;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getFaveColor() {
            return faveColor;
        }

        public void setFaveColor(String faveColor) {
            this.faveColor = faveColor;
        }
    }
}
