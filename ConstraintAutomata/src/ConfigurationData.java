
public class ConfigurationData {
	public final static String PROJECT_NAME = "Vault";
	
	public final static String MESSAGES_FILE = "data/" + PROJECT_NAME + "/Messages.txt";
	
	public final static String FSM_FILE = 
			//"data/" + PROJECT_NAME +"/FSM_Model_ONLY_CONSTRAINT.txt";
			"data/" + PROJECT_NAME +"/FSM_Model_ONLY_CONSTRAINT.sm";
	
	public final static String SCA_FILE = 
			"data/" + PROJECT_NAME +"/SCA.txt";
	
	public final static boolean LOAD_SCA=true;
	
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
	
	public final static Mode MODALITY = Mode.STANDARD_CIT;
	public final static ReparationMode REPAIR_MODALITY = ReparationMode.STOP_AT_ERROR;
	
	public final static boolean USE_MULTITHREAD = true;
	// Use BATCH_PROCESSING=TRUE with MULTITHREAD enabled to avoid GC Overhead
	public final static boolean BATCH_PROCESSING = true;
	public static int AUTOMATONS_PER_BATCH = 10;
	public final static int AUTOMATONS_PER_BATCH_MIN = 10;
	public final static int AUTOMATONS_PER_BATCH_MAX = 10;
	
	// Set LIMIT = 0 to use all the couples
	public final static int LIMIT = 0;
	
	// Set the strength of the test
	public final static Strength TEST_STRENGHT = Strength.THREE_WISE;
	
}
