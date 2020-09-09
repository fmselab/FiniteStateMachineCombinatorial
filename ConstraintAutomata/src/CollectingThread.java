import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import dk.brics.automaton.Automaton;

public class CollectingThread extends Thread {

	private HashSet<String> sequences;
	private boolean batchProcessing;
	private ArrayList<Automaton> automatonList;
	private Automaton fullSystemAutomaton;
	private ArrayList<Automaton> fullSystemAutomatonList;
	
	public ArrayList<Automaton> getFullSystemAutomatonList() {
		return fullSystemAutomatonList;
	}

	public void setFullSystemAutomatonList(ArrayList<Automaton> fullSystemAutomatonList) {
		this.fullSystemAutomatonList = fullSystemAutomatonList;
	}

	public boolean isBatchProcessing() {
		return batchProcessing;
	}

	public void setBatchProcessing(boolean batchProcessing) {
		this.batchProcessing = batchProcessing;
	}
	
	public HashSet<String> getSequences() {
		return sequences;
	}

	public void setSequences(HashSet<String> sequences) {
		this.sequences = sequences;
	}
	
	public ArrayList<Automaton> getAutomatonList() {
		return automatonList;
	}

	public void setAutomatonList(ArrayList<Automaton> automatonList) {
		this.automatonList = automatonList;
	}

	public Automaton getFullSystemAutomaton() {
		return fullSystemAutomaton;
	}

	public void setFullSystemAutomaton(Automaton fullSystemAutomaton) {
		this.fullSystemAutomaton = fullSystemAutomaton;
	}
	
	public synchronized void writeSeq(ArrayList<String> seqs) {
		sequences.addAll(seqs);
	}
	
	/**
	 * The collecting thread executes the collecting operation on the assigned list of
	 * automata 
	 */
	@Override
	public void run() {
		if (this.batchProcessing)
			for(int i=0;i<fullSystemAutomatonList.size();i++)
				try {
					writeSeq(Utils.collecting(fullSystemAutomatonList.get(i), automatonList));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		else
			try {
				writeSeq(Utils.collecting(fullSystemAutomaton, automatonList));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
