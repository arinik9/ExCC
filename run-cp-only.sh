
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then


	    echo "in/""$modifiedName"

        LPFilePath=""
        initMembershipFilePath="in/bestSolutionILS_""$modifiedName"".txt"
        
        # ===============================================================================
        # Vertex pair formulation F_v(G)
        # ===============================================================================
        
	    outFolderName="out/""$modifiedName""-vertex"
	    mkdir -p $outFolderName

        # with redundant triangle inequalities & without a threshold value for gap value
	    #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=false -Dcp=true -DMaxTimeForRelaxationImprovement=60 -DuserCutInBB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=true -DfractionalSolutionGapPropValue=0.01 -DnbThread=4 -Dverbose=true -Dtilim=300 -DtriangleIneqReducedForm=false run
        

        # without redundant triangle inequalities (normally more efficient) & without a threshold value for gap value
	    ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=false -Dcp=true -DMaxTimeForRelaxationImprovement=60 -DuserCutInBB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=true -DfractionalSolutionGapPropValue=-1.0 -DnbThread=4 -Dverbose=true -Dtilim=300 -DtriangleIneqReducedForm=true run
	    
        # without redundant triangle inequalities (normally more efficient) & with a threshold value for gap value
	    ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=false -Dcp=true -DMaxTimeForRelaxationImprovement=60 -DuserCutInBB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=true -DfractionalSolutionGapPropValue=0.01 -DnbThread=4 -Dverbose=true -Dtilim=300 -DtriangleIneqReducedForm=true run
	    
    fi
done
