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
import enums.Strength;
import featuremodels.specificity.BDDCITTestGenerator;
import util.FSMAutomatonBuilderWithCondition;
import util.Utils;

public class FTSTester {

	public static void main(String[] args) {
		generateTests("VendingMachine");
	}

	private static void generateTests(String systemName) {
		ConfigurationData.PROJECT_NAME = systemName;
		ConfigurationData.FSM_FILE = "data/" + systemName + "/" + systemName + ".sm";
		ConfigurationData.MESSAGES_FILE = "data/" + systemName + "/Messages.txt";
		ConfigurationData.TEST_STRENGHT = Strength.PAIR_WISE;
		String fmPath = "data/" + systemName + "/" + systemName + ".xml";
		BiMap<String, Character> msgsMapping = HashBiMap.create();

		FMCoreLibrary.getInstance().install();

		ArrayList<Automaton> automatonListForTRecognition = new ArrayList<>();
		Automaton fullSystemAutomaton = new Automaton();
		long start = System.currentTimeMillis();

		try {
			// Generate a test suite for the Feature Model, in order to have the products to
			// be tested
			IFeatureModel fm = FeatureModelManager.load(Path.of(fmPath));
			BDDCITTestGenerator generator = new BDDCITTestGenerator(fm, 2);
			TestSuite ts = generator.generateTestSuite();

			for (Test t : ts.getTests()) {

				// Generate the automaton corresponding to the Feature Transition System
				fullSystemAutomaton = FSMAutomatonBuilderWithCondition.buildFSMAutomatonFromSMC(msgsMapping, t, ConfigurationData.MESSAGES_FILE, ConfigurationData.FSM_FILE, ConfigurationData.PROJECT_NAME);
				System.out.println(msgsMapping);
				System.out.println(fullSystemAutomaton.toDot());

				// Generate the list of tuples to be covered
				automatonListForTRecognition = SequenceBuilder.getAutomatonListForTRecognition(msgsMapping);

				HashSet<String> sequences = new HashSet<>();

				if (ConfigurationData.MODALITY == Mode.ONLY_CONSTRAINT) {
					// Collecting and Conversion into the message format
					sequences = new HashSet<String>(Utils.collecting(fullSystemAutomaton, automatonListForTRecognition,
							ConfigurationData.MONITORING_ENABLED, ConfigurationData.MAX_STATES, ConfigurationData.AUTOMATONS_PER_BATCH));
					SequenceBuilder.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, false);
				}

				if (ConfigurationData.MODALITY == Mode.STANDARD_CIT) {
					sequences = new HashSet<String>(Utils.sequencesStandardCIT(automatonListForTRecognition,
							ConfigurationData.MONITORING_ENABLED, ConfigurationData.AUTOMATONS_PER_BATCH));
					SequenceBuilder.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, false);
				}

				if (ConfigurationData.MODALITY == Mode.TRANSITIONS_COVERAGE) {
					sequences = new HashSet<String>(SequenceBuilder.getSequencesForTransitionCoverage(
							ConfigurationData.STARTING_STATE, ConfigurationData.SPLIT_SEQ));
				}

				if (ConfigurationData.MODALITY == Mode.STATES_COVERAGE) {
					sequences = new HashSet<String>(
							SequenceBuilder.getSequencesForStateCoverage(ConfigurationData.STARTING_STATE));
				}

				FileWriter fout;
				try {
					fout = new FileWriter(
							new File(ConfigurationData.RESULTS_FILE + "_" + ConfigurationData.AUTOMATONS_PER_BATCH + "_"
									+ ConfigurationData.TEST_STRENGHT + ".txt").getAbsolutePath(),
							false);
					fout.write("==SUMMARY DATA==\n\n");
					fout.write("Sequence generation method: " + ConfigurationData.MODALITY.toString() + "\n");
					fout.write("Sequence repairing method: " + (ConfigurationData.MODALITY == Mode.STANDARD_CIT
							? ConfigurationData.REPAIR_MODALITY.toString()
							: "NONE") + "\n");
					fout.write("Monitoring enabled: " + ConfigurationData.MONITORING_ENABLED + "\n");
					fout.write("-----");
					fout.write("Automatons per batch: " + ConfigurationData.AUTOMATONS_PER_BATCH + "\n");
					fout.write("Total number of transitions: " + fullSystemAutomaton.getNumberOfTransitions() + "\n");
					fout.write("Total number of states: " + fullSystemAutomaton.getNumberOfStates() + "\n");
					fout.write("Total number of events: "
							+ (msgsMapping.containsKey("NO RESPONSE") ? msgsMapping.size() - 1 : msgsMapping.size())
							+ "\n");
					fout.write("Total number of "
							+ ((ConfigurationData.TEST_STRENGHT == Strength.PAIR_WISE) ? "pairs" : "triads") + ": "
							+ automatonListForTRecognition.size() + "\n");
					fout.write("Total number of valid "
							+ ((ConfigurationData.TEST_STRENGHT == Strength.PAIR_WISE) ? "pairs" : "triads") + ": "
							+ Utils.getNumberOfValidTCombinations(automatonListForTRecognition, fullSystemAutomaton)
							+ "\n");
					fout.write("-----");
					fout.write("Number of sequences: " + sequences.size() + "\n");
					fout.write("Max sequence length: " + Utils.getLength(sequences, Length.MAX) + "\n");
					fout.write("Min sequence length: " + Utils.getLength(sequences, Length.MIN) + "\n");
					fout.write("Avg sequence length: " + Utils.getLength(sequences, Length.AVG) + "\n");
					fout.write("Total sequence length: " + Utils.getLength(sequences, Length.TOTAL) + "\n");
					fout.write("Number of valid sequences: "
							+ Utils.getNumberOfValidSequences(sequences, fullSystemAutomaton) + "\n");
					fout.write("Number of covered "
							+ ((ConfigurationData.TEST_STRENGHT == Strength.PAIR_WISE) ? "pairs" : "triads") + ": "
							+ Utils.getNumberOfTCombinationCovered(sequences, automatonListForTRecognition,
									fullSystemAutomaton, ConfigurationData.REPAIR_MODALITY)
							+ "\n");
					fout.write("Number of covered states: " + Utils.getNumberOfStatesCovered(sequences,
							fullSystemAutomaton, ConfigurationData.REPAIR_MODALITY) + "\n");
					fout.write("Number of covered transitions: " + Utils.getNumberOfTransitionsCovered(sequences,
							fullSystemAutomaton, ConfigurationData.REPAIR_MODALITY) + "\n");
					fout.write("Generation time [s]: " + ((System.currentTimeMillis() - start) / 1000F));
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				break;

			}

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
