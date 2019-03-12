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
   public static final String hSVP = "hSVP";
   public static final String dSPD = "dSPD";
   public static final String ySVXD = "ySVXD";
   public static final String nXSV = "nXSV";
   // general constraints
   public static final String RPC1 = "RPC1";
   public static final String RPC2 = "RPC2";
   public static final String PFC1 = "PFC1";
   public static final String PFC2 = "PFC2";
   public static final String FDC1 = "FDC1";
   public static final String FDC2 = "FDC2";
   public static final String FDC3 = "FDC3";
   public static final String FDC4 = "FDC4";
   // model specific constraints
   public static final String IPC = "IPC";
   public static final String IPMGRC = "IPMGRC";
   public static final String REPC = "REPC";
   // extra constraints
   public static final String RC = "RC";
   public static final String FXC = "FXC";
   public static final String SDC = "SDC";
   public static final String DIC1 = "DIC1";
   public static final String DVC1 = "DVC1";
   public static final String DVC2 = "DVC2";
   public static final String DVC3 = "DVC3";
   // service parameters
   public static final String SERVICE_MIN_PATHS = "min_paths";
   public static final String SERVICE_MAX_PATHS = "max_paths";
   public static final String SERVICE_FUNCTIONS_PER_SERVER = "functions_per_server";
   public static final String SERVICE_MAX_DELAY = "max_delay";
   // function parameters
   public static final String FUNCTION_REPLICABLE = "replicable";
   public static final String FUNCTION_LOAD_RATIO = "load_ratio";
   public static final String FUNCTION_OVERHEAD = "overhead";
   public static final String FUNCTION_SYNC_LOAD_RATIO = "sync_load";
   public static final String FUNCTION_PROCESS_DELAY = "process_delay";
   public static final String FUNCTION_MAX_DELAY = "max_delay";
   public static final String FUNCTION_MAX_INSTANCES = "max_instances";
   public static final String FUNCTION_MAX_LOAD = "max_load";
   // link parameters
   public static final String LINK_CAPACITY = "capacity";
   public static final String LINK_DELAY = "delay";
}
