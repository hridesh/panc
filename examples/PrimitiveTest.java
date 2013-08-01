
capsule PrimitiveGiver(boolean bool, byte by, char ch, double d, float f, int i, long l, short s, String str){
	boolean getBoolean(){
		return bool;
	}
	
	byte getByte(){
		return by;
	}

	char getChar(){
		return ch;
	}
	
	double getDouble(){
		return d;
	}
	
	float getFloat(){
		return f;
	}

	int getInteger(){
		return i;
	}

	long getLong(){
		return l;
	}
	
	short getShort(){
		return s;
	}

	String getString(){
		return str;
	}
}

capsule PrimitiveTest(){
	
	design {
		PrimitiveGiver p;
		p(true, 127, 'C', 123.4d, 432.1f, 2147483647, 9223372036854775807L, 32767, "Success!");
	}

	=>{
		System.out.println("Starting Primitive type procedure test...");
	}

	void run(){
		boolean bool = p.getBoolean();
		byte by = p.getByte();
		char ch = p.getChar();
		double d = p.getDouble();
		float f = p.getFloat();
		int i = p.getInteger();
		long l = p.getLong();
		short s = p.getShort();
		String str = p.getString();

		System.out.println("boolean: "+ bool);
		System.out.println("byte: "+ by);
		System.out.println("char: "+ ch);
		System.out.println("double: "+ d);
		System.out.println("float: "+ f);
		System.out.println("int: "+ i);
		System.out.println("long: "+ l);
		System.out.println("short: "+ s);
		System.out.println("String: "+ str);

		System.out.println("Ending primitive type procedure test...");
	}

}