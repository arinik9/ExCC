package formulation;


/**
 * ID of an edge i,j (with i < j)
 *
 */
public class Edge{
	int n; // number of nodes
	int i;
	int j;
	int hashcode;
	double weight;

	public Edge(int n, int i, int j){
		this.n = n;
		this.i=i;
		this.j=j;
		if(j<i){
			this.i=j;
			this.j=i;
		}
		hashcode = this.n * this.i + this.j;
	}
	
	public double getWeight(){
		return(this.weight);
	}
	
	public void setWeight(double w){
		this.weight = w;
	}

	@Override
	public int hashCode(){	
		return hashcode;
	}

	@Override
	public boolean equals(Object o){

		if (this==o)
			return true;
		if (o instanceof Edge) {
			Edge e = (Edge)o;

			return this.i == e.i 
					&& this.j == e.j;
		}
		return false;
	}
	
	public int getSource() {
		return this.i;
	}
	
	public int getDest() {
		return this.j;
	}
	
	public int getOtherVertexId(int vertexId){
		if(vertexId == i)
			return(j);
		return(i);
	}
	
	@Override
	public String toString(){
		return("(source:"+i+", target:"+j+")="+weight);
	}
}