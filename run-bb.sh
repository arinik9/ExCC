
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

	    echo "in/""$modifiedName"
	    
        LPFilePath=""
        initSolutionFilePath="in/bestSolutionILS_""$modifiedName"".txt"
        
        # ===============================================================================
        # Vertex pair formulation F_v(G)
        # ===============================================================================
        
	    outFolderName="out/""$modifiedName""-vertex"
	    mkdir -p $outFolderName

        # with redundant triangle inequalities
        ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=false -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=120 -DtriangleIneqReducedForm=true run
        
        # without redundant triangle inequalities (normally more efficient)
        #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=false -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=120 -DtriangleIneqReducedForm=true run
        
        
        # ===============================================================================
        # Edge formulation F_e(G)
        # ===============================================================================
        
        outFolderName="out/""$modifiedName""-edge"
	    mkdir -p $outFolderName
         
        ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="edge" -DenumAll=false -Dcp=false -DlazyCB=true  -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=120 run

    fi
done
