package utils;

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
        private String favecolor;

        public Data(String id, String source, String target, String label, String favecolor) {
            this.id = id;
            this.source = source;
            this.target = target;
            this.label = label;
            this.favecolor = favecolor;
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

        public String getFavecolor() {
            return favecolor;
        }

        public void setFavecolor(String favecolor) {
            this.favecolor = favecolor;
        }
    }
}
