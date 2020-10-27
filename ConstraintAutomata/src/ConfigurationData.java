
public class ConfigurationData {
	public final static String PROJECT_NAME = "TrafficLight";
	
	public final static String MESSAGES_FILE = "data/" + PROJECT_NAME + "/Messages.txt";
	
	public final static String FSM_FILE = 
			//"data/" + PROJECT_NAME +"/FSM_Model_ONLY_CONSTRAINT2.sm";
			"data/" + PROJECT_NAME +"/TrafficLight_Complete.sm";
	
	public final static String SCA_FILE = 
			"data/" + PROJECT_NAME +"/SCA.txt";
	
	public final static boolean LOAD_SCA=false;
	
	public final static String SEQ_FILE = 
			"data/Sequences.txt";
	
	public final static String LOG_FILE = 
			"data/logFile.txt";
	
	public final static String RESULTS_FILE = 
			"data/ResultFile_" + PROJECT_NAME;
	
	public final static int MAX_STATES = 150000;
	
	public final static boolean MONITORING_ENABLED=true;
	
	public final static boolean ALLOW_REPETITIONS_IN_COUPLES=true;
	
	public final static String ANY_CHAR = ".*";
	public final static boolean NOT_SECOND = false;
	
	// Sequence generation mode
	public final static Mode MODALITY = Mode.STATES_COVERAGE;
	// Reparation mode used when the constraints are not considered during the generation phase
	public final static ReparationMode REPAIR_MODALITY = ReparationMode.SKIP_ERROR;
	
	public final static boolean USE_MULTITHREAD = true;
	
	// Use BATCH_PROCESSING=TRUE with MULTITHREAD enabled to avoid GC Overhead
	public final static boolean BATCH_PROCESSING = true;
	public static int AUTOMATONS_PER_BATCH = 10;
	public final static int AUTOMATONS_PER_BATCH_MIN = 10;
	public final static int AUTOMATONS_PER_BATCH_MAX = 10;
	
	// Set LIMIT = 0 to use all the couples
	public final static int LIMIT = 0;
	
	// Set the strength of the test
	public final static Strength TEST_STRENGHT = Strength.PAIR_WISE;
	
	// Split the test sequence if Transition Coverage is used?
	public final static boolean SPLIT_SEQ = false;
	
	public final static String STARTING_STATE = 
			//"Off";
			"off__off__contr_off__blocked_a";
	
	public final static String RESET_MSG = 
			//"rx_abrt";
			"____turn_off";
}
