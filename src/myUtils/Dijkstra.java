package myUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import formulation.Edge;



public class Dijkstra {

	EdgeWeightedGraph G;
	private int parent[];
	private double dist[];
    private Set<Integer> settled;
    private PriorityQueue<Vertex> pq;
    Set<Edge>[] adj;
  
    public Dijkstra(EdgeWeightedGraph g)
    {
    	this.G = g;
        parent = new int[this.G.V()];
        dist = new double[this.G.V()];
        settled = new HashSet<Integer>();
        pq = new PriorityQueue<Vertex>(this.G.V(), new Vertex());
        adj = G.getAdj();
    }
  
    // Function for Dijkstra's Algorithm
    public ArrayList<Integer> findShortestPath(int src, int target)
    {
	   ArrayList<Integer> path = new ArrayList<Integer>();
	   
	   if(src == target)
		   return(path);
	   
        for (int i = 0; i < this.G.V(); i++){
            dist[i] = Integer.MAX_VALUE;
    	    parent[i] = -1;
        }
  
        // Add source node to the priority queue
        pq.add(new Vertex(src, 0, -1));
  
        // Distance to the source is 0
        dist[src] = 0;
        boolean targetFound = false;
        while (settled.size() != this.G.V() && pq.size()>0) {
  
            // remove the minimum distance node 
            // from the priority queue
        	Vertex vertex = pq.remove();
            int u = vertex.id;
            parent[u] = vertex.prev;
            
            if(u == target){
            	targetFound = true;
            	break;
            }
  
            // adding the node whose distance is finalized
            settled.add(u);
  
            processNeighbours(u, src, target);
        }
        
        
     // trace path
       if(targetFound && parent[target]>=0){
	       int j = target;
	       path.add(j);
	       while(parent[j]>=0){
	    	   j = parent[j];
	    	   path.add(j);
	       }
       }
       
       return(path);
    }
  
    // Function to process all the neighbours 
    // of the passed node
    private void processNeighbours(int u, int src, int target)
    {
        double edgeDistance = -1;
        double newDistance = -1;
  
        // All the neighbors of v
        for (Edge e : adj[u]) {
	           int v = e.getOtherVertexId(u);
	           
	           boolean ok = true;
	           if(u == src && v == target)
	        	   ok = false;
  
            // If current node hasn't already been processed
            if (!settled.contains(v) && ok) {
                edgeDistance = e.getWeight();
                newDistance = dist[u] + edgeDistance;
  
                // If new distance is cheaper in cost
                if (newDistance < dist[v])
                    dist[v] = newDistance;
  
                // Add the current node to the queue
                pq.add(new Vertex(v, dist[v], u));
            }
        }
    }
    
    
    
 // Class to represent a node in the graph
    class Vertex implements Comparator<Vertex> {
        public int id;
        public double cost;
        public int prev;
      
        public Vertex()
        {
        }
      
        public Vertex(int id, double cost, int prev)
        {
            this.id = id;
            this.cost = cost;
            this.prev = prev;
        }
      
        public int compare(Vertex vertex1, Vertex vertex2)
        {
            if (vertex1.cost < vertex2.cost)
                return -1;
            if (vertex1.cost > vertex2.cost)
                return 1;
            return 0;
        }
    }
    
}
