package separation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import formulation.Edge;
import formulation.AbstractFormulation;
import ilog.concert.IloException;
import inequality_family.AbstractInequality;
import inequality_family.CycleInequality;
import myUtils.EdgeWeightedGraph;
import myUtils.StdRandom;
import variable.VariableGetter;



/**
 * Separate the cycle inequalities ...
 * 
 * source: /home/nejat/eclipse/workspace-neon/opengm-master/include/opengm/inference/multicut.hxx
 * 
 * @author Nejat
 *
 */
public class SeparationCycleDijkstra extends AbstractSeparation<AbstractFormulation>{

	int MAXCUT;
	int MAXFOUND;

	boolean[][] inequalityAdded;

	//public TreeSet<ConflictedCycleInequality> foundIneq = new TreeSet<ConflictedCycleInequality>();
	public ArrayList<CycleInequality> foundIneq = new ArrayList<CycleInequality>();
	
	public SeparationCycleDijkstra(AbstractFormulation formulation, VariableGetter vg, int MAXCUT) {
		super("cycle dijkstra", formulation, vg);
		
		this.MAXCUT = MAXCUT;
		MAXFOUND = 5*MAXCUT;

	}

	@Override
	public ArrayList<AbstractInequality<? extends AbstractFormulation>> separate() {

		ArrayList<AbstractInequality<? extends AbstractFormulation>> result = new ArrayList<>();

		foundIneq.clear();	
		
		int n = formulation.n();
		Set<Edge> edges = new HashSet<Edge>();
		Set<Edge> nonZeroEdges = new HashSet<>();

		int[] indexs = new int[formulation.getEdges().size()];
		for(int i=0; i<formulation.getEdges().size(); i++)
			indexs[i] = i;
		StdRandom.shuffle(indexs);
		
		try {
			List<Edge> edgesList = new ArrayList<>(formulation.getEdges());
			for(int c=0; c<formulation.getEdges().size(); c++){
				Edge e = edgesList.get(indexs[c]);
				int i = e.getSource();
				int j = e.getDest();
				double val = vg.getValue(formulation.edgeVar(i, j));
				Edge enew = new Edge(n, i, j);
				enew.setWeight(val);
				if(val>1E-4)
					nonZeroEdges.add(enew);
				edges.add(enew);
			}

			EdgeWeightedGraph g = new EdgeWeightedGraph(n, edges, false);
			EdgeWeightedGraph g_aux = new EdgeWeightedGraph(n, nonZeroEdges, false);
			
				
			for(Edge e : edges){
				int i = e.getSource();
				int j = e.getDest();
				double oneEps = 1-1E-4;
				if(e.getWeight()<oneEps){
	
					if(formulation.isInSameConnComp(i,j) && g_aux.isInSameConnComp(i, j)){
						ArrayList<Integer> path = g.searchShortestPathDijkstra(i,j);
						
						if(path.size()>2 && isPathChordless(path, e, g.getAdj())){
							CycleInequality c = new CycleInequality(this.formulation, n, e, path);
							//c.createRange();
							double res = c.getSlack(vg);
							if(res < -eps){
								//System.out.println("slack: " + res);

								foundIneq.add(c);
							}
						}
					}
				}
			}
			
	
			Iterator<CycleInequality> it = foundIneq.iterator();
			int nb = 0;
			while(it.hasNext() && nb < MAXCUT){
				result.add(it.next());
				nb++;
			}
		
		} catch (IloException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return result;
	}
	
	
	public boolean isPathChordless(ArrayList<Integer> path, Edge e, Set<Edge>[] adj){
		int i = e.getSource();
		int j = e.getDest();
		
		
		for(int v1=0; v1<path.size(); v1++){
			for(int v2=v1+2; v2<path.size(); v2++){
				if(!(path.get(v1)==j && path.get(v2)==i)){ // in case of triangle
					Set<Edge> neighEdges = adj[path.get(v1)];
					for(Edge neighEdge : neighEdges){
						if(neighEdge.getOtherVertexId(path.get(v1)) == path.get(v2)){
							return(false);
						}
					}
				}
			}
		}
	
		return(true);
	}


}
