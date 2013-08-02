PANC=../make/panini/bin/panc
PANINI=../make/panini/bin/panini


echo "Testing Pi"
$PANC Pi.java
$PANINI Pi 8
rm *.class

echo "Testing Barbershop"
$PANC Barbershop2.java
$PANINI Barbershop
rm *.class

echo "Testing Fibonacci"
$PANC Fibonacci.java
$PANINI Fibonaci 8
rm *.class

EXAMPLES="HelloWorld Histogram Philosophers Pipeline PrimitiveTest SequentialConsistency SignatureExample"
for EXAMPLE in $EXAMPLES 
 do
  echo "Testing $EXAMPLE."
  $PANC $EXAMPLE.java
  $PANINI $EXAMPLE
  rm *.class
 done

echo "Testing AddressBook"
cd AddressBook
../$PANC -cp .:htmlparser.jar *.java
../$PANINI -cp .:htmlparser.jar AddressBook
rm *.class 
cd -

#echo "Testing GA"
#cd GA
#../../bin/panc AILib/*.java GA.java
#../../bin/panini GA
#rm *.class 
#cd - 

echo "Testing separately compiled version of the HelloWorld example."
cd HelloWorldSeparate
../$PANC -cp . Console.java
../$PANC -cp . Greeter.java
../$PANC -cp . HelloWorld.java
../$PANINI HelloWorld
rm *.class 
cd -

