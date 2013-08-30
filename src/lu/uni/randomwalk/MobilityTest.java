package lu.uni.randomwalk;

import java.io.IOException;
import java.util.Random;

import org.graphstream.algorithm.randomWalk.RandomWalk;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkImages.OutputPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputType;
import org.graphstream.stream.file.FileSinkImages.Quality;
import org.graphstream.stream.file.FileSinkImages.RendererType;
import org.graphstream.stream.file.FileSinkImages.Resolution;
import org.graphstream.stream.file.FileSinkImages.Resolutions;

public class MobilityTest {

    /** The Graphstream Graph instance */
    public Graph graph;

    /** The random number generator. It will be used intensively as our 
     * mobility model is stochastic
     */
    public Random rand;
    
    /** The x-axis simulation size */
    public int MaxX;
    /** The y-axis simulation size */
    public int MaxY;

    /**
     * Create "corner" nodes to fix Graphstream auto zooming capability.
     * No simulation purpose, only for the sake of movie making
     */
    public void createCornerNodes(){

	/** Node name and creation */
	Node n = graph.addNode("upright");
	/** x-axis position */
	n.setAttribute("x", 1600);
	/** y-axis position */
	n.setAttribute("y", 900);
	/** An attribute to know if a node is capable of moving */
	n.setAttribute("move", false);
	/** An attribute for displaying purpose, see the style.css file */
	n.setAttribute("ui.class", "grid"); 

	n = graph.addNode("upleft");
	n.setAttribute("x", 0);
	n.setAttribute("y", 900);
	n.setAttribute("move", false);
	n.setAttribute("ui.class", "grid");

	n = graph.addNode("downright");
	n.setAttribute("x", 1600);
	n.setAttribute("y", 900);
	n.setAttribute("move", false);
	n.setAttribute("ui.class", "grid");

	n = graph.addNode("downleft");
	n.setAttribute("x", 0);
	n.setAttribute("y", 0);
	n.setAttribute("move", false);
	n.setAttribute("ui.class", "grid");
    }

    /**
     * The main nodes creating routine
     * @param nbNodes the number of nodes to be included in the simulation
     */
    public void createNodes(int nbNodes){

	for (int i = 0; i < nbNodes; i++){
	    Node n = graph.addNode(Integer.toString(i));
	    n.setAttribute("x", rand.nextDouble()*1600);
	    n.setAttribute("y", rand.nextDouble()*900);
	    /** The current direction (heading) of the node */
	    n.setAttribute("dir", rand.nextDouble()* (2*Math.PI));
	    /** The time counter used for moving or pausing */
	    n.setAttribute("time", rand.nextInt(20));
	    /** The attribute to know if a mobile node is currently moving */
	    n.setAttribute("pause", false);
	    n.setAttribute("move", true);
	    /** The current speed */
	    n.setAttribute("speed", rand.nextDouble());

	    /** 
	     * For aesthetic purpose, we display three types of nodes 
	     * This small project was intend to illustrates an heterogeneous 
	     * mobile ad hoc network
	     * */
	    int choice = rand.nextInt(3);
	    if (choice == 0)
		n.setAttribute("ui.class", "moving");
	    if (choice == 1)
		n.setAttribute("ui.class", "moving2");
	    if (choice == 2)
		n.setAttribute("ui.class", "moving3");


	}
    }

    /**
     * The main routine is charge of moving a given node n
     * @param n the considered node
     */
    public void applyMobilityStep(Node n){
	boolean isPaused = n.getAttribute("pause");
	Integer remainingTime = n.getAttribute("time");
	/** 
	 * The considered unit distance, i.e. with speed = 1, a step this
	 * represents a distance of 3. Modify this value at will.
	 * */
	double distance = 3;

	/** 
	 * If the node time counter is passed:
	 * - Alternate the pause attribute
	 * - Re-initialize the time counter 
	 * - Pick up a new direction
	 */
	if (remainingTime <= 0){
	    if (isPaused){
		n.setAttribute("pause", false);
		n.setAttribute("time", rand.nextInt(60));
	    }
	    else{
		n.setAttribute("pause", true);
		n.setAttribute("time", rand.nextInt(20));
	    }
	    n.setAttribute("dir", rand.nextDouble()* (2*Math.PI));
	}
	/** 
	 * If not paused, find a valid next position, i.e. inside the 
	 * simulation area.
	 */
	if (!isPaused){
	    boolean notOK = true;
	    while (notOK){
		Double direction = n.getAttribute("dir");
		Double speed = n.getAttribute("speed");
		double moveX = Math.cos(direction) * distance * speed *2;
		double moveY = Math.sin(direction) * distance * speed *2;

		double curX = n.getAttribute("x");
		double curY = n.getAttribute("y");

		double nextX = curX + moveX;
		double nextY = curY + moveY;

		if ( (nextX > MaxX) || (nextX < 0) || (nextY > MaxY) || (nextY < 0) ){
		    n.setAttribute("dir", rand.nextDouble()* (2*Math.PI));
		}
		else {
		    notOK = false;
		    n.setAttribute("x", curX + moveX);
		    n.setAttribute("y", curY + moveY);

		}

	    }

	}
	/** Decrease time counter */
	remainingTime = n.getAttribute("time");
	remainingTime -= 1;
	n.setAttribute("time", remainingTime);
    }

    /** A simple euclidian distance computation method */
    public double getDistance(Node n1, Node n2){
	Double dist = 0.0;
	Double x1 = n1.getAttribute("x");
	Double y1 = n1.getAttribute("y");
	Double x2 = n2.getAttribute("x");
	Double y2 = n2.getAttribute("y");
	dist = Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	return dist;
    }

    /** Method to recompute edges between close enough nodes */
    public void refreshEdges(){
	for (Node n1 : graph.getNodeSet()){
	    if (n1.getAttribute("move")){
		for (Node n2 : graph.getNodeSet()){
		    if (n1.getId() != n2.getId()){
			if (n2.getAttribute("move")){
			    /** The max range is set to an arbitrary value */
			    if (getDistance(n1, n2) <= 150){
				if ( !(n1.hasEdgeBetween(n2) && 
					n2.hasEdgeBetween(n1) ) ){
				    graph.addEdge(n1.getId()+"-"+n2.getId(), 
					    n1, n2);
				}
			    }
			    else{
				try {
				    graph.removeEdge(n1, n2);
				} catch (Exception e){

				}
				try {
				    graph.removeEdge(n2, n1);
				}
				catch(Exception e){

				}
			    }
			}
		    }
		}
	    }
	}
    }

    public MobilityTest(int MaxX, int MaxY, int nbNodes)
	throws InterruptedException, IOException {
	/** Main simulation parameters */
	rand = new Random();
	this.MaxX = MaxX;
	this.MaxY = MaxY;

	/** Graphstream parameters for printing screenshots */
	OutputPolicy outputPolicy = OutputPolicy.NONE;
	String prefix = "patri_";
	OutputType type = OutputType.PNG;
	Resolution resolution = Resolutions.HD720;
	FileSinkImages fsi = new FileSinkImages(prefix, type, 
		resolution, outputPolicy );
	fsi.setStyleSheet("url('style.css')");
	fsi.setQuality(Quality.HIGH);
	fsi.setRenderer(RendererType.SCALA);
	
	/** Initialization of the graph */
	graph = new SingleGraph("random walk");
	/** Link the screenshot module to the graph updates */
	graph.addSink(fsi);
	fsi.begin(prefix);
	
	createNodes(nbNodes);
	
	/** Uncomment for displaying */
	// graph.display(false);
	// graph.setAttribute("ui.stylesheet", "url('style.css')");
	// graph.addAttribute("ui.quality");
	// graph.addAttribute("ui.antialias");
	
	/** Main mobility loop */
	for (int i = 0; i < 1000; i++){
	    /** For each mobile node, apply mobility */
	    for (Node n : graph.getNodeSet()){
		boolean move = n.getAttribute("move");
		if (move)
		    applyMobilityStep(n);
	    }
	    /** Recompute the edges according to a maximum range */
	    refreshEdges();
	    /** Comment if you do not want to create screenshots */
	    fsi.outputNewImage();
	}
	fsi.end();
	
    }

    /** No default constructor */
    @SuppressWarnings("unused")
    private MobilityTest(){}
    
    /**
     * @param args
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InterruptedException {
	System.setProperty("org.graphstream.ui.renderer", 
		"org.graphstream.ui.j2dviewer.J2DGraphRenderer");
	try {
	    MobilityTest mob = new MobilityTest(1600, 900, 100);
	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

}
