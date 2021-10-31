
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
        #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=false -DinitSolutionFilePath="$initSolutionFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=120 -DtilimForEnumAll=-1 -DsolLim=100 -DtriangleIneqReducedForm=false run
        
        #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=false -DinitSolutionFilePath="$initSolutionFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=120 -DsolLim=100 -DtriangleIneqReducedForm=false run
        
        # without redundant triangle inequalities (more efficient)
        #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=false -DinitSolutionFilePath="$initSolutionFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=120 -DtilimForEnumAll=-1 -DsolLim=100 -DtriangleIneqReducedForm=true run
        
        ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="vertex" -DenumAll=true -Dcp=false -DinitSolutionFilePath="$initSolutionFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=120 -DsolLim=100 -DtriangleIneqReducedForm=true run
        
        # ===============================================================================
        # Edge formulation F_e(G)
        # ===============================================================================
        
        outFolderName="out/""$modifiedName""-edge"
	    mkdir -p $outFolderName
         
        #ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="edge" -DenumAll=true -Dcp=false -DlazyCB=true  -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=120 -DtilimForEnumAll=-1 -DsolLim=100 run
        
        ant -DinFile="in/""$name" -DoutDir="$outFolderName" -DformulationType="edge" -DenumAll=true -Dcp=false -DlazyCB=true  -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=120 -DsolLim=100 run
                
    fi
done
