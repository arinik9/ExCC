package inequality_family;

import formulation.AbstractFormulation;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import variable.VariableGetter;

@SuppressWarnings("serial")
public class Triangle_Inequality extends AbstractInequality<AbstractFormulation> {

	public int s1;
	public int t1, t2;
	public boolean isReducedForm = false;
//	public boolean isReducedForm = true; // TODO: if needed, make this an input parameter

	public Triangle_Inequality(AbstractFormulation formulation, int s1, int t1, int t2, boolean isReducedForm) {
		super(formulation, AbstractFormulation.class);
		
		this.s1 = s1;
		this.t1 = t1;
		this.t2 = t2;
		this.isReducedForm = isReducedForm;
	}
	
//	public boolean isInReducedForm(){
//		if(formulation.edgeWeight(this.t1, this.t2)>=0) // Jarkony et al., Lange et al.
//			return(false);
//		if(formulation.edgeWeight(this.s1, this.t1)<=0) // Jarkony et al., Lange et al. & Miyauchi et al.
//			return(false);
//		if(formulation.edgeWeight(this.s1, this.t2)<=0) // Jarkony et al., Lange et al. & Miyauchi et al.
//			return(false);
//		
//		return(true);
//	}
	
//	public boolean isInReducedForm(){
//		if(formulation.edgeWeight(this.t1, this.t2)>=0) // Jarkony et al., Lange et al.
//			return(false);
//		if(formulation.edgeWeight(this.s1, this.t1)<0) // Jarkony et al., Lange et al.
//			return(false);
//		if(formulation.edgeWeight(this.s1, this.t2)<0) // Jarkony et al., Lange et al.
//			return(false);
//		
//		return(true);
//	}
	
	public boolean isInReducedForm() throws IloException{
		if(formulation.edgeWeight(this.s1, this.t1)>0 || formulation.edgeWeight(this.s1, this.t2)>0) // Miyauchi et al.
			return(true);
		return(false);
	}
	
	
	@Override
	public Range createRange() {

		IloLinearNumExpr expr = formulation.getCplex().linearNumExpr();
		
			try {
					
					expr.addTerm(+1.0, formulation.edgeVar(s1, t1));
					expr.addTerm(+1.0, formulation.edgeVar(s1, t2));
					expr.addTerm(-1.0, formulation.edgeVar(t1, t2));
					
	
			} catch (IloException e) {
				e.printStackTrace();
			}
		
		
		return new Range(expr, 1.0);
	}

	@Override
	public AbstractInequality<AbstractFormulation> clone() {

		return new Triangle_Inequality(formulation, s1, t1, t2, isReducedForm);
	}

	@Override
	protected double evaluate(VariableGetter vg) throws IloException  {
		double result = vg.getValue(formulation.edgeVar(s1, t1));
		result += vg.getValue(formulation.edgeVar(s1, t2));
		result -= vg.getValue(formulation.edgeVar(t1, t2));

		return result;
	}

	@Override
	public double getSlack(VariableGetter vg) throws IloException   {
		return 1.0 - this.evaluate(vg);
	}

}
