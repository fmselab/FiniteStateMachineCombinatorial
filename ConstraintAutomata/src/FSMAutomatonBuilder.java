import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.BiMap;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcTransition;
import net.sf.smc.model.TargetLanguage;
import net.sf.smc.parser.SmcParser;

public class FSMAutomatonBuilder {

	static Automaton buildFSMAutomatonFromTXT(BiMap<String, Character> msgsMapping) throws IOException {
		String readLn, startState, inMessage, endState, outMessage;
		File f = new File(ConfigurationData.FSM_FILE);
		FileReader fin = new FileReader(f.getAbsolutePath());
		BufferedReader fsmFile = new BufferedReader(fin);
		FileWriter fout = new FileWriter(ConfigurationData.MESSAGES_FILE);
		FileWriter flog = new FileWriter(ConfigurationData.LOG_FILE,true);
		BufferedWriter msgFile = new BufferedWriter(fout);
		HashSet<String> msgList = new HashSet<>();
		ArrayList<FSMState> fsm = new ArrayList<>();
		char code = 64;

		flog.write("----------------------------------");
		flog.write("CREATING THE FINAL STATE MACHINE\n");
		flog.write("----------------------------------");
		
		/*
		 * Read the content of the file, Create the automaton of the FSM, Create the
		 * file containing the possible messages
		 */
		while (true) {
			// Read the ASM File
			readLn = null;
			try {
				readLn = fsmFile.readLine();
			} catch (IOException e) {
				break;
			}
			if (readLn == null || readLn.startsWith("Set of Sequences :"))
				break;
			if (readLn.startsWith("Finite State Machine :"))
				continue;
			// Extract information
			startState = readLn.split(",\t")[0].replace("\t", "").replaceAll("\\s+", "");
			inMessage = readLn.split(",\t")[1].replaceAll("\\s+", "");
			if (inMessage.equals("noresponse")) inMessage="no response";
			endState = readLn.split(",\t")[2].replaceAll("\\s+", "");
			outMessage = readLn.split(",\t")[3].replaceAll("\\s+", "");
			if (outMessage.equals("noresponse")) outMessage="no response";
			// Save the messages and the FSM
			msgList.add(inMessage);
			msgList.add(outMessage);
			fsm.add(new FSMState(startState, endState, inMessage, outMessage));
			flog.write("Adding new state: " + startState + "(" + inMessage + ") -> " + endState + "(" + outMessage + ")\n");
		}

		// Write the messages file and build the mapping between extended messages and
		// single char
		for (String s : msgList) {
			msgFile.write(s.toUpperCase() + "\n");
			while (code == ')' || code == '(' || code == '*' || code == '?' || code == '&' || code == '|' || code == '+'
					|| code == '@' || code == '}' || code == '{' || code == '~' || code == '^' || code == '['
					|| code == ']' || code == '-' || code == '.' || code == '#' || code == '\\' || code == '_'
					|| code == '<' || code == '>')
				code++;
			msgsMapping.put(s.toUpperCase(), code);
			code++;
		}
		
		flog.write("END: CREATING THE FINAL STATE MACHINE\n");

		// Close all the streams
		fsmFile.close();
		fin.close();
		msgFile.close();
		fout.close();
		flog.close();

		// Return the built automaton
		return createAutomatonFromFSM(fsm, msgsMapping);
	}
	
	static Automaton buildFSMAutomatonFromSMC(BiMap<String, Character> msgsMapping) throws IOException, IllegalAccessException, InvocationTargetException {
		File f = new File(ConfigurationData.FSM_FILE);
		
		// Parse the SMC file
		SmcParser p = new SmcParser(ConfigurationData.PROJECT_NAME, new FileInputStream(f), TargetLanguage.LANG_NOT_SET, false);
		SmcFSM fsMachine = p.parse();
		
		// Get the list of the states
		ArrayList<SmcMap> stateMaps = (ArrayList<SmcMap>) fsMachine.getMaps();
		
		// Create the new automaton
		HashSet<String> msgList = new HashSet<>();
		ArrayList<FSMState> fsm = new ArrayList<>();
		FileWriter fout = new FileWriter(ConfigurationData.MESSAGES_FILE);
		FileWriter flog = new FileWriter(ConfigurationData.LOG_FILE,true);
		BufferedWriter msgFile = new BufferedWriter(fout);
		String startState, inMessage, endState, outMessage;
		char code = 64;

		flog.write("----------------------------------");
		flog.write("CREATING THE FINAL STATE MACHINE\n");
		flog.write("----------------------------------");
		
		for (SmcMap s : stateMaps) {
			// Get the start state name
			startState = s.getName();
			for (SmcTransition t : s.getTransitions()) {
				inMessage = "";
				endState = "";
				outMessage = "";
				
				// Save the messages and the FSM
				msgList.add(inMessage);
				msgList.add(outMessage);
				fsm.add(new FSMState(startState, endState, inMessage, outMessage));
				flog.write("Adding new state: " + startState + "(" + inMessage + ") -> " + endState + "(" + outMessage + ")\n");
			}
		}

		// Write the messages file and build the mapping between extended messages and
		// single char
		for (String s : msgList) {
			msgFile.write(s.toUpperCase() + "\n");
			while (code == ')' || code == '(' || code == '*' || code == '?' || code == '&' || code == '|' || code == '+'
					|| code == '@' || code == '}' || code == '{' || code == '~' || code == '^' || code == '['
					|| code == ']' || code == '-' || code == '.' || code == '#' || code == '\\' || code == '_'
					|| code == '<' || code == '>')
				code++;
			msgsMapping.put(s.toUpperCase(), code);
			code++;
		}
		
		flog.write("END: CREATING THE FINAL STATE MACHINE\n");

		// Close all the streams
		msgFile.close();
		fout.close();
		flog.close();

		// Return the built automaton
		return createAutomatonFromFSM(fsm, msgsMapping);
	}

	/**
	 * Function that builds the automaton that represents the FSM
	 *  
	 * @param fsm
	 * @return
	 * @throws IOException 
	 */
	static Automaton createAutomatonFromFSM(ArrayList<FSMState> fsm, BiMap<String, Character> msgsMapping) throws IOException {	
		Automaton automa = new Automaton();
		FileWriter flog = new FileWriter(ConfigurationData.LOG_FILE,true);
		Boolean find = false;
		HashMap<String, State> stateMap = new HashMap<>();
		State initialState = new State();
		initialState.setAccept(true);
		automa.setInitialState(initialState);
		stateMap.put(fsm.get(0).getStartStateName(), initialState);

		flog.write("----------------------------------");
		flog.write("CREATING THE AUTOMATON FROM THE FSM\n");
		flog.write("----------------------------------");
		
		// Cycle over all the read states
		for (FSMState s : fsm) {
			Set<State> states = automa.getStates();
			State toState = null;
			State fromState = null;			
			//-------------------------------
			// Find the start state
			find = false;
			for(State st : states) {
				if(st.equals(stateMap.get(s.getStartStateName()))) {
					fromState = st;
					find = true;
				}
			}
			// If the start state has not been found, create a new state
			if (!find) {
				fromState = new State();
				fromState.setAccept(true);
				stateMap.put(s.getStartStateName(), fromState);
			}
			//-------------------------------
			// Find the end state
			find = false;
			for(State st : states) {
				if(st.equals(stateMap.get(s.getFinalStateName()))) {
					toState = st;
					find = true;
				}
			}
			// If the final state has not been found, create a new state
			if (!find) {
				toState = new State();
				toState.setAccept(true);		
				stateMap.put(s.getFinalStateName(), toState);
			}
			//-------------------------------
			// Create the new transition
			fromState.addTransition(new Transition(msgsMapping.get(s.getReceivedMsg().toUpperCase()),toState));
		}
		
		flog.write("END: CREATING THE AUTOMATON FROM THE FSM\n");
		
		// Close the log file
		flog.close();
		
		return automa;
	}

}
