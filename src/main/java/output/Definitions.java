package output;

public class Definitions {
   // objective functions
   public static final String NUM_SERVERS_OBJ = "num_servers";
   public static final String NUM_SERVERS_COSTS_OBJ = "num_servers_and_costs";
   public static final String COSTS_OBJ = "costs";
   public static final String UTILIZATION_OBJ = "utilization";
   public static final String MAX_UTILIZATION_OBJ = "max_utilization";
   // models
   public static final String SERVER_DIMENSIONING = "dimensioning";
   public static final String INITIAL_PLACEMENT = "init";
   public static final String MIGRATION = "mgr";
   public static final String REPLICATION = "rep";
   public static final String MIGRATION_REPLICATION = "mgrep";
   public static final String ALL_CASES = "all";
   public static final String REINFORCEMENT_LEARNING = "mgrep_rl";
   // logs and messages
   public static final String ERROR = "Error: ";
   public static final String INFO = "Info: ";
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
   public static final String xN = "xN";
   public static final String fX = "fX";
   public static final String gSVXY = "gSVXY";
   public static final String hSVP = "hSVP";
   public static final String dSPD = "dSPD";
   public static final String ySVXD = "ySVXD";
   public static final String mS = "mS";
   // general constraints
   public static final String RP1 = "RP1";
   public static final String RP2 = "RP2";
   public static final String PF1 = "PF1";
   public static final String PF2 = "PF2";
   public static final String FD1 = "FD1";
   public static final String FD2 = "FD2";
   public static final String FD3 = "FD3";
   // additional constraints
   public static final String ST = "ST";
   public static final String SD = "SD";
   // model specific constraints
   public static final String NUM_SERVERS = "num_servers";
   public static final String SINGLE_PATH = "single_path";
   public static final String FIX_INIT_PLC = "fix_init_plc";
   // extra constraints
   public static final String CONST_REP = "const_rep";
   public static final String FIX_SRC_DST = "fix_src_dst";
   // service parameters
   public static final String SERVICE_MIN_PATHS = "min_paths";
   public static final String SERVICE_MAX_PATHS = "max_paths";
   public static final String SERVICE_MAX_DELAY = "max_delay";
   public static final String FUNCTION_MIGRATION_DELAY = "migration_delay";
   // function parameters
   public static final String FUNCTION_REPLICABLE = "replicable";
   public static final String FUNCTION_LOAD_RATIO = "load_ratio";
   public static final String FUNCTION_OVERHEAD = "overhead";
   public static final String FUNCTION_SYNC_LOAD_RATIO = "sync_load";
   public static final String FUNCTION_PROCESS_DELAY = "process_delay";
   public static final String FUNCTION_MAX_LOAD = "max_load";
   // node and server parameters
   public static final String NODE_CLOUD = "node_cloud";
   // link parameters
   public static final String LINK_CAPACITY = "capacity";
   public static final String LINK_DELAY = "delay";
   public static final String LINK_DISTANCE = "distance";
   public static final String LINK_CLOUD = "link_cloud";
   // DRL parameters
   public static final String NUM_HIDDEN_LAYERS = "rl_num_hidden_layers";
   public static final String EPSILON = "epsilon";
   public static final String THRESHOLD = "threshold";
   public static final String LEARNING_STEPS = "rl_learning_steps";
   // Aux parameters
   public static final String SERVER_DIMENSIONING_CAPACITY = "server_dimensioning_capacity";
   public static final String OVERPROVISIONING_SERVER_CAPACITY = "overprovisioning_server_capacity";
}
