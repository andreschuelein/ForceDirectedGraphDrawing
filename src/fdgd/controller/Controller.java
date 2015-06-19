package fdgd.controller;


import java.text.DecimalFormat;

import fdgd.model.ForceDirectedDrawing;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

/**
 * 
 * @author Andre Schuelein
 *
 */
public class Controller {
	private GraphicsContext gc;
	private final double canW=1000; 			//canvas width TODO adjust to screen size
	private final double canH=1000; 			//canvas height
	private final double contW=200;				//control area width
	private Color canvasColor=Color.rgb(92,93,112); 		//canvas background color
	private Color nodeColor=Color.web("hsl(120,100%,100%)");//node color
	private Color edgeColor=Color.rgb(211,217,206); 		// edge color
	private double defaultNodeSize=10;			//default node diameter
	private double nodeSize=defaultNodeSize;	//draw diameter of a node
	private boolean nodeBorder=true;			//toggle the display of the node border
	private double nodeBorderSize=2;			//default size of the node  border in pixels
	private Color nodeBorderColor=Color.BLACK;	//default node border color
	private ForceDirectedDrawing fdd;			//model object
	private double paddingFactor=0.2;			//ratio of the padding area on the canvas in auto zoom mode
	private final int defaultNumOfNodes=40;		//default number of nodes for the model
	private AnimationTimer timer;				//animation timer draws graph and executes simulation steps
	private final double defaultProbability=0.1;//default density of the generated random graphs
	private double shiftX=0;					//initial displacement of the display on the canvas in x-direction
	private double shiftY=0;					//initial displacement of the display on the canvas in y-direction
	private double zoom=1;						//initial zoom factor (1=100%,0.5=50%,2=200%) zoom > 0 
	private double stepsPerFrame=20;			//number of simulation steps that are computed per rendered frame
	private boolean autoZoom=true;				//auto zoom mode toggle (true=auto zoom, false=free zoom)
	private double zoomFactor=.2;				//scaling factor for zoom increases/decreases
	private boolean tooltip=true; 				//tooltip toggle
	private boolean renderEdges=true;			//toggle for the rendering of edges
	private boolean graphDrawn=false;			//control state
	private boolean simulationActive=false;		//**toggle for the simulation state (if true the next animation timer call will execute simulation steps)
	private boolean started=false;				//contains application state for tooltip logic
	private int draggedNode=-1;					//tracks the ID of the dragged node, -1 if no node is dragged
	private int tooltipNode=-1;					//tracks the ID of the dragged node for the tooltip logic, -1 if no node is dragged
	private DecimalFormat df0 = new DecimalFormat("0.");
	private DecimalFormat df1 = new DecimalFormat("0.#");
	private double pressedX=0;					//buffers the mouse cursor location on mouse pressed events
	private	double pressedY=0;					//buffers the mouse cursor location on mouse pressed events
	private double shiftXBuffer=0;				//buffers the x-shift between mouse pressed and dragged events
	private double shiftYBuffer=0;				//buffers the y-shift between mouse pressed and dragged events

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
	@FXML private CheckBox tooltipCBox;
	@FXML private CheckBox edgeRenderingCBox;

	/**
	 * Initializes the controller.
	 */
	@FXML 
	public void initialize(){
		fdd = new ForceDirectedDrawing(defaultNumOfNodes);
		gc = drawArea.getGraphicsContext2D();
		initBasicElements();
		initMouseEvents();
		initSliders();
		initSliderLabels();
		initChoiceBox();
		initCheckBox();
		//START ANIMATION
		animationIni();
		startRenderingAndAnimation();
	}

	/**
	 * Initializes miscellaneous UI events with initial values.
	 */
	private void initBasicElements(){
		nodeLabel.setVisible(false);
		nodeLabel.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 20px;");
		splitPane.setDividerPositions(canW/(canW+contW),contW/(canW+contW));
		textField.setPromptText("please input the number of nodes");
		drawArea.setHeight(canH);
		drawArea.setWidth(canW);
	}

	/**
	 * Adds listeners to the canvas for the following mouse events:
	 * mouse dragged
	 * mouse pressed
	 * mouse released
	 * mouse moved
	 * scrolled
	 */
	private void initMouseEvents(){
		drawArea.setOnMouseDragged(new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent event) {
				if (draggedNode!=-1) { // drag node
					fdd.setNodeLocation(draggedNode, drawSpaceToCoordinateSpace(event.getX(), event.getY())[0], drawSpaceToCoordinateSpace(event.getX(), event.getY())[1]);
					displayTooltip(event.getX(), event.getY());
				}else{ 
					shiftX=shiftXBuffer+(event.getX()-pressedX);
					shiftY=shiftYBuffer+(event.getY()-pressedY);
				}

			}
		});

		drawArea.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				draggedNode=checkForMouseNodeCollision(event.getX(), event.getY());
				if(draggedNode==-1){
					pressedX=event.getX();
					pressedY=event.getY();
					shiftXBuffer=shiftX;
					shiftYBuffer=shiftY;
				}
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
				displayTooltip(event.getX(), event.getY());
			}
		});

		drawArea.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				if(!autoZoom){
					double updatedZoom=newZoomFactor(zoom, event.getDeltaY()>0);
					shiftX=(event.getX()-shiftX)*((zoom-updatedZoom)/zoom)+shiftX;
					shiftY=(event.getY()-shiftY)*((zoom-updatedZoom)/zoom)+shiftY;
					zoom=updatedZoom;
				}
			}
		});
	}

	/**
	 * Enable the tooltip display next to the mouse cursor.
	 * @param x
	 * @param y
	 */
	private void displayTooltip(double x, double y){
		if (tooltipNode!=-1&&tooltip) {
			nodeLabel.setText("node: "+Integer.toString(tooltipNode)+"\ndegree: "+fdd.getDegreeDistribution(tooltipNode));
			nodeLabel.setVisible(true);
			nodeLabel.setLayoutX(x+25);
			nodeLabel.setLayoutY(y-25);
		} else {
			nodeLabel.setVisible(false);
		}
	}

	/**
	 * Initiates choice box is listeners and initial states.
	 */
	private void initChoiceBox(){
		cbox.getItems().addAll("auto zoom/pane","free zoom/pane");
		if(autoZoom){
			cbox.getSelectionModel().selectFirst();
		}else{
			cbox.getSelectionModel().selectLast();
		}
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
	}

	/**
	 * Initiates check boxes is listeners and initial states.
	 */
	private void initCheckBox(){
		tooltipCBox.setSelected(tooltip);
		tooltipCBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				tooltip=newValue;
			}
		});

		edgeRenderingCBox.setSelected(renderEdges);
		edgeRenderingCBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				renderEdges=newValue;
			}
		});
	}

	/**
	 * Initiates sliders with listeners and prameters.
	 */
	private void initSliders() {
		initSlider(slider1,1,900);
		slider1.setValue(fdd.getC1());
		slider1.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue <? extends Number> observable, Number oldValue,
					Number newValue) {
				c1label.setText(df0.format(slider1.getValue()));
				fdd.setC1(slider1.getValue());
			}
		});

		initSlider(slider2, 0.1, 2);
		slider2.setValue(fdd.getC2());
		slider2.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue,
					Number newValue) {
				c2label.setText(df1.format(slider2.getValue()));
				fdd.setC2(slider2.getValue());
			}
		});

		initSlider(slider3, 1, 10000);
		slider3.setValue(fdd.getC3());
		slider3.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue,
					Number newValue) {
				c3label.setText(df0.format(slider3.getValue()));
				fdd.setC3(slider3.getValue());

			}
		});

		initSlider(slider4, 1, 100);
		slider4.setValue(fdd.getC4());
		slider4.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue,
					Number newValue) {
				c4label.setText(df0.format(slider4.getValue()));
				fdd.setC4(slider4.getValue());

			}
		});
	}

	/**
	 * Initializes a given slider with a given set of parameters.
	 * @param slider The slider that should be initialized
	 * @param min the minimal slider value
	 * @param max the maximal silder value
	 */
	private void initSlider(Slider slider, double min, double max){
		slider.setShowTickMarks(true);
		slider.setMajorTickUnit(Math.abs(max-min)/4);
		slider.setMinorTickCount(1);
		slider.setMin(min);
		slider.setMax(max);
	}

	/**
	 * Initializes the label texts bases on their associated sliders and their values.
	 */
	private void initSliderLabels(){
		c1label.setText(df0.format(slider1.getValue()));
		c2label.setText(df1.format(slider2.getValue()));
		c3label.setText(df0.format(slider3.getValue()));
		c4label.setText(df0.format(slider4.getValue()));;
	}

	/**
	 * Closes the JavaFX application with error state 0.
	 * @param event
	 */
	@FXML
	protected void exitButtonPressed(ActionEvent event){
		System.exit(0);
	}

	/**
	 * Initiates the drawing of a fully connected / complete graph. The size of graph is derived by evaluating the text field's content.
	 * @param event
	 */
	@FXML 
	protected void fullyConnectedPressed(ActionEvent event){
		readTextField();
		startDrawing();
		fdd.buildCompleteGraph();
		startRenderingAndAnimation(); 
	}

	/**
	 * Initiates the drawing of a tree graph. The size of graph is derived by evaluating the text field's content.
	 * The default random graph density is used.
	 * @param event
	 */
	@FXML 
	protected void randomGraphPressed(ActionEvent event){
		readTextField();
		startDrawing();
		fdd.buildRandomGraph(defaultProbability);
		startRenderingAndAnimation();
	}

	/**
	 * Initiates the drawing of a scale free graph. The size of graph is derived by evaluating the text field's content.
	 * @param event
	 */
	@FXML 
	protected void scaleFreeGraphPressed(ActionEvent event){
		readTextField();
		startDrawing();
		fdd.buildScaleFreeGraph();
		startRenderingAndAnimation();
	}

	/**
	 * Initiates the drawing of a tree graph. The size of graph is derived by evaluating the text field's content.
	 * @param event
	 */
	@FXML
	protected void treeGraphPressed(ActionEvent event){
		readTextField();
		startDrawing();
		fdd.buildFancyTreeGraph();
		startRenderingAndAnimation();

	}
	
	/**
	 * Initiates the simulation of a single step in the model.
	 * @param event
	 */
	@FXML
	protected void singleStepPressed(ActionEvent event){
		fdd.simulateSingleStep(draggedNode);
	}

	/**
	 * Initiates the simulation of ten step in the model.
	 * @param event
	 */
	@FXML
	protected void tenStepsPressed(ActionEvent event){
		for (int i = 0; i < 10; i++) {
			fdd.simulateSingleStep(draggedNode);
		}
	}

	@FXML
	protected void startAnimationPressed(ActionEvent event){
		simulationActive=true;
		//simulationThread.start();
	}

	@FXML
	protected void stopAnimationPressed(ActionEvent event){
		simulationActive=false;
		//simulationThread.interrupt();
	}

	/**
	 * Reads the content of the textField and asserts a positive whole number or displays invalid. Calls the constructor of the model if the textField contained a proper input.
	 */
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

	/**
	 * Initiates the generation of spawn locations for every node and starts the animation timer.
	 */
	private void startRenderingAndAnimation(){
		fdd.generateInitialSpawns(canW, canH, paddingFactor*canW, paddingFactor*canH);
		timer.start();
	}
	
	/**
	 * Initiated the rendering procedure for the entire graphs including background, nodes and edges.
	 */
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

	/**
	 * Initiates the drawing of all nodes from the model into the graphics context. Also computes the size of any given node by it's degree and sets it's color.
	 */
	private void drawNodes(){
		gc.setFill(nodeColor);
		int tmp;
		for (int i = 0; i < fdd.getNumON(); i++) {
			if(fdd.getMaxDegree()!=fdd.getMinDegree()){
				tmp=(int)120+240*(fdd.getDegreeDistribution(i)-fdd.getMinDegree())/(fdd.getMaxDegree()-fdd.getMinDegree());
				gc.setFill(Color.web("hsl("+Integer.toString(tmp)+",100%,100%)"));
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

	/**
	 * Draws edges between connected nodes into the graphics context.
	 * Can be toggled on/off by renderEdges.
	 */
	private void drawEdges(){
		if (renderEdges) {
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
	}

	/**
	 * Initializes the Animation timer, each timer call will simulate the default number of steps and then start the drawing procedure.
	 */
	private void animationIni(){
		timer=new AnimationTimer() {	

			@Override
			public void handle(long now) {
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
		return shifter(x,shiftX);
	}

	/**
	 * Shifts a value by a given displacement amount.
	 * @param x value pre shift
	 * @param shift displacement
	 * @return shifted value
	 */
	private double shifter(double x, double shift){
		return x+shift;
	}

	private double inverseShifterX(double x){
		return inverseShifter(x, shiftX);
	}

	/**
	 * Shifts a value by a given displacement amount in the opposite direction.
	 * @param x value pre shift
	 * @param shift displacement
	 * @return shifted value
	 */
	private double inverseShifter(double x, double shift){
		return x-shift;
	}

	private double shifterY(double y){
		return shifter(y,shiftY);
	}


	private double inverseShifterY(double y){
		return inverseShifter(y, shiftY);
	}

	private double zoomer(double coordinate){
		return zoomer(coordinate,zoom);
	}

	/**
	 * Scales a value by a given zoom factor.
	 * @param coordinate value pre zoom
	 * @param zoom1 zoom factor
	 * @return scaled value
	 */
	private double zoomer(double coordinate, double zoom1){
		return coordinate*zoom1;
	}

	private double inverseZoomer(double coordinate){
		return inverseZoomer(coordinate, zoom);
	}

	/**
	 * Scales a value by the inverse of a given zoom factor. Asserts that the inverse is non zero.
	 * @param coordinate value pre zoom
	 * @param zoom1 zoom factor
	 * @return scaled value or if the inverse was zero it will return the unscaled value
	 */
	private double inverseZoomer(double coordinate, double zoom1){
		if (zoom1!=0) {
			return coordinate/zoom1;
		}else{
			return coordinate;
		}
	}

	/**
	 * Computes the new zoom by using the global zoomFactor.
	 * @param oldZoom previous zoom
	 * @param zoomIn true-zoom in; false-zoom out
	 * @return updated zoom value
	 */
	private double newZoomFactor(double oldZoom, boolean zoomIn){
		return (zoomIn) ? oldZoom*(1+zoomFactor) : oldZoom/(1+zoomFactor);
	}
	
	/**
	 * Finds the outer boundries of the graph in the model space and impletements these boundries into the drawing space using a global displacement shiftX/Y and a global zoomFactor.
	 */
	private void iniCenterAndZoom(){
		double[] vec;
		if (fdd.getNumON()>1) {
			vec=fdd.getBoundaries();
			zoom=Math.min((1-paddingFactor)*canW/(vec[1]-vec[0]), (1-paddingFactor)*canH/(vec[3]-vec[2]));
			shiftX=canW/2-zoomer(vec[0]+vec[1])/2;
			shiftY=canH/2-zoomer(vec[2]+vec[3])/2;
		}
	}
	
	/**
	 * Draws a single node and it's border centered around (x,y) in given the size.
	 * @param x
	 * @param y
	 * @param size
	 */
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
				}
			}
		}
		return found;
	}

	/**
	 * Converts a coordinate pair (x,y) from the drawing space (location relative to the canvas) to the model space.
	 * @param x relative to the canvas
	 * @param y	relative to the canvas
	 * @return vector of the coordinate pair in the model space
	 */
	private double[] drawSpaceToCoordinateSpace(double x,double y){
		double[] vec=new double[2];
		vec[0]=inverseZoomer(inverseShifterX(x));
		vec[1]=inverseZoomer(inverseShifterY(y));
		return vec;
	}

	/**
	 * Sets started to true to enable certain actions like tooltip updates.
	 */
	private void startDrawing(){
		started=true;
	}
}
