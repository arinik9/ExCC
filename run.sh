
inDir="in"

for inFile in `ls $inDir | sort -V` ; do
	name=${inFile##*/}
	modifiedName=${name//.G/}

    outDir="out/""$modifiedName" # out dir is the same name as the graph file
    if [ -d $outDir ]; then
      rm -r $outDir
    fi
    mkdir $outDir
    ant -DinFile="$inDir""/""$name" -DoutDir="$outDir" run
done

