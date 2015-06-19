package fdgd.model;
/**
 * 
 * Models graphs (networks) with a set of nodes and a set of edges.
 * <p>
 * Each pair of nodes and can be connected by exactly one edge or not connected at all.
 * Given a set of N nodes the maximal number of edges is N(N-1).
 * <p>
 * This class is able to generate graphs with the following topologies:
 * <li>fully connected graph: Every pair of nodes is connected by an edge. {@link NetworkBuilder#buildCompleteGraph()}
 * <li>random graph: the edges are drawn uniformly randomly based on a probability p {@link NetworkBuilder#buildRandomGraph(double)}
 * <li>scale free graphs: generated with a preferential attachment rule (rich get richer) {@link NetworkBuilder#buildScaleFreeGraph()}
 * <li>tree graph: every node is connected to a root node or a child of the root node (nodes connected to children of the root node 
 * are considered children of the root node themselves in this context) {@link NetworkBuilder#buildTreeGraph()}
 * <li>fancy tree graph: a special version of a tree graph that also incorporates elements of preferential attachment
 *  {@link NetworkBuilder#buildFancyTreeGraph()}
 * <p>
 * The number of nodes that any given node is connected to determines the degree of said node. The degree is 0 is the node is not connected to any other node in the graph and is N-1 is the node is connected to every other node.
 * @author Andre Schuelein
 */

public class NetworkBuilder {
	int numON=1;// total number of nodes
	boolean[] setOfEdges;// 0 to N*(N-1)/2
	int [] setOfNode; // 0 to N-1, contains the degree of the node
	int [] degreeDistribution;
	int minDegree=0;
	int maxDegree=0;

	public int getMinDegree() {
		return minDegree;
	}

	public int getMaxDegree() {
		return maxDegree;
	}

	public int getDegreeDistribution(int index) {
		return degreeDistribution[index];
	}

	public int getNumON() {
		return numON;
	}

	public void setNumON(int numON) {
		this.numON = numON;

	}

	public boolean[] getSetOfEdges() {
		return setOfEdges;
	}

	public void setSetOfEdges(boolean[] setOfEdges) {
		this.setOfEdges = setOfEdges;
	}

	public int[] getSetOfNode() {
		return setOfNode;
	}

	public void setSetOfNode(int[] setOfNode) {
		this.setOfNode = setOfNode;
	}

	/**
	 * Constructor: Initialises the number of nodes, set of edges and the degree distrubution.
	 * @param numOfNodes the total number of nodes of the graph
	 */
	public NetworkBuilder(int numOfNodes) {
		if (numOfNodes<=0) {
			numON=1;
		}else{
			numON=numOfNodes;
		}

		setOfEdges=new boolean[numOfNodes*(numOfNodes-1)/2];
		degreeDistribution= new int[numON];
		clearGraph();
	}

	/**
	 * Clears the graph by removing all edges and setting all node degrees to 0.
	 */
	public void clearGraph(){
		for (int i = 0; i < setOfEdges.length; i++) {
			setOfEdges[i]=false;
		}
		for (int i = 0; i < degreeDistribution.length; i++) {
			degreeDistribution[i]=0;
		}
	}

	/**
	 * Removes edge from the graph.
	 * @param edge ID of the edge
	 */
	public void removeEdge(int edge){
		setOfEdges[edge]=false;
	}

	/**
	 * Removes the edge between two given nodes.
	 * @param node1 ID of the first node
	 * @param node2 ID of the second node
	 */
	public void removeEdge(int node1, int node2){
		if (node1>=0&&node2>=0&&node1<numON&&node2<numON){
			int i=Math.min(node1, node2);
			int j=Math.max(node1, node2);
			setOfEdges[i*numON-i*(i+1)/2 + j-i-1]=false;
		}else {
			System.err.println("node out of bounds");
		}
	}

	/**
	 * Connects two given nodes with an edge.
	 * @param node1 ID of the first node
	 * @param node2 ID of the second node
	 */
	public void setEdge(int node1, int node2){
		if (node1>=0&&node2>=0&&node1<numON&&node2<numON){
			int i=Math.min(node1, node2);
			int j=Math.max(node1, node2);
			setOfEdges[i*numON-i*(i+1)/2 + j-i-1]=true;
		} else {
			System.err.println("node out of bounds");
		}
	}

	/**
	 * Adds a given edge to the graph.
	 * @param edge
	 */
	public void setEdge(int edge){
		if (edge>=0&&edge<numON) {
			setOfEdges[edge]=true;
		} else {
			System.err.println("node out of bounds");
		}
	}

	/**
	 * Checks whether two given nodes are connected by an edge.
	 * @param node1 ID of the first node
	 * @param node2 ID of the second node
	 * @return true if the nodes are connected, false otherwise
	 */
	public boolean getEdge(int node1, int node2){
		if (node1>=0&&node2>=0&&node1<numON&&node2<numON){
			if (node1==node2) {
				return true;
			} else {
				int i=Math.min(node1, node2);
				int j=Math.max(node1, node2);
				return setOfEdges[i*numON-i*(i+1)/2 + j-i-1];
			}
		} else {
			System.err.println("node out of bounds");
			return false;
		}
	}

	/**
	 * Checks whether a given edge exists.
	 * @param edge ID of the edge
	 * @return true if the edge exists, false otherwise
	 */
	public boolean getEdge(int edge){
		if (edge>=0&&edge<numON) {
			return setOfEdges[edge];
		} else {
			System.err.println("node out of bounds");
			return false;
		}	 
	}

	/**
	 * Generates a folly connected graph.
	 */
	public void buildCompleteGraph(){
		clearGraph();
		for (int i = 0; i < setOfEdges.length; i++) {
			setOfEdges[i]=true;
		}
		for (int i = 0; i < degreeDistribution.length; i++) {
			degreeDistribution[i]=numON-1;
		}
		computeExtremeDegrees();
	}

	/**
	 * Generates a random graph by uniformly randomly drawing edges with a given probability.
	 * @param probability connection probability
	 */
	public void buildRandomGraph(double probability){
		clearGraph();
		for (int i = 0; i < setOfEdges.length; i++) {
			if (Math.random()<=probability) {
				setOfEdges[i]=true;
			} else {
				setOfEdges[i]=false;
			}
		}
		computeDegreeDistibution();
		computeExtremeDegrees();
	}

	/**
	 * Computes the degree distribution of the graph.
	 */
	public void computeDegreeDistibution(){
		for (int i = 0; i < numON; i++) {
			degreeDistribution[i]=0;
			for (int j = 0; j < numON; j++) {
				if (i!=j) {
					if (getEdge(i, j)==true) {
						degreeDistribution[i]++;
					}
				}
			}

		}
	}

	/**
	 * Generates a scale-free graph using preferential attachment.
	 * Each new node is being attached to the existing graph with two edges.
	 * The seed contains two connected nodes.
	 */
	public void buildScaleFreeGraph(){
		int totalDegree=0;
		int numberOfStubs=2;
		clearGraph();
		if (numON>1) {
			// build seed
			setEdge(0,1);
			degreeDistribution[0]=1;
			totalDegree++;
			degreeDistribution[1]=1;
			totalDegree++;
			// loop over nodes the need to be added
			for (int i = 2; i < numON; i++) {
				int[] stubs = new int[numberOfStubs];
				// loop over stubs	
				for (int j = 0; j < numberOfStubs; j++) {
					boolean connected=false;
					double linkProb=Math.random()*(totalDegree-j);
					//loop over candidates
					int c=0;
					double tmp=0;
					while(!connected){
						if(!getEdge(c, i)){
							tmp+=degreeDistribution[c];
							if(linkProb<=tmp){
								// then i->c is new edge
								connected=true;
								stubs[j]=c;
								setEdge(c, i);
								degreeDistribution[i]++;
								degreeDistribution[c]++;
							}
						}
						c++;
					}
				}
				totalDegree+=2;
			} 
		}
		computeExtremeDegrees();
	}

	/**
	 * Generates a generic tree graph, each new node is connected with one edge to the existing graph.
	 * also see {@link NetworkBuilder#buildFancyTreeGraph()}
	 */
	public void buildTreeGraph(){
		clearGraph();
		int j;
		if (numON>1) {
			for (int i = 1; i < numON; i++) { //loop over all nodes
				j=(int)(Math.random()*i);
				setEdge(i,j);
				degreeDistribution[i]++;
				degreeDistribution[j]++;
			}
		} 
		computeExtremeDegrees();
	}

	/**
	 * Generates a fancy version of a tree graph. It combines the methods from {@link NetworkBuilder#buildTreeGraph()} with
	 * {@link NetworkBuilder#buildScaleFreeGraph()}.
	 */
	public void buildFancyTreeGraph(){
		clearGraph();
		int j=0;
		if (numON>1) {
			for (int i = 1; i < numON; i++) { //loop over all nodes
				if (Math.random()<.4) {//random attachment
					j=(int)(Math.random()*i);
				}else{//preferential attachment
					int totalDegree=0;
					boolean connected=false;
					for (int k = 0; k < i; k++) {
						totalDegree+=degreeDistribution[k];
					}
					double linkProb=Math.random()*(totalDegree);
					int c=0;
					double tmp=0;
					while(!connected){
						tmp+=degreeDistribution[c];
						if(linkProb<=tmp){
							// then i->c is new edge
							connected=true;
							j=c;
						}
						c++;
					}
				}
				setEdge(i,j);
				degreeDistribution[i]++;
				degreeDistribution[j]++;
			}
		} 
		computeExtremeDegrees();
	}

	/**
	 * Computes the maximum and minimum degree.
	 */
	public void computeExtremeDegrees(){
		minDegree=degreeDistribution[0];
		maxDegree=degreeDistribution[0];
		for (int i = 1; i < degreeDistribution.length; i++) {
			if (minDegree>degreeDistribution[i]) {
				minDegree=degreeDistribution[i];
			}
			if (maxDegree<degreeDistribution[i]) {
				maxDegree=degreeDistribution[i];
			}
		}
	}

}
