
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
	    #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=true -DMaxTimeForRelaxationImprovement=60 -DlazyCB=false -DuserCutCB=false -DinitSolutionFilePath="$initSolutionFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 -DnbThread=4 -Dverbose=true -Dtilim=300 -DtriangleIneqReducedForm=false run
	    
        # without redundant triangle inequalities (normally more efficient) & without a threshold value for gap value
	    #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=true -DMaxTimeForRelaxationImprovement=60 -DlazyCB=false -DuserCutCB=false -DinitSolutionFilePath="$initSolutionFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 -DnbThread=4 -Dverbose=true -Dtilim=300 -DtriangleIneqReducedForm=true run
	    
        # without redundant triangle inequalities (normally faster) & with a threshold value for gap value & use of 'tilim'
	    #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=true -DMaxTimeForRelaxationImprovement=60 -DlazyCB=false -DuserCutCB=false -DinitSolutionFilePath="$initSolutionFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=0.01 -DnbThread=4 -Dverbose=true -Dtilim=120 -DtriangleIneqReducedForm=true run  
        
        # without redundant triangle inequalities (normally faster) & with a threshold value for gap value & use of 'DtilimForEnumAll'
	    ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=true -DMaxTimeForRelaxationImprovement=60 -DlazyCB=false -DuserCutCB=false -DinitSolutionFilePath="$initSolutionFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=0.01 -DnbThread=4 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=300 -DtriangleIneqReducedForm=true run  
	    
        # ===============================================================================
        
        # ===============================================================================
        # Edge formulation F_e(G)
        # ===============================================================================
        
	    outFolderName="out/""$modifiedName""-edge"
	    mkdir -p $outFolderName
        
        ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="edge" -DenumAll=true -Dcp=true -DMaxTimeForRelaxationImprovement=20 -DuserCutInBB=false -DlazyCB=true -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 -DnbThread=4 -Dverbose=true -Dtilim=120 -DtilimForEnumAll=-1 -DsolLim=100 run
        
        #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="edge" -DenumAll=true -Dcp=true -DMaxTimeForRelaxationImprovement=60 -DuserCutInBB=false -DlazyCB=true -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 -DnbThread=4 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=120 -DsolLim=100 run
        
        
    fi
done
