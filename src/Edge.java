
public class Edge {

	public String firstNode;
	public String secondNode;
	
	public int firstNodePort;
	public int secondNodePort;
	
	public Edge(String mFirstNode, String mSecondNode, int mFirstNodePort, int mSecondNodePort) {
		
		this.firstNode = mFirstNode;
		this.secondNode = mSecondNode;
		this.firstNodePort = mFirstNodePort;
		this.secondNodePort = mSecondNodePort;
	}
}
