import java.util.ArrayList;
import java.util.Scanner;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.ParserException;

/***
 * This AddressBook handler searches for the name in the 
 * white pages provided by the Yellow Pages.
 * 
 * @author Hridesh Rajan
 */
capsule YellowBook() implements Book {
	Address search(AddressRequest r) {
		String url = "http://www.yellowpages.com/findaperson?fap_terms[first]="+r.getFirstname()+"&fap_terms[last]="+r.getLastname()+"&fap_terms[city]=&fap_terms[state]=&fap_terms[searchtype]=phone";	
		Parser par;
		Address newAddress = new Address();
		newAddress.setFirstname(r.getFirstname());
		newAddress.setLastname(r.getLastname());
		try {
			par = new Parser(url);
			org.htmlparser.util.NodeList list;
			list = par.parse(new HasAttributeFilter("data-street"));
			Node[] nodes = list.toNodeArray();
			Scanner scan = new Scanner(nodes[0].getText());
			scan.useDelimiter("\"");
			if(!(scan.next().equals("address data-street="))){
				scan.next();
				scan.next();
			}
			String address = scan.next();
			scan.reset();
			scan=new Scanner(address);
			ArrayList<String> arrylist = new ArrayList<String>();
			while(scan.hasNext()){
				arrylist.add(scan.next());
			}
			newAddress.setZipcode(arrylist.get(arrylist.size()-1));
			newAddress.setState(arrylist.get(arrylist.size()-2));
			newAddress.setCity(arrylist.get(arrylist.size()-3));
			String street = "";
			for(int x = 0; x<=arrylist.size()-4;x++){
				street += " " + arrylist.get(x);
			}
			newAddress.setStreet(street.trim());
			return newAddress;

		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}	
}
