
public class MsgPairExtended<T,K,V> extends MsgPair<T,K> {
		
	private V event;
		
	public MsgPairExtended(T first, K second, V event) {
		super(first,second);
		this.event = event;
	}
	
	public V getEvent() {
		return event;
	}

	public void setEvent(V event) {
		this.event = event;
	}
	
	public String toString() {
		return "(" + super.getFirst().toString() + " - " + super.getSecond().toString() + ") with " + event.toString(); 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((event == null) ? 0 : event.hashCode());
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
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
		MsgPairExtended other = (MsgPairExtended) obj;
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
		return true;
	}
}
