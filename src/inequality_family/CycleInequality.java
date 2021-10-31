package inequality_family;

import java.util.ArrayList;

import formulation.AbstractFormulation;
import formulation.Edge;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import variable.VariableGetter;


@SuppressWarnings("serial")
public class CycleInequality extends AbstractInequality<AbstractFormulation> {
	// conflicted cycle is a cycle with only one negative edge
	
	public int n;
	public ArrayList<Integer> onePath; // a path consisting of vertices, where their edge variables have the value of 1 in the current integer solution
	public Edge zeroEdge; // a single edge variable , where it has the value of 0 in the current integer solution
	
	public CycleInequality(AbstractFormulation formulation, int n, Edge zeroEdge, ArrayList<Integer> onePath) {
		super(formulation, AbstractFormulation.class);
		
		this.n = n;
		this.onePath = new ArrayList<Integer>(onePath);
		this.zeroEdge = new Edge(n, zeroEdge.getSource(), zeroEdge.getDest());
	}

	
	@Override
	public Range createRange() {

		IloLinearNumExpr expr = formulation.getCplex().linearNumExpr();
		
			try {
					
				for(int v=1; v<onePath.size(); v++)
					expr.addTerm(+1.0, formulation.edgeVar(onePath.get(v-1), onePath.get(v)));
				
				expr.addTerm(-1.0, formulation.edgeVar(zeroEdge.getSource(), zeroEdge.getDest()));
				
	
			} catch (IloException e) {
				e.printStackTrace();
			}

		return new Range(expr, onePath.size() - 2.0);
	}

	
	
	@Override
	public AbstractInequality<AbstractFormulation> clone() {

		return new CycleInequality(formulation, this.n, this.zeroEdge, this.onePath);
	}

	
	
	@Override
	protected double evaluate(VariableGetter vg) throws IloException  {
		double result = 0.0;
		for(int v=1; v<onePath.size(); v++)
			 result += vg.getValue(formulation.edgeVar(onePath.get(v-1), onePath.get(v)));
		result -= vg.getValue(formulation.edgeVar(zeroEdge.getSource(), zeroEdge.getDest()));
		
		return result;
	}

	
	
	@Override
	public double getSlack(VariableGetter vg) throws IloException   {
		//double res = (onePath.size()-2.0) - this.evaluate(vg);
		//System.out.println("lhs: "+this.evaluate(vg)+", slack: " + res);

		return (onePath.size()-2.0) - this.evaluate(vg);
	}

	
	
	@Override
	public String toString(){
		return("("+zeroEdge.getSource()+","+zeroEdge.getDest()+") => "+onePath.toString());
	}

}
