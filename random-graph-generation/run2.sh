#!/bin/sh

for h in 1 2 3 4 5  
do
	for n in 11000 12000 13000 14000 15000 16000 17000 18000 19000 20000
	do 
		python randomPolicyGraph.py $n $h
	done
done
