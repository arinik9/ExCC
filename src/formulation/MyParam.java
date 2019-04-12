package formulation;

import cplex.Cplex;

public class MyParam extends PartitionParam{
	
	public boolean useLower = true;
	public boolean useUpper = true;

	public MyParam(String inputFile, Cplex cplex){
		super(inputFile, cplex);
	}
	
	public MyParam(MyParam pCopy){
		super(pCopy);
	}
	
	
}
