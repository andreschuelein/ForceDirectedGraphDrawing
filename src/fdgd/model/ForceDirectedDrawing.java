/**
 * 
 */
package fdgd.model;

/**
 * @author Andre S
 * @version 0.1
 * 
 *  
 */
public class ForceDirectedDrawing extends NetworkBuilder {

	double[][] nodeLocations;
	double[][] totalForce;

	// constants for Eades:
	//* large tree graphs
	double c1=1;	// spring force 		c1*Math.log(distance/c2);
	double c2=1;	// spring scale
	double c3=5000;	// repulsion scale 	c3/Math.pow(distance, 2);
	double c4=1; 	// animation speed

	// constants for Eades:

	//good for complete graphs:
	/*double c1=4;	// spring force 		c1*Math.log(distance/c2);
	double c2=1;	// spring scale
	double c3=5000;	// repulsion scale 	c3/Math.pow(distance, 2);
	double c4=1; 	// animation speed
	 */	// 1 10 10000 1 works but slow
	// 5 1 10000 1 works well, nodes a bit too spread



	double stabilizer1=1;
	double stabilizer2=1;//numON;


	/**
	 * @return the c1
	 */
	public double getC1() {
		return c1;
	}

	/**
	 * @param c1 the c1 to set
	 */
	public void setC1(double c1) {
		this.c1 = c1;
	}

	/**
	 * @return the c2
	 */
	public double getC2() {
		return c2;
	}

	/**
	 * @param c2 the c2 to set
	 */
	public void setC2(double c2) {
		this.c2 = c2;
	}

	/**
	 * @return the c3
	 */
	public double getC3() {
		return c3;
	}

	/**
	 * @param c3 the c3 to set
	 */
	public void setC3(double c3) {
		this.c3 = c3;
	}

	/**
	 * @return the c4
	 */
	public double getC4() {
		return c4;
	}

	/**
	 * @param c4 the c4 to set
	 */
	public void setC4(double c4) {
		this.c4 = c4;
	}

	public ForceDirectedDrawing(int numberOfNodes) {
		super(numberOfNodes);
		nodeLocations = new double [numON][2];
		totalForce = new double [numON][2];

	}

	public void initForces(){
		for (int i = 0; i < numON; i++) {
			totalForce[i][0]=0;
			totalForce[i][1]=0;
		}
	}

	public void generateInitialSpawns(double xBoundary, double yBoundary, double xPadding, double yPadding){
		for (int i = 0; i < numON; i++) {
			nodeLocations[i][0]=Math.random()*(Math.pow(numON,.3)*xBoundary-2*xPadding)+xPadding;
			nodeLocations[i][1]=Math.random()*(Math.pow(numON,.3)*yBoundary-2*yPadding)+yPadding;		
		}

	}

	public void updateNode(int node, double x, double y){
		nodeLocations[node][0]=x;
		nodeLocations[node][1]=y;
	}

	public double getNodeX(int node){
		return nodeLocations[node][0];
	}

	public double getNodeY(int node){
		return nodeLocations[node][1];
	}

	public void setNodeLocation(double x, double y, int node){
		nodeLocations[node][0]=x;
		nodeLocations[node][1]=y;
	}

	public void updateForce(){
		for (int i = 0; i < numON; i++) {
			// loop over all node pairs
			for (int j = 0; j < numON; j++) {
				if (i!=j) {
					if (this.getEdge(i, j)) {
						// if connected
						totalForce[i][0]=totalForce[i][0]+repellingForce(i, j)[0]+attractiveForce(i, j)[0];
						totalForce[i][1]=totalForce[i][1]+repellingForce(i, j)[1]+attractiveForce(i, j)[1];
					} else {
						// if not connected
						totalForce[i][0]=totalForce[i][0]+repellingForce(i, j)[0];
						totalForce[i][1]=totalForce[i][1]+repellingForce(i, j)[1];
					}
				}
			}	
		}
	}

	public void applyForce(int ignoreNode){
		for (int i = 0; i < numON; i++) {
			if(i!=ignoreNode){
				nodeLocations[i][0]=nodeLocations[i][0]+c4*totalForce[i][0];
				nodeLocations[i][1]=nodeLocations[i][1]+c4*totalForce[i][1];
			}
		}
	}

	public double[] computeVector(int node1, int node2){
		double[] vec=
			{this.getNodeX(node2)-this.getNodeX(node1),
				this.getNodeY(node2)-this.getNodeY(node1)};
		return vec;
	}

	public double[] normalizeVector(double x, double y){
		double length=Math.sqrt(Math.pow(Math.abs(x),2)+Math.pow(Math.abs(y),2));
		double[] vec={x/length,y/length};
		return vec;
	}

	public double[] attractiveForce(int from, int to){
		double [] vec;
		double distance=getDistance(from, to);
		vec=computeVector(from, to);//*distance;
		vec=normalizeVector(vec[0], vec[1]);
		double factor = attractiveFunction(distance);
		vec[0]=vec[0]*factor;
		vec[1]=vec[1]*factor;
		return vec;
	}

	private double attractiveFunction(double distance) {
		if (distance<stabilizer1){
			distance=stabilizer1;
			//System.out.println("stabilized");
		}
		return c1*Math.log(distance/c2)*(1/(stabilizer2*numON));
	}

	public double[] repellingForce(int from, int to){
		double [] vec;
		double distance=getDistance(from, to);
		vec=computeVector(from, to);//*distance;
		vec=normalizeVector(vec[0], vec[1]);
		double factor = -repellingFunction(distance);
		vec[0]=vec[0]*factor;
		vec[1]=vec[1]*factor;
		return vec;
	}

	private double repellingFunction(double distance) {
		if (distance<stabilizer1){
			distance=stabilizer1;
			//System.out.println("stabilized");
		}
		return c3/Math.pow(distance, 2);


	}

	public double getDistance(int node1, int node2){
		return Math.sqrt(Math.pow(Math.abs(this.getNodeX(node1)-this.getNodeX(node2)),2)
				+Math.pow(Math.abs(this.getNodeY(node1)-this.getNodeY(node2)),2));

	}

	public void simulateSingleStep(int ignoreNode){
		initForces();
		updateForce();
		applyForce(ignoreNode);
	}

	public double[] getBoundaries(){
		double xMin, xMax, yMin, yMax;
		xMin=getNodeX(0);
		xMax=getNodeX(0);
		yMin=getNodeY(0);
		yMax=getNodeY(0);
		for (int i = 1; i < numON; i++) {
			if (getNodeX(i)<xMin) {
				xMin=getNodeX(i);
			}
			if (getNodeX(i)>xMax) {
				xMax=getNodeX(i);
			}
			if (getNodeY(i)<yMin) {
				yMin=getNodeY(i);
			}
			if (getNodeY(i)>yMax) {
				yMax=getNodeY(i);
			}
		}
		double [] vec={xMin,xMax,yMin,yMax};
		return vec;
	}

}
