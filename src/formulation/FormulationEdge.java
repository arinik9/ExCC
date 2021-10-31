package formulation;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import callback.lazy_callback.LazyCBCycle;
import formulation.MyParam.Transitivity;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import inequality_family.AbstractInequality;
import myUtils.EdgeWeightedGraph;


/**
 * Program which use cplex to solve a k-partition problem of n nodes (0, 1, ...,
 * n-1)
 * 
 * 
 * The 0-1 variables are:
 * 
 * - xi,j = 1 if the edge (i,j) is in the partition; 0 otherwise
 * 
 * - xi = 1 if i the representative of its cluster (i.e. the point with lower
 * index); 0 otherwise
 * 
 * 
 * 
 * Three families of constraints are considered:
 * 
 * - triangular inequalities (if i is with j and k, j and k are together)
 * 
 * - upper representative (no more than 1 representative by cluster)
 * 
 * - lower representative (at least one representative by cluster)
 * 
 * @author Nejat ARINIK
 * 
 */
public class FormulationEdge extends AbstractFormulation {


	public FormulationEdge(MyParam rp) {
		super(rp);
	}
	


	
	
	/**
	 * This method reads input graph file, then stocks it as weighted adjacency matrix, 
	 * finally writes the graph in lower triangle format into a temp file.
	 * 
	 * @param filename  input graph filename
	 * @return 
	 */
	@Override
	public int readGraphFromInputFile(String fileName) {
		int n=-1;
		
		// =====================================================================
		// read input graph file
		// =====================================================================
		try {
			InputStream ips = new FileInputStream(fileName);
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;

			ligne = br.readLine();

			/* Get the number of nodes from the first line */
			n = Integer.parseInt(ligne.split("\t")[0]);
			System.out.println("n: " + n);
			
			/* For all the other lines */
			while ((ligne = br.readLine()) != null) {

				String[] split = ligne.split("\t");

				if (split.length >= 3) {
					int i = Integer.parseInt(split[0]);
					int j = Integer.parseInt(split[1]);
					double w = Double.parseDouble(split[2]);

					if(w != 0.0){
						Edge e = new Edge(n, i, j);
						e.setWeight(w);
						edges.add(e);
						int pos = e.hashcode;
						d.put(pos, w);
					}
				} else
					System.err.println(
							"All the lines of the input file must contain three values" + " separated by tabulations"
									+ "(except the first one which contains two values).\n" + "Current line: " + ligne);
			}
			br.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		// end =================================================================
		
		
		return(n);
	}
	
	

	public void registerLPmodel(String filenameLP,
			ArrayList<AbstractInequality<? extends AbstractFormulation>> addedCuts
			) throws IloException {

		if(addedCuts.size()>0) {
			
			try {
				FormulationEdge formulation = new  FormulationEdge((MyParam)this.p);
				/* Add the previously tight constraints to the formulation */
				for(AbstractInequality<? extends AbstractFormulation> i : addedCuts){
	
					i.setFormulation(formulation);
					try {
						formulation.getCplex().addRange(i.createRange());
					} catch (IloException e) {
						e.printStackTrace();
					}
				}
				
				// ================================================================================
				// Export model with valid inequalities generated during lazy callback
				// ================================================================================
				//formulation.getCplex().iloCplex.exportModel(this.outputDir+"/"+"strengthedModelAfterRootRelaxation.lp");
				formulation.getCplex().iloCplex.exportModel(this.getOutputDirPath()+"/"+filenameLP+"_edge.lp");

				// ====================================================================================
				
			} catch (IloException e) {
				e.printStackTrace();
			}
		
		}
	}

	
	
	public void addConflictedCycleConstraint(ArrayList<Integer> path){
		
		IloLinearNumExpr expr = getCplex().linearNumExpr();
		
		try {
				
			for(int v=1; v<path.size(); v++)
				expr.addTerm(+1.0, edgeVar(path.get(v-1), path.get(v)));
			
			expr.addTerm(-1.0, edgeVar(path.get(0), path.get(path.size()-1)));
			
			getCplex().addLe(expr, path.size() - 2.0);
			
		} catch (IloException e) {
			e.printStackTrace();
		}
		
	}
	
	
	// Iterative Cycle Packing. Lange et al. 2018
	public void createConflictedCyleConstraints(){
		EdgeWeightedGraph g_temp = new EdgeWeightedGraph(n, edges, false);
		Set<Edge> edges_temp = new HashSet<>(edges);

		double lower_bound = 0.0;
		int cycle_counter = 0;
		
		Map<Integer, Double> capacity = new HashMap<Integer, Double>();
		for(Edge e : edges_temp)
			capacity.put(e.hashcode, Math.abs(e.getWeight()));
		
		for(int l=3; l<10; l++){
			Iterator<Edge> it = edges_temp.iterator();
			while(it.hasNext()){
				Edge e = it.next();
				if(e.weight<0.0){
					ArrayList<Integer> path = g_temp.searchShortestPathBFS(e.i, e.j, l, true);
					
					if(path.size()>2){
//						System.out.println("path.size(): "+path.size());
						addConflictedCycleConstraint(path);

						cycle_counter++;
						
						// determine the min capacity
						double c_min = capacity.get(e.hashcode); // init
						for(int i=1; i<path.size(); i++){
							int pos = new Edge(g_temp.V(), path.get(i-1), path.get(i)).hashcode;
							if(capacity.get(pos)<c_min)
								c_min = capacity.get(pos);
						}
						lower_bound += c_min;
						
						// update the capacity on positive path
						for(int i=1; i<path.size(); i++){
							int pos = new Edge(g_temp.V(), path.get(i-1), path.get(i)).hashcode;
							capacity.put(pos, capacity.get(pos)-c_min);
							if(capacity.get(pos)<1E-4)
								g_temp.removeEdge(new Edge(g_temp.V(), path.get(i-1),path.get(i)));
								//g.removeEdge(path.get(i-1), path.get(i));
						}
						// update the capacity on the negative edge
						capacity.put(e.hashcode, capacity.get(e.hashcode)-c_min);
						if(capacity.get(e.hashcode)<1E-4)
							it.remove();
					}
				}
			}
		}
		
		System.out.println("lower_bound: "+lower_bound);
		System.out.println("cycle_counter: "+cycle_counter);
		System.out.println("edges.size(): "+edges.size());
		System.out.println("edges_temp.size(): "+edges_temp.size());

	}





	@Override
	public void createConstraints(MyParam rp) throws IloException {
		if(rp.transitivity == Transitivity.USE
				|| (rp.transitivity == Transitivity.USE_IN_BC_ONLY && p.isInt == true)){
			// TODO add all conflicted cycle constraints
			// !!!!!!!!!
		}
		else if(rp.transitivity == Transitivity.USE_LAZY
				|| (rp.transitivity == Transitivity.USE_LAZY_IN_BC_ONLY && p.isInt == true)){
			System.out.println("!!!!!!! " + "LazyCBCycle");

			getCplex().use(new LazyCBCycle(this, 500));
		}
		else
			System.out.println("\n!!Don't add any cycle constraints or lazy callback");
	}



 
	
	
	

	
	
	
	
}
