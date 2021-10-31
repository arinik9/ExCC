package formulation;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import cplex.Cplex;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import myUtils.EdgeWeightedGraph;
import variable.CplexVariableGetter;
import variable.VariableLister;
import variable.VariableLister.VariableListerException;
import variable.VariableValueProvider;


/**
 * 
 * @author Nejat ARINIK
 *
 */
public abstract class AbstractFormulation implements VariableValueProvider{

	String outputDirPath = ".";

	CplexVariableGetter cvg;

	//public PartitionParam p;
	public MyParam p;

	/* Number of points to cluster */
	public int n;

	public EdgeWeightedGraph g;

	public Map<Integer, Double> d; // store the weights of the edges
	public Set<Edge> edges;

	/**
	 * Edges variables Array of n elements. v_edge[0] is empty v_edge[i]
	 * contains an array of i variables (which corresponds to x0i, ..., xi-1,i)
	 * for i=1..n-1
	 */
	public Map<Integer, IloNumVar> v_edge;


	public List<TreeSet<Integer>> clusters; // output
	int[] membership;
	
	
	
	
	public AbstractFormulation(MyParam rp) {
		
		if(rp instanceof MyParam)
			this.p = new MyParam(rp);
		
		
		edges = new HashSet<>();
		d = new HashMap<Integer, Double>();
		this.n = readGraphFromInputFile(rp.inputFile); // fill in 'edges' and 'd'
		
		cvg = new CplexVariableGetter(getCplex());
		g = new EdgeWeightedGraph(n, edges, false);
		

		if(!rp.cplexOutput)
			getCplex().turnOffCPOutput();

		if(!rp.useCplexAutoCuts)
			getCplex().removeAutomaticCuts();

		if(!rp.useCplexPrimalDual)
			getCplex().turnOffPrimalDualReduction();

		try {

			if(rp.LPFilePath.equals("")){
				/* Create the model */
				getCplex().iloCplex.clearModel();
				getCplex().iloCplex.clearCallbacks();
			}

			/* Reinitialize the parameters to their default value */
			getCplex().setDefaults();
			
			System.out.println("rp.tilim: " + rp.tilim);

			if(rp.tilim != -1)
				getCplex().setParam(IloCplex.DoubleParam.TiLim, Math.max(10,rp.tilim));
			
			
			getCplex().setParam(IloCplex.Param.Threads, rp.nbThread);


//			getCplex().setParam(IloCplex.Param.Threads, 1);
//			getCplex().setParam(IloCplex.Param.Threads, getCplex().iloCplex.getNumCores()-1);
			
			
			if(!rp.LPFilePath.equals("")){
				readVarsFromLPModelFile();
				
			} else {
			
				/* Create the variables */
				createVariables();
				createObjectiveFunction();
				createConstraints(rp);
			}
			
//			createConflictedCyleConstraints(); // iterative cycle packing Lange et al

		} catch (IloException e) {
			System.err.println("Concert exception caught: " + e);
			e.printStackTrace();
			System.exit(0);
		}
		
	}


	public abstract void createConstraints(MyParam rp) throws IloException;
	
	
	public abstract int readGraphFromInputFile(String fileName);
	
	
	public void readVarsFromLPModelFile() {

		IloNumVar[] vars;
		try {
		    System.out.println("!!readVarsFromLPModelFile!! ");

			vars = VariableLister.parse(getCplex().iloCplex);
		    System.out.println("!!!!! " + vars.length);

			v_edge = new HashMap<Integer, IloNumVar>();

			for(int a=0; a<vars.length; a++){
				IloNumVar var = vars[a];
				String[] parts = var.getName().split("_");
				int i = Integer.parseInt(parts[1]);
				int j = Integer.parseInt(parts[2]);
				Edge e = new Edge(n, i, j);
	            int pos = e.hashcode;
	            v_edge.put(pos, var);
			}
			
		    System.out.println("!!!!!");
		    
		} catch (IloException | VariableListerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}



	/** 
	 * Get the variable associated to an edge.
	 * @param i First node of the edge.
	 * @param j Second node of the edge.
	 * @return The variable associated to edge (ij).
	 * @throws IloException
	 */
	public Set<Edge> getEdges(){
		return(edges);
	} 
	
	/**
	 * Display the value of the edge variables which are equal to 1
	 * 
	 * @param numberOfElementsByLine
	 *            Number of variables displayed by line
	 * @throws UnknownObjectException
	 * @throws IloException
	 */
	public int[] retreiveEdgeVariables()
			throws UnknownObjectException, IloException {
		int[] edgeVars = new int[edges.size()];

		/* For each edge */
		int k=0;
		for(Edge e : edges) {
			double value = cvg.getValue(this.edgeVar(e.i, e.j));
			if(value > 1E-4)
				edgeVars[k++] = 1;
			else
				edgeVars[k++] = 0;
		}
		
		return(edgeVars);
	}
	


	/** 
	 * Get the variable associated to an edge.
	 * we can equally call edgeVar(2,4) and edgeVar(4,2)
	 * 
	 * @param i First node of the edge.
	 * @param j Second node of the edge.
	 * @return The variable associated to edge (ij).
	 * @throws IloException
	 */
	public IloNumVar edgeVar(int i, int j) throws IloException {
		int pos = new Edge(n, i, j).hashcode;
		return v_edge.get(pos);
	}
	
	
	public double edgeWeight(int i, int j) throws IloException {
		int pos = new Edge(n, i, j).hashcode;
		return d.get(pos);
	}



	public void setOutputDirPath(String _outputDirPath){
		this.outputDirPath = _outputDirPath;
	}

	public String getOutputDirPath(){
		return(this.outputDirPath);
	}


	public boolean isInSameConnComp(int i, int j){
		return(this.g.isInSameConnComp(i, j));
	}



	/**
	 * Display the value of the edge variables which are equal to 1
	 * 
	 * @param numberOfElementsByLine
	 *            Number of variables displayed by line
	 * @throws UnknownObjectException
	 * @throws IloException
	 */
	public void displayEdgeVariables(int numberOfElementsByLine)
			throws UnknownObjectException, IloException {

		int i=0;
		/* While all the edges variables have not been displayed */
		for(Edge e : edges) {

			if((i % numberOfElementsByLine) == 0)
				System.out.println(" ");

			double value = cvg.getValue(edgeVar(e.i,e.j));
			//if(value > 1E-4){
			System.out.print("x" + e.i + "-" + e.j + "(" + value + ")\t\t");
			//}
			i++;
		}

	}


	/**
	 * @return Number of nodes in the graph 
	 */
	public int n() {
		return n;
	}




	public Cplex getCplex() {
		return p.cplex;
	}



	@Override
	public CplexVariableGetter variableGetter() {
		return cvg;
	}




	public void setLogPath(String s) throws FileNotFoundException{
		OutputStream output = new FileOutputStream(s);
		getCplex().iloCplex.setOut(output);
	}



	public void displaySolution(){

		//		if(isSolved)

		try {
			int l = 6;

			/* Display the edge variables different from 0 (<l> by line) */
			System.out.println(" ");
			System.out.println("Edges variables");
			displayEdgeVariables(l);

		} catch (UnknownObjectException e) {
			e.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}





	/**
	 * Add : n-3 variables xi which represent the fact that i is representative
	 * of its cluster (i in [3,n-1]) n * n-1 / 2 variables xi,j (i in [0,n-2], j
	 * in [i+1,n-1])
	 */
	void createVariables() throws IloException {

		v_edge = new HashMap<Integer, IloNumVar>();
		//getCplex().iloCplex.conversion(v_edge[i], IloNumVarType.Float);

		/* For each edge */
		for(Edge e : edges) {
			int pos = e.hashcode;
			if(p.isInt)
				v_edge.put(pos, getCplex().iloCplex.intVar(0,1));
			else
				v_edge.put(pos, getCplex().iloCplex.numVar(0,1));

			v_edge.get(pos).setName("x_" + e.i + "_" + e.j);
			getCplex().iloCplex.add(v_edge.get(pos));
		}

	}



	/**
	 * Create the objective function
	 * 
	 * x_ij=1 means nodes i and j are ina common set
	 * 
	 * The objective function is:
	 * 	 min (  sum_{ (i,j) \in E^- } w_ij x_ij + sum_{ (i,j) \in E^+ } w_ij (1- x_ij)  )
	 * 
	 * But here, we exclude the constant term which is:
	 *  sum_{ (i,j) \in E^+ } w_ij.
	 * Because we will add it at the end
	 * So the objective function becomes: 
	 * 	min - sum_{ (i,j) \in E^+ } w_ij x_ij + sum_{ (i,j) \in E^- } w_ij x_ij
	 * Actually, they are equivalent
	 *
	 * @throws IloException
	 */
	void createObjectiveFunction() throws IloException {

		IloLinearNumExpr obj = getCplex().linearNumExpr();

		double sum = 0.0;
		for(Edge e : edges) {
			if(e.getWeight() > 0.0d){ // process positive edges
				obj.addTerm(-e.getWeight(), edgeVar(e.i,e.j));
				sum += e.getWeight(); // constant term
			}
			else if(e.getWeight() < 0.0d) // process negative edges
				obj.addTerm(Math.abs(e.getWeight()), edgeVar(e.i,e.j));
		}

		//		cplex.addMinimize(obj);
		getCplex().iloCplex.addMinimize(getCplex().iloCplex.sum(obj, sum));

	}




	/**
	 * Create the optimality consraint
	 * 
	 * x_ij=1 means nodes i and j are in a common set
	 * 
	 * The objective function is:
	 * 	 min (  sum_{ (i,j) \in E^- } w_ij x_ij + sum_{ (i,j) \in E^+ } w_ij (1- x_ij)  )
	 * 
	 * But here, we exclude the constant term which is:
	 *  sum_{ (i,j) \in E^+ } w_ij.
	 * Because we will add it at the end
	 * So the objective function becomes: 
	 * 	min - sum_{ (i,j) \in E^+ } w_ij x_ij + sum_{ (i,j) \in E^- } w_ij x_ij
	 * Actually, they are equivalent
	 *
	 * @throws IloException
	 */
	public void createOptimalityConstraint(double upperBound) throws IloException {

		IloLinearNumExpr expr = getCplex().linearNumExpr();

		double sum = 0.0;
		for(Edge e : edges) {
			if(e.getWeight() > 0.0d){ // process positive edges
				expr.addTerm(-e.getWeight(), edgeVar(e.i,e.j));
				sum += e.getWeight(); // constant term
			}
			else if(e.getWeight() < 0.0d) // process negative edges
				expr.addTerm(Math.abs(e.getWeight()), edgeVar(e.i,e.j));
		}

		getCplex().addLe(expr, upperBound-sum);
	}


	/**
	 * Write the resulting graph into file. IT us useful for fractional edge variables
	 * 
	 * @param numberOfElementsByLine
	 *            Number of variables displayed by line
	 * @throws UnknownObjectException
	 * @throws IloException
	 * @throws IOException 
	 */
	public void writeEdgeVariablesIntoFile(String filePath, boolean keepOnlyExistingEdges)
			throws UnknownObjectException, IloException, IOException {
		System.out.println(filePath);
		String content2 = "";
		int nbEdges = 0;

		for(Edge e : edges) {
			int pos = e.hashcode;

			boolean process = true;

			if(keepOnlyExistingEdges && e.getWeight()==0)
				process = false;
			//					if(!keepOnlyExistingEdges && value > 1E-4)
			//						process = false;

			double value = cvg.getValue(edgeVar(e.i,e.j));

			//if(process && value > 1E-4){
			nbEdges++;
			content2 = content2 + e.i + "\t" + e.j + "\t" + value + "\n";
			//}
		}

		String firstLine = this.n + "\t" + nbEdges + "\n";
		String content = firstLine + content2;

		BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
		writer.write(content);
		writer.close();

	}


	public void computeObjectiveValueFromSolution() {

		try {

			double obj = 0.0;
			/*
			 * Display the representative variables different from 0 (<l> by
			 * line)
			 */
			//				System.out.println(" ");
			//				System.out.println("Representative variables");
			//
			//				for (int m = 0; m < n; ++m){
			//					double value = cvg.getValue(v_rep[m]);
			//					System.out.println(m + " : " + value);
			//				}

			/* Display the edge variables different from 0 (<l> by line) */

			for(Edge e : edges) {
				int pos = e.hashcode;

				double value = cvg.getValue(edgeVar(e.i,e.j));

				//						System.out.println(i + "-" + j + " : " + value);
				if(value == 1.0){ // if they are in the same cluster
					if(e.getWeight() < 0) // if negative
						obj += Math.abs(e.getWeight());
				} else {
					if(e.getWeight() > 0) // if positive
						obj += e.getWeight();
				}

			}

			System.out.println("Objective (from model): "+ obj);

			System.out.println(" ");

		} catch (UnknownObjectException e) {
			e.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}

	}



	public void computeObjectiveValueFromClusters() {


		double obj = 0.0;
		/*
		 * Display the representative variables different from 0 (<l> by
		 * line)
		 */
		//				System.out.println(" ");
		//				System.out.println("Representative variables");
		//
		//				for (int m = 0; m < n; ++m){
		//					double value = cvg.getValue(v_rep[m]);
		//					System.out.println(m + " : " + value);
		//				}

		/* Display the edge variables different from 0 (<l> by line) */


		for(Edge e : edges) {
			if(this.membership[e.i] != this.membership[e.j] &&  e.getWeight()>0)
				obj += e.getWeight();
			else if(this.membership[e.i] == this.membership[e.j] &&  e.getWeight()<0)
				obj -= e.getWeight();
		}

		System.out.println("Objective (from membership): " + obj);

	}


	public double solve(){
		try {

			double time = -getCplex().iloCplex.getCplexTime();		
			getCplex().iloCplex.solve();
			return time + getCplex().iloCplex.getCplexTime();


		} catch (IloException e) {
			e.printStackTrace();
			return -1.0;
		}
	}
	
	
	
	public double solve(ArrayList<int[]> prevEdgeVarsList){
		try {
			if(prevEdgeVarsList.size()>0){
			    for(int[] prevEdgeVars : prevEdgeVarsList){
				    createDistinctSolutionConstraint(prevEdgeVars, 1);
			    }
			}
			
			double time = -getCplex().iloCplex.getCplexTime();		
			getCplex().iloCplex.solve();
			return time + getCplex().iloCplex.getCplexTime();
			
			
		} catch (IloException e) {
			e.printStackTrace();
			return -1.0;
		}
	}
	
	
	
	/**
	 *
	 *
	 */
	public void createDistinctSolutionConstraint(int[] prevEdgeVars, double lowerBound) throws IloException {
		// https://www.ibm.com/support/pages/using-cplex-examine-alternate-optimal-solutions
		IloLinearNumExpr expr = getCplex().linearNumExpr();

		int cardinalitySameCluster=0;
		int i=0;
		for(Edge e : edges) {
			if(prevEdgeVars[i] == 1){ // same cluster, so x*(i,j)=1
				cardinalitySameCluster++;
				expr.addTerm(+1.0, edgeVar(e.i, e.j));
			}
			else {
				expr.addTerm(-1.0, edgeVar(e.i, e.j));
			}
			i++;
		}
		getCplex().addLe(expr, cardinalitySameCluster-lowerBound);
		
	}
	



	// =========================================================================


	public List<TreeSet<Integer>> retreiveClusters(int solutionNo){

		List<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();

		membership = new int[n];
		for(int i=0; i<n; i++) // init
			membership[i] = -1;

		/* Check if each point is a cluster of size > 1 */
		boolean[] tb = new boolean[n];

		/* For each edge */
		for(Edge e : edges) {
			double value;
			double w=0.0;
			
			try {
				w = edgeWeight(e.i,e.j);
				
				if(solutionNo > 0) {
					IloNumVar[] currX = {edgeVar(e.i,e.j)}; // an array with one element
					/* source: https://www.ibm.com/support/knowledgecenter/
											SSSA5P_12.8.0/ilog.odms.cplex.help/CPLEX/
											UsrMan/topics/APIs/Java/12_access_soln.html */
					double[] xsol = getCplex().iloCplex.getValues(currX, solutionNo);
					value = xsol[0];
				} else {
					value = getCplex().iloCplex.getValue(edgeVar(e.i,e.j));
				}
			} catch (UnknownObjectException exc) {
				value = 0.0;
				exc.printStackTrace();
			} catch (IloException exc) {
				value = 0.0;
				exc.printStackTrace();
			}

			/* If the edge is in a cluster */
			boolean ok = false;
			if(!p.triangleIneqReducedForm && value > 1E-4){
				ok = true;
			}
			else if(p.triangleIneqReducedForm && w>0 && value > 1E-4){
				ok = true;
			}
			
			if(ok){

				if(membership[e.i]==-1 && membership[e.j]==-1){
					ArrayList<Integer> newCluster = new ArrayList<Integer>();
					newCluster.add(e.i);
					newCluster.add(e.j);
					clusters.add(newCluster);
					membership[e.i] = clusters.size();
					membership[e.j] = clusters.size();
				}
				else if(membership[e.i]!=-1 && membership[e.j]==-1){
					clusters.get(membership[e.i]-1).add(e.j);
					membership[e.j] = membership[e.i];
				} 
				else if(membership[e.i]==-1 && membership[e.j]!=-1){
					clusters.get(membership[e.j]-1).add(e.i);
					membership[e.i] = membership[e.j];
				} 
				else if(membership[e.i]!=-1 && membership[e.j]!=-1 && membership[e.i]!=membership[e.j]){

					// delete the cluster, whose id is larger
					int oldClusterId = membership[e.i];
					int newClusterId = membership[e.j];
					if(membership[e.i]<membership[e.j]){
						oldClusterId = membership[e.j];
						newClusterId = membership[e.i];
					}
					boolean doRenumbering = (clusters.size()>oldClusterId);

					ArrayList<Integer> vertices = clusters.get(oldClusterId-1);
					clusters.get(newClusterId-1).addAll(clusters.get(oldClusterId-1));
					for(int v : vertices)
						membership[v] = newClusterId;
					clusters.remove(oldClusterId-1);

					// renumber cluster ids, if needed
					if(doRenumbering){
						for(int v=0; v<n; v++)
							if(membership[v]>oldClusterId)
								membership[v]--;
					}

				}

			}
		}


		int counter = 0;	
		for(int v=0; v<n; v++)
			if(membership[v]>counter)
				counter = membership[v];

		/* Add the clusters reduced to one point */
		counter++;
		for(int i = 0 ; i < n ; i++){
			if(membership[i]==-1){
				ArrayList<Integer> newCluster = new ArrayList<>();
				newCluster.add(i);
				clusters.add(newCluster);
				membership[i]=counter;
				counter++;
			}
		}


		List<TreeSet<Integer>> clusters2 = new ArrayList<TreeSet<Integer>>();
		for(int i = 0 ; i < clusters.size(); i++){
			TreeSet<Integer> newCluster = new TreeSet<>(clusters.get(i));
			clusters2.add(newCluster);
		}

		//before returning the result, set the "clusters" object
		setClusters(clusters2);


		return(clusters2);

	}


	
	
	public int[] retreiveMembership(){
		int[] membership = new int[this.n];
		int clusterId = 1;
		for(TreeSet<Integer> cluster : clusters) {
			for(Integer v : cluster)
				membership[v] = clusterId;
			clusterId++;
		}
		
		String content = "";
		for(int v=0; v<membership.length; v++){
			if(!content.equals(""))
				content += "\n";
			content += membership[v];
		}
		
		return(membership);
	}
	




	public List<TreeSet<Integer>> retreiveClusters(){
		// -1: solution no is not specified
		return( retreiveClusters(-1) );
	}



	public List<TreeSet<Integer>> getClusters() {
		return(this.clusters);
	}


	public void setClusters(List<TreeSet<Integer>> clusters) {
		this.clusters = new ArrayList<TreeSet<Integer>>();

		for(TreeSet<Integer> cluster : clusters) {
			TreeSet<Integer> newCluster = new TreeSet<Integer>();
			// cloning tree into clinetree
			newCluster = (TreeSet)cluster.clone();
			this.clusters.add(newCluster);
		}
	}


	/**
	 * Display clusters
	 * 
	 */
	public void displayClusters(){
		clusters = getClusters();

		for(TreeSet<Integer> cluster : clusters)
			System.out.println(cluster);
	}


	/**
	 * Write a solution into file
	 * 
	 */
	public void writeClusters(String fileName){
		//String filepath = getOutputDirPath() + "/" + fileName;
		clusters = getClusters();

		String content = "";
		for(TreeSet<Integer> cluster : clusters) {
			if(!content.equals(""))
				content += "\n";
			content += cluster.toString();
		}

		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			writer.write(content);
			writer.close();
		} catch(IOException ioe){
			System.out.print("Erreur in writing output file: ");
			ioe.printStackTrace();
		}
	}		


	
    /**
	 * Write a solution into file
	 * 
	 */
	public void writeMembershipIntoFile(String outputDirPath, String fileName){
		String filepath = outputDirPath + "/" + fileName;
		
		String content = "";
		for(int i=0; i<n; i++){ // for each node
			if(!content.equals(""))
				content += "\n";
			content += membership[i];
		}
			
		try{
			 BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
			 writer.write(content);
			 writer.close();
		 } catch(IOException ioe){
		     System.out.print("Erreur in writing output file: ");
		     ioe.printStackTrace();
		 }
	}
	
	
	
	// ======================================================================
	
	// To enumerate all optimal solution use this method instead of cplex.solve()
	/* source: https://github.com/AdrianBZG/IBM_ILOG_CPLEX_Examples/
	 * 			blob/master/java/src/examples/Populate.java */
	public void populate(long tilimForEnumAll, int solLim) {
				
		/* set GAP to 0.5 instead of 0 for accepting rounding error
		 * source: 
		 * 		https://orinanobworld.blogspot.fr/2013/01/
		 * 		finding-all-mip-optima-cplex-solution.html
		 * */
		double GAP = 0.5;
		
		try {
			/* source: https://www.ibm.com/support/knowledgecenter/
			 * 			SS9UKU_12.5.0/com.ibm.cplex.zos.help/UsrMan/topics/
			 * 			discr_optim/soln_pool/18_howTo.html */
			// to enumerate all optimal solutions, use those parameters
			
			// gap from the obj value of the optimal solution
			getCplex().iloCplex.setParam(IloCplex.Param.MIP.Pool.AbsGap, GAP);
			// For the value 4: the algorithm generates all solutions to your model
			getCplex().iloCplex.setParam(IloCplex.Param.MIP.Pool.Intensity,4);
			
			if(solLim>0){
				System.out.println("solution limit: " + solLim);
				getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.Populate, solLim-1);
			}
			else // 2100000000 is used as a high value
				getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.Populate, 2100000000);
			
			
			System.out.println("----- MIP solve -----");
			long startTime = System.currentTimeMillis();
			boolean isOk = getCplex().iloCplex.solve();
			long endTime = System.currentTimeMillis();
			float execTimeFirstPhase = (float) (endTime-startTime)/1000;
			System.out.println("cplex is ok: " + isOk + " with exec time: " + execTimeFirstPhase);
			System.out.println("cplex status: " + getCplex().iloCplex.getCplexStatus());
			
			System.out.println("----- Populate -----");
			if(tilimForEnumAll>0) {
				System.out.println("tilim for enum all: " + tilimForEnumAll);
				getCplex().iloCplex.setParam(IloCplex.Param.TimeLimit, tilimForEnumAll);
			}
			startTime = System.currentTimeMillis();
			isOk = getCplex().iloCplex.populate();
			endTime = System.currentTimeMillis();
			System.out.println("cplex is ok: " + isOk);
			System.out.println("cplex status: " + getCplex().iloCplex.getCplexStatus());
			
//			System.out.println("----- 2nd populate -----");
//			start = System.currentTimeMillis();
//			isOk = getCplex().iloCplex.populate();
//			end = System.currentTimeMillis();
//			System.out.println("cplex is ok: " + isOk);
//			System.out.println("cplex status: " + getCplex().iloCplex.getCplexStatus());
//			
//			System.out.println("----- 3rd populate -----");
//			start = System.currentTimeMillis();
//			isOk = getCplex().iloCplex.populate();
//			end = System.currentTimeMillis();
//			System.out.println("cplex is ok: " + isOk);
//			System.out.println("cplex status: " + getCplex().iloCplex.getCplexStatus());
			
						
			String filename = getOutputDirPath() + "/exec-time.txt";
			NumberFormat formatter = new DecimalFormat("#0.00000");
			System.out.print("Execution time is "
					+ formatter.format((endTime - startTime) / 1000d) + " seconds");
			
			try{
				 BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
				 writer.write( formatter.format(execTimeFirstPhase +((endTime - startTime) / 1000d)) + " seconds");
				 writer.close();
			 } catch(IOException ioe){
			     System.out.print("Erreur in writing output file: ");
			     ioe.printStackTrace();
			 }
			
			if (isOk) {
	            System.out.println("Solution status = " + getCplex().iloCplex.getStatus());
	            System.out.println("Incumbent objective value  = "
	                               + getCplex().iloCplex.getObjValue());
	          
	
	            /* Get the number of solutions in the solution pool */		
	            int numsol = getCplex().iloCplex.getSolnPoolNsolns();
	            System.out.println("The solution pool contains " + numsol +
	                               " solutions.");
	
	            
	            // -------------------------------------------------------------
	            /* Since GAP=0.5, there will be some sub-optimal solutions, we need not to choose them.
	             * So, determine best optimal solutions in the pool, get their indexes */
	            HashSet<Integer> opt = determineBestSolsFromPool();
	            
	            System.out.println("out: " + getOutputDirPath());
	            
	            /* extract the optimal solutions from the pool and write them into files */
	            int h = 0;    // cumulative index of optimal solutions
	            for (int k : opt) {  // for each index of an optimal solution ...
	                System.out.println("Solution #" + h 
	                   + " (obj value = " + getCplex().iloCplex.getObjValue(k) + "):");
	
	            	retreiveClusters(k);
	            	displayClusters();
	            	computeObjectiveValueFromClusters();
	            	//filename = getOutputDirPath() + "/" + "sol" + h + ".txt";
	            	//writeClusters(filename);
	            	writeMembershipIntoFile(getOutputDirPath(), "sol" + h + ".txt");
	            	h = h + 1;
	            }
	            // -------------------------------------------------------------
	        }
		}
	    catch (IloException e) {
	         System.err.println("Concert exception caught: " + e);
	    }
	}

	
	
	
	
	
	public HashSet<Integer> determineBestSolsFromPool() {
		
		double TOL = 1e-5; // tolerance
		
		// Get the number of solutions in the pool.
	    int nsol = 0;
		try {
			nsol = getCplex().iloCplex.getSolnPoolNsolns();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    // Create a container for the indices of optimal solutions.
	    HashSet<Integer> opt = new HashSet<>();
	    double best = Double.POSITIVE_INFINITY;  // best objective value found
	    /*
	     * Check which pool solutions are truly optimal; if the pool capacity
	     * exceeds the number of optimal solutions, there may be suboptimal
	     * solutions lingering in the pool.
	     */
	    for (int i = 0; i < nsol; i++) {
	    	// Get the objective value of the i-th pool solution.
	    	double z;
			
	    	try {
				z = getCplex().iloCplex.getObjValue(i);
				
				/* retreive solutions from [z - TOL, z + TOL] where z is the best obj val.
				 * Note that the problem is a minimization problem */
				if (z < best - TOL) {
			        /*
			         * If this solution is better than the previous best, the previous
			         * solutions must have been suboptimal; drop them all and count this one.
			         */
			        best = z;
			        opt.clear();
			        opt.add(i);
			      } else if (z < best + TOL) {
			        /*
			         * If this solution is within rounding tolerance of optimal, count it.
			         */
			        opt.add(i);
			      }
			} catch (IloException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      
	    }
	    System.out.println("\n\nFound " + nsol + " solutions, of which "
	                       + opt.size() + " are optimal.");
	    
	    return opt;
	}
	
	
	
	

}
