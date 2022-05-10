import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import os

# Data Processing ##
####################
file = "output.csv"
fr = open(file, "r")

file = "processed.csv"
fw = open(file, "w")

line = fr.readline()
fw.write("nodes,height,delay\n")
while line:
    line = fr.readline()
    if line:
        things = line.split(",")
        print(things)
        nodes = int(things[0]) + int(things[1])
        height = int(things[3])
        delay = float(things[4])/1000
        fw.write(str(nodes)+","+str(height)+","+ str(delay)+"\n")

fw.close()
fr.close()

file = os.getcwd()+'/processed.csv'
print(file)
df = pd.read_csv(file)

# who v/s fare barplot
s = sns.barplot(x='nodes',
            y='delay',
            hue='height',
            data=df,
            palette="icefire")
plt.xticks(rotation=45)

plt.ylabel("Average Delay (μs)")
plt.xlabel("Number of nodes (User and Objects)")
plt.tight_layout()

# Show the plot
#plt.show()
plt.savefig("policy.pdf")





