FLAGS=-Xlint:unchecked
Dict:Dict.class D.class
	java D
Dict.class:Dict.java 
	javac -g $(FLAGS) Dict.java
D.class:D.java 
	javac -g $(FLAGS) D.java

