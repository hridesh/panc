EXAMPLES="Barbershop Barbershop2 HelloWorld Histogram Philosophers Pi SequentialConsistency SignatureExample"
for EXAMPLE in $EXAMPLES 
 do
  echo "Testing $EXAMPLE."
  ../bin/panc $EXAMPLE.java
  ../bin/panini $EXAMPLE
  rm *.class
 done

echo "Testing AddressBook"
cd AddressBook
../../bin/panc -cp .:htmlparser.jar:../../lib/panini_rt.jar *.java
../../bin/panini -cp .:htmlparser.jar:../../lib/panini_rt.jar AddressBook 
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
../../bin/panc Console.java
../../bin/panc Greeter.java
../../bin/panc HelloWorld.java
../../bin/panini HelloWorld 
rm *.class 
cd -

