package myUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/******************************************************************************
 *  Compilation:  javac EdgeWeightedGraph.java
 *  Execution:    java EdgeWeightedGraph filename.txt
 *  Dependencies: Bag.java Edge.java In.java StdOut.java
 *  Data files:   https://algs4.cs.princeton.edu/43mst/tinyEWG.txt
 *                https://algs4.cs.princeton.edu/43mst/mediumEWG.txt
 *                https://algs4.cs.princeton.edu/43mst/largeEWG.txt
 *
 *  An edge-weighted undirected graph, implemented using adjacency lists.
 *  Parallel edges and self-loops are permitted.
 *
 *  % java EdgeWeightedGraph tinyEWG.txt 
 *  8 16
 *  0: 6-0 0.58000  0-2 0.26000  0-4 0.38000  0-7 0.16000  
 *  1: 1-3 0.29000  1-2 0.36000  1-7 0.19000  1-5 0.32000  
 *  2: 6-2 0.40000  2-7 0.34000  1-2 0.36000  0-2 0.26000  2-3 0.17000  
 *  3: 3-6 0.52000  1-3 0.29000  2-3 0.17000  
 *  4: 6-4 0.93000  0-4 0.38000  4-7 0.37000  4-5 0.35000  
 *  5: 1-5 0.32000  5-7 0.28000  4-5 0.35000  
 *  6: 6-4 0.93000  6-0 0.58000  3-6 0.52000  6-2 0.40000
 *  7: 2-7 0.34000  1-7 0.19000  0-7 0.16000  5-7 0.28000  4-7 0.37000
 *
 ******************************************************************************/

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import formulation.Edge;

/**
 *  The {@code EdgeWeightedGraph} class represents an edge-weighted
 *  graph of vertices named 0 through <em>V</em> – 1, where each
 *  undirected edge is of type {@link Edge} and has a real-valued weight.
 *  It supports the following two primary operations: add an edge to the graph,
 *  iterate over all of the edges incident to a vertex. It also provides
 *  methods for returning the degree of a vertex, the number of vertices
 *  <em>V</em> in the graph, and the number of edges <em>E</em> in the graph.
 *  Parallel edges and self-loops are permitted.
 *  By convention, a self-loop <em>v</em>-<em>v</em> appears in the
 *  adjacency list of <em>v</em> twice and contributes two to the degree
 *  of <em>v</em>.
 *  <p>
 *  This implementation uses an <em>adjacency-lists representation</em>, which
 *  is a vertex-indexed array of {@link Bag} objects.
 *  It uses &Theta;(<em>E</em> + <em>V</em>) space, where <em>E</em> is
 *  the number of edges and <em>V</em> is the number of vertices.
 *  All instance methods take &Theta;(1) time. (Though, iterating over
 *  the edges returned by {@link #adj(int)} takes time proportional
 *  to the degree of the vertex.)
 *  Constructing an empty edge-weighted graph with <em>V</em> vertices takes
 *  &Theta;(<em>V</em>) time; constructing a edge-weighted graph with
 *  <em>E</em> edges and <em>V</em> vertices takes
 *  &Theta;(<em>E</em> + <em>V</em>) time. 
 *  <p>
 *  For additional documentation,
 *  see <a href="https://algs4.cs.princeton.edu/43mst">Section 4.3</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public class EdgeWeightedGraph {
    private static final String NEWLINE = System.getProperty("line.separator");

    private final int V;
    private int E;
    private Set<Edge>[] adj;
    private int[] connComponents;
    
    /**
     * Initializes an empty edge-weighted graph with {@code V} vertices and 0 edges.
     *
     * @param  V the number of vertices
     * @throws IllegalArgumentException if {@code V < 0}
     */
    public EdgeWeightedGraph(int V) {
        if (V < 0) throw new IllegalArgumentException("Number of vertices must be nonnegative");
        this.V = V;
        this.E = 0;
        adj = (Set<Edge>[]) new HashSet[V];
        for (int v = 0; v < V; v++) {
            adj[v] = new HashSet<Edge>();
        }
        
        connComponents = new int[V];
        for(int v=0; v<V; v++)
        	connComponents[v] = -1;
    }

//    /**
//     * Initializes a random edge-weighted graph with {@code V} vertices and <em>E</em> edges.
//     *
//     * @param  V the number of vertices
//     * @param  E the number of edges
//     * @throws IllegalArgumentException if {@code V < 0}
//     * @throws IllegalArgumentException if {@code E < 0}
//     */
//    public EdgeWeightedGraph(int V, int E) {
//        this(V);
//        if (E < 0) throw new IllegalArgumentException("Number of edges must be nonnegative");
//        for (int i = 0; i < E; i++) {
//            int v = StdRandom.uniform(V);
//            int w = StdRandom.uniform(V);
//            double weight = Math.round(100 * StdRandom.uniform()) / 100.0;
//            Edge e = new Edge(v, w, weight);
//            addEdge(e);
//        }
//    }
    
    

    /**  
     * Initializes an edge-weighted graph from an adjacency matrix.
     *
     */
    public EdgeWeightedGraph(int n, Set<Edge> edges, boolean onlyPosWeight) {
    	V = n;
        adj = (Set<Edge>[]) new HashSet[V];
        for (int v = 0; v < V; v++) {
            adj[v] = new HashSet<Edge>();
        }
        
        for(Edge e : edges){
    		 double weight = e.getWeight();
    		 if(!onlyPosWeight || (onlyPosWeight && weight>0)){
                 //validateVertex(i);
                 //validateVertex(j);
                 addEdge(e);
    		 }
        }
        
        connComponents = new int[V];
        for(int v=0; v<V; v++)
        	connComponents[v] = -1;

        connectedComponents();
        //System.out.println("====");
    }
    
    
    
    /**  
     * Initializes an edge-weighted graph from a subset of an adjacency matrix.
     *
     */
    public EdgeWeightedGraph(double[][] adjMat, ArrayList<Integer> nodeIds, boolean onlyPosWeight) {
    	V = nodeIds.size();
        adj = (Set<Edge>[]) new HashSet[V];
        for (int v = 0; v < V; v++) {
            adj[v] = new HashSet<Edge>();
        }
        
        for(int i=1; i<nodeIds.size(); i++){
        	for(int j=0; j<i; j++){
        		 int u = nodeIds.get(i);
        		 int v = nodeIds.get(j);
        		 double weight = adjMat[u][v];
        		 if(!onlyPosWeight || (onlyPosWeight && weight>0)){
	                 //validateVertex(i);
	                 //validateVertex(j);
	                 Edge e = new Edge(V, i, j);
	                 e.setWeight(weight);
	                 addEdge(e);
        		 }
        	}
        }
        
        connComponents = new int[V];
        for(int v=0; v<V; v++)
        	connComponents[v] = -1;

        connectedComponents();
    }
    
    

    /**
     * Initializes a new edge-weighted graph that is a deep copy of {@code G}.
     *
     * @param  G the edge-weighted graph to copy
     */
    public EdgeWeightedGraph(EdgeWeightedGraph G) {
        this(G.V());
        this.E = G.E();
        for (int v = 0; v < G.V(); v++) {
            // reverse so that adjacency list is in same order as original
            Stack<Edge> reverse = new Stack<Edge>();
            for (Edge e : G.adj[v]) {
                reverse.push(e);
            }
            for (Edge e : reverse) {
                adj[v].add(e);
            }
        }
        
        connComponents = new int[V];
        for(int v=0; v<V; v++)
        	connComponents[v] = -1;
        
        connectedComponents();
    }


    /**
     * Returns the number of vertices in this edge-weighted graph.
     *
     * @return the number of vertices in this edge-weighted graph
     */
    public int V() {
        return V;
    }

    /**
     * Returns the number of edges in this edge-weighted graph.
     *
     * @return the number of edges in this edge-weighted graph
     */
    public int E() {
        return E;
    }

    // throw an IllegalArgumentException unless {@code 0 <= v < V}
    private void validateVertex(int v) {
        if (v < 0 || v >= V)
            throw new IllegalArgumentException("vertex " + v + " is not between 0 and " + (V-1));
    }
    
    
    public Set<Edge>[] getAdj(){
    	return(adj);
    }

    /**
     * Adds the undirected edge {@code e} to this edge-weighted graph.
     *
     * @param  e the edge
     * @throws IllegalArgumentException unless both endpoints are between {@code 0} and {@code V-1}
     */
    public void addEdge(Edge e) {
        int v = e.getSource();
        int w = e.getDest();
        validateVertex(v);
        validateVertex(w);
        adj[v].add(e);
        adj[w].add(e);
        E++;
    }
    
    /**
     * Remove an edge
     */
    public void removeEdge(Edge e){
        int v = e.getSource();
        int w = e.getDest();
        adj[v].remove(e);
        adj[w].remove(e);
        E--;
    }
    
    /**
     * Remove an edge
     */
    public void removeEdge(int v, int w){
        Iterator<Edge> it = adj[v].iterator();
        while(it.hasNext()){
        	Edge e = it.next();
        	if(e.getOtherVertexId(v) == w){
        		adj[v].remove(e);
        		break;
        	}
        }
        it = adj[w].iterator();
        while(it.hasNext()){
        	Edge e = it.next();
        	if(e.getOtherVertexId(w) == v){
        		adj[w].remove(e);
        		break;
        	}
        }
        E--;
    }

    /**
     * Returns the edges incident on vertex {@code v}.
     *
     * @param  v the vertex
     * @return the edges incident on vertex {@code v} as an Iterable
     * @throws IllegalArgumentException unless {@code 0 <= v < V}
     */
    public Iterable<Edge> adj(int v) {
        validateVertex(v);
        return adj[v];
    }

    /**
     * Returns the degree of vertex {@code v}.
     *
     * @param  v the vertex
     * @return the degree of vertex {@code v}               
     * @throws IllegalArgumentException unless {@code 0 <= v < V}
     */
    public int degree(int v) {
        validateVertex(v);
        return adj[v].size();
    }

    /**
     * Returns all edges in this edge-weighted graph.
     * To iterate over the edges in this edge-weighted graph, use foreach notation:
     * {@code for (Edge e : G.edges())}.
     *
     * @return all edges in this edge-weighted graph, as an iterable
     */
    public Iterable<Edge> edges() {
        Set<Edge> list = new HashSet<Edge>();
        for (int v = 0; v < V; v++) {
            int selfLoops = 0;
            for (Edge e : adj(v)) {
                if (e.getOtherVertexId(v) > v) {
                    list.add(e);
                }
                // add only one copy of each self loop (self loops will be consecutive)
                else if (e.getOtherVertexId(v) == v) {
                    if (selfLoops % 2 == 0) list.add(e);
                    selfLoops++;
                }
            }
        }
        return list;
    }

    /**
     * Returns a string representation of the edge-weighted graph.
     * This method takes time proportional to <em>E</em> + <em>V</em>.
     *
     * @return the number of vertices <em>V</em>, followed by the number of edges <em>E</em>,
     *         followed by the <em>V</em> adjacency lists of edges
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(V + " " + E + NEWLINE);
        for (int v = 0; v < V; v++) {
            s.append(v + ": ");
            for (Edge e : adj[v]) {
                s.append(e + "  ");
            }
            s.append(NEWLINE);
        }
        return s.toString();
    }
    
    
    
    void DFSUtil(int v, boolean[] visited)
    {
        // Mark the current node as visited and print it
        visited[v] = true;
        // Recur for all the vertices
        // adjacent to this vertex
        for (Edge e : adj[v]) {
        	int u = e.getOtherVertexId(v);
            if (!visited[u]){
            	connComponents[u] =  connComponents[v];
                DFSUtil(u, visited);
            }
        }
    }
    void connectedComponents()
    {
        // Mark all the vertices as not visited
        boolean[] visited = new boolean[V];
        int counter = 1;
        for (int v = 0; v < V; ++v) {
            if (!visited[v]) {
            	connComponents[v] = counter;
            	counter++;

                DFSUtil(v, visited);
            }
        }
    }
    
    
    public boolean isInSameConnComp(int i, int j){
    	if(connComponents[i] == connComponents[j])
    		return(true);
    	return(false);
    }
    
   
    
   public ArrayList<Integer> searchShortestPathBFS(int u, int v){
	// Mark all the vertices as not visited(By default
       // set as false)
	   ArrayList<Integer> path = new ArrayList<Integer>();
	   
	   if(u == v)
		   return(path);
	   
       boolean visited[] = new boolean[V];
       int parent[] = new int[V];
       for(int i=0; i<V; i++)
    	   parent[i] = -1;

       // Create a queue for BFS
       LinkedList<Integer> queue = new LinkedList<Integer>();

       // Mark the current node as visited and enqueue it
       visited[u]=true;
       //parent[u] = -1;
       queue.add(u);

       boolean finish= false;
       while (queue.size() != 0 && !finish)
       {
           // Dequeue a vertex from queue and print it
           int i = queue.poll();

           // Get all adjacent vertices of the dequeued vertex i
           // If a adjacent has not been visited, then mark it
           // visited and enqueue it
           for (Edge e : adj[i]) {
	           int j = e.getOtherVertexId(i);
	           
	           if(j == v){
	        	   parent[j] = i;
        		   finish = true;
        		   break;
        	   }
	           
	           if (!visited[j])
	           {
	               queue.add(j);
	        	   parent[j] = i;
	               visited[j] = true;
	           }
           }
       }
       
       // trace path
       int j = v;
       path.add(j);
       while(parent[j]>=0){
    	   j = parent[j];
    	   path.add(j);
       }
       
       return(path);
    }
   
   
   public ArrayList<Integer> searchShortestPathBFS(int u, int v, int cycleLength, boolean onlyPos){
		// Mark all the vertices as not visited(By default
	       // set as false)
		   ArrayList<Integer> path = new ArrayList<Integer>();
		   
		   if(u == v)
			   return(path);
		   
//		   if(adj[u].contains(new Edge(V, u, v)))
//			   return(path);
		   
	       boolean visited[] = new boolean[V];
	       int parent[] = new int[V];
	       for(int i=0; i<V; i++)
	    	   parent[i] = -1;
	       
	       int distance[] = new int[V];
	       for(int i=0; i<V; i++)
	    	   distance[i] = 0;
	       
	       

	       // Create a queue for BFS
	       LinkedList<Integer> queue = new LinkedList<Integer>();

	       // Mark the current node as visited and enqueue it
	       visited[u]=true;
	       //parent[u] = -1;
	       queue.add(u);

	       boolean finish= false;
	       while (queue.size() != 0 && !finish)
	       {
	           // Dequeue a vertex from queue and print it
	           int i = queue.poll();

	           // Get all adjacent vertices of the dequeued vertex i
	           // If a adjacent has not been visited, then mark it
	           // visited and enqueue it
	           if(distance[i]<(cycleLength-1)){
		           for (Edge e : adj[i]) {
			           int j = e.getOtherVertexId(i);
			           
			           boolean ok = true;
			           if(onlyPos && e.getWeight()<0)
			        	   ok = false;
			           
			           if(j == v && ok){
			        	   parent[j] = i;
		        		   finish = true;
		        		   break;
		        	   }
			           
			           
			           if (!visited[j] && ok)
			           {
			               queue.add(j);
			        	   parent[j] = i;
			        	   distance[j] = distance[i]+1;
			               visited[j] = true;
			           }
		           }
	           }
	       }
	       
	       // trace path
	       if(parent[v]>=0){
		       int j = v;
		       path.add(j);
		       while(parent[j]>=0){
		    	   j = parent[j];
		    	   path.add(j);
		       }
	       }
	       
	       return(path);
    }
	 
   // source: https://www.geeksforgeeks.org/dijkstras-shortest-path-algorithm-in-java-using-priorityqueue/
   public ArrayList<Integer> searchShortestPathDijkstra(int u, int v){
	   Dijkstra dijsktra = new Dijkstra(this);
	   ArrayList<Integer> path = dijsktra.findShortestPath(u, v);
	   return(path);
   }
   
   
    
    

//    /**
//     * Unit tests the {@code EdgeWeightedGraph} data type.
//     *
//     * @param args the command-line arguments
//     */
//    public static void main(String[] args) {
//        In in = new In(args[0]);
//        EdgeWeightedGraph G = new EdgeWeightedGraph(in);
//        StdOut.println(G);
//    }

}


