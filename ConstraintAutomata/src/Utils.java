import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import com.google.common.collect.BiMap;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;

public class Utils {

	/**
	 * Function that reads the list of the messages from an input text file
	 * 
	 * @return the list of the possible messages
	 * @throws IOException
	 */
	public static ArrayList<String> getAntidoteMessages() throws IOException {
		ArrayList<String> msgList = new ArrayList<>();
		File f = new File(ConfigurationData.MESSAGES_FILE);
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
	 * Function that loads the SCAs and returns the sequences mapped with letters instead of numbers
	 * 
	 * @ returns the list of sequences
	 */
	static HashSet<String> mapSCAIntoString(BiMap<String, Character> msgsMapping) throws IOException {
		String readLn, startState, inMessage, endState, outMessage;
		File f = new File(ConfigurationData.SCA_FILE);
		FileReader fin = new FileReader(f.getAbsolutePath());
		BufferedReader scaFile = new BufferedReader(fin);
		HashSet<String> msgList = new HashSet<>();
		char code = 64;
		
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
			if (readLn == null) break;
			String seq = "";
			for(String index : readLn.split(",")) {
				seq = seq + msgsMapping.values().toArray()[Integer.parseInt(index)];
			}
			msgList.add(seq);
		}
		
		return msgList;
	}
	
	
	/**
	 * Function verifying whether is possible the intersection between two
	 * automatons
	 * 
	 * @param full : the automaton of the full system
	 * @param a    : the automaton of the single couple
	 * @return "true" if the intersection is possible, "false" otherwise
	 * @throws IOException
	 */
	public static boolean isInteresctionPossible(Automaton full, Automaton a) throws IOException {
		FileWriter flog = new FileWriter(ConfigurationData.LOG_FILE, true);
		flog.write("Check intersection between " + full.toString() + " and " + a.toString());
		String shortestExample2 = full.intersection(a).getShortestExample(true);

		if (shortestExample2 != "" && shortestExample2 != null)
			flog.write("Intersection possible");
		else
			flog.write("Intersection not possible");

		flog.close();
		return (shortestExample2 != "" && shortestExample2 != null) ? true : false;
	}

	/**
	 * Function executing the MONITORING Procedure: given the list of the remaining
	 * pairs that have to be recognized, this procedure checks if the string already
	 * satisfy some other constraints
	 * 
	 * @param automatonList        : the list of the automatons that recognize the
	 *                             couples
	 * @param stringToBeRecognised : string of the example
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
	 * Collecting operation to create the shortest sequences of messages possible
	 * 
	 * @param fullSystemAutomaton             : automaton representing the FSM -
	 *                                        Full System
	 * @param automatonListForPairRecognition : list of the automatons for each
	 *                                        couple of messages
	 * @return : the list of the test strings
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<String> collecting(Automaton fullSystemAutomaton,
			ArrayList<Automaton> automatonListForTWiseRecognition) throws IOException {
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
				if (tempFullSystem.getNumberOfStates() >= ConfigurationData.MAX_STATES || i >= ConfigurationData.AUTOMATONS_PER_BATCH) {
					i=0;
					break;
				}
			}
			String shortestExample = tempFullSystem.getShortestExample(true);
			// Apply the monitoring operation
			if (ConfigurationData.MONITORING_ENABLED)
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
	 * Max/Min/Avg length in a set of sequences
	 * 
	 * @param seq   : the set of sequences
	 * @param which : the type ot the required length
	 * 
	 * @return the required length
	 */
	public static int getLength(HashSet<String> seq, Length which) {
		int count = 0;
		switch (which) {
		case AVG:
			for (String s : seq) {
				count += s.length();
			}
			count /= seq.size();
			break;
		case MAX:
			count = seq.toArray()[0].toString().length();
			for (String s : seq) {
				if (s.length() > count)
					count = s.length();
			}
			break;
		case MIN:
			count = seq.toArray()[0].toString().length();
			for (String s : seq) {
				if (s.length() < count)
					count = s.length();
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
	 * Number of valid sequences
	 * 
	 * @param seq  : the set of sequences
	 * @param full : the automaton of the system
	 * 
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
	 * Number of states covered by the set of sequences
	 * 
	 * @param seq  : the set of sequences
	 * @param full : the automaton of the system
	 * 
	 * @return the number of states covered
	 */
	public static int getNumberOfStatesCovered(HashSet<String> seq, Automaton full) {
		HashSet<Integer> statesVisited = new HashSet<Integer>();
		for (String s : seq) {
			String repairedString = repairSequence(s, full);
			if (repairedString.length()>0) {
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
		for(Integer i : statesVisited)
			System.out.println(i.toString() + " ");
		
		return statesVisited.size();
	}

	/**
	 * Number of transitions covered by the set of sequences
	 * 
	 * @param seq  : the set of sequences
	 * @param full : the automaton of the system
	 * 
	 * @return the number of transitions covered
	 */
	public static int getNumberOfTransitionsCovered(HashSet<String> seq, Automaton full) {
		HashSet<MsgPairExtended<Integer, Integer, Character>> transitionsCovered = new HashSet<MsgPairExtended<Integer, Integer, Character>>();
		
		for (String s : seq) {
			String repairedString = repairSequence(s, full);
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
		for(MsgPairExtended<Integer, Integer, Character> i : transitionsCovered)
			System.out.println(i.toString() + " ");
		
		return transitionsCovered.size();
	}
	
	/**
	 * Operation to create the sequences using the standard CIT approach
	 * 
	 * @param automatonListForTRecognition : list of the automatons for each
	 *                                        T-Combination of messages
	 * @return : the list of the test strings
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<String> sequencesStandardCIT(ArrayList<Automaton> automatonListForTRecognition)
			throws IOException {
		ArrayList<String> stringList = new ArrayList<String>();
		ArrayList<Automaton> temp = new ArrayList<Automaton>();
		temp = (ArrayList<Automaton>) automatonListForTRecognition.clone();

		while (temp.size() > 0) {
			int numTCombinationConsidered = 0;
			Automaton sequenceAutomaton = temp.get(numTCombinationConsidered);
			temp.remove(0);
			while (numTCombinationConsidered <= ConfigurationData.AUTOMATONS_PER_BATCH && temp.size() > 0) {
				numTCombinationConsidered++;
				sequenceAutomaton = sequenceAutomaton.intersection(temp.get(0));
				temp.remove(0);
			}
			String shortestExample = sequenceAutomaton.getShortestExample(true);
			if (shortestExample != "")
				stringList.add(shortestExample);
			
			// Apply the monitoring operation
			if (ConfigurationData.MONITORING_ENABLED)
				monitoring(temp, shortestExample);
		}

		return stringList;
	}

	/**
	 * Function to repair the sequence
	 * 
	 * @param seq  : the set of sequences
	 * @param full : the automaton of the system
	 * 
	 * @return the repaired sequence
	 */
	public static String repairSequence(String seq, Automaton fullSystemAutomaton) {
		String newStr = "";
		switch (ConfigurationData.REPAIR_MODALITY) {
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
	 * Function compute the number of T-combination couples
	 * 
	 * @param sequences                       : the set of sequences
	 * @param automatonListForTRecognition : the list of the automatons for each
	 *                                        T-combination recognition
	 * @param fullSystemAutomaton
	 * @param full                            : the automaton of the full system
	 * 
	 * @return the number of covered T-combination
	 */
	@SuppressWarnings("unchecked")
	public static int getNumberOfTCombinationCovered(HashSet<String> sequences,
			ArrayList<Automaton> automatonListForTRecognition, Automaton fullSystemAutomaton) {
		int tCombinationCovered = 0;
		ArrayList<Automaton> temp = (ArrayList<Automaton>) automatonListForTRecognition.clone();
				
		for (String s : sequences) {
			String fixedSeq = (!fullSystemAutomaton.run(s)) ? repairSequence(s, fullSystemAutomaton) : s;
			
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
	 * Function compute the number of valid T-combination 
	 * 
	 * @param automatonListForTRecognition : the list of the automatons for each
	 *                                        T-combination recognition
	 * @param fullSystemAutomaton	: the automaton of the full system
	 * 
	 * @return the number of valid T-combination
	 */
	public static int getNumberOfValidTCombinations(ArrayList<Automaton> automatonListForTRecognition, Automaton fullSystemAutomaton) {
		int n = 0;
		
		for(Automaton a : automatonListForTRecognition) {
			if (!fullSystemAutomaton.intersection(a).isEmpty())
				n++;
		}
		
		return n;
	}
}
