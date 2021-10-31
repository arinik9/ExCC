package formulation;

import cplex.Cplex;

public class PartitionParam extends Param{
	
	public boolean useNN_1 = false;
	
	/** Maximal number of nodes read into the input file */
	public int maxNumberOfNodes = -1;
	
	public boolean triangleIneqReducedForm = false;

	
	public PartitionParam(String inputFile, Cplex cplex){
		super(inputFile, cplex);
	}

	public PartitionParam(PartitionParam p){
		super(p);
		//System.out.println("p.triangleIneqReducedForm: " + p.triangleIneqReducedForm);

		useNN_1 = p.useNN_1;
		maxNumberOfNodes = p.maxNumberOfNodes;
		this.triangleIneqReducedForm = p.triangleIneqReducedForm; // TODO do we need it here?
	}
}
