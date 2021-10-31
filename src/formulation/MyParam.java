package formulation;

import cplex.Cplex;

public class MyParam extends PartitionParam{
	public boolean useLower = true;
	public boolean useUpper = true;
	public Transitivity transitivity = Transitivity.USE;
	public int nbThread = 1;
	public boolean userCutInBB = false;
	public boolean lazyInBB = false;
	public String LPFilePath = "";
	public boolean triangleIneqReducedForm = false;
	
	/**
	 * Specify how the triangle inequalities must be used
	 * USE : the triangle inequalities are always put into the model
	 * USE_LAZY : the triangle inequalities are not into the model and are generated
	 * 		 lazily in a callback during the cutting plane step and the branch and cut.
	 * USE_LAZY_IN_BC_ONLY : Same as USE_LAZY but the triangle inequalities are
	 * 		 generated like any other inequalities in the cutting plane step 
	 * 		(otherwise all the triangle inequalities must be satisfied by 
	 * 		the relaxation until the other separation algorithms are called)
	 * USE_IN_BC_ONLY : the triangle inequalities are generated like other families
	 * 		 in the cutting plane step; they are added to the model during the branch and cut step 
	 * @author zach
	 *
	 */
	public enum Transitivity{
		USE, USE_LAZY, USE_LAZY_IN_BC_ONLY, USE_IN_BC_ONLY
	}
	
	public MyParam(String inputFile, Cplex cplex){
		super(inputFile, cplex);
	}
	
	public MyParam(MyParam pCopy){
		super(pCopy);
		this.transitivity = pCopy.transitivity;
		this.userCutInBB = pCopy.userCutInBB;
		this.lazyInBB = pCopy.lazyInBB;
		this.nbThread = pCopy.nbThread;
		this.LPFilePath = pCopy.LPFilePath;
		this.triangleIneqReducedForm = pCopy.triangleIneqReducedForm;

	}
	

	public MyParam(String inputFile, Cplex cplex, Transitivity transitivity, boolean userCutInBB, boolean lazyInBB, int nbThread, 
			String LPFilePath, boolean triangleIneqReducedForm){
		this(inputFile, cplex);

		this.transitivity = transitivity;
		this.userCutInBB = userCutInBB;
		this.lazyInBB = lazyInBB;
		this.nbThread = nbThread;
		this.LPFilePath = LPFilePath;
		this.triangleIneqReducedForm = triangleIneqReducedForm;
	}
	
}
