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
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import com.google.common.collect.BiMap;

import ctwedge.util.Test;
import dk.brics.automaton.Automaton;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcState;
import net.sf.smc.model.SmcTransition;
import net.sf.smc.model.TargetLanguage;
import net.sf.smc.parser.SmcParser;

public class FSMAutomatonBuilderWithCondition extends FSMAutomatonBuilder {

	/**
	 * Function that builds the automaton that represents the FSM starting from the
	 * SMC description * @param fsm
	 * 
	 * @return
	 * @throws IOException
	 */
	public static Automaton buildFSMAutomatonFromSMC(BiMap<String, Character> msgsMapping, Test product,
			String fsmFile, String projectName) throws IOException, IllegalAccessException, InvocationTargetException {

		// Logger
		Logger logger = Logger.getLogger(FSMAutomatonBuilder.class);

		// SMC file
		File f = new File(fsmFile);

		// Parse the SMC file
		SmcParser p = new SmcParser(projectName, new FileInputStream(f), TargetLanguage.LANG_NOT_SET, false);
		SmcFSM fsMachine = p.parse();

		// Get the list of the states
		ArrayList<SmcMap> stateMaps = (ArrayList<SmcMap>) fsMachine.getMaps();

		// Create the new automaton
		HashSet<String> msgList = new HashSet<>();
		ArrayList<FSMState> fsm = new ArrayList<>();
		String startState, inMessage, endState, outMessage, condition;
		char code = 64;

		logger.debug("----------------------------------");
		logger.debug("CREATING THE FINITE STATE MACHINE\n");
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

						// Evaluate the condition if a specific product is given
						if (product != null) {
							Map<String, String> assignments = product.entrySet().stream().collect(
									Collectors.toMap(Entry<String, String>::getKey, Entry<String, String>::getValue));
							Map<String, Boolean> assignmentsBoolean = new HashMap<>();
							assignments.entrySet().stream().forEach(entry -> {
								assignmentsBoolean.put(entry.getKey(), Boolean.valueOf(entry.getValue()));
							});
							
							if (MVEL.evalToBoolean(condition, assignmentsBoolean)) {
								// Save the messages and the FSM
								msgList.add(inMessage);
								if (!outMessage.equals("no_response"))
									msgList.add(outMessage);
								fsm.add(new FSMState(startState, endState, inMessage, outMessage, condition));
								logger.debug("Adding new state: " + startState + "(" + inMessage + ") -> " + endState + "("
										+ outMessage + ")\n");
							}
						} else {
							// Save the messages and the FSM without checking any condition
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
		}

		// Write the messages file and build the mapping between extended messages and
		// single char
		for (String s : msgList) {
			while (code == ')' || code == '(' || code == '*' || code == '?' || code == '&' || code == '|' || code == '+'
					|| code == '@' || code == '}' || code == '{' || code == '~' || code == '^' || code == '['
					|| code == ']' || code == '-' || code == '.' || code == '#' || code == '\\' || code == '_'
					|| code == '<' || code == '>')
				code++;
			msgsMapping.put(s.toUpperCase(), code);
			code++;
		}

		logger.debug("END: CREATING THE FINAL STATE MACHINE\n");

		// Return the built automaton
		return createAutomatonFromFSM(fsm, msgsMapping);
	}

}
