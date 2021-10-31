package separation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import formulation.Edge;
import formulation.AbstractFormulation;
import ilog.concert.IloException;
import inequality_family.AbstractInequality;
import inequality_family.CycleInequality;
import myUtils.EdgeWeightedGraph;
import variable.VariableGetter;



/**
 * Separate the cycle inequalities ...
 * 
 * @author zach
 *
 */
public class SeparationCycleBFS extends AbstractSeparation<AbstractFormulation>{

	int MAXCUT;
	int MAXFOUND;

	boolean[][] inequalityAdded;

	//public TreeSet<ConflictedCycleInequality> foundIneq = new TreeSet<ConflictedCycleInequality>();
	public ArrayList<CycleInequality> foundIneq = new ArrayList<CycleInequality>();
	
	public SeparationCycleBFS(AbstractFormulation formulation, VariableGetter vg, int MAXCUT) {
		super("cycle bfs", formulation, vg);
		
		this.MAXCUT = MAXCUT;
		MAXFOUND = 5*MAXCUT;

	}

	@Override
	public ArrayList<AbstractInequality<? extends AbstractFormulation>> separate() {

		ArrayList<AbstractInequality<? extends AbstractFormulation>> result = new ArrayList<>();

		foundIneq.clear();	
		
		Set<Edge> zeroEdges = new HashSet<>();
		Set<Edge> oneEdges = new HashSet<>();
		
		int n = formulation.n();
		
		try {
			for(Edge e : formulation.getEdges()){
				int i = e.getSource();
				int j = e.getDest();
				double val = vg.getValue(formulation.edgeVar(i, j));
				if(val<0.5)
					zeroEdges.add(e);
				else
					oneEdges.add(e);
			}

		} catch (IloException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		EdgeWeightedGraph g = new EdgeWeightedGraph(n, formulation.getEdges(), false);
		EdgeWeightedGraph g_aux = new EdgeWeightedGraph(n, oneEdges, false);
		
		for(Edge e : zeroEdges){
			int i = e.getSource();
			int j = e.getDest();

			if(formulation.isInSameConnComp(i,j) && g_aux.isInSameConnComp(i, j)){
				ArrayList<Integer> path = g_aux.searchShortestPathBFS(i,j);
				
				//if(path.size()>2 && isPathChordless(path, e, g.getAdj())){
				if(path.size()>2){
					CycleInequality c = new CycleInequality(this.formulation, n, e, path);
					double res;
					try {
						res = c.getSlack(vg);
						if(res < -eps){
							//System.out.println("slack: " + res);					
							//c.createRange();
							foundIneq.add(c);
						}
					} catch (IloException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				}
			}
		}
		
		

		Iterator<CycleInequality> it = foundIneq.iterator();
		int nb = 0;

//		while(it.hasNext() && nb < MAXCUT){
		while(it.hasNext()){
			result.add(it.next());
			nb++;
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
