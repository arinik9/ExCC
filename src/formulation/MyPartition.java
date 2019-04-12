package formulation;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;

import formulation.interfaces.IFEdgeVEdgeW;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
// import ilog.cplex.IloCplex.DoubleParam;
// import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;
// import inequality_family.Range;
import inequality_family.Triangle_Inequality;


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
 * @author zach
 * 
 */
public class MyPartition extends Partition implements IFEdgeVEdgeW{



	/**
	 * Representative variables Array of n-3 elements. v_rep[i] contains the
	 * value of xi+3 (1=0..n-4)
	 */
	public IloNumVar[] v_rep;

	public MyPartition(MyParam rp){
		this(readDissimilarityInputFile(rp), rp);
	}

	public static boolean test = true;

	public MyPartition(double objectif[][], MyParam rp) {

		super(rp);

		this.d = objectif;
		this.n = d.length;

		if(rp instanceof MyParam)
			this.p = new MyParam(rp);
			

		if(!rp.cplexOutput)
			getCplex().turnOffCPOutput();

		if(!rp.useCplexAutoCuts)
			getCplex().removeAutomaticCuts();

		if(!rp.useCplexPrimalDual)
			getCplex().turnOffPrimalDualReduction();

		try {

			/* Reinitialize the parameters to their default value */
			getCplex().setDefaults();


//			getCplex().setParam(DoubleParam.WorkMem, 5000);
//			getCplex().setParam(DoubleParam.TreLim, 4000);
//			getCplex().setParam(IntParam.NodeFileInd, 3);
			
//			if(p.isInt == true)
//				getCplex().setParam(IloCplex.Param.Threads, 16);

			
			/* Create the variables */
			createVariables();
			createObjectiveFunction();
			createConstraints();

			//Turn off preprocessing
//			cplex.setParam(IloCplex.BooleanParam.PreInd, false);

		} catch (IloException e) {
			System.err.println("Concert exception caught: " + e);
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void createConstraints() throws IloException{

		/*
		 * Add triangular constraints : xi,j + xi,k - xj,k <= 1
		 * 
		 * - if i is with j and k, then j and k are together
		 */
		
		createTriangleConstraints();
//		System.out.println("\n!!Add triangle constraints to the model");


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
		for (int i = 1; i < n; ++i)
			for (int j = 0; j < i; ++j)
				if(d[i][j] > 0.0d){ // process positive edges
					obj.addTerm(-d[i][j], v_edge[i][j]);
					sum += d[i][j]; // constant term
				}
				else if(d[i][j] < 0.0d) // process negative edges
					obj.addTerm(Math.abs(d[i][j]), v_edge[i][j]);

//		cplex.addMinimize(obj);
		getCplex().iloCplex.addMinimize(getCplex().iloCplex.sum(obj, sum));

	}



	/**
	 * Add : n-3 variables xi which represent the fact that i is representative
	 * of its cluster (i in [3,n-1]) n * n-1 / 2 variables xi,j (i in [0,n-2], j
	 * in [i+1,n-1])
	 */
	void createVariables() throws IloException {

		if(p.isInt)
			v_edge = new IloIntVar[n][];
		else
			v_edge = new IloNumVar[n][];


		/* Create the edge variables (lower triangular part of v_edge) */
		for (int i = 0 ; i < n; ++i){
			if(p.isInt)
				v_edge[i] = new IloIntVar[n];
			else
				v_edge[i] = new IloNumVar[n];

			getCplex().iloCplex.conversion(v_edge[i], IloNumVarType.Float);
			
			for(int j = 0 ; j < i ; ++j){
				if(p.isInt)
					v_edge[i][j] = getCplex().iloCplex.intVar(0, 1);
				else
					v_edge[i][j] = getCplex().iloCplex.numVar(0,1);
				
				v_edge[i][j].setName("x_" + i + "_" + j);
				
				// ==========================================
				
				/* Link the symetric variables to their equivalent in the lower triangular
					part of v_edge => Ex : v[1][0] = v[0][1] */
				v_edge[j][i] = v_edge[i][j];
			}
			
		}

		
		/* WORKAROUD: This workaround works all the time and does not duplicate
		 *  any edge variables in the model. Because, we do not add any variables
		 *   to the model if the graph is complete
		 * 
		 * When the graph is incomplete and we use Cutting Planes approach,
		 *  we add constraints and cuts lazily.
		 * This causes ObjectException from cplex.getValue() since not all the edge
		 *  variables are in the model or constraints in the beginning.
		 * I found this thread as a solution: 
		 * 			https://www.ibm.com/developerworks/community/forums/
		 * 			html/topic?id=4f8bc2e4-a514-48d3-af62-10b9f417516d
		 */
		for (int i = 0 ; i < n; ++i){
			
			for(int j = i+1 ; j < n ; ++j) {
				
				// if the edge does not exist in the graph, i.e the corresponding weight = 0
				if(d[i][j] == 0) { 
					getCplex().iloCplex.add(v_edge[i][j]); // add it to the model
					getCplex().iloCplex.add(v_edge[j][i]); // add it to the model
				}
			}
		}
		
	}

	
	
	
	/**
	 * Add triangular constraints : xi,j + xi,k - xj,k <= 1 - if i is with j and
	 * k, then j and k are together
	 * @param solution 
	 * 
	 * @throws IloException
	 */
	void createTriangleConstraints() throws IloException {
		
		for (int i = 0; i < n - 2; ++i)
			for (int j = i + 1; j < n - 1; ++j)
				for (int k = j + 1; k < n; ++k) {

					getCplex().addRange(new Triangle_Inequality(this, i, j, k).createRange());
					getCplex().addRange(new Triangle_Inequality(this, j, i, k).createRange());

					IloLinearNumExpr expr3 = getCplex().linearNumExpr();
					expr3.addTerm(1.0, v_edge[k][i]);
					expr3.addTerm(1.0, v_edge[k][j]);
					
					expr3.addTerm(-1.0, v_edge[j][i]);
					getCplex().addLe(expr3, 1.0);
				}
	}
	
	
	



	


	public void computeObjectiveValueFromSolution() {

		try {

			double obj = 0.0;
			/*
			 * Display the representative variables different from 0 (<l> by
			 * line)
			 */
//			System.out.println(" ");
//			System.out.println("Representative variables");
//
//			for (int m = 0; m < n; ++m){
//				double value = cvg.getValue(v_rep[m]);
//				System.out.println(m + " : " + value);
//			}

			/* Display the edge variables different from 0 (<l> by line) */
			System.out.println(" ");
			System.out.println("Edges variables");

			/* While all the edges variables have not been displayed */
			for(int i = 1 ; i < n ; ++i)
				for(int j = 0 ; j < i ; ++j){

					double value = cvg.getValue(v_edge[i][j]);

//					System.out.println(i + "-" + j + " : " + value);
					if(value == 1.0){ // if they are in the same cluster
						if(d[i][j] < 0) // if negative
							obj += Math.abs(d[i][j]);
					} else {
						if(d[i][j] > 0) // if positive
							obj += d[i][j];
					}

				}

			System.out.println("Objective: "+ obj);

			System.out.println(" ");

		} catch (UnknownObjectException e) {
			e.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}

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
	
	
	
	
	// ======================================================================
	
	// To enumerate all optimal solution use this method instead of cplex.solve()
	/* source: https://github.com/AdrianBZG/IBM_ILOG_CPLEX_Examples/
	 * 			blob/master/java/src/examples/Populate.java */
	public void populate(String outputFolder) {
		
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
			getCplex().iloCplex.setParam(IloCplex.Param.MIP.Pool.Intensity, 4);
			// 2100000000 is used as a high value
			getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.Populate, 2100000000);

			long start = System.currentTimeMillis();
			boolean isOk = getCplex().iloCplex.populate();
			long end = System.currentTimeMillis();
			
			String filename = outputFolder + "/exec-time.txt";
			NumberFormat formatter = new DecimalFormat("#0.00000");
			System.out.print("Execution time is "
					+ formatter.format((end - start) / 1000d) + " seconds");
			
			try{
				 BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
				 writer.write( formatter.format((end - start) / 1000d) + " seconds");
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
	            /* Since GAP=0.5, there will be some sub-optimal olsutions, we need not to choose them.
	             * So, determine best optimal solutions in the pool, get their indexes */
	            HashSet<Integer> opt = determineBestSolsFromPool();
	            
	            /* extract the optimal solutions from the pool and write them into files */
	            int h = 0;    // cumulative index of optimal solutions
	            for (int k : opt) {  // for each index of an optimal solution ...
	                System.out.println("Solution #" + h 
	                   + " (obj value = " + getCplex().iloCplex.getObjValue(k) + "):");
	
	            	retreiveClusters(k);
	            	filename = outputFolder + "/" + "sol" + h + ".txt";
	            	writeClusters(filename);
	            	
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
