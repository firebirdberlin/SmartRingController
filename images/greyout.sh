#! /bin/bash

function run(){
	echo "\$ $1"
	eval $1
}




EXT=${1#*.} # schneidet Anfang 'dateiname.' weg
BASE=${1%.$EXT} # schneidet Endung '.dat' weg
echo $BASE " " $EXT


out=${BASE}_grayed.${EXT}

run "convert -density 300 -brightness-contrast +70x-70 -type Grayscale $1 ${out}"

echo -e "\n\
\includegraphics<-1>[]{talk/img/$1} \n\
\includegraphics<2->[]{talk/img/$out} \n"
