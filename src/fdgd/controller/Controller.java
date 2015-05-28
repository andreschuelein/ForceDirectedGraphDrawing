package fdgd.controller;


import fdgd.model.ForceDirectedDrawing;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;


public class Controller {
	private GraphicsContext gc;
	private final double canW=1000; //canvas width
	private final double canH=1000; //canvas height
	private final double contW=200;	//control area width
	private Color canvasColor=Color.rgb(92,93,112); //canvas background color
	private Color nodeColor=Color.DARKSLATEGRAY; //node color
	private Color nodeTargetColor=Color.rgb(237,74,74);
	private Color edgeColor=Color.rgb(211,217,206); // edge color
	private double defaultNodeSize=10;//canW/100;
	private double nodeSize=defaultNodeSize;	// draw diameter of a node
	private double nodeZoomScale=.1;
	private boolean nodeBorder=true;
	private double nodeBorderSize=2;
	private Color nodeBorderColor=Color.BLACK;
	private ForceDirectedDrawing fdd;
	private double paddingFactor=0.2;
	private final int defaultNumOfNodes=40;
	private AnimationTimer timer;
	private final double defaultProbability=0.1;
	private final int defaultNumberOfStubs=2;
	private boolean continueAnimation=false;
	private double shiftX=0;
	private double shiftY=0;
	private double zoom=1;
	private double stepsPerFrame=20;
	private boolean autoZoom=true;
	private double dragOriginX;
	private double dragOriginY;
	private double zoomFactor=.2;
	private boolean tooltip=true; //tooltip toggle
	private boolean graphDrawn=false;
	private boolean simulationActive=false;
	private boolean started=false;
	private boolean mousePressed=false;
	private int draggedNode=-1;
	private int tooltipNode=-1;
	
	
	@FXML private SplitPane splitPane;
	@FXML private Label nodeLabel;
	@FXML private Canvas drawArea;
	@FXML private TextField textField;	
	@FXML private ChoiceBox<String> cbox;
	@FXML private Label c1label;
	@FXML private Label c2label;
	@FXML private Label c3label;
	@FXML private Label c4label;
	@FXML private Slider slider1;
	@FXML private Slider slider2;
	@FXML private Slider slider3;
	@FXML private Slider slider4;
	
	@FXML 
	public void initialize(){
		fdd = new ForceDirectedDrawing(defaultNumOfNodes);
		gc = drawArea.getGraphicsContext2D();
		nodeLabel.setVisible(false);
		nodeLabel.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 20px;");
		splitPane.setDividerPositions(canW/(canW+contW),contW/(canW+contW));
		textField.setPromptText("please input the number of nodes");
		drawArea.setHeight(canH);
		drawArea.setWidth(canW);
		
		drawArea.setOnMouseDragged(new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent event) {
				if (draggedNode!=-1) {
					fdd.setNodeLocation(drawSpaceToCoordinateSpace(event.getX(), event.getY())[0],
							drawSpaceToCoordinateSpace(event.getX(), event.getY())[1], draggedNode);
				}
				if (tooltipNode!=-1) {
					nodeLabel.setText("node: "+Integer.toString(tooltipNode)+"\ndegree: "+fdd.getDegreeDistribution(tooltipNode));
					nodeLabel.setVisible(true);
					nodeLabel.setLayoutX(event.getX()+25);
					nodeLabel.setLayoutY(event.getY()-25);
				} else {
					nodeLabel.setVisible(false);
				}
				
			}
		});
		
		drawArea.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				draggedNode=checkForMouseNodeCollision(event.getX(), event.getY());
				}
		});
		
		drawArea.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				draggedNode=-1;
			}
		});
		
		drawArea.setOnMouseMoved(new EventHandler<MouseEvent>() {
			@Override public void handle(MouseEvent event){
				tooltipNode=checkForMouseNodeCollision(event.getX(),event.getY());
				if (tooltipNode!=-1) {
					nodeLabel.setText("node: "+Integer.toString(tooltipNode)+"\ndegree: "+fdd.getDegreeDistribution(tooltipNode));
					nodeLabel.setVisible(true);
					nodeLabel.setLayoutX(event.getX()+25);
					nodeLabel.setLayoutY(event.getY()-25);
				} else {
					nodeLabel.setVisible(false);
				}
			}
		});
		
		drawArea.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				if (!autoZoom) {
					if (event.getDeltaY()>0) { // scroll up - zoom in
						zoom+=zoomFactor;
					}else
						if (zoom-zoomFactor>0) {
							zoom-=zoomFactor; //scroll down - zoom out
						}
				}
			}
		});
		initSliders();
		
		
		cbox.getItems().addAll("auto zoom/pane","free zoom/pane");
		cbox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {	
				switch ((int)newValue) {
				case 0:
					autoZoom=true;
					System.out.println("auto zoom on");
					break;
				case 1:
					autoZoom=false;
					System.out.println("auto zoom off");
				default:
					break;
				}
			}
		});
		//START ANIMATION
		animationIni();
		startRenderingAndAnimation();
	}
	
	private void initSliders() {
		initSlider(slider1,1,900);
		c1label.setText(Double.toString(slider1.getValue()));
		slider1.valueProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue observable, Object oldValue,
					Object newValue) {
				c1label.setText(Double.toString(slider1.getValue()));
				fdd.setC1(slider1.getValue());
				
			}
		});
		initSlider(slider2, 0.1, 2);
		c2label.setText(Double.toString(slider2.getValue()));
		slider2.valueProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue observable, Object oldValue,
					Object newValue) {
				c2label.setText(Double.toString(slider2.getValue()));
				fdd.setC2(slider2.getValue());
				
			}
		});
		initSlider(slider3, 1, 10000);
		c3label.setText(Double.toString(slider3.getValue()));
		slider3.valueProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue observable, Object oldValue,
					Object newValue) {
				c3label.setText(Double.toString(slider3.getValue()));
				fdd.setC3(slider3.getValue());
				
			}
		});
		initSlider(slider4, 1, 100);
		c4label.setText(Double.toString(slider4.getValue()));
		slider4.valueProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue observable, Object oldValue,
					Object newValue) {
				c4label.setText(Double.toString(slider4.getValue()));
				fdd.setC4(slider4.getValue());
				
			}
		});
	}

	private void initSlider(Slider slider, double min, double max){
		slider.setShowTickMarks(true);
		slider.setMajorTickUnit(50);
		slider.setMinorTickCount(5);
		slider.setMin(min);
		slider.setMax(max);
	}
	
	@FXML
	protected void exitButtonPressed(ActionEvent event){
		System.exit(0);
	}
	
	@FXML 
	protected void fullyConnectedPressed(ActionEvent event){
		readTextField();
		startDrawing();
		fdd.buildCompleteGraph();
		startRenderingAndAnimation(); 
	}
	
	@FXML 
	protected void randomGraphPressed(ActionEvent event){
		readTextField();
		startDrawing();
		fdd.buildRandomGraph(defaultProbability);
		startRenderingAndAnimation();
	}
	
	@FXML 
	protected void scaleFreeGraphPressed(ActionEvent event){
		readTextField();
		startDrawing();
		fdd.buildScaleFreeGraph();
		startRenderingAndAnimation();
	}
	
	@FXML
	protected void treeGraphPressed(ActionEvent event){
		readTextField();
		startDrawing();
		//fdd.buildTreeGraph();
		fdd.buildFancyTreeGraph();
		startRenderingAndAnimation();
		
	}
	
	@FXML
	protected void singleStepPressed(ActionEvent event){
		fdd.simulateSingleStep(draggedNode);
		//renderGraph();
	}
	
	@FXML
	protected void tenStepsPressed(ActionEvent event){
		for (int i = 0; i < 10; i++) {
			fdd.simulateSingleStep(draggedNode);
		}		
		//renderGraph();
	}
	
	@FXML
	protected void startAnimationPressed(ActionEvent event){
		simulationActive=true;
		//timer.start();
		//continueAnimation=true;
		
	}
	
	@FXML
	protected void stopAnimationPressed(ActionEvent event){
		simulationActive=false;
		//timer.stop();
		//continueAnimation=false;
	}
	
	private void readTextField(){
		 if ((textField.getText() != null && !textField.getText().isEmpty())) {
			 if ( textField.getText().matches("[0-9]{1,5}")) {
				 fdd= new ForceDirectedDrawing(Integer.parseInt(textField.getText()));
				 textField.clear();
				 textField.setPromptText("set new number of nodes here");
				
			} else {
				textField.clear();
				textField.setPromptText("invalid - input the number of nodes");;
			}
			 textField.clear();
			 textField.setPromptText("please input the number of nodes");
		 }
	}
	
	private void startRenderingAndAnimation(){
		 fdd.generateInitialSpawns(canW, canH, paddingFactor*canW, paddingFactor*canH);
		 //if (continueAnimation) {
				//timer.stop();
		//	}
		 //nodeSize=defaultNodeSize;
		 //renderGraph();
		// animationIni();
		// if (continueAnimation) {
			timer.start();
		//}
	}
	
	private void renderGraph(){
		//draw background
		gc.setFill(canvasColor);
		gc.fillRect(0, 0, canW, canH);
		if (autoZoom) {
			iniCenterAndZoom();
		}
		if (started) {
			drawEdges();
			drawNodes();
			graphDrawn=true;
		}
	}
	
	private void drawNodes(){
		gc.setFill(nodeColor);
		for (int i = 0; i < fdd.getNumON(); i++) {
			if(fdd.getMaxDegree()!=fdd.getMinDegree()){
				double colorFactor=(double)(fdd.getDegreeDistribution(i)-fdd.getMinDegree())/(fdd.getMaxDegree()-fdd.getMinDegree());
				//System.out.println("CF:"+colorFactor+" D(i)"+fdd.getDegreeDistribution(i)+" min:"+fdd.getMinDegree()+" max:"+fdd.getMaxDegree());
				gc.setFill(nodeColor.interpolate(nodeTargetColor, colorFactor));
			}
			if(fdd.getMinDegree()!=fdd.getMaxDegree()){ //doesn't solve the issue for high density graphs
				drawSingleNode(fdd.getNodeX(i), fdd.getNodeY(i), nodeSize+fdd.getDegreeDistribution(i));
			}else{
				drawSingleNode(fdd.getNodeX(i), fdd.getNodeY(i), nodeSize);
			}
		}
	}
	
	/**
	 * Computed the hitbox of a rendered node. The the size of the reticle hitbox depends both on the screen position of the node and the size of the node.
	 * @param node 	index of the node of which the hitbox has to be computed
	 * @return double[4]: 
	 * 				[0] - x component of top-left corner
	 * 				[1] - y component of top-left corner
	 *  			[2] - x component of bottom-right corner
	 * 				[3] - y component of bottom-right corner
	 */
	private double[] nodeHitbox(int node){
		double[] hitbox = new double[4];
		double size;
		if(fdd.getMinDegree()!=fdd.getMaxDegree()){
			size=nodeSize+fdd.getDegreeDistribution(node);
		}else{
			size= nodeSize;
		}
		double x = fdd.getNodeX(node);
		double y = fdd.getNodeY(node);
		if (nodeBorder) {
			hitbox[0]=shifterX(zoomer(x))-(size+nodeBorderSize)/2;
			hitbox[1]=shifterY(zoomer(y))-(size+nodeBorderSize)/2;
			hitbox[2]=shifterX(zoomer(x))+(size+nodeBorderSize)/2;
			hitbox[3]=shifterY(zoomer(y))+(size+nodeBorderSize)/2;
		}else{
			hitbox[0]=shifterX(zoomer(x))-size/2;
			hitbox[1]=shifterY(zoomer(y))-size/2;
			hitbox[2]=shifterX(zoomer(x))+size/2;
			hitbox[3]=shifterY(zoomer(y))+size/2;
		}
		
		
		return hitbox;
	}
	
	private void drawEdges(){
		gc.setStroke(edgeColor);
		for (int i = 0; i < fdd.getNumON(); i++) {
			for (int j = 0; j < fdd.getNumON(); j++) {		
		if (i!=j&&i<j) {				
					if (fdd.getEdge(i, j)) {					
					gc.strokeLine(shifterX(zoomer(fdd.getNodeX(i))),
							shifterY(zoomer(fdd.getNodeY(i))),
									shifterX(zoomer(fdd.getNodeX(j))),
											shifterY(zoomer(fdd.getNodeY(j))));
					}
				}
			}
		}
	}
	
	public void animationIni(){
		timer=new AnimationTimer() {
			
			@Override
			public void handle(long now) {
				// TODO Auto-generated method stub
				if (simulationActive) {
					for (int i = 0; i < stepsPerFrame; i++) {
						fdd.simulateSingleStep(draggedNode);
					}
					
				}
				renderGraph();
			}
		};
	}
	
	private double shifterX(double x){
		return x+shiftX;
	}
	
	private double inverseShifterX(double x){
		return x-shiftX;
	}
	
	private double shifterY(double y){
		return y+shiftY;
	}
	
	private double inverseShifterY(double y){
		return y-shiftY;
	}
	
	private double zoomer(double coordinate){
		return coordinate*zoom;
	}
	
	private double inverseZoomer(double coordinate){
		if (zoom!=0) {
			return coordinate/zoom;
		}else{
			return coordinate;
		}
		
	}
	
	private void iniCenterAndZoom(){
		double[] vec;
		if (fdd.getNumON()>1) {
			vec=fdd.getBoundaries();
			zoom=Math.min((1-paddingFactor)*canW/(vec[1]-vec[0]), (1-paddingFactor)*canH/(vec[3]-vec[2]));
			shiftX=canW/2-zoomer(vec[0]+vec[1])/2;
			shiftY=canH/2-zoomer(vec[2]+vec[3])/2;
		}
	}
	
	private double[] coordinateSpaceToDrawSpace(double x,double y){
		double [] vec = null;
		return vec;
	}
	
	private void drawSingleNode(double x, double y, double size){
		if (nodeBorder) {
			Color colorbuffer=(Color) gc.getFill();
			gc.setFill(nodeBorderColor);
			gc.fillOval(shifterX(zoomer(x))-(size+nodeBorderSize)/2,
					shifterY(zoomer(y))-(size+nodeBorderSize)/2, 
						size+nodeBorderSize, 
						size+nodeBorderSize);
			gc.setFill(colorbuffer);
		}
		
		gc.fillOval(shifterX(zoomer(x))-size/2,
				shifterY(zoomer(y))-size/2, 
					size, 
					size);
	}
	
	/**
	 * Tests whether the mouse pointer collided with any drawn node on the canvas and returns the index of the node in case of a collision.
	 * @param x (double) - x coordinate of the mouse relative to the canvas
	 * @param y (double) - y coordinate of the mouse relative to the canvas
	 * @return int:
	 * 		-1 in case of no collision or the index of the node that the mouse collided with otherwise
	 */
	private int checkForMouseNodeCollision(double x, double y){
		double[] node;
		int found=-1;
		if (graphDrawn) {
			for (int i = 0; i < fdd.getNumON(); i++) {
				node=nodeHitbox(i);
				if (node[0]<x&&node[1]<y&&node[2]>x&&node[3]>y) {
					found=i;
					//System.out.println(i);
				}
			}
		}
		return found;
	}
	
	private double[] drawSpaceToCoordinateSpace(double x,double y){
		double[] vec=new double[2];
		vec[0]=inverseZoomer(inverseShifterX(x));
		vec[1]=inverseZoomer(inverseShifterY(y));
		return vec;
	}
	
	private void startDrawing(){
		started=true;
	}
}

	
