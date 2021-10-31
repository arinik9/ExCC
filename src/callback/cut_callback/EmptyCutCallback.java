package callback.cut_callback;

import formulation.AbstractFormulation;
import ilog.concert.IloException;

public class EmptyCutCallback extends AbstractCutCallback{

	public EmptyCutCallback(AbstractFormulation formulation) {
		super(formulation);
	}

	@Override
	public void separates() throws IloException {
	}

}
