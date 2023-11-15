package util;

/**
 * Class representing the transitions in a FSM. We save, for each transition: -
 * startStateName: that is the name of the initial state - finalStateName: that
 * is the name of the final state, when executing the transition - receivedMsg:
 * that is the message that trigger the transition - sentMsg: that is the output
 * event when a transition is executed
 * 
 * @author Andrea Bombarda
 *
 */
public class FSMState {
	private String startStateName;
	private String finalStateName;
	private String receivedMsg;
	private String sentMsg;
	private String condition;

	public FSMState(String startStateName, String finalStateName, String receivedMsg, String sentMsg,
			String condition) {
		this.startStateName = startStateName;
		this.finalStateName = finalStateName;
		this.receivedMsg = receivedMsg;
		this.sentMsg = sentMsg;
		this.condition = condition;
	}

	public String getStartStateName() {
		return startStateName;
	}

	public void setStartStateName(String startStateName) {
		this.startStateName = startStateName;
	}

	public String getFinalStateName() {
		return finalStateName;
	}

	public void setFinalStateName(String finalStateName) {
		this.finalStateName = finalStateName;
	}

	public String getReceivedMsg() {
		return receivedMsg;
	}

	public void setReceivedMsg(String receivedMsg) {
		this.receivedMsg = receivedMsg;
	}

	public String getSentMsg() {
		return sentMsg;
	}

	public void setSentMsg(String sentMsg) {
		this.sentMsg = sentMsg;
	}

	public String getCondition() {
		return condition;
	}
}
