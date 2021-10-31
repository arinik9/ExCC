
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

        #LPFilePath="in/strengthedModelAfterRootRelaxation.lp"
        initMembershipFilePath="in/bestSolution_""$modifiedName"".txt"
        
        # ===============================================================================
        # Vertex pair formulation F_v(G)
        # ===============================================================================    
        
	    outFolderName="out/""$modifiedName""-vertex"
	    mkdir -p $outFolderName
        LPFilePath="in/strengthedModel_vertex.lp"

        # with redundant triangle inequalities
	    #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=false -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=60 run	    
	    
        # with redundant triangle inequalities	    
  	    ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=false -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=60 run
	  	    
	  	              
        # ===============================================================================
        # Edge formulation F_e(G)
        # ===============================================================================
        
        # ===============================================================================
        
	    outFolderName="out/""$modifiedName""-edge"
	    mkdir -p $outFolderName
        LPFilePath="in/strengthedModel_edge.lp"
                
        ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="edge" -DenumAll=false -Dcp=false -DlazyCB=true -DuserCutCB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=60 run
	  	 
    fi
done
