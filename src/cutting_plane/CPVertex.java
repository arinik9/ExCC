package cutting_plane;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import callback.cut_callback.CutCallback_all;
import callback.lazy_callback.LazyCBCycle;

import formulation.FormulationVertex;
import formulation.Edge;
import formulation.FormulationEdge;
import formulation.AbstractFormulation;
import formulation.MyParam;
import formulation.MyParam.Transitivity;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import inequality_family.AbstractInequality;
import mipstart.AbstractMIPStartGenerate;
import mipstart.PrimalHeuristicRounding;
import mipstart.SolutionManager;
import separation.SeparationCycleBFS;
import separation.SeparationCycleDijkstra;
import separation.SeparationSTGrotschell;
import separation.SeparationSTKL;
import separation.SeparationSTLabbe;
import separation.SeparationTCCKLFixedSize;
import separation.SeparationTriangle;

public class CPVertex extends AbstractCuttingPlane<AbstractFormulation>{

	int MAX_CUT;
	boolean userCutInBB;
	boolean lazyInBB;
	int nbThread;
	
	int solLim = -1; 
	int tilimForEnumAll = -1;
	
	/**
	 * 
	 * @param p  partition parameter object
	 * @param MAX_CUT  the number of user cuts to be added (among all)
	 * @param modFindIntSolution  the frequency of applying primal heuristic during the iterations.
	 *  For instance, if mod=2, primal heuristic is applied once in every 2 subsequent iterations
	 * @param reordering  ordering separation algorithms ==> OBSOLETE
	 * @param tilim -1 if there is no limit on the time ; a time in seconds otherwise
	 * @throws IloException 
	 */
	public CPVertex(MyParam p, int MAX_CUT, int modRemovingUntightCuts, int modFindIntSolution,
			boolean reordering, double tilim, int tilimForEnumAll_, int solLim_, String outputDir, int MaxTimeForRelaxationImprovement, boolean isEnumAll_,
			boolean verbose_, int[] initialPartitionMembership_, boolean onlyFractionalSolution_, double rootRelaxationGapPropValue_)
			throws IloException {
		super(p, modRemovingUntightCuts, modFindIntSolution, reordering, tilim, outputDir, MaxTimeForRelaxationImprovement,
				isEnumAll_, verbose_, initialPartitionMembership_, onlyFractionalSolution_, rootRelaxationGapPropValue_);
		
		this.userCutInBB = p.userCutInBB;
		this.lazyInBB = p.lazyInBB;
		this.nbThread = p.nbThread;
		
		this.solLim = solLim_;
		this.tilimForEnumAll = tilimForEnumAll_;
		
		formulation = new FormulationVertex(p);
		
		try
	    {
			formulation.setOutputDirPath(outputDir);
			formulation.setLogPath(outputDir + "/logcplex.txt");
	    }
	    catch (FileNotFoundException e)
	    {
	    	System.err.println("File not found exception caught: " + e);
	    }
		
		this.MAX_CUT = MAX_CUT;
	}

	
	/** 
	 * Adds separation algorithms (which generates inequalities) into array
	 *  in order that they are used during Root Relaxation approach.
	 * 
	 * @param remaining_time
	 * @param mipStart  the best feasible solution obtained in the Root Relaxation part
	 */
	@Override
	public void createSeparationAlgorithm() {
		boolean toAddInBB;
		boolean isQuick;


		/* Grotschell ST */
		toAddInBB = true;
		isQuick = true;
		sep.add(new CP_Separation<AbstractFormulation>(
				new SeparationSTGrotschell(formulation, formulation.variableGetter(), MAX_CUT),
				toAddInBB,
				isQuick
				));

		/* Labbe ST */
		toAddInBB = true;
		isQuick = true;
		sep.add(new CP_Separation<AbstractFormulation>(
				new SeparationSTLabbe(formulation, formulation.variableGetter()),
				toAddInBB,
				isQuick
				));
			

		/* Triangle inequalities */
		MyParam rp = (MyParam)this.formulation.p;

		/* If the triangle inequalities are:
		 * - not in cutting plane model 
		 * - not generated lazily in the cutting plane step 
		 * - not contained in the branch and cut model */ 
		if(rp.transitivity == Transitivity.USE_LAZY_IN_BC_ONLY) {
			toAddInBB = false; // TODO true ?
			isQuick = true;
			sep.add(new CP_Separation<AbstractFormulation>(
					new SeparationTriangle(formulation, formulation.variableGetter(), MAX_CUT, rp.triangleIneqReducedForm),
					toAddInBB, isQuick
					));
		}

		
		/* If the triangle inequalities are:
		 * - not in cutting plane model 
		 * - not generated lazily in the cutting plane step 
		 * - contained in the branch and cut model */ 
		else if(rp.transitivity == Transitivity.USE_IN_BC_ONLY) {
			toAddInBB = false;
			isQuick = true;
			sep.add(new CP_Separation<AbstractFormulation>(
					new SeparationTriangle(formulation, formulation.variableGetter(), MAX_CUT, rp.triangleIneqReducedForm),
					toAddInBB,
					isQuick
					));
		}

		/* Kernighan-Lin ST and ?? inequalities */
		toAddInBB = true;
		isQuick = false;
		sep.add(new CP_Separation<AbstractFormulation>(
				new SeparationSTKL(formulation, formulation.variableGetter(), 5, true),
				toAddInBB,
				isQuick
				));
		
		toAddInBB = true;
		isQuick = false;
		sep.add(new CP_Separation<AbstractFormulation>(
				new SeparationTCCKLFixedSize(formulation, formulation.variableGetter(), 2, null, true),
				toAddInBB,
				isQuick
				));
	}

	
	
//	@Override
//	public void registerLPmodelAfterCP(ArrayList<AbstractInequality<? extends AbstractFormulation>> addedCuts) throws IloException {
//
//		/* Get the tight constraints */
//		ArrayList<AbstractInequality<? extends AbstractFormulation>> ineq =  this.getTightConstraints();
////		ArrayList<Abstract_Inequality> ineq = this.getAllConstraints();
//		if(addedCuts.size()>0)
//			ineq.addAll(addedCuts);
//		
//		// indicate that the formulation/solution will be integer during the Branch&Bound
//		formulation.p.isInt = true;
//
//		
//		try {
//			/* Create the partition with integer variables */
//			formulation = new FormulationVertex((MyParam)formulation.p);
////			formulation = new MyPartition((RepParam)formulation.p);
//
//			/* Add the previously tight constraints to the formulation */
//			for(AbstractInequality<? extends AbstractFormulation> i : ineq){
//
//				i.setFormulation(formulation);
//				try {
//					formulation.getCplex().addRange(i.createRange());
//				} catch (IloException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			// ================================================================================
//			// Export model with valid inequalities generated during lazy callback
//			// ================================================================================
//			formulation.getCplex().iloCplex.exportModel(this.outputDir+"/"+"strengthedModelAfterRootRelaxation.lp");
//			
//			// ====================================================================================
//			
//		} catch (IloException e) {
//			e.printStackTrace();
//		}
//	}
	
	
	
	/** 
	 * Creates Integer formulation and provides it with the best feasible solution
	 *  obtained during the Cutting Planes approach.
	 * If Lazy callback or User Cuts are allowed in this Branch&Bound, 
	 * CPLEX solves it with 1 thread. Otherwise, use the maximal number of threads
	 * This Branch&Bound part is handled entirely by CPLEX (as a blackbox function,
	 *  as contrary to the previous Root Relaxation part)
	 * If time limit is specified in input parameters and the integer optimal solution
	 *  is reached before time limit, the solution is written into file
	 * 
	 * @param remaining_time
	 * @param mipStart  the best feasible solution obtained in the Root Relaxation part
	 */
	@Override
	public void findAllSolutionsAfterCP(double remaining_time, SolutionManager mipStart) {

		System.out.println("remaining_time" + remaining_time);
		double cp_time = tilim - remaining_time;
		System.out.println("cp_time: " + cp_time);

		try {
			formulation.writeEdgeVariablesIntoFile(outputDir+"/fractionalGraph.G", false);
		} catch (IloException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		/* Get the tight constraints */
		ArrayList<AbstractInequality<? extends AbstractFormulation>> ineq =  this.getTightConstraints();
//		ArrayList<Abstract_Inequality> ineq = this.getAllConstraints();

		// indicate that the formulation/solution will be integer during the Branch&Bound
		formulation.p.isInt = true;
		formulation.p.cplexOutput = true;
		
		String outputDirPath = formulation.getOutputDirPath();
		
		try {
			/* Create the partition with integer variables */
			formulation = new FormulationVertex((MyParam)formulation.p);
			formulation.setOutputDirPath(outputDirPath);
//			formulation = new MyPartition((RepParam)formulation.p);
			
			if(mipStart != null){
				try {
					if(this.verbose)
						System.out.println("!!! MIP START eval:"+mipStart.evaluate()+" !!!");
					
					mipStart.updateFormulationAndVariables(formulation);

					mipStart.setVar();
					formulation.getCplex().addMIPStart(mipStart.var, mipStart.val);
					
					if(this.verbose)
						System.out.println("!!!!!!!!!!MIP START DONE!!!!!!!");
				} catch (IloException e) {
					e.printStackTrace();
				}		
			}
			
			

			/* Add the previously tight constraints to the formulation */
			for(AbstractInequality<? extends AbstractFormulation> i : ineq){

				i.setFormulation(formulation);
				try {
					formulation.getCplex().addRange(i.createRange());
				} catch (IloException e) {
					e.printStackTrace();
				}
			}

//			cpresult.time = - formulation.getCplex().getCplexTime();
//			formulation.getCplex().solve();
//			cpresult.time += formulation.getCplex().getCplexTime();
			
			formulation.getCplex().setParam(IloCplex.Param.Threads, this.nbThread);


			if(this.verbose)
				System.out.println("out: " + formulation.getOutputDirPath());
		
			double GAP = 0.5;
		
			/* source: https://www.ibm.com/support/knowledgecenter/
			 * 			SS9UKU_12.5.0/com.ibm.cplex.zos.help/UsrMan/topics/
			 * 			discr_optim/soln_pool/18_howTo.html */
			// to enumerate all optimal solutions, use those parameters
			
			// gap from the obj value of the optimal solution
			formulation.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Pool.AbsGap, GAP);
			// For the value 4: the algorithm generates all solutions to your model
			formulation.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Pool.Intensity, 4);
			if(solLim>0){
				System.out.println("solution limit: " + solLim);
				formulation.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.Populate, solLim-1);
			}
			else // 2100000000 is used as a high value
				formulation.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.Populate, 2100000000);
			
			
			
			System.out.println("----- MIP solve -----");
			long startTime = System.currentTimeMillis();
			boolean isOk = formulation.getCplex().iloCplex.solve();
			long endTime = System.currentTimeMillis();
			float execTimeFirstPhase = (float) (endTime-startTime)/1000;
			System.out.println("cplex is ok: " + isOk + " with exec time: " + execTimeFirstPhase);
			System.out.println("cplex status: " + formulation.getCplex().iloCplex.getCplexStatus());
			//if(tilim>0 && tilimForEnumAll<0)
			//	remaining_time -= execTimeFirstPhase;
			
			
			
			if(this.verbose)
				System.out.println("----- populate -----");
			
			if(tilimForEnumAll>0) {
				System.out.println("tilim for enum all: " + this.tilimForEnumAll);
				formulation.getCplex().iloCplex.setParam(IloCplex.Param.TimeLimit, this.tilimForEnumAll);
			} else if(remaining_time>0) {
				remaining_time -= execTimeFirstPhase;
				if(remaining_time<0)
					remaining_time = 1;
				System.out.println("remaining_time: " + remaining_time);
				formulation.getCplex().iloCplex.setParam(IloCplex.Param.TimeLimit, remaining_time);
			}
			
			startTime = System.currentTimeMillis();
			isOk = formulation.getCplex().iloCplex.populate();
			endTime = System.currentTimeMillis();
			float execTimeSecondPhase = (float) (endTime-startTime)/1000;

			
			if(this.verbose){
				System.out.println("cplex is ok: " + isOk);
				System.out.println("cplex status: " + formulation.getCplex().iloCplex.getCplexStatus());
			}

			
			NumberFormat formatter = new DecimalFormat("#0.00000");
			if(this.verbose)
				System.out.print("Execution time is "
						+ formatter.format((endTime - startTime) / 1000d) + " seconds");
			
			String filename = outputDirPath + "/exec-time.txt";
			try{
				 BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
				 writer.write( formatter.format(cp_time + execTimeFirstPhase +execTimeSecondPhase) + " seconds");
				 writer.close();
			 } catch(IOException ioe){
			     System.out.print("Erreur in writing output file: ");
			     ioe.printStackTrace();
			 }
			
			
			
			if (isOk) {
				if(this.verbose){
		            System.out.println("Solution status = " + formulation.getCplex().iloCplex.getStatus());
		            System.out.println("Incumbent objective value  = "
		                               + formulation.getCplex().iloCplex.getObjValue());
				}
	
	            /* Get the number of solutions in the solution pool */		
	            int numsol = formulation.getCplex().iloCplex.getSolnPoolNsolns();
	            if(this.verbose)
	            	System.out.println("The solution pool contains " + numsol +
	                               " solutions.");
	            
	            // -------------------------------------------------------------
	            /* Since GAP=0.5, there will be some sub-optimal olsutions, we need not to choose them.
	             * So, determine best optimal solutions in the pool, get their indexes */
	            HashSet<Integer> opt = formulation.determineBestSolsFromPool();
	            
	            /* extract the optimal solutions from the pool and write them into files */
	            int h = 0;    // cumulative index of optimal solutions
	            for (int k : opt) {  // for each index of an optimal solution ...
	            	if(this.verbose)
		                System.out.println("Solution #" + h 
		                   + " (obj value = " + formulation.getCplex().iloCplex.getObjValue(k) + "):");
	
	                formulation.retreiveClusters(k);
	                formulation.displayClusters();
	                formulation.computeObjectiveValueFromClusters();
	            	//String filename = formulation.getOutputDirPath() + "/sol" + h + ".txt";
	            	//formulation.writeClusters(filename);
	            	formulation.writeMembershipIntoFile(formulation.getOutputDirPath(),"sol" + h + ".txt");
	            	h = h + 1;
	            }
	            // -------------------------------------------------------------
			}
		

		} catch (IloException e) {
			e.printStackTrace();
		}

	}
	
	

	
	
	/** 
	 * Creates Integer formulation and provides it with the best feasible solution
	 *  obtained during the Cutting Planes approach.
	 * If Lazy callback or User Cuts are allowed in this Branch&Bound, 
	 * CPLEX solves it with 1 thread. Otherwise, use the maximal number of threads
	 * This Branch&Bound part is handled entirely by CPLEX (as a blackbox function,
	 *  as contrary to the previous Root Relaxation part)
	 * If time limit is specified in input parameters and the integer optimal solution
	 *  is reached before time limit, the solution is written into file
	 * 
	 * @param remaining_time
	 * @param mipStart  the best feasible solution obtained in the Root Relaxation part
	 */
	@Override
	public ArrayList<AbstractInequality<? extends AbstractFormulation>> findIntSolutionAfterCP(double remaining_time, SolutionManager mipStart) {
		System.out.println("Remaining time for b&c: " + remaining_time);
		ArrayList<AbstractInequality<? extends AbstractFormulation>> addedCuts = new ArrayList<>();

		try {
			formulation.writeEdgeVariablesIntoFile(outputDir+"/fractionalGraph.G", false);
		} catch (IloException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		/* Get the tight constraints */
		this.tightIneqs =  this.getTightConstraints();
//		ArrayList<Abstract_Inequality> ineq = this.getAllConstraints();
		addedCuts.addAll(this.tightIneqs);

		// indicate that the formulation/solution will be integer during the Branch&Bound
		formulation.p.isInt = true;
		
		formulation.p.cplexOutput = true;
		if(remaining_time != -1)
			formulation.p.tilim = remaining_time;
		
		try {
			/* Create the partition with integer variables */
			formulation = new FormulationVertex((MyParam)formulation.p);
//			formulation = new MyPartition((RepParam)formulation.p);
			
			if(mipStart != null){
				try {
					if(this.verbose)
						System.out.println("!!! MIP START eval:"+mipStart.evaluate()+" !!!");
					
					mipStart.updateFormulationAndVariables(formulation);

					mipStart.setVar();
					formulation.getCplex().addMIPStart(mipStart.var, mipStart.val);
					
					if(this.verbose)
						System.out.println("!!!!!!!!!!MIP START DONE!!!!!!!");
				} catch (IloException e) {
					e.printStackTrace();
				}		
			}

			/* Add the previously tight constraints to the formulation */
			for(AbstractInequality<? extends AbstractFormulation> i : this.tightIneqs){

				i.setFormulation(formulation);
				try {
					formulation.getCplex().addRange(i.createRange());
				} catch (IloException e) {
					e.printStackTrace();
				}
			}
			
//			// ================================================================================
//			// Export model with valid inequalities generated during lazy callback
//			// ================================================================================
//			formulation.getCplex().iloCplex.exportModel(this.outputDir+"/"+"strengthedModelAfterRootRelaxation.lp");
//			
//			// ====================================================================================
			
			
			CutCallback_all acc = null;
//			FastCutCallback acc = null;
			if(this.userCutInBB) {
				acc = new CutCallback_all(formulation, 500);
//				acc = new FastCutCallback(formulation, 500);
				formulation.getCplex().use(acc);
				addedCuts = acc.getAddedCuts();
			}
			
			LazyCBCycle lcc = null;
//			FastCutCallback acc = null;
			if(this.lazyInBB) {
				lcc = new LazyCBCycle(formulation, 500);
//				acc = new FastCutCallback(formulation, 500);
				formulation.getCplex().use(lcc);
				addedCuts = lcc.getAddedCuts();
			}
			
			formulation.getCplex().setParam(IloCplex.Param.Threads, this.nbThread);

			
			
//			BranchFurtherFromInteger bcc = new BranchFurtherFromInteger();
//			bcc.setPartition(formulation);
//			TreeSet<ArrayList<Integer>> mipStartInArrayFormat = getMIPStartSolutionInArrayFormat(mipStart.membership);
//			bcc.setMIPStartSolution(mipStartInArrayFormat);
////			// // BranchDisplayInformations bcc = new BranchDisplayInformations();
//			formulation.getCplex().use(bcc);
			
			cpresult.time = - formulation.getCplex().getCplexTime();

			formulation.getCplex().solve();
			cpresult.time += formulation.getCplex().getCplexTime();
			cpresult.getResults(formulation, acc, false);


			
			if(this.userCutInBB) {
				addedCuts.addAll(acc.getAddedCuts());
			}
			
			if(this.lazyInBB) {
				addedCuts.addAll(lcc.getAddedCuts());
			}

			
//			try {
//    			formulation.getCplex().iloCplex.exportModel(outputDir+"/"+"strengthedModelAfterRootRelaxation_final.lp");
//				formulation.writeEdgeVariablesIntoFile(outputDir+"/vars.G", false);
//			} catch (IloException | IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}			
			
			if(this.verbose){
				System.out.println("bestInt = " + formulation.getCplex().getObjValue());
				System.out.println("bestRelax. = " + formulation.getCplex().getBestObjValue());
			}
			
//			formulation.retreiveClusters();
//			formulation.displayClusters();
//			formulation.writeClusters(outputDir + "/result.txt");
			
		} catch (IloException e) {
			e.printStackTrace();
		}

		return(addedCuts);
	}
	
	
	
	public void registerLPmodelAfterCP(String filenameLP,
			ArrayList<AbstractInequality<? extends AbstractFormulation>> addedCuts
			) throws IloException {

		if(addedCuts.size()>0) {
			
			try {
				/* Create the partition with integer variables */
				formulation = new FormulationVertex((MyParam)formulation.p);

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
				formulation.getCplex().iloCplex.exportModel(this.outputDir+"/"+filenameLP);

				// ====================================================================================
				
			} catch (IloException e) {
				e.printStackTrace();
			}
		
		}
	}
	
	
    public TreeSet<ArrayList<Integer>> getMIPStartSolutionInArrayFormat(int[] membership){
    	int n = membership.length;
    	int nbCluster=0;
		for(int i=0; i<n; i++){
			if(membership[i]>nbCluster)
				nbCluster = membership[i];
		}
		
		TreeSet<ArrayList<Integer>> orderedClusters = new TreeSet<ArrayList<Integer>>(
				new Comparator<ArrayList<Integer>>(){
					// descending order by array size
					@Override
					public int compare(ArrayList<Integer> o1, ArrayList<Integer> o2) {
						int value=-1;
						if(o1.size() < o2.size())
							value = 1;
//						else if(o1.size() < o2.size())
//								value = -1;
						return value;
					}
				}
		);

		
    	ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>(nbCluster);
		for(int i=1; i<=nbCluster; i++) // for each cluster
			clusters.add(new ArrayList<Integer>());
		for(int i=0; i<n; i++) // for each node
			clusters.get(membership[i]-1).add(i); // membership array has values starting from 1
		
		for(int i=1; i<=nbCluster; i++){ // for each cluster
			ArrayList<Integer> newCluster = clusters.get(i-1);
			orderedClusters.add(newCluster);
		}
		

		return(orderedClusters);
    }
    
    
    

	
	public SolutionManager getMIPStart() throws IloException{

		PrimalHeuristicRounding mipGetter = new PrimalHeuristicRounding(formulation);

		return mipGetter.generateMIPStart();
	}


	/**
	 * Test if the current solution of the formulation is integer
	 * 
	 * @return True if the current solution is integer; false otherwise
	 */
	public boolean isInteger(){

		boolean result = true;

		try {

			for(Edge e : formulation.getEdges()){
				int i = e.getSource();
				int j = e.getDest();
				double val = formulation.variableGetter().getValue(formulation.edgeVar(i, j));
					
				if(!isInteger(val)){
					result = false;
					break;
				}

			}
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
		}	

		return result;
	}

	
	@Override
	public AbstractMIPStartGenerate initializeMIPStartGenerator() {
		return new PrimalHeuristicRounding(formulation);	
	}

	
	@Override
	public AbstractFormulation getFormulation() {
		return formulation;
	}
	
	
	
	/**
	 * It obtains the method id of the Cycle Separation method.
	 * It is called during manual lazy callback in Root Relaxation
	 * ==> 'checkLazilyIfIntegerSolutionValid()'
	 * 
	 * @return methodId  the corresponding index
	 */
	@Override
	public int getCycleOrTriangleSeparationMethodId() {

		Iterator<CP_Separation<?>> it = sep.iterator();
		
		int methodId = -1;
		for(int i=0; i< sep.size(); i++){
			if(sep.get(i).se.name.equals("triangle iterative")) {
				methodId = i;
				break;
			}
		}
		
		return methodId;
	}



}
