package callback.lazy_callback;

import java.util.ArrayList;

import formulation.AbstractFormulation;
import formulation.FormulationEdge;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import inequality_family.AbstractInequality;
import inequality_family.Range;
import separation.SeparationCycleBFS;
import variable.CallbackVariableGetter;

public class LazyCBCycle extends AbstractLazyCallback{

	SeparationCycleBFS sep;
	ArrayList<AbstractInequality<? extends AbstractFormulation>> addedCuts;
	
	public LazyCBCycle(AbstractFormulation p, int MAX_CUT) {
		super(p);
//		IFormulationEdgeVarNodeVar formulation, VariableGetter vg, 
		sep = new SeparationCycleBFS(p, this.variableGetter(), MAX_CUT);
		addedCuts = new ArrayList<>();
	}


	@Override
	public void separates() throws IloException {
		//System.out.println("LazyCBCycle");
		
		// ======================================================
		// without this block of code, the separation procedure with multi-threading does not work.
		// This is because the object 'rvg' is initialized at the beginning,
		//	and during the branching it needs to be updated to access the values of the variables
		rvg = new CallbackVariableGetter(formulation.getCplex(), this);
		sep.setVariableGetter(this.variableGetter());
		// ======================================================

		addedCuts.addAll(sep.separate());
		
		for(AbstractInequality<? extends AbstractFormulation> i : addedCuts){
			Range r = i.getRange();
			this.add(formulation.getCplex().range(r.lbound, r.expr, r.ubound),
					IloCplex.CutManagement.UseCutPurge);
			//formulation.getCplex().addRange(i.createRange());
		}
		
		System.out.println(addedCuts.size() + " lazy cycle");
	}
	
	
	
	public ArrayList<AbstractInequality<? extends AbstractFormulation>> getAddedCuts() {
		return(addedCuts);
	}

}
