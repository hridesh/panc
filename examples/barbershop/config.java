system Barbershop {
    Barber b;
    WaitingRoom w;
    Customers cs[5];

    b(w, true);
    w(0, 10);       
	for(Customers c : cs){
		c(b, w);
	}
}
