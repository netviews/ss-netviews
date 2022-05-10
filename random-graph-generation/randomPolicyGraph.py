#########################################
# Author: Iffat Anjum
# Target: Random Policy Graph Generation
#########################################
import json
import sys
import math
import random

class Node:
    def __init__(self, name, type):
        self.name = name
        self.type = type
        self.left = None
        self.right = None
        self.fanout = 0
        self.op = []

class operationRelation:
    def __init__(self, ua, op):
        self.ua = ua
        self.op = op

def createNodes(list, count, type, file):
    for i in range(0, count):
        name = type + str(i+1)
        node = Node(name, type)
        list.append(node)
        if type!="OP":
            file.write(",\n{\n\"name\": \"" + name+ "\",\n \"type\": \"" + type+ "\",\n\"properties\": {}\n}")

def printNodes(userNodeList):
    for n in userNodeList:
        print(n.name)

def createBinaryTree(alist, llist, root, h, pc, file):
    if h == 0:
        root.left = pc
        file.write(",\n{\n\"source\": \"" + root.name + "\",\n \"target\": \"" + pc.name + "\" \n}")
        llist.append(root)
        return
    else:
        if root.left == None:
            root.left = alist.pop()
            file.write(",\n{\n\"source\": \"" + root.name + "\",\n \"target\": \"" + root.left.name + "\" \n}")
            createBinaryTree(alist, llist, root.left, h - 1, pc, file)
        if root.right == None:
            root.right = alist.pop()
            file.write(",\n{\n\"source\": \"" + root.name + "\",\n \"target\": \"" + root.right.name + "\" \n}")
            createBinaryTree(alist, llist, root.right, h - 1, pc, file)


def createPartialDAG(list, alist, llist, pc, height, file, flag):
    for u in list:
        root = alist.pop()
        u.left = root
        if flag == 0:
            file.write("\n{\n\"source\": \"" + u.name + "\",\n \"target\": \"" + root.name + "\" \n}")
            flag = 1
        else:
            file.write(",\n{\n\"source\": \"" + u.name + "\",\n \"target\": \"" + root.name + "\" \n}")
        createBinaryTree(alist, llist, root, height, pc, file)


def createAssociationRelations(ualList, oalList, opTypeList, file):	
    flag = 0
    count = 0
    for ua in ualList:
        for oa in oalList:
		   p = random.randint(1,101)
		   if p <= 40:
		       permission = random.choice(opTypeList)
		       ua.fanout = ua.fanout + 1
		       #opR = operationRelation(oa, permission)
		       #ua.op.append(opR)
		       count = count + 1
		       if flag == 0:
		           file.write("\n{\n\"source\": \"" + ua.name + "\",\n \"target\": \"" + oa.name + "\" ,\n \"operations\": [\"" + permission + "\"] \n}")
		           flag = 1
		       else:
		           file.write(",\n{\n\"source\": \"" + ua.name + "\",\n \"target\": \"" + oa.name + "\" ,\n \"operations\": [\"" + permission + "\"] \n}")
		   elif ua.fanout >=10:
		       break
    return count


def createGraphAlgorithm01(n, h, run):
        node = int(n)
        height = int(h)
        user = int(node * 0.6)
        object = int(node * 0.4)

        userNodeList = []
        objectNodeList = []
        userAttributeList = []
        objectAttributeList = []
        #operationNodeList = []
        uaLeafNodeList = []
        oaLeafNodeList = []

        oaCount = (math.pow(2, (height + 1)) - 1) * object
        uaCount = (math.pow(2, (height + 1)) - 1) * user
        #opCount = object * user


        total = height + user + int(uaCount) + object + int(oaCount) + 1
        fileName = "policy" + str(node) + "N" + str(height) + "H.json"
        run.write(str(node) + " " + str(height) + " 5 "+ fileName + "\n")
	   
        file = open(fileName, "w")
        file.write("{\n")

        opTypeList = []    
        for op in range(0,5):
            name = "permission"+ str(op)
            opTypeList.append(name)

        ## Creating and Writing Node List ##
        file.write("\"nodes\": [\n")
        pcNode = Node("department", "PC")
        file.write("{\n\"name\": \"department\",\n \"type\": \"PC\",\n\"properties\": {}\n}")

        createNodes(userNodeList, user, "U", file)
        createNodes(objectNodeList, object, "O", file)
        #createNodes(operationNodeList, int(opCount), "OP", file)
        createNodes(objectAttributeList, int(oaCount), "OA", file)
        createNodes(userAttributeList, int(uaCount), "UA",file)

        file.write("\n],\n")

        ## Creating and Writing User and Object DAG
        ## Assignment Relations
        file.write("\"assignments\": [")

        createPartialDAG(userNodeList, userAttributeList, uaLeafNodeList, pcNode, height, file, flag=0)
        createPartialDAG(objectNodeList, objectAttributeList, oaLeafNodeList, pcNode, height, file, flag=1)

        file.write("\n],\n")

        ## Creating and Writing Operation Connection
        ## Association Relations
        file.write("\"associations\": [")

        opCount = createAssociationRelations(uaLeafNodeList, oaLeafNodeList, opTypeList, file)

        file.write("\n]\n")

        file.write("\n}\n")
        file.close()

        print("Tree Height: "+ str(height))
        print("Total Number of Users: "+ str(user))
        print("Total Number of User Attributes: "+ str(int(uaCount)))
        print("Total Number of Objects: "+ str(object))
        print("Total Number of Object Attributes: "+ str(int(oaCount)))
        print("Total Number of Association relation: "+ str(opCount))
        print("Total Number of Nodes: "+ str(total))


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    height = 0
    fileName = "runList.txt"
    file = open(fileName, "a+")
    print(sys.argv[0] + " "+ sys.argv[1] +" "+sys.argv[2])
    createGraphAlgorithm01(sys.argv[1], sys.argv[2], file)
    file.close()




