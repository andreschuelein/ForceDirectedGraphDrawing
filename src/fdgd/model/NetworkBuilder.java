package fdgd.model;

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
 
	public void clearGraph(){
		for (int i = 0; i < setOfEdges.length; i++) {
			setOfEdges[i]=false;
		}
		for (int i = 0; i < degreeDistribution.length; i++) {
			degreeDistribution[i]=0;
		}
	}
 
	public void removeEdge(int node){
		setOfEdges[node]=false;
	}
 
	public void removeEdge(int node1, int node2){
		if (node1>=0&&node2>=0&&node1<numON&&node2<numON){
			int i=Math.min(node1, node2);
			int j=Math.max(node1, node2);
			setOfEdges[i*numON-i*(i+1)/2 + j-i-1]=false;
		}else {
			System.err.println("node out of bounds");
		}
	}
 
 	public void setEdge(int node1, int node2){
 		if (node1>=0&&node2>=0&&node1<numON&&node2<numON){
 			int i=Math.min(node1, node2);
 			int j=Math.max(node1, node2);
 			//System.out.println(i+"->"+j+" : "+(i*numON-i*(i+1)/2 + j-i-1));
 			setOfEdges[i*numON-i*(i+1)/2 + j-i-1]=true;
 		} else {
 			System.err.println("node out of bounds");
 		}
 	}
 
 	public void setEdge(int node){
 		if (node>=0&&node<numON) {
 			setOfEdges[node]=true;
 		} else {
 			System.err.println("node out of bounds");
 		}
 	}
 
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
 
 	public boolean getEdge(int node){
 		if (node>=0&&node<numON) {
 			return setOfEdges[node];
 		} else {
 			System.err.println("node out of bounds");
 			return false;
 		}	 
 	}
 
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
 	
 	public void buildNiceTreeGraph(){
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
 		//System.out.println(minDegree+" "+maxDegree);
 	}
 	
}
