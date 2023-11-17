import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import config.ConfigurationData;
import ctwedge.util.Test;
import ctwedge.util.TestSuite;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import dk.brics.automaton.Automaton;
import enums.Mode;
import enums.ReparationMode;
import featuremodels.specificity.BDDCITTestGenerator;
import util.FSMAutomatonBuilderWithCondition;
import util.Utils;

public class FTSTester {

	private static final int AUTOMATA_PER_BATCH = 10;

	static {
		FMCoreLibrary.getInstance().install();
	}

	public static void main(String[] args) {
		generateTests("VendingMachine", "Idle", 2, Mode.STATES_COVERAGE, ReparationMode.SKIP_ERROR, true,
				AUTOMATA_PER_BATCH, false, "");
	}

	private static void generateTests(String systemName, String initialState, int strength, Mode generationMode,
			ReparationMode repairMode, Boolean useMonitoring, int automatonsPerBatch, Boolean splitSequences,
			String resetMessage) {
		// Configurations
		String fsmFilePath = "data/" + systemName + "/" + systemName + ".sm";
		String fmPath = "data/" + systemName + "/" + systemName + ".xml";
		String resultFile = "data/" + systemName + "/ResultFile_" + systemName + automatonsPerBatch + "_" + strength
				+ ".txt";
		String sequenceFile = "data/" + systemName + "/Sequences_" + systemName + automatonsPerBatch + "_" + strength
				+ ".txt";

		// Structures
		BiMap<String, Character> msgsMapping = HashBiMap.create();
		ArrayList<Automaton> automatonListForTRecognition = new ArrayList<>();
		Automaton fullSystemAutomaton = new Automaton();

		// Experiments data
		long start = System.currentTimeMillis();

		try {
			// Generate a test suite for the Feature Model, in order to have the products to
			// be tested
			IFeatureModel fm = FeatureModelManager.load(Path.of(fmPath));
			BDDCITTestGenerator generator = new BDDCITTestGenerator(fm, strength);
			TestSuite ts = generator.generateTestSuite();

			for (Test t : ts.getTests()) {
				// Reset previously created structures
				msgsMapping = HashBiMap.create();
				automatonListForTRecognition = new ArrayList<>();
				fullSystemAutomaton = new Automaton();

				// Generate the automaton corresponding to the Feature Transition System
				fullSystemAutomaton = FSMAutomatonBuilderWithCondition.buildFSMAutomatonFromSMC(msgsMapping, t,
						fsmFilePath, systemName);

				// Generate the list of tuples to be covered
				automatonListForTRecognition = Utils.getAutomatonListForTRecognition(msgsMapping, strength,
						ConfigurationData.ALLOW_REPS_IN_TUPLES);

				HashSet<String> sequences = new HashSet<>();

				switch (generationMode) {
				case ONLY_CONSTRAINT:
					// Collecting and Conversion into the message format
					sequences = new HashSet<String>(Utils.collecting(fullSystemAutomaton, automatonListForTRecognition,
							useMonitoring, ConfigurationData.MAX_STATES_PER_AUTOMATA, automatonsPerBatch));
					Utils.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, true, sequenceFile);
					break;
				case STANDARD_CIT:
					// Collecting and Conversion into the message format
					sequences = new HashSet<String>(Utils.sequencesStandardCIT(automatonListForTRecognition,
							useMonitoring, automatonsPerBatch));
					Utils.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, true, sequenceFile);
					break;
				case TRANSITIONS_COVERAGE:
					// Collecting and Conversion into the message format
					sequences = new HashSet<String>(Utils.getSequencesForTransitionCoverage(initialState,
							splitSequences, fsmFilePath, systemName, resetMessage));
					Utils.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, true, sequenceFile);
					break;
				case STATES_COVERAGE:
					// Collecting and Conversion into the message format
					sequences = new HashSet<String>(
							Utils.getSequencesForStateCoverage(initialState, fsmFilePath, systemName));
					Utils.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, true, sequenceFile);
					break;
				}

				Utils.extractStatistics(strength, false, generationMode, repairMode, automatonsPerBatch, resultFile,
						msgsMapping, automatonListForTRecognition, fullSystemAutomaton, start, sequences);
			}

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
