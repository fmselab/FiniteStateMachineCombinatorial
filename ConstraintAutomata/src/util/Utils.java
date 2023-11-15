package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.cycle.ChinesePostman;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.tour.HeldKarpTSP;
import org.jgrapht.nio.dot.DOTExporter;

import com.google.common.collect.BiMap;

import config.ConfigurationData;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import enums.Length;
import enums.Mode;
import enums.ReparationMode;
import javafx.util.Pair;

// TODO: Auto-generated Javadoc
/**
 * The Class Utils contains some utility functions, used by the automata-based
 * approach.
 */
public class Utils {

	/**
	 * Function that reads the list of the messages from an input text file.
	 *
	 * @param fileName the name of the file containing the messages
	 * @return the list of the possible messages
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static ArrayList<String> getSystemMessages(String fileName) throws IOException {
		ArrayList<String> msgList = new ArrayList<>();
		File f = new File(fileName);
		FileReader fin = new FileReader(f.getAbsolutePath());
		BufferedReader msgFile = new BufferedReader(fin);
		// Read the content of the file and put it into the list
		while (true) {
			String readMsg;
			try {
				readMsg = msgFile.readLine();
			} catch (IOException e) {
				break;
			}
			if (readMsg == null)
				break;
			if (!readMsg.equals("NO RESPONSE"))
				msgList.add(readMsg);
		}
		msgFile.close();
		fin.close();
		// Return the list of the messages
		return msgList;
	}

	/**
	 * Extract statistics.
	 *
	 * @param strength the strength
	 * @param useMonitoring the use monitoring
	 * @param generationMode the generation mode
	 * @param repairMode the repair mode
	 * @param automataPerBatch the automata per batch
	 * @param resultFile the result file
	 * @param msgsMapping the msgs mapping
	 * @param automatonListForTRecognition the automaton list for T recognition
	 * @param fullSystemAutomaton the full system automaton
	 * @param startTime the start time
	 * @param sequences the sequences
	 */
	public static void extractStatistics(int strength, boolean useMonitoring, Mode generationMode,
			ReparationMode repairMode, int automataPerBatch, String resultFile, BiMap<String, Character> msgsMapping,
			ArrayList<Automaton> automatonListForTRecognition, Automaton fullSystemAutomaton, long startTime, HashSet<String> sequences) {
		FileWriter fout;
		try {
			fout = new FileWriter(new File(resultFile).getAbsolutePath(), false);
			fout.write("==SUMMARY DATA==\n\n");
			fout.write("Sequence generation method: " + generationMode.toString() + "\n");
			fout.write("Sequence repairing method: "
					+ (generationMode == Mode.STANDARD_CIT ? repairMode.toString() : "NONE") + "\n");
			fout.write("Monitoring enabled: " + useMonitoring + "\n");
			fout.write("-----");
			fout.write("Automatons per batch: " + automataPerBatch + "\n");
			fout.write("Total number of transitions: " + fullSystemAutomaton.getNumberOfTransitions() + "\n");
			fout.write("Total number of states: " + fullSystemAutomaton.getNumberOfStates() + "\n");
			fout.write("Total number of events: "
					+ (msgsMapping.containsKey("NO RESPONSE") ? msgsMapping.size() - 1 : msgsMapping.size()) + "\n");
			fout.write("Total number of " + ((strength == 2) ? "pairs" : "triads") + ": "
					+ automatonListForTRecognition.size() + "\n");
			fout.write("Total number of valid " + ((strength == 2) ? "pairs" : "triads") + ": "
					+ Utils.getNumberOfValidTCombinations(automatonListForTRecognition, fullSystemAutomaton) + "\n");
			fout.write("-----");
			fout.write("Number of sequences: " + sequences.size() + "\n");
			fout.write("Max sequence length: " + Utils.getLength(sequences, Length.MAX) + "\n");
			fout.write("Min sequence length: " + Utils.getLength(sequences, Length.MIN) + "\n");
			fout.write("Avg sequence length: " + Utils.getLength(sequences, Length.AVG) + "\n");
			fout.write("Total sequence length: " + Utils.getLength(sequences, Length.TOTAL) + "\n");
			fout.write("Number of valid sequences: " + Utils.getNumberOfValidSequences(sequences, fullSystemAutomaton)
					+ "\n");
			fout.write("Number of covered " + ((strength == 2) ? "pairs" : "triads") + ": "
					+ Utils.getNumberOfTCombinationCovered(sequences, automatonListForTRecognition, fullSystemAutomaton,
							repairMode)
					+ "\n");
			fout.write("Number of covered states: "
					+ Utils.getNumberOfStatesCovered(sequences, fullSystemAutomaton, repairMode) + "\n");
			fout.write("Number of covered transitions: "
					+ Utils.getNumberOfTransitionsCovered(sequences, fullSystemAutomaton, repairMode) + "\n");
			fout.write("Generation time [s]: " + ((System.currentTimeMillis() - startTime) / 1000F));
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function that builds the couples of the messages, for pairwise testing.
	 *
	 * @param msgsMapping the mapping of messages in characters
	 * @param allowReps   allow repetitions?
	 * @return the list containing the couples of the possible messages, for
	 *         pairwise testing
	 */
	public static ArrayList<MsgPair<String, String>> getMsgCouples(BiMap<String, Character> msgsMapping,
			Boolean allowReps) {
		ArrayList<MsgPair<String, String>> msgCouples = new ArrayList<>();
		/*
		 * Double "for" cycle, to build all the possible couples
		 */
		ArrayList<String> msgList = new ArrayList<>(msgsMapping.keySet());
		for (int i = 0; i < msgList.size(); i++) {
			for (int j = 0; j < msgList.size(); j++) {
				if (i != j || allowReps)
					msgCouples.add(new MsgPair<String, String>(msgsMapping.get(msgList.get(i).toUpperCase()).toString(),
							msgsMapping.get(msgList.get(j).toUpperCase()).toString()));
			}
		}

		// Random sort has proved to be more efficient in terms of duration of the
		// collecting operation
		Collections.shuffle(msgCouples);
		return msgCouples;
	}

	/**
	 * Function that builds the triads of the messages, for 3-wise testing.
	 *
	 * @param msgsMapping the mapping of messages in characters
	 * @param allowReps   allow repetitions?
	 * @return the list containing the couples of the possible messages, for
	 *         pairwise testing
	 */
	public static ArrayList<MsgTriad<String, String, String>> getMsgTriads(BiMap<String, Character> msgsMapping,
			Boolean allowReps) {
		ArrayList<MsgTriad<String, String, String>> msgTriads = new ArrayList<>();
		/*
		 * Double "for" cycle, to build all the possible couples
		 */
		ArrayList<String> msgList = new ArrayList<>(msgsMapping.keySet());
		for (int i = 0; i < msgList.size(); i++) {
			for (int j = 0; j < msgList.size(); j++) {
				for (int k = 0; k < msgList.size(); k++) {
					if ((i != j && i != k && k != j) || allowReps)
						msgTriads.add(new MsgTriad<String, String, String>(
								msgsMapping.get(msgList.get(i).toUpperCase()).toString(),
								msgsMapping.get(msgList.get(j).toUpperCase()).toString(),
								msgsMapping.get(msgList.get(k).toUpperCase()).toString()));
				}
			}
		}

		// Random sort has proved to be more efficient in terms of duration of the
		// collecting operation
		Collections.shuffle(msgTriads);
		return msgTriads;
	}

	/**
	 * Function that loads the SCAs and returns the sequences mapped with letters
	 * instead of numbers.
	 *
	 * @param msgsMapping the map mapping each message on a single character
	 * @param scaFilePath the path of the file containing sequence covering arrays
	 * @return the hash set of strings corresponding to the content of the given
	 *         scaFilePath
	 * @throws IOException Signals that an I/O exception has occurred. @ returns the
	 *                     list of sequences
	 */
	public static HashSet<String> mapSCAIntoString(BiMap<String, Character> msgsMapping, String scaFilePath)
			throws IOException {
		String readLn;
		File f = new File(scaFilePath);
		FileReader fin = new FileReader(f.getAbsolutePath());
		BufferedReader scaFile = new BufferedReader(fin);
		HashSet<String> msgList = new HashSet<>();

		/*
		 * Read the content of the file, and convert the indexes into messages
		 */
		while (true) {
			readLn = null;
			try {
				readLn = scaFile.readLine();
			} catch (IOException e) {
				break;
			}
			if (readLn == null)
				break;
			String seq = "";
			for (String index : readLn.split(",")) {
				seq = seq + msgsMapping.values().toArray()[Integer.parseInt(index)];
			}
			msgList.add(seq);
		}

		scaFile.close();

		return msgList;
	}

	/**
	 * Function verifying whether is possible the intersection between two
	 * automatons.
	 *
	 * @param full the automaton of the full system
	 * @param a    the automaton of the single couple
	 * @return "true" if the intersection is possible, "false" otherwise
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static boolean isInteresctionPossible(Automaton full, Automaton a) throws IOException {
		// Logger
		Logger logger = Logger.getLogger(FSMAutomatonBuilder.class);
		logger.debug("Check intersection between " + full.toString() + " and " + a.toString());
		String shortestExample2 = full.intersection(a).getShortestExample(true);

		if (shortestExample2 != "" && shortestExample2 != null)
			logger.debug("Intersection possible");
		else
			logger.debug("Intersection not possible");

		return (shortestExample2 != "" && shortestExample2 != null) ? true : false;
	}

	/**
	 * Function executing the MONITORING Procedure: given the list of the remaining
	 * pairs that have to be recognized, this procedure checks if the string already
	 * satisfy some other constraints.
	 *
	 * @param automatonList        the list of the automatons that recognize the
	 *                             couples
	 * @param stringToBeRecognized the string to be recognized
	 */
	public static void monitoring(ArrayList<Automaton> automatonList, String stringToBeRecognized) {
		@SuppressWarnings("unchecked")
		ArrayList<Automaton> list = (ArrayList<Automaton>) automatonList.clone();
		for (Automaton a : automatonList) {
			if (a.run(stringToBeRecognized))
				list.remove(a);
		}
		automatonList = list;
	}

	/**
	 * Collecting operation to create the shortest sequences of messages possible.
	 *
	 * @param fullSystemAutomaton              : automaton representing the FSM -
	 *                                         Full System
	 * @param automatonListForTWiseRecognition the automaton list for T wise
	 *                                         recognition
	 * @param useMonitoring                    the use monitoring
	 * @param nMaxStates the n max states
	 * @param nMaxAutomatonsPerBatch the n max automatons per batch
	 * @return the list of the test strings
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<String> collecting(Automaton fullSystemAutomaton,
			ArrayList<Automaton> automatonListForTWiseRecognition, Boolean useMonitoring, int nMaxStates,
			int nMaxAutomatonsPerBatch) throws IOException {
		ArrayList<String> stringList = new ArrayList<String>();
		ArrayList<Automaton> temp = new ArrayList<Automaton>();
		temp = (ArrayList<Automaton>) automatonListForTWiseRecognition.clone();
		int oldDim = temp.size();
		int i = 0;

		System.out.println("Total automaton number: " + temp.size());

		System.out.println("FULL SYSTEM: " + fullSystemAutomaton.toDot());

		while (temp.size() > 0) {
			Automaton tempFullSystem = fullSystemAutomaton.clone();
			ArrayList<Automaton> temp2 = (ArrayList<Automaton>) temp.clone();
			int index = 0;
			for (Automaton a : temp) {
				System.out.println("T-WISE AUTOMATON: " + a.toDot());
				System.out.println("Processing Automaton " + (++index) + " of " + temp.size() + " - #States: "
						+ tempFullSystem.getNumberOfStates());
				Automaton intersection = tempFullSystem.intersection(a);
				System.out.println("INTERSEZIONE: " + intersection.toDot());
				String shortestExample = intersection.getShortestExample(true);
				if (shortestExample != null && shortestExample != "") {
					tempFullSystem = intersection;
					temp2.remove(a);
					i++;
				} else {
					// Try to find out whether the intersection is not possible also with the
					// original automaton.
					// Removing unreachable sequences can lead to a shorter evaluation
					if (!isInteresctionPossible(fullSystemAutomaton, a))
						temp2.remove(a);
				}
				// Limit the size of the intersection Automaton
				if (tempFullSystem.getNumberOfStates() >= nMaxStates || i >= nMaxAutomatonsPerBatch) {
					i = 0;
					break;
				}
			}
			String shortestExample = tempFullSystem.getShortestExample(true);
			// Apply the monitoring operation
			if (useMonitoring)
				monitoring(temp2, shortestExample);

			temp = (ArrayList<Automaton>) temp2.clone();

			System.out.println(shortestExample);
			System.out.println("Remaining automaton to be processed: " + temp2.size());
			if (shortestExample != "")
				stringList.add(shortestExample);
			if (temp2.size() == oldDim)
				break;
			oldDim = temp2.size();
		}

		return stringList;
	}

	/**
	 * Max/Min/Avg length in a set of sequences.
	 *
	 * @param seq   the set of sequences
	 * @param which the type ot the required length
	 * @return the required length
	 */
	public static int getLength(HashSet<String> seq, Length which) {
		int count = 0;
		switch (which) {
		case AVG:
			for (String s : seq) {
				count += s.length();
			}
			if (seq.size() > 0) {
				count /= seq.size();
			} else {
				count = 0;
			}
			break;
		case MAX:
			if (seq.size() > 0) {
				count = seq.toArray()[0].toString().length();
				for (String s : seq) {
					if (s.length() > count)
						count = s.length();
				}
			} else {
				count = 0;
			}
			break;
		case MIN:
			if (seq.size() > 0) {
				count = seq.toArray()[0].toString().length();
				for (String s : seq) {
					if (s.length() < count)
						count = s.length();
				}
			} else {
				count = 0;
			}
			break;
		case TOTAL:
			for (String s : seq) {
				count += s.length();
			}
			break;
		}
		return count;
	}

	/**
	 * Number of valid sequences.
	 *
	 * @param seq  the set of sequences
	 * @param full the automaton of the system
	 * @return the number of accepted sequences
	 */
	public static int getNumberOfValidSequences(HashSet<String> seq, Automaton full) {
		int num = 0;
		for (String s : seq) {
			if (full.run(s))
				num++;
		}
		return num;
	}

	/**
	 * Number of states covered by the set of sequences.
	 *
	 * @param seq        the set of sequences
	 * @param full       the automaton of the system
	 * @param repairMode the repair mode
	 * @return the number of states covered
	 */
	public static int getNumberOfStatesCovered(HashSet<String> seq, Automaton full, ReparationMode repairMode) {
		HashSet<Integer> statesVisited = new HashSet<Integer>();
		for (String s : seq) {
			String repairedString = repairSequence(s, full, repairMode);
			if (repairedString.length() > 0) {
				State initialState = full.getInitialState();
				statesVisited.add(initialState.hashCode());
				for (char c : repairedString.toCharArray()) {
					initialState = initialState.step(c);
					statesVisited.add(initialState.hashCode());
				}
			}
		}

		// Debug code
		System.out.println("\nCovered States: ");
		for (Integer i : statesVisited)
			System.out.println(i.toString() + " ");

		return statesVisited.size();
	}

	/**
	 * Number of transitions covered by the set of sequences.
	 *
	 * @param seq        the set of sequences
	 * @param full       the automaton of the system
	 * @param repairMode the repair mode
	 * @return the number of transitions covered
	 */
	public static int getNumberOfTransitionsCovered(HashSet<String> seq, Automaton full, ReparationMode repairMode) {
		HashSet<MsgPairExtended<Integer, Integer, Character>> transitionsCovered = new HashSet<MsgPairExtended<Integer, Integer, Character>>();

		for (String s : seq) {
			String repairedString = repairSequence(s, full, repairMode);
			if (repairedString.length() > 0) {
				State initialState = full.getInitialState();
				State followingState = initialState.step(repairedString.toCharArray()[0]);
				if (initialState != null && followingState != null)
					transitionsCovered.add(new MsgPairExtended<Integer, Integer, Character>(initialState.hashCode(),
							followingState.hashCode(), repairedString.toCharArray()[0]));
				repairedString = repairedString.substring(1);
				for (char c : repairedString.toCharArray()) {
					initialState = followingState;
					if (followingState != null)
						followingState = followingState.step(c);
					if (initialState != null && followingState != null)
						transitionsCovered.add(new MsgPairExtended<Integer, Integer, Character>(initialState.hashCode(),
								followingState.hashCode(), c));
				}
			}
		}

		// Debug code
		System.out.println("\nCovered Transitions: ");
		for (MsgPairExtended<Integer, Integer, Character> i : transitionsCovered)
			System.out.println(i.toString() + " ");

		return transitionsCovered.size();
	}

	/**
	 * Operation to create the sequences using the standard CIT approach.
	 *
	 * @param automatonListForTRecognition list of the automatons for each
	 *                                     T-Combination of messages
	 * @param useMonitoring                use monitoring or not?
	 * @param nMaxAutomatonsPerBatch       the n max automatons per batch
	 * @return : the list of the test strings
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<String> sequencesStandardCIT(ArrayList<Automaton> automatonListForTRecognition,
			Boolean useMonitoring, int nMaxAutomatonsPerBatch) throws IOException {
		ArrayList<String> stringList = new ArrayList<String>();
		ArrayList<Automaton> temp = new ArrayList<Automaton>();
		temp = (ArrayList<Automaton>) automatonListForTRecognition.clone();

		while (temp.size() > 0) {
			int numTCombinationConsidered = 0;
			Automaton sequenceAutomaton = temp.get(numTCombinationConsidered);
			temp.remove(0);
			while (numTCombinationConsidered <= nMaxAutomatonsPerBatch && temp.size() > 0) {
				numTCombinationConsidered++;
				sequenceAutomaton = sequenceAutomaton.intersection(temp.get(0));
				temp.remove(0);
			}
			String shortestExample = sequenceAutomaton.getShortestExample(true);
			if (shortestExample != "")
				stringList.add(shortestExample);

			// Apply the monitoring operation
			if (useMonitoring)
				monitoring(temp, shortestExample);
		}

		return stringList;
	}

	/**
	 * Function to repair the sequence.
	 *
	 * @param seq                 the set of sequences
	 * @param fullSystemAutomaton the full system automaton
	 * @param repairMode          the repair mode
	 * @return the repaired sequence
	 */
	public static String repairSequence(String seq, Automaton fullSystemAutomaton, ReparationMode repairMode) {
		String newStr = "";
		switch (repairMode) {
		case SKIP_ERROR:
			for (int i = 0; i < seq.length(); i++) {
				if (fullSystemAutomaton.run(newStr + seq.charAt(i)))
					newStr = newStr + seq.charAt(i);
			}
			break;
		case REJECT_NOT_VALID:
			if (!fullSystemAutomaton.run(seq))
				newStr = "";
			else
				newStr = seq;
			break;
		case STOP_AT_ERROR:
			for (int i = 0; i < seq.length(); i++) {
				if (!fullSystemAutomaton.run(seq.substring(0, i)))
					break;
				else
					newStr = seq.substring(0, i);
			}
			break;
		}
		return newStr;
	}

	/**
	 * Function compute the number of T-combination couples.
	 *
	 * @param sequences                    the set of sequences
	 * @param automatonListForTRecognition the list of the automatons for each
	 *                                     T-combination recognition
	 * @param fullSystemAutomaton          the full system automaton
	 * @param repairingMode                the repairing mode
	 * @return the number of covered T-combination
	 */
	@SuppressWarnings("unchecked")
	public static int getNumberOfTCombinationCovered(HashSet<String> sequences,
			ArrayList<Automaton> automatonListForTRecognition, Automaton fullSystemAutomaton,
			ReparationMode repairingMode) {
		int tCombinationCovered = 0;
		ArrayList<Automaton> temp = (ArrayList<Automaton>) automatonListForTRecognition.clone();

		for (String s : sequences) {
			String fixedSeq = (!fullSystemAutomaton.run(s)) ? repairSequence(s, fullSystemAutomaton, repairingMode) : s;

			ArrayList<Automaton> temp2 = (ArrayList<Automaton>) temp.clone();
			for (Automaton tAut : temp) {
				if (tAut.run(fixedSeq)) {
					tCombinationCovered++;
					temp2.remove(tAut);
				}
			}
			temp = (ArrayList<Automaton>) temp2.clone();
		}

		return tCombinationCovered;
	}

	/**
	 * Function compute the number of valid T-combination.
	 *
	 * @param automatonListForTRecognition the list of the automatons for each
	 *                                     T-combination recognition
	 * @param fullSystemAutomaton          the automaton of the full system
	 * @return the number of valid T-combination
	 */
	public static int getNumberOfValidTCombinations(ArrayList<Automaton> automatonListForTRecognition,
			Automaton fullSystemAutomaton) {
		int n = 0;

		for (Automaton a : automatonListForTRecognition) {
			if (!fullSystemAutomaton.intersection(a).isEmpty())
				n++;
		}

		return n;
	}

	/**
	 * Function to create the file containing the sequence of messages to be tested
	 * on ProTest.
	 *
	 * @param collectingResult the collecting result
	 * @param msgsMapping the msgs mapping
	 * @param append the append
	 * @param outputFilePath the output file path
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void createMessageSequences(ArrayList<String> collectingResult, BiMap<String, Character> msgsMapping,
			boolean append, String outputFilePath) throws IOException {
		FileWriter fout = new FileWriter(new File(outputFilePath).getAbsolutePath(), append);

		// Write the converted sequences to the file
		for (String msg : collectingResult) {
			String str = msg;
			String msgOut = "";
			// Substitute each char of the sequence
			for (int i = 0; i < str.length(); i++)
				msgOut = msgOut + msgsMapping.inverse().get(str.charAt(i)) + " ";

			fout.write(msgOut.toLowerCase() + "\n");
		}

		fout.close();
	}

	/**
	 * Merthod used to generate the test sequence that guarantee the State Coverage.
	 * It is guaranteed by solving the TravellingSalesmanProblem: all the states in
	 * a graph must be visited at least once.
	 *
	 * @param fromState   is the name of the state from which the test must start.
	 * @param fsmFilePath the fsm file path
	 * @param projectName the project name
	 * @return the ArrrayList containing the test sequence that covers all the
	 *         transitions in the graph
	 * @throws IllegalAccessException    the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws IOException               Signals that an I/O exception has occurred.
	 */
	public static ArrayList<String> getSequencesForStateCoverage(String fromState, String fsmFilePath,
			String projectName) throws IllegalAccessException, InvocationTargetException, IOException {
		ArrayList<Pair<Integer, String>> msgsIntegerMapping = new ArrayList<>();
		// Convert the SMC into the corresponding JGraphT
		Graph<String, Integer> automatonGraph = FSMAutomatonBuilder.convertSMCToGraph(msgsIntegerMapping, fsmFilePath,
				projectName);
		String resultList;
		ArrayList<String> lst = new ArrayList<String>();

		// Check if the graph has at least a single vertex since the TSP is solved only
		// for this kind
		// of graphs
		if (automatonGraph.vertexSet().size() > 0) {
			HeldKarpTSP<String, Integer> visitTSP = new HeldKarpTSP<>();

			DOTExporter<String, Integer> exporter = new DOTExporter<>();
			Writer writer = new StringWriter();
			exporter.exportGraph(automatonGraph, writer);
			System.out.println(writer.toString());

			GraphPath<String, Integer> sol;
			sol = visitTSP.getTour(automatonGraph);

			List<Integer> edgeList = new ArrayList<>();
			List<String> vertexList = new ArrayList<>();
			int lastIndexOfUnassociated = 0;
			resultList = "";

			if (sol != null) {
				// Since we want to start from a specific state, shift the event into the result
				// list
				edgeList = sol.getEdgeList();
				vertexList = sol.getVertexList();
				lastIndexOfUnassociated = vertexList.lastIndexOf(fromState);
			}
			{
				// TSP is not computable -> Simulate the shortest path by using Dijkstra
				Set<String> vertexSet = automatonGraph.vertexSet();
				DijkstraShortestPath<String, Integer> path = new DijkstraShortestPath<String, Integer>(automatonGraph);

				// Add step by step all the states
				for (String s : vertexSet) {
					String initial = (vertexList.size() > 0) ? vertexList.get(vertexList.size() - 1) : fromState;
					sol = path.getPath(initial, s);

					edgeList.addAll(sol.getEdgeList());
					vertexList.addAll(sol.getVertexList());
				}

				// Come back to the initial state
				sol = path.getPath(vertexList.get(vertexList.size() - 1), fromState);

				edgeList.addAll(sol.getEdgeList());
				vertexList.addAll(sol.getVertexList());
			}

			List<Integer> shiftedEdgeList = edgeList.subList(lastIndexOfUnassociated, edgeList.size());
			shiftedEdgeList.addAll(edgeList.subList(0, lastIndexOfUnassociated));

			for (Integer msg : shiftedEdgeList)
				resultList += decodeMessage(msg, msgsIntegerMapping) + " ";

			lst.add(resultList);
		}

		return lst;
	}

	/**
	 * Since we had some problem with using Strings for transition, we managed to
	 * use Integers numbers instead of the string description of the message. Thus
	 * we need a function to convert Integers into the corresponding String when
	 * generating the test sequence
	 * 
	 * @param message : the message to be converted
	 * @param list    : the list of the mappings
	 * @return the String representing the message
	 */
	public static String decodeMessage(Integer message, ArrayList<Pair<Integer, String>> list) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getKey() == message) {
				return list.get(i).getValue();
			}
		}
		return "";
	}

	/**
	 * Function that creates a single automaton for the recognition of each couple.
	 * The automaton has to be able to recognize the string composed by "FIRST -
	 * SECOND"
	 * 
	 * @param coupleList : the list of all the possible couples
	 * @return the list of all the automatons A(P_i)
	 */
	public static ArrayList<Automaton> getAutomatonList(ArrayList<MsgPair<String, String>> coupleList) {
		ArrayList<Automaton> automatonList = new ArrayList<>();
		for (MsgPair<String, String> m : coupleList) {
			String msg = m.toString().replace(")", "").replace("(", "");
			String notSecond = "";
			if (ConfigurationData.NOT_SECOND)
				notSecond = "[^" + msg.split(" - ")[1] + "]*";
			else
				notSecond = ConfigurationData.ANY_CHAR;
			RegExp rex = new RegExp(notSecond + msg.split(" - ")[0] + ConfigurationData.ANY_CHAR + msg.split(" - ")[1]
					+ ConfigurationData.ANY_CHAR);
			automatonList.add(rex.toAutomaton());
		}
		return automatonList;
	}

	/**
	 * Function that creates a single automaton for the recognition of each triads.
	 * The automaton has to be able to recognize the string composed by "FIRST -
	 * SECOND - THIRD"
	 * 
	 * @param triadsList : the list of all the possible triads
	 * @return the list of all the automatons A(P_i)
	 */
	public static ArrayList<Automaton> getAutomatonListForTriads(
			ArrayList<MsgTriad<String, String, String>> triadsList) {
		ArrayList<Automaton> automatonList = new ArrayList<>();
		for (MsgTriad<String, String, String> m : triadsList) {
			String msg = m.toString().replace(")", "").replace("(", "");
			String notSecond = "";
			if (ConfigurationData.NOT_SECOND)
				notSecond = "[^(" + msg.split(" - ")[1] + "|" + msg.split(" - ")[2] + ")]*";
			else
				notSecond = ConfigurationData.ANY_CHAR;
			RegExp rex = new RegExp(notSecond + msg.split(" - ")[0] + ConfigurationData.ANY_CHAR + msg.split(" - ")[1]
					+ ConfigurationData.ANY_CHAR + msg.split(" - ")[2] + ConfigurationData.ANY_CHAR);
			automatonList.add(rex.toAutomaton());
		}
		return automatonList;
	}

	/**
	 * Function that combines the previous three functions to build the list of the
	 * automatons for each T-combination recognition.
	 *
	 * @param msgsMapping the msgs mapping
	 * @param strength the strength
	 * @param withReps the with reps
	 * @return the list of all the automatons A(P_i)
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static ArrayList<Automaton> getAutomatonListForTRecognition(BiMap<String, Character> msgsMapping,
			int strength, Boolean withReps) throws IOException {
		return strength == 2 ? getAutomatonList(Utils.getMsgCouples(msgsMapping, withReps))
				: getAutomatonListForTriads(Utils.getMsgTriads(msgsMapping, withReps));
	}

	/**
	 * Method used to generate the test sequence that guarantee the Transition
	 * Coverage. It is guaranteed by solving the ChinesePostmanProblem, that is also
	 * called Route Inspection Problem: all the transitions in a graph must be
	 * executed at least once.
	 *
	 * @param fromState   is the name of the state from which the test must start.
	 * @param split       the split
	 * @param fsmFilePath the fsm file path
	 * @param projectName the project name
	 * @param resetMsg    the reset msg
	 * @return the ArrrayList containing the test sequence that covers all the
	 *         transitions in the graph
	 * @throws IllegalAccessException    the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws IOException               Signals that an I/O exception has occurred.
	 */
	public static ArrayList<String> getSequencesForTransitionCoverage(String fromState, Boolean split,
			String fsmFilePath, String projectName, String resetMsg)
			throws IllegalAccessException, InvocationTargetException, IOException {
		ArrayList<Pair<Integer, String>> msgsIntegerMapping = new ArrayList<>();
		// Convert the SMC into the corresponding JGraphT
		Graph<String, Integer> automatonGraph = FSMAutomatonBuilder.convertSMCToGraph(msgsIntegerMapping, fsmFilePath,
				projectName);
		String resultList;
		ArrayList<String> lst = new ArrayList<String>();

		// Check if the graph is strongly connected and generate the test sequence ->
		// We must have a Strongly Connected graph since the ChinesePostman problem is
		// solved only for this kind
		// of graphs
		if (GraphTests.isStronglyConnected(automatonGraph)) {
			ChinesePostman<String, Integer> visitCFP = new ChinesePostman<>();
			GraphPath<String, Integer> sol = visitCFP.getCPPSolution(automatonGraph);

			// Since we want to start from a specific state, shift the event into the result
			// list
			resultList = "";
			List<Integer> edgeList = sol.getEdgeList();
			List<String> vertexList = sol.getVertexList();
			int lastIndexOfUnassociated = vertexList.lastIndexOf(fromState);

			List<Integer> shiftedEdgeList = edgeList.subList(lastIndexOfUnassociated, edgeList.size());
			shiftedEdgeList.addAll(edgeList.subList(0, lastIndexOfUnassociated));
			List<String> shiftedVertexList = vertexList.subList(lastIndexOfUnassociated, vertexList.size());
			shiftedVertexList.addAll(vertexList.subList(0, lastIndexOfUnassociated));

			// Check if the sequence has to be splitted
			if (!split) {
				for (Integer msg : shiftedEdgeList)
					resultList += Utils.decodeMessage(msg, msgsIntegerMapping) + " ";

				lst.add(resultList);
			} else {
				resultList = resetMsg + " ";

				for (int i = 0; i < shiftedEdgeList.size(); i++) {
					resultList += Utils.decodeMessage(shiftedEdgeList.get(i), msgsIntegerMapping) + " ";

					if (shiftedVertexList.get(i + 1).equals(fromState)) {
						lst.add(resultList);
						resultList = resetMsg + " ";
					}

				}
			}
		}

		return lst;
	}

}
