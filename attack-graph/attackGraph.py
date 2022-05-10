

count = 0
targetPath = []

class NODE:
	def __init__(self, h ,t):
		self.h = h
		self.t = t

	def __hash__(self):
		return hash((self.h, self.t))

	def __eq__(self, other):
		return self.t == other.t


def neighbourPrintAllPathsUtil(u, d, visited, path, attackGraph):
	# Mark the current node as visited and store in path
	global targetPath
	visited = visited or {}
	visited[u] = True
	path.append(u)

	if u == d:
		#print(path)
		for p in path:
			#print(p.h , end=" ")
			targetPath.append(p)
		# print("--------", targetPath)
	else:
		# If current vertex is not destination
		# Recur for all the vertices adjacent to this vertex
		if u in attackGraph:
			for i in attackGraph[u]:
				if i not in visited:
					neighbourPrintAllPathsUtil(i, d, visited, path, attackGraph)

	# Remove current vertex from path[] and mark it as unvisited
	path.pop()
	visited[u] = False



def neighbourNotInPath(s, d, target, attackGraph):
	global targetPath;
	visited = {}
	path = []
	targetPath = []
	neighbourPrintAllPathsUtil(s, d, visited, path, attackGraph)
	# print("destination-" + d.h , end=" ")
	# print("target-" + target, end=" ")
	for node in targetPath:
		# print(node.h , end=" ")
		x = int(node.h)
		t = int(target)
		if x == t:
			# print("$$$$$")
			# print()
			return False
		elif targetPath.__len__() >= 6:
			# print("####")
			# print()
			return False

	return True

def printAllPathsUtil(u, d, visited, path, pathCount, attackGraph):
	global count
	# Mark the current node as visited and store in path
	visited = visited or {}
	visited[u] = True
	path.append(u)
	# If current vertex is same as destination, then print
	# current path[]
	if u.h == d:
		count += 1
		print(("path "+str(count)), end =" ")
		for node in path:
			print(node.h, end =" ")
		len = path.__len__() - 2
		pathCount[len] = pathCount[len] + 1
		print(pathCount)

	else:
		# If current vertex is not destination
		# Recur for all the vertices adjacent to this vertex
		if u in attackGraph:
			for i in attackGraph[u]:
				if i not in visited:
					printAllPathsUtil(i, d, visited, path, pathCount, attackGraph)

	# Remove current vertex from path[] and mark it as unvisited
	path.pop()
	visited[u] = False


def generateAttackGraph(head, graph):
	global count
	fileName = "outputAttacker" + str(head) + ".txt"
	out = open(fileName, "w")

	attackGraph = {}
	queue = []

	tag = 0
	headNode = NODE(head, tag)
	attackGraph[headNode] = []

	tag = tag + 1
	queue.append(headNode)

	while queue:
		curr = queue.pop(0)
		#print("current "+ str(curr.h), end=" ")
		# print()
		attackGraph[curr] = []

		for neighbour in graph[int(curr.h)-1]:
			if headNode == curr and int(neighbour)!=0:
				neighbourNode = NODE(neighbour, tag)
				tag += 1
				attackGraph[curr].append(neighbourNode)
				queue.append(neighbourNode)

			elif headNode != curr and int(neighbour)!=0:
				if neighbourNotInPath(headNode, curr, neighbour, attackGraph) is True:
					neighbourNode = NODE(neighbour, tag)
					tag += 1
					attackGraph[curr].append(neighbourNode)
					queue.append(neighbourNode)

	for key in attackGraph:
		out.write(str(key.h) + ":" + str(key.t)+ " # ")
		for node in attackGraph[key]:
			out.write(str(node.h)+":"+ str(key.t) + " ")
		out.write("\n")
		out.write("----------------------------------")


	server = ["4", "12", "13"]
	for s in server:
		print("\nFor server "+ str(s))
		visited = {}
		path = []
		pathCount = [0] * 5
		count = 0
		printAllPathsUtil(headNode, s, visited, path, pathCount, attackGraph)
		print("SERVER "+ s +": "+ str(count))

graph = []

with open("firewall.txt", "r") as reader:
	for line in reader:
		values = line.split(" ")
		new = []
		for val in values:
			new.append(val.replace("\n",""))
		graph.append(new)

for host in range(1, 16):
	print("#################")
	print("HOST "+ str(host))
	generateAttackGraph(host, graph)

print("************************************")

# graph = []
# with open("firewall.txt", "r") as reader:
# #with open("test.txt", "r") as reader:
# 	for line in reader:
# 		values = line.split(" ")
# 		new = []
# 		for val in values:
# 			new.append(val.replace("\n",""))
# 		graph.append(new)
#
# for host in range(1, 16):
# 	print("#################")
# 	print("HOST "+ str(host))
# 	generateAttackGraph(host, graph)




