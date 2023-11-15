package util;

/**
 * Class representing a triad of messages, with two events among them
 * 
 * @param <T> the type of the first message
 * @param <K> the type of the second message
 * @param <Z> the type of the third message
 * @param <V> the type of the first event
 * @param <X> the type of the second event
 */
public class MsgTriadExtended<T, K, V, Z, X> extends MsgTriad<T, K, Z> {

	private V event;
	private X secondEvent;

	public MsgTriadExtended(T first, K second, V event, Z third, X secondEvent) {
		super(first, second, third);
		this.event = event;
		this.secondEvent = secondEvent;
	}

	public X getSecondEvent() {
		return secondEvent;
	}

	public void setSecondEvent(X secondEvent) {
		this.secondEvent = secondEvent;
	}

	public V getEvent() {
		return event;
	}

	public void setEvent(V event) {
		this.event = event;
	}

	public String toString() {
		return "1: (" + first.toString() + " - " + second.toString() + ") with " + event.toString() + "\n" + "2: ("
				+ second.toString() + " - " + third.toString() + ") with " + secondEvent.toString() + "\n";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((event == null) ? 0 : event.hashCode());
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		result = prime * result + ((secondEvent == null) ? 0 : secondEvent.hashCode());
		result = prime * result + ((third == null) ? 0 : third.hashCode());
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MsgTriadExtended other = (MsgTriadExtended) obj;
		if (event == null) {
			if (other.event != null)
				return false;
		} else if (!event.equals(other.event))
			return false;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		if (secondEvent == null) {
			if (other.secondEvent != null)
				return false;
		} else if (!secondEvent.equals(other.secondEvent))
			return false;
		if (third == null) {
			if (other.third != null)
				return false;
		} else if (!third.equals(other.third))
			return false;
		return true;
	}
}
