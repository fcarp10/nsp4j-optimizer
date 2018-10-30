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
    public static final String ALL_OPT_MODELS = "all_optimization_models";
    public static final String MIGRATION_REPLICATION_RL_MODEL = "migration_replication_rl";

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

    static double avg(List<Double> utilizationResults) {
        double tmpU = 0;
        for (Double utilizationResult : utilizationResults) tmpU += utilizationResult;
        tmpU = tmpU / utilizationResults.size();
        return roundDouble(tmpU, 2);
    }

    static double vrc(List<Double> utilizationResults, double avg) {
        double variance = 0;
        for (Double utilizationResult : utilizationResults)
            variance += Math.pow(utilizationResult - avg, 2);
        variance = variance / utilizationResults.size();
        return roundDouble(variance, 2);
    }

    public static double max(List<Double> utilizationResults) {
        double max = 0;
        for (Double utilizationResult : utilizationResults)
            if (utilizationResult > max)
                max = utilizationResult;
        return roundDouble(max, 2);
    }

    public static double min(List<Double> utilizationResults) {
        double min = Double.MAX_VALUE;
        for (Double utilizationResult : utilizationResults)
            if (utilizationResult < min)
                min = utilizationResult;
        return roundDouble(min, 2);
    }

    static double avgF(List<Integer> results) {
        double tmp = 0;
        for (Integer i : results) tmp += i;
        tmp = tmp / results.size();
        return roundDouble(tmp, 2);
    }

    static double vrcF(List<Integer> results, double avg) {
        double variance = 0;
        for (Integer i : results)
            variance += Math.pow(i - avg, 2);
        variance = variance / results.size();
        return roundDouble(variance, 2);
    }

    static int maxF(List<Integer> results) {
        int max = 0;
        for (Integer i : results)
            if (i > max)
                max = i;
        return max;
    }

    static int minF(List<Integer> results) {
        int min = Integer.MAX_VALUE;
        for (Integer i : results)
            if (i < min)
                min = i;
        return min;
    }

    static double roundDouble(double value, int decimals) {
        return DoubleRounder.round(value, decimals);
    }
}
