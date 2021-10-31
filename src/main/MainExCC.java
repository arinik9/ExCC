package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.*;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

import callback.lazy_callback.LazyCBCycle;
import cplex.Cplex;
import cutting_plane.AbstractCuttingPlane;
import cutting_plane.CPEdge;
import cutting_plane.CPVertex;
import formulation.AbstractFormulation;
import formulation.FormulationEdge;
import formulation.FormulationVertex;
import formulation.MyParam;
import formulation.MyParam.Transitivity;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;
import mipstart.AbstractMIPStartGenerate;
import mipstart.PrimalHeuristicRounding;
import mipstart.SolutionManager;
import variable.VariableLister;
import variable.VariableLister.VariableListerException;


/**
* The ExCC program aims at solving Correlation Clustering (CC) problem.
* 
* There are two strategies: obtaining a single vs. all optimal partitions/solution(s).
* When all optimal partitions of a given signed graph are required, we call this method "OneTreeCC".
* Each mentioned strategy above, i.e. single vs. all optimal solutions, can be performed with two resolutions methods:
* 	- Branch&Bound (B&B)
* 	- Branch&Cut (B&C)
* In both methods, an ILP model to solve the CC problem can be constructed in two different ways: 
* 		1) decision variables defined on vertex-pair (Fv: "vertex" formulation type) or 2) edge (Fe: "edge" formulation type).
* If we denote "n" by the number of vertices in the graph and "m" by the number of edges, 
* 		there are (n*(n-1)/2) variables in Fv, whereas there are m variables in Fe.  
* 
* In B&B, we just let Cplex solve it with B&B.
* In B&C, there are two successful applications of B&C, as shown in the literature:
* 	1) adding violated valid inequalities only at the root of the B&B tree through the Cutting Plane (CP) method and then proceeding to the branching phase as in B&B,
*   2) adding violated valid inequalities only for integer solutions during the branching phase.
* According to the literature, the first B&C application is better suited for Fv (that we call B&C(Fv)), 
* 	whereas the second one is for Fe (that we call B&C(Fv)).
* 
* So, we have two successful resolutions methods, B&C(Fv) and B&C(Fe), to solve the CC problem. But, which one should be used?
* In chapter 2 of my PhD thesis, I conducted some experiments based on unweighted random signed graphs to clarify this point.
* We found that the choice of the formulation and its resolution method depends on the characteristics of the network at hand.
* When a network is sparse (resp. dense), the  B&C(Fe) (resp. B&C(Fv)) better performs. Moreover, for a medium graph density, 
*  the B&C(Fv) type is less sensitive to increase in "n" than the other methods, hence preferable in this case.
* 
* 
* To solve the CC problem we can either read a signed graph with a .G graph file format or import a Cplex LP file, 
* 	where a ILP model is already recorded in a previous run. The advantage of doing the second option is that
* 	 if we provide ExCC with a ILP model containing violated valid inequalities found during a CP method, 
* 	then it amounts to skip the CP phase of the B&C method. So, it directly proceeds to the second phase: branching. 
* 	This allows to gain a considerable amount of time.
* 
* 
* Some references for the CC problem:
* <ul>
* <li> Cartwright D, Harary F (1956) Structural balance:
* 	 a generalization of Heider’s theory. Psychol Rev 63:277-293 <\li>
* <li> Heider F (1946) Attitudes and cognitive organization. J Psychol 21:107-112 <\li>
* <li> N. Arınık, Multiplicity in the Partitioning of Signed Graphs. PhD thesis in Avignon Université (2021). <\li>
* <\lu>
*/
public class MainExCC {

	
	/**
	 * 
	 * Input parameters:
	 * <ul>
 	 * <li> formulationType (String): ILP formulation type. Either "vertex" for Fv or "edge" for Fe. </li>
	 * <li> inFile (String): Graph input file path. </li>
	 * <li> outDir (String): Output directory path. Default "." 
	 * 		(i.e. the current directory). </li>
	 * <li> cp (Boolean): True if B&C (i.e. Cutting Plane approach) will be used.
	 * 		 Default false. </li>
	 * <li> enumAll (Boolean): True if enumerating all optimal solutions is desired. Default false. </li>
	 * <li> tilim (Integer): Time limit in seconds for the whole execution process. </li>
	 * <li> tilimForEnumAll (Integer): Time limit in seconds when enumerating all optimal solutions, except the first one. 
	 * 			This is useful when doing a benchmarking with EnumCC for the OneTreeCC method. </li>
	 * <li> solLim (Integer): max number of optimal solutions to be discovered when OneTreeCC is called.
	 * 						This can be useful when there are a huge number of optimal solutions, e.g. 50,000. </li>
	 * <li> MaxTimeForRelaxationImprovement (Integer): Max time limit for relaxation improvement in the first phase of the Cutting Plane method.
	 *  				This is independent of the time limit. If there is no considerable improvement for X seconds, it stops and passes to the 2nd phase, which is branching.
	 *  				This parameter can be a considerable impact on the resolution time. For medium-sized instances (e.g. 50,60), 
	 *  				it might be beneficial to increase the value of this parameter (e.g. 1800 or 3600s). The default value is 600s.	
	 *  				Moreover, it might be beneficial to decrease the default value to 30s or 60s if the graph is easy to solve or 
	 *  				the number of vertices is below 28.				
	 * </li>
	 * <li> lazyInBB (Boolean): Used only for B&C(Fe) method for the formulation Fe. True if adding lazily triangle constraints (i.e. lazy callback approach) in the branching phase. If it is False and the B&C(Fv),
	 * 						 the whole set of triangle constraints is added before branching. Default false. </li>
	 * <li> userCutInBB (Boolean): Used only for B&C method. True if adding user cuts during the branching phase of the B&C method or in B&B method is desired.
	 * 		 Based on our experiments, we can say that it does not yield any advantage, and it might even slow down the optimization process. Default false. </li>
	 * <li> nbThread (Integer): Default value is the max number of CPU minus 1.
	 * <li> verbose (Boolean): Default value is True. When True, it enables to display log outputs during the Cutting Plane approach.
	 * <li> initMembershipFilePath (String): Default value is "". It allows to import an already known solution into the optimization process. 
	 * 										Since we solve a minimization problem, the imbalance value of the imported solution is served as the upper bound.
	 * 										It is usually beneficial to use this option, when we possess some good-quality heuristics. </li>
	 * <li> LPFilePath (String): Default value is "". It allows to import a LP file, corresponding to a ILP formulation. Remark: such a file can be obtained through Cplex by doing 'exportModel'. </li>
	 * <li> triangleIneqReducedForm (Boolean): Used only for the Fv formulation type. When it is set to true, this amounts to remove "redundant" triangle inequalities from the formulation.
	        This "redundancy" is with respect to the definition of Miyauchi et al.: 
	            A. Miyauchi, T. Sonobe, and N. Sukegawa, « Exact Clustering via Integer Programming and Maximum Satisfiability », in: AAAI Conference on Artificial Intelligence 32.1 (2018).
	        Default value is false, which keeps the wole set of triangle constraints. See Chapter 2 in my Phd thesis.
             Note that removing redundant triangle inequalities can substantially accelerate the optimization process for finding a single optimal solution. 
             However, if the goal is to enumerate all optimal solutions, then such removing can degrade the performance. 
             This last point is briefly mentioned in Chapter 5 in my PhD thesis, but it needs to be investigated thoroughly in a follow-up work.
	 * <li> onlyFractionalSolution (boolean): Default value is False. Useful mostly for the Fv formulation type. It allows to run only the cutting plane method in B&C, 
	 * 											so the program does not proceed to the branching phase </li>
	 * <li> fractionalSolutionGapPropValue (Double): Useful mostly for the Fv formulation type. It allows to limit the gap value to some proportion value during the cutting plane method in B&C.
	 * 												it can be useful when we solve an easy graph. Hence, we do not spent much time by obtaining very tiny improvement 
	 * 												when the solution is already close to optimality. </li>
	 * </ul>
	 * 
	 * Different possible resolution methods for the Fv vertex formulation type.
	 * 1) B&B: Branch&Bound for finding a single optimal solution
	 * 2) B&B enum all: Branch&Bound for enumerating all optimal solutions
	 * 3) CP B&B: B&C method with 2 phases for finding a single optimal solution >> 1) Cutting Plane, 2) B&B
	 * 4) CP B&B enum all: enumerating all optimal solutions with B&C
	 * 5) CP only: only Cutting Plane method, i.e. strengthing the initial LP model with tight valid inequalities
	 * 6) LP B&B: reading the ILP formulation from Cplex LP file and run B&B for finding a single optimal solution.
	 * 7) LP B&B enum all: reading the ILP formulation from Cplex LP file and run B&B for enumerating all optimal solutions.
	 * 
	 * 
	 * 
	 * Different possible resolution methods for the Fe edge formulation type.
	 * 1) CP B&B: B&C method with 2 phases for finding a single optimal solution >> 1) Cutting Plane, 2) B&B
	 * 2) CP B&B enum all: enumerating all optimal solutions with B&C
	 * 3) LP B&B: reading the ILP formulation from Cplex LP file and run B&B for finding a single optimal solution.
	 * 4) LP B&B enum all: reading the ILP formulation from Cplex LP file and run B&B for enumerating all optimal solutions.
	 * 
	 * 
	 * 
	 * 
	 * Example of CP B&B >> B&C(Fv) for the Fv formulation type (when to use B&C approach, set 'cp' to true.):
	 * <pre>
	 * {@code
	 * ant clean compile jar
	 * ant -DinFile=in/net.G -DoutDir=out/net -DformulationType="vertex" -Dcp=true -DenumAll=false 
	 * -DMaxTimeForRelaxationImprovement=120 -DfractionalSolutionGapPropValue=0.01 -DnbThread=4 -Dverbose=true
	 * -Dtilim=300 -DtriangleIneqReducedForm=true run
	 * 
	 * java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio201/cplex/bin/x86-64_linux/ 
	 * 	-DinFile=in/net.G -DoutDir=out/net -DformulationType="vertex" -DenumAll=false -Dcp=true
	 *  -DMaxTimeForRelaxationImprovement=120 -DlazyCB=false -DuserCutCB=false -DinitMembershipFilePath=""
	 *   -DLPFilePath="" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=0.01 -DnbThread=4 -Dverbose=true
	 *    -Dtilim=300 -DtriangleIneqReducedForm=true -jar exe/ExCC.jar
	 * }
	 * </pre>
	 * 
	 * 
	 * 
	 * Example of CP B&B >> B&C(Fe) for the Fe formulation type (when to use B&C approach, set 'cp' to true.):
	 * <pre>
	 * {@code
	 * ant clean compile jar
	 * 
	 * ant -DinFile=in/net.G -DoutDir=out/net -DformulationType="edge" -DenumAll=false -Dcp=true 
	 * -DMaxTimeForRelaxationImprovement=120 -DlazyCB=true -DuserCutCB=false -DinitMembershipFilePath="" 
	 * -DLPFilePath="" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 -DnbThread=4 -Dverbose=true -Dtilim=300 run
	 * 
	 * java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio201/cplex/bin/x86-64_linux/ 
	 * 	-DinFile=in/net.G -DoutDir=out/net -DformulationType="edge" -DenumAll=false -Dcp=true
	 *  -DMaxTimeForRelaxationImprovement=120 -DlazyCB=true -DuserCutCB=false -DinitMembershipFilePath="" 
	 * 	-DnbThread=4 -Dverbose=true -DLPFilePath="" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 
	 * 	-DMaxTimeForRelaxationImprovement=20 -Dtilim=100 -DtilimForEnumAll=-1 -DsolLim=1 -jar exe/ExCC.jar
	 * 
	 * }
	 * </pre>
	 * 
	 * 
	 * @param args  (Not used in this program. Instead, user parameters are obtained
	 * 	 through ant properties. See the build.xml for more details).
	 * 
	 * @throws FileNotFoundException.
	 * @throws UnknownObjectException. 
	 * @throws IloException.
	 */
	public static void main(String[] args) throws FileNotFoundException, UnknownObjectException, IloException {

		
		int tilim = -1;
		int tilimForEnumAll = -1;
		int solLim = -1;
		boolean lazyCB = false;
		boolean userCutCB = false;
		String inputFilePath = "";
		String outputDirPath = ".";
		boolean isCP = false;
		boolean isEnumAll = false;
		int MaxTimeForRelaxationImprovement = -1;
		boolean verbose = true;
		String initMembershipFilePath = "";
		int nbThread = 1;
		String formulationType = "";
		
		String LPFilePath = "";
		boolean triangleIneqReducedForm = false;
		
		boolean onlyFractionalSolution = false;
		double fractionalSolutionGapPropValue = -1.0; // 0.03 => 3 percent gap
		
		
		
		if( !System.getProperty("inFile").equals("${inFile}") )
			inputFilePath = System.getProperty("inFile");
		else {
			System.out.println("input file is not specified. Exit");
			return;
		}
		
		if( !System.getProperty("formulationType").equals("${formulationType}") && (System.getProperty("formulationType").equals("vertex") || System.getProperty("formulationType").equals("edge")) )
			formulationType = System.getProperty("formulationType");
		else {
			System.out.println("formulationType is not specified. Let us choose for you through an estimation ");
			// estimate which formulation type would be more beneficial
			// based on the number of vertices and graph density
			
			formulationType = estimateFormulationType(inputFilePath);
			System.out.println("Estimated formulation type is '"+formulationType+"'");
			if(formulationType.equals("edge"))
			    System.out.println("Lazy callback is also enabled with this formulation type.");
		}
		
		
		
		if( !System.getProperty("outDir").equals("${outDir}") )
			outputDirPath = System.getProperty("outDir");
		if( !System.getProperty("cp").equals("${cp}") )
			isCP = Boolean.valueOf(System.getProperty("cp"));
		if( !System.getProperty("enumAll").equals("${enumAll}") )
			isEnumAll = Boolean.valueOf(System.getProperty("enumAll"));
		
		
		
		if( !System.getProperty("tilim").equals("${tilim}") )
			tilim = Integer.parseInt(System.getProperty("tilim"));
		
		if( !System.getProperty("tilimForEnumAll").equals("${tilimForEnumAll}") )
			tilimForEnumAll = Integer.parseInt(System.getProperty("tilimForEnumAll"));
		
		if( !System.getProperty("solLim").equals("${solLim}") )
			solLim = Integer.parseInt(System.getProperty("solLim"));
		
		// Those 3 options are  available with cutting plane approach
		if( isCP && !System.getProperty("MaxTimeForRelaxationImprovement").equals("${MaxTimeForRelaxationImprovement}") )
			MaxTimeForRelaxationImprovement = Integer.parseInt(System.getProperty("MaxTimeForRelaxationImprovement"));
		
//		if( !isEnumAll && isCP && !System.getProperty("lazyCB").equals("${lazyCB}") )
		// if( !System.getProperty("lazyCB").equals("${lazyCB}") )
		// lazyCB = Boolean.valueOf(System.getProperty("lazyCB"));
		if(formulationType.equals("edge"))
			lazyCB = true;

//		if( !isEnumAll && isCP && !System.getProperty("userCutCB").equals("${userCutCB}") )
		if( !isEnumAll && isCP && !System.getProperty("userCutCB").equals("${userCutCB}") )
			userCutCB = Boolean.valueOf(System.getProperty("userCutCB"));

		if( !System.getProperty("verbose").equals("${verbose}") )
			verbose = Boolean.valueOf(System.getProperty("verbose"));
//		if( !System.getProperty("verbose").equals("${verbose}") )
//			verbose = Boolean.valueOf(System.getProperty("verbose"));
		
		
		if(!System.getProperty("nbThread").equals("${nbThread}") )
			nbThread = Integer.parseInt(System.getProperty("nbThread"));
		
		if( !System.getProperty("initMembershipFilePath").equals("${initMembershipFilePath}") ) // it is not usefull
			initMembershipFilePath = System.getProperty("initMembershipFilePath");

		
		if( !System.getProperty("LPFilePath").equals("${LPFilePath}") )
			LPFilePath = System.getProperty("LPFilePath");
		else {
			System.out.println("LPFilePath file is not specified.");
		}
		
		
		if( formulationType.equals("vertex") &&  !System.getProperty("triangleIneqReducedForm").equals("${triangleIneqReducedForm}") )
			triangleIneqReducedForm = Boolean.valueOf(System.getProperty("triangleIneqReducedForm"));
	

		if( !System.getProperty("onlyFractionalSolution").equals("${onlyFractionalSolution}") )
			onlyFractionalSolution = Boolean.valueOf(System.getProperty("onlyFractionalSolution"));
		else {
			System.out.println("onlyFractionalSolution is not specified. The default value is false, i.e. performing B&B after cutting plane phase");
		}
		
		if( !System.getProperty("fractionalSolutionGapPropValue").equals("${fractionalSolutionGapPropValue}") )
			fractionalSolutionGapPropValue = Double.parseDouble(System.getProperty("fractionalSolutionGapPropValue"));
		else {
			System.out.println("fractionalSolutionGapPropValue is not specified. The default value is -1.0");
		}	
			
		
		System.out.println("===============================================");
		System.out.println("formulationType: " + formulationType);
		System.out.println("inFile: " + inputFilePath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("isCP: " + isCP);
		System.out.println("isEnumAll: " + isEnumAll);
		System.out.println("tilim: " + tilim + "s");
		System.out.println("tilimForEnumAll: " + tilimForEnumAll + "s");
		System.out.println("solLim: " + solLim);
		System.out.println("MaxTimeForRelaxationImprovement: " + MaxTimeForRelaxationImprovement + "s");
		System.out.println("lazyCB: " + lazyCB);
		System.out.println("userCutCB: " + userCutCB);
		System.out.println("nbThread: " + nbThread);
		System.out.println("verbose: " + verbose);
		System.out.println("initMembershipFilePath: " + initMembershipFilePath);
		System.out.println("LPFilePath: " + LPFilePath);
		System.out.println("triangleIneqReducedForm: " + triangleIneqReducedForm);
		System.out.println("onlyFractionalSolution: " + onlyFractionalSolution);
		System.out.println("fractionalSolutionGapPropValue: " + fractionalSolutionGapPropValue);
		System.out.println("===============================================");


		// ------------------------------------------
		
		int[] initMembership = null;
		if(!initMembershipFilePath.equals("")){
			// initMembership
			initMembership = readMembership(initMembershipFilePath);
		}
		
		// -------------------------------------
		
		MyParam myp = null;
		Cplex cplex = new Cplex(); // start
		cplex.setParam(IntParam.ClockType, 2);
		
		if(!LPFilePath.equals("")){ 
			System.out.println("laod LP");			
			cplex.iloCplex.importModel(LPFilePath);
		}
		
		// -------------------------------------
		
		if(isCP && LPFilePath.equals("")) { // Cutting Plane approach (without LP file path)
			System.out.println("Cutting Plane");

			// =================================================================
			
			if(lazyCB)
				myp = new MyParam(inputFilePath, cplex, Transitivity.USE_LAZY_IN_BC_ONLY, userCutCB, lazyCB, nbThread, LPFilePath, triangleIneqReducedForm);
			else
				myp = new MyParam(inputFilePath, cplex, Transitivity.USE_IN_BC_ONLY, userCutCB, lazyCB, nbThread, LPFilePath, triangleIneqReducedForm);
		
//			myp.cplexOutput = false;
			myp.useCplexPrimalDual = true;
			myp.useCplexAutoCuts = true;
			myp.tilim = tilim;
			myp.userCutInBB = userCutCB;
			// 		p.getCplex().setParam(IloCplex.Param.Threads, nbThread);

			
			int MAXCUT = 500;
			int minimalTimeBeforeRemovingUntightCuts = 1;
			int modFindIntSolution = 5;
			
			
			/* 'reordering' is not important if 'isQuick' is set to TRUE
			 *  for Separation methods in cutting planes */
			boolean reordering = false;
			AbstractCuttingPlane<AbstractFormulation> cp = null;
			if(formulationType.equals("vertex"))
				cp = new CPVertex(myp, MAXCUT, minimalTimeBeforeRemovingUntightCuts,
						modFindIntSolution, reordering, tilim, tilimForEnumAll, solLim, 
						outputDirPath, MaxTimeForRelaxationImprovement,
						isEnumAll, verbose, initMembership, onlyFractionalSolution, fractionalSolutionGapPropValue);
			else if(formulationType.equals("edge"))
				cp = new CPEdge(myp, MAXCUT, minimalTimeBeforeRemovingUntightCuts,
						modFindIntSolution, reordering, tilim, tilimForEnumAll, solLim, 
						outputDirPath, MaxTimeForRelaxationImprovement,
						isEnumAll, verbose, initMembership, onlyFractionalSolution, fractionalSolutionGapPropValue);
			
			cp.solve();
			
			// end =============================================================

		} else { // B&B approach
			System.out.println("Branch&Bound");
			
			// note that when LPFilePath.equals("")=FALSE, we load directly the model from file, so the choice of 'Triangle' does not affect it
			if(lazyCB)
				myp = new MyParam(inputFilePath, cplex, Transitivity.USE_LAZY, userCutCB, lazyCB, nbThread, LPFilePath, triangleIneqReducedForm);
			else
				myp = new MyParam(inputFilePath, cplex, Transitivity.USE, userCutCB, lazyCB, nbThread, LPFilePath, triangleIneqReducedForm);
			
			
			myp.useCplexPrimalDual = true;
			myp.useCplexAutoCuts = true;
			myp.tilim = tilim;
			myp.userCutInBB = userCutCB;
			
			try {
				
				AbstractFormulation p = null;
				LazyCBCycle lcb = null;
				if(formulationType.equals("vertex"))
					p = new FormulationVertex(myp); // LPFilePath.equals("")=FALSE, we will just load variables
				else if(formulationType.equals("edge")) {
					p = new FormulationEdge(myp);
					
					// since the creation of constraints is omitted, we need to do it here
					lcb = new LazyCBCycle(p, 500);
					p.getCplex().use(lcb);
				}
				
				p.setLogPath(outputDirPath + "/logcplex.txt");
				p.setOutputDirPath(outputDirPath); // it writes all solutions into files
				
				
				
				if(initMembership != null){
					AbstractMIPStartGenerate mipStartGenerator = new PrimalHeuristicRounding(p);
					SolutionManager mipStart = mipStartGenerator.loadIntSolution(initMembership);
					mipStart.setVar();
					p.getCplex().iloCplex.addMIPStart(mipStart.var, mipStart.val);
				}
				
				
				if(isEnumAll) { // Enumerate all optimal solutions
					System.out.println("BEFORE POPULATE() in main");
				
					p.populate(tilimForEnumAll, solLim);
				
					System.out.println("AFTER POPULATE() in main");
				
				} 
				else { // Obtain only one optimal solution
					p.solve();
					
					p.retreiveClusters();
					p.computeObjectiveValueFromSolution();
					p.computeObjectiveValueFromClusters();
					p.displayClusters();
					//p.writeClusters(outputDirPath + "/sol0.txt");
					p.writeMembershipIntoFile(outputDirPath, "sol0.txt");
					
					
					if(formulationType.equals("vertex"))
						p.getCplex().iloCplex.exportModel(outputDirPath+"/"+"strengthedModel.lp");
					else
						((FormulationEdge) p).registerLPmodel("strengthedModel.lp", lcb.getAddedCuts());
				}
			
			}
			catch (OutOfMemoryError e) {
			    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
	            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
	            long maxMemory = heapUsage.getMax() / 1024;
	            long usedMemory = heapUsage.getUsed() / 1024;
	            System.out.println(" : Memory Use :" + usedMemory + "M/" +maxMemory+"M");
	        }
			
		}
		
		
		cplex.end(); // end
	}
	
	
	
	
	
	/**
	 * It estimates the formulation type, which is either "edge" or "vertex", based on the number of vertices in the graph and graph density.
	 * This estimation reflects the observations regarding the experiments conducted in Chapter 2 of my PhD thesis.
	 * 
	 * @param inputFilePath The file path for the considered graph 
	 */
	public static String estimateFormulationType(String inputFilePath) {
		String formulationType = "";
		
		int n = extractVertexNumber(inputFilePath);
		double dens = extractGraphDensity(inputFilePath);
		if(dens<0.45)
			formulationType = "edge";
		else if(dens>0.45)
			formulationType = "vertex";
		if(dens==0.45) {
			if(n<40)
				formulationType = "edge";
			else
				formulationType = "vertex";
		}
		
		return(formulationType);
	}

	
	
	/**
	 * extract the number of vertices of the graph in input
	 * 
	 * @param inputFilePath The file path for the considered graph 
	 * 
	 */
	public static int extractVertexNumber(String inputFilePath) {
		int n = -1;
		InputStream ips;
		try {
			ips = new FileInputStream(inputFilePath);
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;
			ligne = br.readLine();
			br.close();

			/* Get the number of nodes from the first line */
			n = Integer.parseInt(ligne.split("\t")[0]);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return(n);
	}
	
	
	
	/**
	 * It extracts the density of the graph in input
	 * 
	 * @param inputFilePath The file path for the considered graph 
	 * 
	 */
	public static double extractGraphDensity(String inputFilePath) {
		double dens = -1;
		InputStream ips;
		try {
			ips = new FileInputStream(inputFilePath);
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;
			ligne = br.readLine();
			br.close();

			/* Get the number of nodes from the first line */
			int n = Integer.parseInt(ligne.split("\t")[0]);
			int m = Integer.parseInt(ligne.split("\t")[1]);
			int max_m = (n*(n-1))/2; 
			dens = (float) m/max_m;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return(dens);
	}
	
	
	
	/**
	 * It reads a membership information of a given file in input.
	 * 
	 * @param inputFilePath The membership file path (i.e. partition information) of a given graph
	 * 
	 */
	public static int[] readMembership(String membershipFilepath) {
		int[] membership_;
		try{
			int n = getNbLinesInFile(membershipFilepath);
			membership_ = new int[n];
		

			InputStream  ips = new FileInputStream(membershipFilepath);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			  
			for(int i=0; i<n; i++){ // for each node
				line = br.readLine();
				membership_[i] = Integer.parseInt(line);	
			}
			
			line = br.readLine();
			br.close();
			
			// verify that the file we just read corresponds to a correct nb node
			if(line != null){
				return(null);
			}
		
		}catch(Exception e){
		  System.out.println(e.toString());
		  return(null);
		}
		
		return(membership_);
	}

	
	
	/**
	 * It extracts the number of lines in a given membership file in input.
	 * This information represents the number of nodes of the corresponding graph.
	 * 
	 * @param inputFilePath The membership file path (i.e. partition information) of a given graph
	 * 
	 */
	public static int getNbLinesInFile(String membershipFilepath) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(membershipFilepath));
		int lines = 0;
		while (reader.readLine() != null) lines++;
		reader.close();
		return(lines);
	}
	
	
}
