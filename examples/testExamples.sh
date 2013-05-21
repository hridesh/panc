PANC=../make/panini/bin/panc
PANINI=../make/panini/bin/panini


echo "Testing Pi"
$PANC Pi.java
$PANINI Pi 8
rm *.class

#EXAMPLES="Barbershop Barbershop2 HelloWorld Histogram Philosophers Pipeline SequentialConsistency SignatureExample"
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
../$PANC Console.java
../$PANC Greeter.java
../$PANC HelloWorld.java
../$PANINI HelloWorld
rm *.class 
cd -

