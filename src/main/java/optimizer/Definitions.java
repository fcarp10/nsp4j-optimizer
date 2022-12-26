package optimizer;

public class Definitions {
   // objective functions
   public static final String DIMEN_NUM_SERVERS = "DIMEN_NUM_SERVERS";
   public static final String DIMEN_LINK_CAP = "DIMEN_LINK_CAP";
   public static final String DIMEN_SERVER_CAP = "DIMEN_SERVER_CAP";
   public static final String NUM_SERVERS = "NUM_SERVERS";
   public static final String NUM_SERVERS_AND_UTIL_COSTS = "NUM_SERVERS_AND_UTIL_COSTS";
   public static final String UTIL_COSTS = "UTIL_COSTS";
   public static final String UTIL_COSTS_AND_MAX_UTIL = "UTIL_COSTS_AND_MAX_UTIL";
   public static final String UTILIZATION = "UTILIZATION";
   public static final String OPEX_SERVERS = "OPEX_SERVERS";
   public static final String FUNCTIONS_CHARGES = "FUNCTIONS_CHARGES";
   public static final String QOS_PENALTIES = "QOS_PENALTIES";
   public static final String ALL_MONETARY_COSTS = "ALL_MONETARY_COSTS";
   public static final String MGR = "MGR";
   public static final String REP = "REP";
   public static final String CLOUD = "CLOUD";
   public static final String MGR_REP = "MGR_REP";
   public static final String UTILIZATION_CLOUD = "UTILIZATION_CLOUD";
   public static final String NUM_SERVERS_CLOUD = "NUM_SERVERS_CLOUD";
   public static final String MGR_CLOUD = "MGR_CLOUD";
   public static final String REP_CLOUD = "REP_CLOUD";
   public static final String MGR_REP_CLOUD = "MGR_REP_CLOUD";

   // scenarios
   public static final String LP = "LP";
   public static final String FF = "FF";
   public static final String RF = "RF";
   public static final String GRD = "GRD";
   // scenarios journal
   public static final String JOURNAL_GRD_FIRST = "JOURNAL_GRD_FIRST";
   public static final String JOURNAL_LP_INIT = "JOURNAL_LP_INIT";
   public static final String JOURNAL_ALL = "JOURNAL_ALL";
   public static final String JOURNAL_ALL_SFC_LENGTH = "JOURNAL_ALL_SFC_LENGTH";
   public static final String JOURNAL_ALL_SERVER_CAP = "JOURNAL_ALL_SERVER_CAP";
   public static final String JOURNAL_HEU = "JOURNAL_HEU";
   public static final String JOURNAL_HEU_SFC_LENGTH = "JOURNAL_HEU_SFC_LENGTH";
   public static final String JOURNAL_HEU_SERVER_CAP = "JOURNAL_HEU_SERVER_CAP";

   public static final String OBSV_1 = "obsv1";
   public static final String OBSV_2 = "obsv2";
   public static final String PRED_2 = "pred2";
   public static final String OVER_2 = "over2";
   public static final String NULL = "";

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
   public static final String cLT = "cLT";
   public static final String cXT = "cXT";
   public static final String kL = "kL";
   public static final String kX = "kX";
   public static final String uMax = "uMax";
   public static final String oX = "oX";
   public static final String oSV = "oSV";
   public static final String qSDP = "qSDP";
   public static final String ySDP = "ySDP";

   // service delay variables
   public static final String dSVXD = "dSVXD";

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
   public static final String PATHS_SERVERS_CLOUD = "paths_servers_cloud";

   // other constraints
   public static final String FORCE_SRC_DST = "force_src_dst";
   public static final String CONST_REP = "const_rep";

   // service parameters
   public static final String SERVICE_MIN_PATHS = "min_paths";
   public static final String SERVICE_MAX_PATHS = "max_paths";
   public static final String SERVICE_DOWNTIME = "downtime";
   public static final String SERVICE_LENGTH = "service_length";
   public static final String SERVICES = "services";

   // function parameters
   public static final String FUNCTION_REPLICABLE = "replicable";
   public static final String FUNCTION_LOAD_RATIO = "load_ratio";
   public static final String FUNCTION_OVERHEAD_RATIO = "overhead_ratio";
   public static final String FUNCTION_SYNC_LOAD_RATIO = "sync_load";
   public static final String FUNCTION_PROCESS_TRAFFIC_DELAY = "process_traffic_delay";
   public static final String FUNCTION_MAX_DEM = "max_dem";
   public static final String FUNCTION_MAX_BW = "max_bw";
   public static final String FUNCTION_MAX_DELAY = "max_delay";
   public static final String FUNCTION_MIN_PROCESS_DELAY = "min_process_delay";
   public static final String FUNCTION_PROCESS_DELAY = "process_delay";
   public static final String FUNCTION_CHARGES = "charges";

   // node and server parameters
   public static final String NODE_CLOUD = "node_cloud";
   public static final String SERVER_DIMENSIONING_CAPACITY = "server_dimensioning_capacity";
   public static final String OVERPROVISIONING_NUM_SERVERS = "overprovisioning_num_servers";
   public static final String OVERPROVISIONING_SERVER_CAPACITY = "overprovisioning_server_capacity";
   public static final String SERVER_IDLE_ENERGY_COST = "server_idle_energy_cost";
   public static final String SERVER_UTIL_ENERGY_COST = "server_util_energy_cost";
   public static final String LONGITUDE_LABEL_1 = "Longitude";
   public static final String LATITUDE_LABEL_1 = "Latitude";
   public static final String LONGITUDE_LABEL_2 = "x";
   public static final String LATITUDE_LABEL_2 = "y";
   public static final String CLOUD_SERVER_CAPACITY = "cloud_server_capacity";
   public static final String SERVER_CAPACITY = "server_capacity";
   public static final String NODE_NUM_SERVERS = "num_servers_node";
   public static final String CLOUD_NUM_SERVERS = "cloud_num_servers";

   // link parameters
   public static final String LINK_CAPACITY = "capacity";
   public static final String OVERPROVISIONING_LINK_CAPACITY = "overprovisioning_link_capacity";
   public static final String LINK_DELAY = "delay";
   public static final String LINK_DISTANCE = "distance";
   public static final String LINK_CLOUD = "link_cloud";
   public static final String CLOUD_LINK_CAPACITY = "cloud_link_capacity";
   public static final String LINK_CAPACITY_DEFAULT = "link_capacity";

   // Aux parameters
   public static final String INITIAL_TRAFFIC_LOAD = "initial_traffic_load";
   public static final String dSPD = "dSPD";
   public static final String QOS_PENALTY_RATIO = "qos_penalty_ratio";
   public static final String LINKS_WEIGHT = "links_weight";
   public static final String SERVERS_WEIGHT = "servers_weight";
   public static final String MAXU_WEIGHT = "maxU_weight";
   public static final String DIRECTED_EDGES = "directed_edges";
   public static final String ALL_NODES_TO_CLOUD = "all_nodes_to_cloud";
   public static final String SCENARIOS_PATH = "scenarios";
   public static final String LINK_CAPACITY_TYPES = "link_capacity_types";
   public static final String SERVER_CAPACITY_TYPES = "server_capacity_types";

   // GUI parameters
   public static final String NODE_COLOR = "Black";
   public static final String NODE_SHAPE = "ellipse";
   public static final String SERVER_COLOR = "Black";
   public static final String SERVER_SHAPE = "rectangle";
   public static final String LINK_COLOR = "Black";
   public static final String LINK_CLOUD_COLOR = "LightGray";
   public static final String X_SCALING = "x_scaling";
   public static final String Y_SCALING = "y_scaling";
   public static final int MAX_NUM_SERVERS = 24;
   public static final int PORT = 8082;

   // DRL parameters
   public static final int NUM_HIDDEN_LAYERS = 150;
   public static final int MEMORY_CAPACITY = 100000;
   public static final float DISCOUNT_FACTOR = 0.1f;
   public static final int BATCH_SIZE = 10;
   public static final int START_SIZE = 10;
   public static final int FREQUENCY = 100;
   public static final String ROUTING_MAX_REPETITIONS = "";
   public static final String PLACEMENT_MAX_REPETITIONS = "";
   public static final String ROUTING_EPSILON_DECREMENT = "";
   public static final String EPSILON_STEPPER = "epsilon_stepper";
   public static final String ROUTING_DRL_CONF_FILE = "routing_drl_conf";
   public static final String PLACEMENT_DRL_CONF_FILE = "placement_drl_conf";

   // logs and messages
   public static final String ERROR = "ERROR - ";
   public static final String INFO = "INFO - ";
   public static final String WARNING = "WARN - ";

}
