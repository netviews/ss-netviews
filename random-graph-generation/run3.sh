#!/bin/sh

for h in 9 10 
do
	for n in 1000 2000 3000 4000 5000 6000 7000 8000 9000 10000
	do 
		python randomPolicyGraph.py $n $h
	done
done
