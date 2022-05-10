#!/bin/sh

for h in 1 2 3 4 5 
do
	for n in 1000 3000 5000 7000 9000 11000 13000 15000 17000 19000 20000
	do 
		python randomPolicyGraph.py $n $h
	done
done
