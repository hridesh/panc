/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 * 
 * For more details and the latest version of this code please see
 * http://paninij.org
 * 
 * Contributor(s): Hridesh Rajan
 */

public class Address {
    private String firstname;
    private String lastname;
    private String street;
    private String city;
    private State state;
    private int zipcode;

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreet() {
        return street;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setState(String state) {
        this.state = State.valueOf(state);
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void setZipcode(int zipcode) {
        this.zipcode = zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = Integer.parseInt(zipcode);
    }

    public int getZipcode() {
        return zipcode;
    }

    enum State {
        AL, AK, AS, AZ, AR, CA, CO, CT, DE, DC, FM, FL,
        GA, GU, HI, ID, IL, IN, IA, KS, KY, LA, ME, MH,
        MD, MA, MI, MN, MS, MO, MT, NE, NV, NH, NJ, NM,
        NY, NC, ND, MP, OH, OK, OR, PW, PA, PR, RI, SC,
        SD, TN, TX, UT, VT, VI, VA, WA, WV, WI, WY
    }

    public String toString() {
        return this.firstname + " " + this.lastname + "\n" +
                this.street + "\n" +
                this.city + " " + this.state.toString() + " " + this.zipcode + "\n";
    }
    
    public static class Comparator implements java.util.Comparator<Address>{
    	public int compare(Address o1, Address o2) {
    		String last1, last2;

    		last1 = o1.getLastname();
    		last2 = o2.getLastname();

    		if (last1.equals(last2)) {
    			return o1.getFirstname().compareTo(o1.getFirstname());
    		} else {
    			return last1.compareTo(last2);
    		}
    	}
    }
}
