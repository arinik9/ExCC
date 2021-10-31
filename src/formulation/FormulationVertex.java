package formulation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import callback.lazy_callback.LazyCBTriangle;
import formulation.MyParam.Transitivity;
import ilog.concert.IloException;
import inequality_family.Triangle_Inequality;

public class FormulationVertex extends AbstractFormulation {
	
	public FormulationVertex(MyParam rp) {
		super(rp);
	}
	
	
	@Override
	public int readGraphFromInputFile(String fileName) {
		int n=-1;
		
		// =====================================================================
		// read input graph file
		// =====================================================================
		try {
			InputStream ips = new FileInputStream(fileName);
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;

			ligne = br.readLine();

			/* Get the number of nodes from the first line */
			n = Integer.parseInt(ligne.split("\t")[0]);
			System.out.println("n: " + n);
			
			double[][] adj = new double[n][n];

			
			/* For all the other lines */
			while ((ligne = br.readLine()) != null) {

				String[] split = ligne.split("\t");

				if (split.length >= 3) {
					int i = Integer.parseInt(split[0]);
					int j = Integer.parseInt(split[1]);
					double w = Double.parseDouble(split[2]);
					
					adj[i][j] = w;
					adj[j][i] = w;

				} else
					System.err.println(
							"All the lines of the input file must contain three values" + " separated by tabulations"
									+ "(except the first one which contains two values).\n" + "Current line: " + ligne);
			}
			br.close();
			
			
			for(int i=1; i<n; i++){
				for(int j=0; j<i; j++){
					Edge e = new Edge(n, i, j);
					e.setWeight(adj[i][j]);
					edges.add(e);
					int pos = e.hashcode;
					d.put(pos, adj[i][j]);
				}
			}
			
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		// end =================================================================
		
		
		return(n);
	}
	

	public void createConstraints(MyParam rp) throws IloException{

		/*
		 * Add triangular constraints : xi,j + xi,k - xj,k <= 1
		 * 
		 * - if i is with j and k, then j and k are together
		 */
		if(rp.transitivity == Transitivity.USE
				|| (rp.transitivity == Transitivity.USE_IN_BC_ONLY && p.isInt == true)){
			createTriangleConstraints();
//			System.out.println("\n!!Add triangle constraints to the model");
			
			// TODO: DENEME
//			int cost = 61;
//			createCostConstraint(cost);
		}
		else if(rp.transitivity == Transitivity.USE_LAZY
				|| (rp.transitivity == Transitivity.USE_LAZY_IN_BC_ONLY && p.isInt == true)){
			System.out.println("\n!!Add lazy CB in BC");
			getCplex().use(new LazyCBTriangle(this, 500,  p.triangleIneqReducedForm));
		}
		else {
			System.out.println("\n!!Don't add triangle or lazy callback for triangles");
		}

	}


	
	
	/**
	 * Add triangular constraints : xi,j + xi,k - xj,k <= 1 - if i is with j and
	 * k, then j and k are together
	 * @param solution 
	 * 
	 * @throws IloException
	 */
	void createTriangleConstraints() throws IloException {

        int count = 0;
		for (int i = 0; i < n - 2; ++i)
			for (int j = i + 1; j < n - 1; ++j)
				for (int k = j + 1; k < n; ++k) {
					Triangle_Inequality ineq1 = new Triangle_Inequality(this, i, j, k, p.triangleIneqReducedForm);

					if(!ineq1.isReducedForm || (ineq1.isReducedForm && ineq1.isInReducedForm())){
						getCplex().addRange(ineq1.createRange());
                        count++;
					}
					
					Triangle_Inequality ineq2 = new Triangle_Inequality(this, j, i, k, p.triangleIneqReducedForm);
					if(!ineq2.isReducedForm || (ineq2.isReducedForm && ineq2.isInReducedForm())){
						getCplex().addRange(ineq2.createRange());
                        count++;
					}
					
					Triangle_Inequality ineq3 = new Triangle_Inequality(this, k, i, j, p.triangleIneqReducedForm);
					if(!ineq3.isReducedForm || (ineq3.isReducedForm && ineq3.isInReducedForm())){
						getCplex().addRange(ineq3.createRange());
                        count++;
					}
					
//					IloLinearNumExpr expr3 = getCplex().linearNumExpr();
//					expr3.addTerm(1.0, v_edge[k][i]);
//					expr3.addTerm(1.0, v_edge[k][j]);
//					
//					expr3.addTerm(-1.0, v_edge[j][i]);
//					getCplex().addLe(expr3, 1.0);
				}

			System.out.println("nb triangles added: " + count);
	}


}
