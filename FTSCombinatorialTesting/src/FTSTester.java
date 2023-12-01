import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import enums.Length;
import enums.Mode;
import enums.ReparationMode;
import featuremodels.specificity.BDDCITTestGenerator;
import util.FSMAutomatonBuilderWithCondition;
import util.Utils;

public class FTSTester {

	private static final int AUTOMATA_PER_BATCH = 10;
	private static final int NREP = 5;

	static {
		FMCoreLibrary.getInstance().install();
	}

	public static void main(String[] args) {
		for (int i = 0; i < NREP; i++) {
			// Generate with StandardCIT
			generateTests("VendingMachine", "Idle", 2, Mode.STANDARD_CIT, ReparationMode.SKIP_ERROR, true,
					AUTOMATA_PER_BATCH, false, "");
			// Generate with CT4FTS
			generateTests("VendingMachine", "Idle", 2, Mode.ONLY_CONSTRAINT, ReparationMode.SKIP_ERROR, true,
					AUTOMATA_PER_BATCH, false, "");
		}
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
		String csvFile = "data/" + systemName + "/Results_" + systemName + ".csv";

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

			int i = 0;

			System.out.println(ts.getTests());

			for (Test t : ts.getTests()) {
				String thisResultFile = resultFile.replace(".txt", "_" + i + ".txt");
				String thisSequenceFile = sequenceFile.replace(".txt", "_" + i + ".txt");
				i++;

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
					Utils.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, true, thisSequenceFile);
					break;
				case STANDARD_CIT:
					// If StandardCT is used, no guard (feature) has to be considered in the
					// generation of the the FSM
					msgsMapping = HashBiMap.create();
					Automaton systemAutomatonNoFeatures = FSMAutomatonBuilderWithCondition
							.buildFSMAutomatonFromSMC(msgsMapping, (Test) null, fsmFilePath, systemName);
					// Collecting and Conversion into the message format
					sequences = new HashSet<String>(
							Utils.collecting(systemAutomatonNoFeatures, automatonListForTRecognition, useMonitoring,
									ConfigurationData.MAX_STATES_PER_AUTOMATA, automatonsPerBatch));
					Utils.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, true, thisSequenceFile);
					break;
				default:
					break;
				}

				float time = ((System.currentTimeMillis() - start) / 1000F);
				Utils.extractStatistics(strength, useMonitoring, generationMode, repairMode, automatonsPerBatch,
						thisResultFile, msgsMapping, automatonListForTRecognition, fullSystemAutomaton, time,
						sequences);
				exportCSV(csvFile, sequences, time, i, generationMode, fullSystemAutomaton, repairMode);
			}

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private static void exportCSV(String fileName, HashSet<String> sequences, float time, int product,
			Mode generationMode, Automaton fullSystemAutomaton, ReparationMode repairMode) throws IOException {
		File f = new File(fileName);
		BufferedWriter fw = new BufferedWriter(new FileWriter(f, true));

		// Write the row for the execution
		fw.write(product + ";" + generationMode.toString() + ";" + sequences.size() + ";" + time + ";"
				+ (((float)Utils.getNumberOfTransitionsCovered(sequences, fullSystemAutomaton, repairMode)
						/ fullSystemAutomaton.getNumberOfTransitions()) * 100) + ";"
				+ (((float)Utils.getNumberOfStatesCovered(sequences, fullSystemAutomaton, repairMode)
						/ fullSystemAutomaton.getNumberOfStates()) * 100) + ";"
				+ Utils.getLength(sequences, Length.AVG) + "\n");

		fw.close();
	}
}
