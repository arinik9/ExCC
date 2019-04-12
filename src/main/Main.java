package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cplex.Cplex;
import formulation.MyPartition;
import formulation.Partition;
import formulation.MyParam;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;


/**
* The ExCC program aims at solving Correlation Clustering problem.
* <p>
* 
* Some references:
* <ul>
* <li> Cartwright D, Harary F (1956) Structural balance:
* 	 a generalization of Heider’s theory. Psychol Rev 63:277-293 <\li>
* <li> Heider F (1946) Attitudes and cognitive organization. J Psychol 21:107-112 <\li>
* <\lu>
*/
public class Main {


	
	static String tempFile = "temp.txt";
	
	/**
	 * 
	 * It proposes 2 approaches to solve the problem: 
	 * 		Pure CPLEX approach and Cutting Planes approach.
	 * 
	 * Input parameters:
	 * <ul>
	 * <li> inFile (String): Input file path. </li>
	 * <li> outDir (String): Output directory path. Default "." 
	 * 		(i.e. the current directory). </li>
	 * </ul>
	 * 
	 * Example:
	 * <pre>
	 * {@code
	 * ant clean jar
	 * ant ant -DinFile=data/signed.G -DoutDir=out/signed  run
	 * }
	 * </pre>
	 * 
	 * 
	 * @param args  (Not used in this program. Instead, user parameters are obtained
	 * 	 through ant properties. See the buil.xml for more details).
	 * 
	 * @throws FileNotFoundException.
	 * @throws UnknownObjectException. 
	 * @throws IloException.
	 */
	public static void main(String[] args) throws FileNotFoundException, UnknownObjectException, IloException {

		String inputFilePath = "";
		String outputDirPath = ".";
		
		
		
		if( !System.getProperty("inFile").equals("${inFile}") )
			inputFilePath = System.getProperty("inFile");
		else {
			System.out.println("input file is not specified. Exit");
			return;
		}
		
		if( !System.getProperty("outDir").equals("${outDir}") )
			outputDirPath = System.getProperty("outDir");
		
		
		System.out.println("===============================================");
		System.out.println("inputFilePath: " + inputFilePath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("===============================================");

		
		createTempFileFromInput(inputFilePath);
		
		MyParam myp = null;
		Cplex cplex = new Cplex(); // start
		cplex.setParam(IntParam.ClockType, 2);

		
							
			// =================================================================

			myp = new MyParam(tempFile, cplex);
//			MyPartition p = new MyPartition(myp);
			MyPartition p = (MyPartition)Partition.createPartition(myp);
			p.setLogPath(outputDirPath + "/logcplex.txt");

			p.solve();
			p.retreiveClusters(); // 
			p.writeClusters(outputDirPath + "/ExCC-result.txt");	
			
//			p.computeObjectiveValueFromSolution();


			// end =============================================================

		
		
		cplex.end(); // end

	}

	
	
	/**
	 * This method reads input graph file, then stocks it as weighted adjacency matrix, 
	 * finally writes the graph in lower triangle format into a temp file.
	 * 
	 * @param filename  input graph filename
	 * @return 
	 */
	private static void createTempFileFromInput(String fileName) {
		
		  double[][] weightedAdjMatrix = null;
		  
		// =====================================================================
		// read input graph file
		// =====================================================================
		try{
		  InputStream  ips=new FileInputStream(fileName);
		  InputStreamReader ipsr=new InputStreamReader(ips);
		  BufferedReader   br=new
		  BufferedReader(ipsr);
		  String ligne;
		  
		  ligne = br.readLine();
		  
		  /* Get the number of nodes from the first line */
		  int n = Integer.parseInt(ligne.split("\t")[0]);
		  

		  weightedAdjMatrix = new double[n][n];
		  if(weightedAdjMatrix[0][0] != 0.0d)
			  System.out.println("Main: Error default value of doubles");
		  
		  /* For all the other lines */
		  while ((ligne=br.readLine())!=null){
			  
			  String[] split = ligne.split("\t");
			  
			  if(split.length >= 3){
				  int i = Integer.parseInt(split[0]);
				  int j = Integer.parseInt(split[1]);
				  double v = Double.parseDouble(split[2]);
				  weightedAdjMatrix[i][j] = v;
				  weightedAdjMatrix[j][i] = v;
			  }
			  else
				  System.err.println("All the lines of the input file must contain three values" 
						+ " separated by tabulations"
						+ "(except the first one which contains two values).\n"
				  		+ "Current line: " + ligne);
		  }
		  br.close();
		}catch(Exception e){
		  System.out.println(e.toString());
		}
		// end =================================================================


		// =====================================================================
		// write into temp file (in lower triangle format)
		// =====================================================================
		if(weightedAdjMatrix != null){
			 try{
			     FileWriter fw = new FileWriter(tempFile, false);
			     BufferedWriter output = new BufferedWriter(fw);

			     for(int i = 1 ; i < weightedAdjMatrix.length ; ++i){
			    	 String s = "";
			    	 
			    	 for(int j = 0 ; j < i ; ++j) // for each line, iterate over columns
			    		 s += weightedAdjMatrix[i][j] + " ";

			    	 s += "\n";
			    	 output.write(s);
			    	 output.flush();
			     }
			     
			     output.close();
			 }
			 catch(IOException ioe){
			     System.out.print("Erreur in reading input file: ");
			     ioe.printStackTrace();
			 }

		}
		// end =================================================================

	}

}
