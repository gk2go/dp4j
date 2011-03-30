#!/bin/bash

v=$1
testdrive=target/TESTDRIVE
template=tools/TESTDRIVE

printf "#!/bin/sh\n" > $testdrive
printf "v=$v\n" >> $testdrive

while read line; do
	echo $line >> $testdrive
done < $template
