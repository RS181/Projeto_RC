JAVA = javac
RM = /bin/rm -f

all: 
	${JAVA} src/*.java

clean:
	${RM} *.class
	${RM} src/*.class