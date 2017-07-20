#!/bin/bash
# script to check for data integrity of walkthrough

# change directory (storing the current one)
current_dir=${PWD}
cd $(dirname $0)/../docs/walkthrough/data

status=0
echo "[`date`] Scanning walkthrough files"

untracked=0
corrupted=0
missing=0

# first iterate over the files in the 
for n in `ls * | grep -v checksum`; do
	grep_result=`grep " ${n}$" checksum`
	if [ "$grep_result" == "" ]; then
		echo "+ Untracked data: $n"
		untracked=$((untracked+1))
	fi
done

# 
while read line; do
	file=`echo $line | awk '{print $2}'`
	if [ -f $file ]; then
		previous=`echo $line | awk '{print $1}'`
		current=`md5sum $file | awk '{print $1}'`
		if [ "$previous" != "$current" ]; then
			corrupted=$((corrupted+1))
			echo "* Corrupted data: $line (now $current)"
		fi
	else
		missing=$((missing+1))
		echo "- Missing data: $file"
	fi
done < checksum

echo "[`date`] Data integrity: $untracked untracked, $missing missing, $corrupted corrupted files"


cd $current_dir
exit $((untracked + missing + corrupted))
