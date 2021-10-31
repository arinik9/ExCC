package cutting_plane;

import java.util.ArrayList;

import formulation.AbstractFormulation;
import inequality_family.AbstractInequality;
import separation.AbstractSeparation;

/**
 * It handles a separation method used during the Cutting Plane approach.
 * Basically, it allows determining when the given separation algorithm is used
 *  (in Root Relaxation (RR) only or  both RR and Branch&Bound)
 * It counts the number of added or removed cuts (of that separation method)
 *  during the Cutting Plane approach.
 *   
 */
public class CP_Separation<Formulation extends AbstractFormulation>{

	public AbstractSeparation<Formulation> se;
	
	/* as this class concerns only 1 separation  method, 'addedIneq' contains
	 *  all violated inequalities generated by that method */
	public ArrayList<AbstractInequality<? extends AbstractFormulation>> addedIneq = new ArrayList<>();
	
	/* if FALSE, a given separation algorithm will not used if another algo
	 *  has already generated a cut in the same iteration */
	public boolean isQuick;
	
	/* the number of untight inequalities already removed */
	public int removedIneq = 0;

	/**
	 * True if the inequalities separated by this family must be added in the branch and cut
	 *  if the cutting plane does not return an integer solution
	 */
	public boolean toAddInBB;
	
	public boolean usedAtThisIteration = false;

	public CP_Separation(AbstractSeparation<Formulation> se, boolean toAdd, boolean isQuick){
		this.se = se;
		
		/* if FALSE, a given separation algorithm will not used to generate cuts
		 *  during Branch&Bound */
		toAddInBB = toAdd; 
		
		this.isQuick = isQuick;
	}

	public void remove(int i) {
		addedIneq.remove(i);
		removedIneq++;
	}
}