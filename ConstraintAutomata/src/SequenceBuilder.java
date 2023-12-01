import java.util.ArrayList;
import java.util.HashSet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import config.ConfigurationData;
import dk.brics.automaton.Automaton;
import enums.Mode;
import enums.ReparationMode;
import util.FSMAutomatonBuilder;
import util.Utils;

public class SequenceBuilder {

	public static void main(String[] args) {
		generateTestSequences("TrafficLight", 10, 10, 2, "off__off__contr_off__blocked_a", "____turn_off", true,
				Mode.ONLY_CONSTRAINT, ReparationMode.SKIP_ERROR, false, false);
	}

	private static void generateTestSequences(String systemName, int automataMin, int automataMax, int strength,
			String startingState, String resetMsg, boolean useMonitoring, Mode generationMode,
			ReparationMode repairMode, Boolean loadSCA, Boolean splitSequences) {
		for (int automataPerBatch = automataMin; automataPerBatch <= automataMax; automataPerBatch++) {
			// Configurations
			String fsmFilePath = "data/" + systemName + "/" + systemName + ".sm";
			String messageFile = "data/" + systemName + "/Messages.txt";
			String scaFile = "data/" + systemName + "/SCA.txt";
			String resultFile = "data/" + systemName + "/ResultFile_" + systemName + automataPerBatch + "_" + strength
					+ ".txt";
			String sequenceFile = "data/" + systemName + "/Sequences_" + systemName + automataPerBatch + "_" + strength
					+ ".txt";

			HashSet<String> sequences = new HashSet<>();
			BiMap<String, Character> msgsMapping = HashBiMap.create();
			ArrayList<Automaton> automatonListForTRecognition = new ArrayList<>();
			Automaton fullSystemAutomaton = new Automaton();
			long start = System.currentTimeMillis();

			try {
				fullSystemAutomaton = FSMAutomatonBuilder.buildFSMAutomatonFromSMC(msgsMapping, fsmFilePath, systemName,
						messageFile);
				// List of the Automatons for the recognition of each T-Combination
				automatonListForTRecognition = Utils.getAutomatonListForTRecognition(msgsMapping, strength,
						ConfigurationData.ALLOW_REPS_IN_TUPLES);

				if (loadSCA) {
					sequences = Utils.mapSCAIntoString(msgsMapping, scaFile);
				} else {
					if (generationMode == Mode.ONLY_CONSTRAINT) {
						// Collecting and Conversion into the message format
						sequences = new HashSet<String>(
								Utils.collecting(fullSystemAutomaton, automatonListForTRecognition, useMonitoring,
										ConfigurationData.MAX_STATES_PER_AUTOMATA, automataPerBatch));
						Utils.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, false,
								sequenceFile);
					}

					if (generationMode == Mode.STANDARD_CIT) {
						sequences = new HashSet<String>(Utils.sequencesStandardCIT(automatonListForTRecognition,
								useMonitoring, automataPerBatch));
						Utils.createMessageSequences(new ArrayList<String>(sequences), msgsMapping, false,
								sequenceFile);
					}

					if (generationMode == Mode.TRANSITIONS_COVERAGE) {
						sequences = new HashSet<String>(Utils.getSequencesForTransitionCoverage(startingState,
								splitSequences, fsmFilePath, systemName, resetMsg));
					}

					if (generationMode == Mode.STATES_COVERAGE) {
						sequences = new HashSet<String>(
								Utils.getSequencesForStateCoverage(startingState, fsmFilePath, systemName));
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			for (String s : sequences)
				System.out.println(s + " ");

			Utils.extractStatistics(strength, useMonitoring, generationMode, repairMode, automataPerBatch, resultFile,
					msgsMapping, automatonListForTRecognition, fullSystemAutomaton, ((System.currentTimeMillis() - start) / 1000F), sequences);
		}
	}

}