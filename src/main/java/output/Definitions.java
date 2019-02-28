package output;

public class Definitions {
   // objective functions
   public static final String NUM_OF_SERVERS_OBJ = "num_of_servers";
   public static final String COSTS_OBJ = "costs";
   public static final String UTILIZATION_OBJ = "utilization";
   public static final String MAX_UTILIZATION_OBJ = "max_utilization";
   // models
   public static final String INITIAL_PLACEMENT = "init";
   public static final String MIGRATION = "mgr";
   public static final String REPLICATION = "rep";
   public static final String MIGRATION_REPLICATION = "mgrep";
   public static final String ALL_CASES = "all";
   public static final String REINFORCEMENT_LEARNING = "mgrep_rl";
   // logs and messages
   public static final String ERROR = "Error: ";
   public static final String INFO = "Info: ";
   public static final String READY = "ready";
   // objective variables
   public static final String kL = "kL";
   public static final String kX = "kX";
   public static final String uL = "uL";
   public static final String uX = "uX";
   public static final String uMax = "uMax";
   // elementary variables
   public static final String zSP = "zSP";
   public static final String zSPD = "zSPD";
   public static final String fXSV = "fXSV";
   public static final String fXSVD = "fXSVD";
   // additional variables
   public static final String fX = "fX";
   public static final String gSVXY = "gSVXY";
   public static final String sSVP = "hSVP";
   public static final String dSPD = "dSPD";
   public static final String ySVXD = "ySVXD";
   // parameters
   public static final String LOAD_FUNCTION = "load";
   public static final String OVERHEAD_FUNCTION = "overhead";
   public static final String SYNC_LOAD = "sync_load";
   public static final String REPLICABLE_FUNCTION = "replicable";
   public static final String PROCESS_DELAY = "process_delay";
   public static final String LINK_CAPACITY = "capacity";
   public static final String LINK_DELAY = "delay";
   public static final String MIN_PATHS = "min_paths";
   public static final String MAX_PATHS = "max_paths";
   public static final String FUNCTION_SERVER = "function_server";
   public static final String MAX_DELAY = "max_delay";
}
