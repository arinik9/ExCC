
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

        #LPFilePath="in/strengthedModelAfterRootRelaxation.lp"
        initMembershipFilePath="in/bestSolution_""$modifiedName"".txt"
        
	    outFolderName="out/""$modifiedName""-vertex"
	    mkdir -p $outFolderName
        LPFilePath="in/strengthedModel_vertex.lp"
        
        # with redundant triangle inequalities & without a threshold value for gap value
        #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=2 -Dverbose=true -Dtilim=120 -DtilimForEnumAll=-1 -DsolLim=100 run 
                
        # without redundant triangle inequalities (normally more efficient)
        ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=120 -DsolLim=100 run
        
        
	  	    
	  	              
        # ===============================================================================
        # Edge formulation F_e(G)
        # ===============================================================================
        
        # ===============================================================================
        
	    outFolderName="out/""$modifiedName""-edge"
	    mkdir -p $outFolderName
        LPFilePath="in/strengthedModel_edge.lp"
        
        ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="edge" -DenumAll=true -Dcp=false -DlazyCB=true -DuserCutCB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=120 -DtilimForEnumAll=-1 run
        #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="edge" -DenumAll=true -Dcp=false -DlazyCB=true -DuserCutCB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=60 run
        
        
        
    fi
done
