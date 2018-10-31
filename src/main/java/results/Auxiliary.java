package results;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lp.CostFunctions;
import org.decimal4j.util.DoubleRounder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Auxiliary {

    public static final int OFFSET = 0;
    public static CostFunctions costFunctions;
    public static final String NUM_OF_SERVERS_OBJ = "num_of_servers";
    public static final String COSTS_OBJ = "costs";
    public static final String UTILIZATION_OBJ = "utilization";
    public static final String INITIAL_PLACEMENT_MODEL = "initial_placement";
    public static final String MIGRATION_MODEL = "migration";
    public static final String REPLICATION_MODEL = "replication";
    public static final String MIGRATION_REPLICATION_MODEL = "migration_replication";
    public static final String[] ALL_OPT_MODELS = new String[]{INITIAL_PLACEMENT_MODEL, MIGRATION_MODEL, REPLICATION_MODEL, MIGRATION_REPLICATION_MODEL};
    public static final String ALL_OPT_MODELS_STRING = "all_optimization_models";
    public static final String MIGRATION_REPLICATION_RL_MODEL = "migration_replication_rl";
    public static final String ERROR = "Error: ";
    public static final String INFO = "Info: ";

    public Auxiliary() {
        TypeReference<CostFunctions> typeReference = new TypeReference<>() {
        };
        InputStream inputStream = TypeReference.class.getResourceAsStream("/aux_files/linear-cost-functions.yml");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            costFunctions = mapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static double avg(List<Object> list) {
        if (list.size() == 0) return 0;
        double average = 0;
        for (Object aList : list) average += Double.valueOf(aList.toString());
        average = average / list.size();
        return roundDouble(average, 2);
    }

    static double vrc(List<Object> list, double avg) {
        if (list.size() == 0) return 0;
        double variance = 0;
        for (Object aList : list) variance += Math.pow(Double.valueOf(aList.toString()) - avg, 2);
        variance = variance / list.size();
        return roundDouble(variance, 2);
    }

    public static double max(List<Object> list) {
        if (list.size() == 0) return 0;
        double max = 0;
        for (Object aList : list)
            if (Double.valueOf(aList.toString()) > max)
                max = Double.valueOf(aList.toString());
        return roundDouble(max, 2);
    }

    public static double min(List<Object> list) {
        if (list.size() == 0) return 0;
        double min = Double.MAX_VALUE;
        for (Object aList : list)
            if (Double.valueOf(aList.toString()) < min)
                min = Double.valueOf(aList.toString());
        return roundDouble(min, 2);
    }

    static double roundDouble(double value, int decimals) {
        return DoubleRounder.round(value, decimals);
    }
}
