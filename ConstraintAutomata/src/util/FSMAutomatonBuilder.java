package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.util.SupplierUtil;

import com.google.common.collect.BiMap;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import javafx.util.Pair;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcState;
import net.sf.smc.model.SmcTransition;
import net.sf.smc.model.TargetLanguage;
import net.sf.smc.parser.SmcParser;

/**
 * The Class FSMAutomatonBuilder contains methods building automatons from state
 * machines
 */
public class FSMAutomatonBuilder {

	/**
	 * This method creates the transitions list starting from a State Machine
	 * described using SMC.
	 *
	 * @param msgIntegerMapping the mapping between integers and string messages
	 * @param fsmFilePath       the fsm file path
	 * @param projectName       the project name
	 * @return the list of Transitions
	 * @throws IllegalAccessException    the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws IOException               Signals that an I/O exception has occurred.
	 */
	public static Graph<String, Integer> convertSMCToGraph(ArrayList<Pair<Integer, String>> msgIntegerMapping,
			String fsmFilePath, String projectName)
			throws IllegalAccessException, InvocationTargetException, IOException {
		int start = 0;

		File f = new File(fsmFilePath);

		// Parse the SMC file
		SmcParser p = new SmcParser(projectName, new FileInputStream(f), TargetLanguage.LANG_NOT_SET, false);
		SmcFSM fsMachine = p.parse();

		// Get the list of the states and compose the Graph
		ArrayList<SmcMap> stateMaps = (ArrayList<SmcMap>) fsMachine.getMaps();
		String startState, inMessage, endState;
		Graph<String, Integer> graph = new DirectedPseudograph<String, Integer>(SupplierUtil.createStringSupplier(),
				SupplierUtil.createIntegerSupplier(
						fsMachine.getTransitions().size() * fsMachine.getMaps().get(0).getAllStates().size()),
				false);

		// Get all the informations contained into the SMC file and compose the Graph
		for (SmcMap s : stateMaps) {
			for (SmcState st : s.getStates()) {
				for (SmcTransition t : st.getTransitions()) {

					startState = t.getState().getName().toString().split("[.]")[0];
					if (!graph.containsVertex(startState))
						graph.addVertex(startState);

					for (SmcGuard g : t.getGuards()) {
						inMessage = g.getName();
						msgIntegerMapping.add(new Pair<Integer, String>(start++, inMessage));
						endState = g.getEndState().split("[.]")[0];

						if (!graph.containsVertex(endState))
							graph.addVertex(endState);

						// Add the transition
						graph.addEdge(startState, endState, start - 1);
					}
				}
			}
		}

		return graph;
	}

	/**
	 * Function that builds the automaton that represents the FSM starting from the
	 * SMC description
	 * 
	 *
	 * @param msgsMapping the msgs mapping
	 * @param fsmFilePath the fsm file path
	 * @param projectName the project name
	 * @param msgFilePath the msg file path
	 * @return the automaton
	 * @throws IOException               Signals that an I/O exception has occurred.
	 * @throws IllegalAccessException    the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 */
	public static Automaton buildFSMAutomatonFromSMC(BiMap<String, Character> msgsMapping, String fsmFilePath,
			String projectName, String msgFilePath)
			throws IOException, IllegalAccessException, InvocationTargetException {
		File f = new File(fsmFilePath);

		// Parse the SMC file
		SmcParser p = new SmcParser(projectName, new FileInputStream(f), TargetLanguage.LANG_NOT_SET, false);
		SmcFSM fsMachine = p.parse();

		// Get the list of the states
		ArrayList<SmcMap> stateMaps = (ArrayList<SmcMap>) fsMachine.getMaps();

		// Create the new automaton
		Logger logger = Logger.getLogger(FSMAutomatonBuilder.class);
		HashSet<String> msgList = new HashSet<>();
		ArrayList<FSMState> fsm = new ArrayList<>();
		FileWriter fout = new FileWriter(msgFilePath);
		BufferedWriter msgFile = new BufferedWriter(fout);
		String startState, inMessage, endState, outMessage, condition;
		char code = 64;

		logger.debug("----------------------------------");
		logger.debug("CREATING THE FINAL STATE MACHINE\n");
		logger.debug("----------------------------------");

		for (SmcMap s : stateMaps) {
			for (SmcState st : s.getStates()) {
				for (SmcTransition t : st.getTransitions()) {
					startState = t.getState().getName().toString().split("[.]")[0];
					condition = "";
					for (SmcGuard g : t.getGuards()) {
						inMessage = g.getName();
						endState = g.getEndState().split("[.]")[0];
						outMessage = g.getActions().get(0).getName();
						condition = g.getCondition();

						// Save the messages and the FSM
						msgList.add(inMessage);
						if (!outMessage.equals("no_response"))
							msgList.add(outMessage);
						fsm.add(new FSMState(startState, endState, inMessage, outMessage, condition));
						logger.debug("Adding new state: " + startState + "(" + inMessage + ") -> " + endState + "("
								+ outMessage + ")\n");
					}
				}
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

		logger.debug("END: CREATING THE FINAL STATE MACHINE\n");

		// Close all the streams
		msgFile.close();
		fout.close();

		// Return the built automaton
		return createAutomatonFromFSM(fsm, msgsMapping);
	}

	/**
	 * Function that builds the automaton that represents the FSM
	 * 
	 *
	 * @param fsm         the fsm
	 * @param msgsMapping the msgs mapping
	 * @return the automaton
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected static Automaton createAutomatonFromFSM(ArrayList<FSMState> fsm, BiMap<String, Character> msgsMapping)
			throws IOException {
		// Logger
		Logger logger = Logger.getLogger(FSMAutomatonBuilder.class);

		Automaton automa = new Automaton();
		Boolean find = false;
		HashMap<String, State> stateMap = new HashMap<>();
		State initialState = new State();
		initialState.setAccept(true);
		automa.setInitialState(initialState);
		stateMap.put(fsm.get(0).getStartStateName(), initialState);

		logger.debug("----------------------------------");
		logger.debug("CREATING THE AUTOMATON FROM THE FSM\n");
		logger.debug("----------------------------------");

		// Cycle over all the read states
		for (FSMState s : fsm) {
			Set<State> states = automa.getStates();
			State toState = null;
			State fromState = null;
			// -------------------------------
			// Find the start state
			find = false;
			for (State st : states) {
				if (st.equals(stateMap.get(s.getStartStateName()))) {
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
			// -------------------------------
			// Find the end state
			find = false;
			for (State st : states) {
				if (st.equals(stateMap.get(s.getFinalStateName()))) {
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
			// -------------------------------
			// Create the new transition
			fromState.addTransition(new Transition(msgsMapping.get(s.getReceivedMsg().toUpperCase()), toState));
		}

		logger.debug("END: CREATING THE AUTOMATON FROM THE FSM\n");

		return automa;
	}

}
