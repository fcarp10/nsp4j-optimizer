package optimizer;

public class Parameters {
   // objective functions
   public static final String NUM_SERVERS_OBJ = "num_servers";
   public static final String NUM_SERVERS_UTIL_COSTS_OBJ = "num_servers_util_costs";
   public static final String UTIL_COSTS_OBJ = "util_costs";
   public static final String UTIL_COSTS_MIGRATIONS_OBJ = "util_costs_migrations";
   public static final String UTIL_COSTS_MAX_UTIL_OBJ = "util_costs_max_utilization";
   public static final String UTILIZATION_OBJ = "utilization";
   public static final String OPER_COSTS_OBJ = "oper_costs";

   // scenarios
   public static final String SERVER_DIMENSIONING = "dimensioning";
   public static final String INITIAL_PLACEMENT = "init";
   public static final String PLACEMENT = "place";

   // general variables
   public static final String zSP = "zSP";
   public static final String zSPD = "zSPD";
   public static final String fX = "fX";
   public static final String fXSV = "fXSV";
   public static final String fXSVD = "fXSVD";
   public static final String uX = "uX";
   public static final String uL = "uL";

   // model specific variables
   public static final String xN = "xN";
   public static final String kL = "kL";
   public static final String kX = "kX";
   public static final String uMax = "uMax";
   public static final String oX = "oX";
   public static final String oSV = "oSV";
   public static final String hSVX = "hSVX";
   public static final String qSDP = "qSDP";

   // service delay variables
   public static final String dSVX = "dSVX";
   public static final String mS = "mS";
   public static final String dSVXD = "dSVXD";
   public static final String ySDP = "dSDP";

   // synchronization traffic variables
   public static final String gSVXY = "gSVXY";
   public static final String hSVP = "hSVP";

   // general constraints
   public static final String RP1 = "RP1";
   public static final String RP2 = "RP2";
   public static final String PF1 = "PF1";
   public static final String PF2 = "PF2";
   public static final String PF3 = "PF3";
   public static final String FD1 = "FD1";
   public static final String FD2 = "FD2";
   public static final String FD3 = "FD3";

   // additional constraints
   public static final String SYNC_TRAFFIC = "sync_traffic";
   public static final String MAX_SERV_DELAY = "max_serv_delay";
   public static final String CLOUD_ONLY = "cloud_only";
   public static final String EDGE_ONLY = "edge_only";
   public static final String SINGLE_PATH = "single_path";
   public static final String SET_INIT_PLC = "set_init_plc";

   // other constraints
   public static final String FORCE_SRC_DST = "force_src_dst";
   public static final String CONST_REP = "const_rep";

   // service parameters
   public static final String SERVICE_MIN_PATHS = "min_paths";
   public static final String SERVICE_MAX_PATHS = "max_paths";
   public static final String SERVICE_MAX_DELAY = "max_delay";

   // function parameters
   public static final String FUNCTION_REPLICABLE = "replicable";
   public static final String FUNCTION_LOAD_RATIO = "load_ratio";
   public static final String FUNCTION_OVERHEAD = "overhead";
   public static final String FUNCTION_SYNC_LOAD_RATIO = "sync_load";
   public static final String FUNCTION_PROCESS_TRAFFIC_DELAY = "process_traffic_delay";
   public static final String FUNCTION_MAX_CAP_SERVER = "max_cap_server";
   public static final String FUNCTION_MAX_DELAY = "max_delay";
   public static final String FUNCTION_MIN_PROCESS_DELAY = "min_process_delay";
   public static final String FUNCTION_PROCESS_DELAY = "process_delay";
   public static final String FUNCTION_MIGRATION_DELAY = "migration_delay";
   public static final String FUNCTION_OPEX = "opex";

   // node and server parameters
   public static final String NODE_CLOUD = "node_cloud";
   public static final String SERVER_DIMENSIONING_CAPACITY = "server_dimensioning_capacity";
   public static final String SERVER_IDLE_AVG_ENERGY_COST = "server_idle_avg_energy_cost";
   public static final String SERVER_UTIL_AVG_ENERGY_COST = "server_util_avg_energy_cost";
   public static final String SERVER_IDLE_MAX_ENERGY_COST = "server_idle_max_energy_cost";
   public static final String SERVER_UTIL_MAX_ENERGY_COST = "server_util_max_energy_cost";
   public static final String SERVER_OTHER_OPEX = "server_other_opex";
   public static final String OVERPROVISIONING_SERVER_CAPACITY = "overprovisioning_server_capacity";

   // link parameters
   public static final String LINK_CAPACITY = "capacity";
   public static final String LINK_DELAY = "delay";
   public static final String LINK_DISTANCE = "distance";
   public static final String LINK_CLOUD = "link_cloud";

   // Aux parameters
   public static final String INITIAL_TRAFFIC_LOAD = "initial_traffic_load";
   public static final String dSPD = "dSPD";
   public static final String QOS_PENALTY = "qos_penalty";
   public static final String LINKS_WEIGHT = "links_weight";
   public static final String SERVERS_WEIGHT = "servers_weight";
   public static final String MAXU_WEIGHT = "maxU_weight";

   // GUI parameters
   public static final String NODE_COLOR = "Gray";
   public static final String NODE_SHAPE = "ellipse";
   public static final String SERVER_COLOR = "Gray";
   public static final String SERVER_SHAPE = "rectangle";
   public static final String LINK_COLOR = "Gray";
   public static final String LINK_CLOUD_COLOR = "LightGray";
   public static final String X_SCALING = "x_scaling";
   public static final String Y_SCALING = "y_scaling";
   public static final int MAX_NUM_SERVERS = 24;
   public static final int PORT = 8080;

   // logs and messages
   public static final String ERROR = "ERROR - ";
   public static final String INFO = "INFO - ";
   public static final String WARNING = "WARN - ";
}
