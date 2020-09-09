import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javafx.util.Pair;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.cycle.ChinesePostman;
import org.jgrapht.alg.tour.HeldKarpTSP;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

public class SequenceBuilder {

	private static HashSet<String> sequences = new HashSet<>();

	/**
	 * Function that builds the couples of the messages, for pairwise testing
	 * 
	 * @param msgList : the list of the possible messages
	 * @return the list containing the couples of the possible messages, for
	 *         pairwise testing
	 */
	public static ArrayList<MsgPair<String, String>> getMsgCouples(ArrayList<String> msgList,
			BiMap<String, Character> msgsMapping) {
		ArrayList<MsgPair<String, String>> msgCouples = new ArrayList<>();
		/*
		 * Double "for" cycle, to build all the possible couples
		 */
		for (int i = 0; i < msgList.size(); i++) {
			for (int j = 0; j < msgList.size(); j++) {
				if (i != j || ConfigurationData.ALLOW_REPETITIONS_IN_COUPLES)
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
	 * Function that builds the triads of the messages, for 3-wise testing
	 * 
	 * @param msgList : the list of the possible messages
	 * @return the list containing the couples of the possible messages, for
	 *         pairwise testing
	 */
	public static ArrayList<MsgTriad<String, String, String>> getMsgTriads(ArrayList<String> msgList,
			BiMap<String, Character> msgsMapping) {
		ArrayList<MsgTriad<String, String, String>> msgTriads = new ArrayList<>();
		/*
		 * Double "for" cycle, to build all the possible couples
		 */
		for (int i = 0; i < msgList.size(); i++) {
			for (int j = 0; j < msgList.size(); j++) {
				for (int k = 0; k < msgList.size(); k++) {
					if ((i != j && i != k && k != j) || ConfigurationData.ALLOW_REPETITIONS_IN_COUPLES)
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
	 * automatons for each T-combination recognition
	 * 
	 * @return the list of all the automatons A(P_i)
	 * @throws IOException
	 */
	public static ArrayList<Automaton> getAutomatonListForTRecognition(BiMap<String, Character> msgsMapping)
			throws IOException {
		return ConfigurationData.TEST_STRENGHT == Strength.PAIR_WISE
				? getAutomatonList(getMsgCouples(Utils.getAntidoteMessages(), msgsMapping))
				: getAutomatonListForTriads(getMsgTriads(Utils.getAntidoteMessages(), msgsMapping));
	}

	/**
	 * Function to create the file containing the sequence of messages to be tested
	 * on ProTest
	 * 
	 * @param collectingResult
	 * @param msgsMapping
	 * @throws IOException
	 */
	public static void createMessageSequences(ArrayList<String> collectingResult, BiMap<String, Character> msgsMapping,
			boolean append) throws IOException {
		FileWriter fout = new FileWriter(new File(ConfigurationData.SEQ_FILE).getAbsolutePath(), append);

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

	public static void main(String[] args) {

		for (ConfigurationData.AUTOMATONS_PER_BATCH = ConfigurationData.AUTOMATONS_PER_BATCH_MIN; ConfigurationData.AUTOMATONS_PER_BATCH <= ConfigurationData.AUTOMATONS_PER_BATCH_MAX; ConfigurationData.AUTOMATONS_PER_BATCH++) {

			BiMap<String, Character> msgsMapping = HashBiMap.create();
			ArrayList<Automaton> automatonListForTRecognition = new ArrayList<>();
			Automaton fullSystemAutomaton = new Automaton();
			long start = System.currentTimeMillis();

			try {
				//fullSystemAutomaton = FSMAutomatonBuilder.buildFSMAutomatonFromTXT(msgsMapping);
				fullSystemAutomaton = FSMAutomatonBuilder.buildFSMAutomatonFromSMC(msgsMapping);
				// List of the Automatons for the recognition of each T-Combination
				automatonListForTRecognition = getAutomatonListForTRecognition(msgsMapping);

				if (ConfigurationData.LOAD_SCA) {
					sequences = Utils.mapSCAIntoString(msgsMapping);
				}
				else {
					if (ConfigurationData.MODALITY == Mode.ONLY_CONSTRAINT) {
						// Collecting and Conversion into the message format
						sequences = new HashSet<String>(
								Utils.collecting(fullSystemAutomaton, automatonListForTRecognition));
						createMessageSequences(new ArrayList<String>(sequences), msgsMapping, false);
					}
	
					if (ConfigurationData.MODALITY == Mode.STANDARD_CIT) {
						sequences = new HashSet<String>(Utils.sequencesStandardCIT(automatonListForTRecognition));
						createMessageSequences(new ArrayList<String>(sequences), msgsMapping, false);
					}
					
					if (ConfigurationData.MODALITY == Mode.TRANSITIONS_COVERAGE) {
						sequences = new HashSet<String>(getSequencesForTransitionCoverage("Unassociated"));
					}
					
					if (ConfigurationData.MODALITY == Mode.STATES_COVERAGE) {
						sequences = new HashSet<String>(getSequencesForStateCoverage("Unassociated"));
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			for (String s : sequences)
				System.out.println(s + " ");

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
				fout.write("Total number of events: " + (msgsMapping.containsKey("NO RESPONSE") ? msgsMapping.size()-1 : msgsMapping.size()) + "\n");
				fout.write("Total number of "
						+ ((ConfigurationData.TEST_STRENGHT == Strength.PAIR_WISE) ? "pairs" : "triads") + ": "
						+ automatonListForTRecognition.size() + "\n");
				fout.write("Total number of valid "
						+ ((ConfigurationData.TEST_STRENGHT == Strength.PAIR_WISE) ? "pairs" : "triads") + ": "
						+ Utils.getNumberOfValidTCombinations(automatonListForTRecognition, fullSystemAutomaton) + "\n");
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
								fullSystemAutomaton)
						+ "\n");
				fout.write("Number of covered states: " + Utils.getNumberOfStatesCovered(sequences, fullSystemAutomaton)
						+ "\n");
				fout.write("Number of covered transitions: "
						+ Utils.getNumberOfTransitionsCovered(sequences, fullSystemAutomaton) + "\n");
				fout.write("Generation time [s]: " + ((System.currentTimeMillis() - start) / 1000F));
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Merthod used to generate the test sequence that guarantee the Transition Coverage. 
	 * It is guaranteed by solving the ChinesePostmanProblem, that is also called Route Inspection Problem:
	 * all the transitions in a graph must be executed at least once.
	 * 
	 * @param fromState is the name of the state from which the test must start.
	 * @return the ArrrayList containing the test sequence that covers all the transitions in the graph
	 */
	private static ArrayList<String> getSequencesForTransitionCoverage(String fromState) throws IllegalAccessException, InvocationTargetException, IOException {
		ArrayList<Pair<Integer, String>> msgsIntegerMapping = new ArrayList<>();
		// Convert the SMC into the corresponding JGraphT
		Graph<String, Integer> automatonGraph = FSMAutomatonBuilder.convertSMCToGraph(msgsIntegerMapping);		
		String resultList;
		ArrayList<String> lst = new ArrayList<String>();
		
		// Check if the graph is strongly connected and generate the test sequence ->
		// We must have a Strongly Connected graph since the ChinesePostman problem is solved only for this kind 
		// of graphs
		if (GraphTests.isStronglyConnected(automatonGraph)) {	
			ChinesePostman<String,Integer> visitCFP = new ChinesePostman<>();
			GraphPath<String, Integer> sol = visitCFP.getCPPSolution(automatonGraph);
			
			// Since we want to start from a specific state, shift the event into the result list
			resultList = "";
			List<Integer> edgeList = sol.getEdgeList();
			List<String> vertexList = sol.getVertexList();			
			int lastIndexOfUnassociated = vertexList.lastIndexOf(fromState);
			
			List<Integer> shiftedEdgeList = edgeList.subList(lastIndexOfUnassociated, edgeList.size());
			shiftedEdgeList.addAll(edgeList.subList(0, lastIndexOfUnassociated));
			List<String> shiftedVertexList = vertexList.subList(lastIndexOfUnassociated, vertexList.size());
			shiftedVertexList.addAll(vertexList.subList(0, lastIndexOfUnassociated));
			
			// Check if the sequence has to be splitted
			if (!ConfigurationData.SPLIT_SEQ) {
				for (Integer msg : shiftedEdgeList) 
					resultList += decodeMessage(msg,msgsIntegerMapping) + " ";
			
				lst.add(resultList);
			} else {
				resultList = "rx_abrt ";
				
				for(int i=0; i<shiftedEdgeList.size(); i++) {
					resultList += decodeMessage(shiftedEdgeList.get(i),msgsIntegerMapping) + " ";
					
					if (shiftedVertexList.get(i+1).equals(fromState)) {
						lst.add(resultList);
						resultList = "rx_abrt ";
					}
						
				}
			}
		}	
		
		return lst;
	}
	
	
	/**
	 * Since we had some problem with using Strings for transition, we managed to use Integers numbers instead
	 * of the string description of the message. Thus we need a function to convert Integers into the corresponding
	 * String when generating the test sequence
	 * 
	 * @param message	: the message to be converted
	 * @param list		: the list of the mappings 
	 * @return			the String representing the message
	 */
	private static String decodeMessage(Integer message, ArrayList<Pair<Integer, String>> list) {
		for (int i = 0; i<list.size(); i++) {
			if (list.get(i).getKey() == message) {
				return list.get(i).getValue();
			}
		}
		return "";
	}
	
	/**
	 * Merthod used to generate the test sequence that guarantee the State Coverage. 
	 * It is guaranteed by solving the TravellingSalesmanProblem:
	 * all the states in a graph must be visited at least once.
	 * 
	 * @param fromState is the name of the state from which the test must start.
	 * @return the ArrrayList containing the test sequence that covers all the transitions in the graph
	 */
	private static ArrayList<String> getSequencesForStateCoverage(String fromState) throws IllegalAccessException, InvocationTargetException, IOException {
		ArrayList<Pair<Integer, String>> msgsIntegerMapping = new ArrayList<>();
		// Convert the SMC into the corresponding JGraphT
		Graph<String, Integer> automatonGraph = FSMAutomatonBuilder.convertSMCToGraph(msgsIntegerMapping);		
		String resultList;
		ArrayList<String> lst = new ArrayList<String>();
		
		// Check if the graph has at least a single vertex since the TSP is solved only for this kind 
		// of graphs
		if (automatonGraph.vertexSet().size()>0) {	
			HeldKarpTSP<String,Integer> visitTSP = new HeldKarpTSP<>();
			GraphPath<String, Integer> sol = visitTSP.getTour(automatonGraph);
			
			// Since we want to start from a specific state, shift the event into the result list
			resultList = "";
			List<Integer> edgeList = sol.getEdgeList();
			List<String> vertexList = sol.getVertexList();			
			int lastIndexOfUnassociated = vertexList.lastIndexOf(fromState);
			
			List<Integer> shiftedEdgeList = edgeList.subList(lastIndexOfUnassociated, edgeList.size());
			shiftedEdgeList.addAll(edgeList.subList(0, lastIndexOfUnassociated));
			
			for (Integer msg : shiftedEdgeList) 
				resultList += decodeMessage(msg,msgsIntegerMapping) + " ";
			
			lst.add(resultList);
		}	
		
		return lst;
	}

}