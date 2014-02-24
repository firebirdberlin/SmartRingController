#! /bin/bash

sub=com/firebirdberlin/smartringcontroller/
tgt=src_lite/${sub}

mkdir -p ${tgt}
for i in src/${sub}/*.java; do
	base=`basename $i`;
	sed 's/com\.firebirdberlin\.smartringcontrollerpro/com.firebirdberlin.smartringcontrollerlite/g' ${i} > ${tgt}/${base};
done;

