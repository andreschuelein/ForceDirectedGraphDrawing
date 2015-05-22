package fdgd.controller;


import java.text.DecimalFormat;

import fdgd.model.ForceDirectedDrawing;
import fdgd.model.NetworkBuilder;
import javafx.animation.AnimationTimer;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;


public class Controller {
	//private NetworkBuilder graph;
	private GraphicsContext gc;
	private final double canW=1000; //canvas width
	private final double canH=1000; //canvas height
	private final double contW=200;	//control area width
	private Color canvasColor=Color.GRAY; //canvas background color
	private Color nodeColor=Color.DARKSLATEGRAY; //node color
	private Color edgeColor=Color.WHITE; // edge color
	private double defaultNodeSize=20;//canW/100;
	private double nodeSize=defaultNodeSize;	// draw diameter of a node
	private double nodeZoomScale=.1;
	private ForceDirectedDrawing fdd;
	private double paddingFactor=0.2;
	private final int defaultNumOfNodes=25;
	private AnimationTimer timer;
	private final double defaultProbability=0.1;
	private final int defaultNumberOfStubs=2;
	private boolean continueAnimation=false;
	private double shiftX=0;
	private double shiftY=0;
	private double zoom=1;
	private double stepsPerFrame=20;
	private boolean autoZoom=true;
	
	
	
	@FXML private SplitPane splitPane;
	@FXML private Label lbl1;
	@FXML private Canvas drawArea;
	@FXML private TextField textField;	
	@FXML private ChoiceBox<String> cbox;
	
	@FXML 
	public void initialize(){
		gc = drawArea.getGraphicsContext2D();
		splitPane.setDividerPositions(canW/(canW+contW),contW/(canW+contW));
		drawArea.setHeight(canH);
		drawArea.setWidth(canW);
		
		drawArea.setOnDragDetected(new EventHandler<MouseEvent>(){

			@Override
			public void handle(MouseEvent event) {
				System.out.println("drag detected!");
				
			}
			
		});
		
		fdd = new ForceDirectedDrawing(defaultNumOfNodes);
		textField.setPromptText("please input the number of nodes");
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
		

	}
	
	@FXML
	protected void exitButtonPressed(ActionEvent event){
		System.exit(0);
	}
	
	@FXML 
	protected void fullyConnectedPressed(ActionEvent event){
		readTextField();
		fdd.buildCompleteGraph();
		startRenderingAndAnimation(); 
	}
	
	@FXML 
	protected void randomGraphPressed(ActionEvent event){
		readTextField();
		fdd.buildRandomGraph(defaultProbability);
		startRenderingAndAnimation();
	}
	
	@FXML 
	protected void scaleFreeGraphPressed(ActionEvent event){
		readTextField();
		fdd.buildScaleFreeGraph();
		startRenderingAndAnimation();
	}
	
	
	@FXML
	protected void singleStepPressed(ActionEvent event){
		fdd.simulateSingleStep();
		renderGraph();
	}
	
	@FXML
	protected void tenStepsPressed(ActionEvent event){
		for (int i = 0; i < 10; i++) {
			fdd.simulateSingleStep();
		}		
		renderGraph();
	}
	
	@FXML
	protected void startAnimationPressed(ActionEvent event){
		timer.start();
		continueAnimation=true;
		
	}
	
	@FXML
	protected void stopAnimationPressed(ActionEvent event){
		timer.stop();
		continueAnimation=false;
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
		 if (continueAnimation) {
				timer.stop();
			}
		 //nodeSize=defaultNodeSize;
		 renderGraph();
		 animationIni();
		 if (continueAnimation) {
			timer.start();
		}
	}
	
	private void renderGraph(){
		//draw background
		gc.setFill(canvasColor);
		gc.fillRect(0, 0, canW, canH);
		
		iniCenterAndZoom();
		
		//generate complete graph
		drawEdges();
		drawNodes();
	}
	
	private void drawNodes(){
		gc.setFill(nodeColor);
		for (int i = 0; i < fdd.getNumON(); i++) {
			gc.fillOval(shifterX(zoomer(fdd.getNodeX(i))),
					shifterY(zoomer(fdd.getNodeY(i))), 
						zoomer(nodeSize), 
						zoomer(nodeSize));
		}
	}
	
	private void drawEdges(){
		gc.setStroke(edgeColor);
		for (int i = 0; i < fdd.getNumON(); i++) {
			for (int j = 0; j < fdd.getNumON(); j++) {		
		if (i!=j&&i<j) {				
					if (fdd.getEdge(i, j)) {					
					gc.strokeLine(shifterX(zoomer(fdd.getNodeX(i)+nodeSize/2)),
							shifterY(zoomer(fdd.getNodeY(i)+nodeSize/2)),
									shifterX(zoomer(fdd.getNodeX(j)+nodeSize/2)),
											shifterY(zoomer(fdd.getNodeY(j)+nodeSize/2)));
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
				for (int i = 0; i < stepsPerFrame; i++) {
					fdd.simulateSingleStep();
				}
				renderGraph();
			}
		};
	}
	
	private double shifterX(double x){
		return x+shiftX;
	}
	
	private double shifterY(double y){
		return y+shiftY;
	}
	
	private double zoomer(double coordinate){
		return coordinate*zoom;
	}
	
	
	private void iniCenterAndZoom(){
		double[] vec;
		if (fdd.getNumON()>1) {
			vec=fdd.getBoundaries();
			zoom=Math.min((1-paddingFactor)*canW/(vec[1]-vec[0]), (1-paddingFactor)*canH/(vec[3]-vec[2]));
			shiftX=canW/2-zoomer(vec[0]+vec[1])/2;
			shiftY=canH/2-zoomer(vec[2]+vec[3])/2;
			//nodeSize=nodeSize*zoom;
		}
		
		//if ((vec[1]-vec[0]>canW)||(vec[3]-vec[2]>canH)) {
			
		//}else{
		//	zoom=Math.max((vec[1]-vec[0])/canW, (vec[3]-vec[2])/canH);
		//}
		//DecimalFormat nf = new DecimalFormat("#.0");
		//System.out.println("xMin:"+nf.format(vec[0])+" xMax:"+nf.format(vec[1])+" xDiff:"+nf.format(vec[1]-vec[0])+" yMin:"+nf.format(vec[2])+" yMax:"+nf.format(vec[3])+
		//		" Xshift:"+nf.format(shiftX)+" Yshift:"+nf.format(shiftY)+" yDiff:"+nf.format(vec[3]-vec[2])+" zoom:"+nf.format(zoom));
	}
	
}

	
