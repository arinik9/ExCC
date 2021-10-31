package callback.lazy_callback;

import java.util.ArrayList;

import formulation.AbstractFormulation;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import inequality_family.AbstractInequality;
import inequality_family.Range;
import separation.SeparationTriangle;
import variable.CallbackVariableGetter;

public class LazyCBTriangle extends AbstractLazyCallback{

	SeparationTriangle sep;
	
	public LazyCBTriangle(AbstractFormulation p, int MAX_CUT, boolean triangleIneqReducedForm) {
		super(p);
//		IFormulationEdgeVarNodeVar formulation, VariableGetter vg, 
		sep = new SeparationTriangle(p, this.variableGetter(), MAX_CUT, triangleIneqReducedForm);		
	}


	@Override
	public void separates() throws IloException {
		
		// ======================================================
		// without this block of code, the separation procedure with multi-threading does not work.
		// This is because the object 'rvg' is initialized at the beginning,
		//	and during the branching it needs to be updated to access the values of the variables
		rvg = new CallbackVariableGetter(formulation.getCplex(), this);
		sep.setVariableGetter(this.variableGetter());
		// ======================================================

		ArrayList<AbstractInequality<? extends AbstractFormulation>> al = sep.separate();
		
		for(AbstractInequality<? extends AbstractFormulation> i : al){
			Range r = i.getRange();
			this.add(formulation.getCplex().range(r.lbound, r.expr, r.ubound),
					IloCplex.CutManagement.UseCutPurge);
		}
		
		System.out.println(al.size() + " lazy triangle");
	}

}
