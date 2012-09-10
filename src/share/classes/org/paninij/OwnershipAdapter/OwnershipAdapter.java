package org.paninij.OwnershipAdapter;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class OwnershipAdapter extends ClassLoader{
	static boolean inJar = false;
	protected boolean loadJar(final String name) throws IOException{
		Hashtable<String, Long> sizes = new Hashtable<String, Long>(); 
		try{
			ZipFile file = new ZipFile(name);
			Enumeration<? extends ZipEntry> enm = file.entries();
			while(enm.hasMoreElements()){
				ZipEntry entry = (ZipEntry)enm.nextElement();
				sizes.put(entry.getName(), entry.getSize());
			}
			file.close();
		}catch (FileNotFoundException e){
			System.err.println(name+ " not found.");
			return false;
		}
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(name.substring(0, name.length()-4)+"_transformed.jar"));
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(name)));
		ZipEntry entry = null;
		while((entry = zis.getNextEntry())!=null){
			if(entry.isDirectory()){
				zos.putNextEntry(entry);
				zos.closeEntry();
			}
			else if(!entry.getName().endsWith(".class")){
				long size = entry.getSize();
				if(size == -1){
					size = sizes.get(entry.getName()).longValue();
				}
				byte[] b = new byte[(int)size];
				int rb=0;
	             int chunk=0;
	             while (((int)size - rb) > 0) {
	                 chunk = zis.read(b, rb, (int)size - rb);
	                 if (chunk == -1) {
	                    break;
	                 }
	                 rb += chunk;
	             }
	             
	             
	            zip(zos, entry, b);
			}
			else{
				long size = entry.getSize();
				
				if(size == -1){
					size = sizes.get(entry.getName()).longValue();
				}
				byte[] b = new byte[(int)size];
				int rb=0;
	             int chunk=0;
	             while (((int)size - rb) > 0) {
	                 chunk = zis.read(b, rb, (int)size - rb);
	                 if (chunk == -1) {
	                    break;
	                 }
	                 rb += chunk;
	             }
	             
	             //optimize///
				byte[] outputClass;
				ClassReader cr = new ClassReader(b);
				ClassWriter cw = new ClassWriter(cr, 0);
	            ClassVisitor cv = new ClassOwnershipAdapter(cw, inJar, name);
	            cr.accept(cv, 0);
	            outputClass = cw.toByteArray();
	            
	            zip(zos, entry, outputClass);
			}
		}
		zos.close();
		return true;
	}
	
	public void zip (ZipOutputStream zos, ZipEntry entry, byte[] b){
		CRC32 crc = new CRC32();
        crc.reset();
        crc.update(b);
        ZipEntry newEntry = new ZipEntry(entry.getName());
        newEntry.setMethod(ZipEntry.STORED);
        newEntry.setCompressedSize(b.length);
        newEntry.setSize(b.length);
        newEntry.setCrc(crc.getValue());
        
		try {
			zos.putNextEntry(newEntry);
			zos.write(b);
			zos.closeEntry();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	protected synchronized Class<?> loadClass(
	        final String name,
	        final boolean resolve) throws ClassNotFoundException
	    {
	        InputStream is = this.getClass().getResourceAsStream(name);
	        byte[] b = null;
	        
	        try {
	            ClassReader cr = new ClassReader(is);
	            ClassWriter cw = new ClassWriter(cr, 0);
	            ClassVisitor cv = new ClassOwnershipAdapter(cw, inJar, name);
	            cr.accept(cv, 0);
	            b = cw.toByteArray();
	        } catch (Exception e) {
	        	System.err.println(name + " not found.");
	        }
	        
	        if(b!=null){
		        try {
		            FileOutputStream fos = new FileOutputStream(name.substring(0, name.length()-6)+"_transformed.class");
//		            FileOutputStream fos = new FileOutputStream(name);
		            fos.write(b);
		            fos.close();
		        } catch (IOException e) {
		        }
	        }
	        return null;
	    }

	public static void main(final String args[]) throws Exception {
		OwnershipAdapter loader = new OwnershipAdapter();
		for(int i=0;i<args.length;i++){
			if(args[i].endsWith(".jar")){
				inJar = true;
				loader.loadJar(args[i]);
				inJar = false;
			}
			else if(args[i].endsWith(".class")){
				loader.loadClass(args[i]);
			}
			else{
				System.err.println(args[i] + "is not a class or jar file.");
			}
        }
    }
}

class ClassOwnershipAdapter extends ClassVisitor {
	boolean inJar;
	String jar;
    public ClassOwnershipAdapter(final ClassVisitor cv, boolean inJar, String jar) {
        super(Opcodes.ASM4, cv);
        this.inJar = inJar;
        this.jar = jar;
    }

    @Override
    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces)
    {
    	System.out.println("Transforming "+name+".class.");
    	String sign;
		int index =0;
    	if(signature == null){
    		sign = "<OWNER:Ljava/lang/Object;>";
			sign = sign + "L"+ superName + "<TOWNER;>;";
    		for(int i=0;i<interfaces.length;i++){
    			sign = sign + "L"+interfaces[i]+"<TOWNER;>;";
    		}
    	}
    	else{
    		//class
    		if(signature.startsWith("<")){
    			if(signature.startsWith("<OWNER:Ljava/lang/Object;")){
    				if(inJar)
    					System.out.println(jar+" is already adapted.");
    				else
    					System.out.println(name+".class is already adapted.");
    				System.out.println("Program terminating.");
    				System.exit(55555);
    			}
    			sign = "<OWNER:Ljava/lang/Object;" + signature.substring(1);
    			index = sign.indexOf(">")-24;
    		}
    		else{
    			sign = "<OWNER:Ljava/lang/Object;>" + signature;
    		}
    		sign = sign.substring(0, sign.indexOf('>')+1);
    		
    		//extension
    		if(signature.charAt(index+2+superName.length()) == '<')
    			sign = sign + "L" + superName + "<TOWNER;"+ signature.substring(index+3+superName.length(), signature.indexOf(">", index+1)+2);
    		else
    			sign = sign + "L" + superName + "<TOWNER;>;";
    		
    		//interfaces
    		for(int i=0;i<interfaces.length;i++){
    			index = signature.indexOf("L"+interfaces[i], index);
    			index += interfaces[i].length()+1;
    			if(signature.charAt(index)=='<'){
    				sign = sign + "L" + interfaces[i]+"<TOWNER;"+signature.substring(index+1, signature.indexOf(">", index))+">;";
    			}
    			else
    				sign = sign + "L"+interfaces[i]+"<TOWNER;>;";
    		}
    	}
    	cv.visit(version, access, name, sign, superName, interfaces);
    }
    
    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String desc,
            String signature,
            final Object value)
	{
		String sign=signature;
		if(signature!=null){
				sign = (signature.substring(0, signature.indexOf('<')+1)+"TOWNER;"+signature.substring(signature.indexOf('<')+1));
		}
		else{
			if(desc.startsWith("L")){
				sign = desc.substring(0, desc.length()-1)+"<TOWNER;>;";
			}
		}
		return cv.visitField(access, name, desc, sign, value);
    }
    
    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions)
        {
    		String rpType = "";
    		String rpSignature = signature;
    		String argument = desc.substring(1, desc.indexOf(')'));
    		String resType = desc.substring(desc.indexOf(')')+1,desc.length());
    		String rpArg="";
    		String rpRes=resType;
    		if(signature==null){
				rpArg = ArgumentTransform(argument);
    			if(resType.startsWith("L"))
    				rpRes = resType.substring(0, resType.length()-1) + "<TOWNER;>;";
    		}
    		else{
    			int index =0;
    			resType = signature.substring(signature.indexOf(")")+1);
    			if(signature.startsWith("<")){
    				rpType = signature.substring(0, signature.indexOf(">")+1);
    			}
    			//Argument Parsing
    			rpArg = "";
    			index += rpType.length();
    			String signatureArgument = signature.substring(signature.indexOf('(')+1, signature.indexOf(')'));
    			if(!signatureArgument.equals("")){
	    			rpArg = ArgumentTransform(signatureArgument);
    			}
    			
    			//Return Type Parsing
    			rpRes = resType;
    			if(resType.startsWith("L")){
    				String signatureRestype;
    				signatureRestype = signature.substring(signature.indexOf(")")+1);
    				index = 0;
    				index += resType.length()-1;
    				if(signatureRestype.charAt(index)=='<'){
    					rpRes = resType.substring(0, resType.length()-1)+"<TOWNER;"+ signatureRestype.substring(index+1);
    				}
    				else
    					rpRes = resType.substring(0, resType.length()-1)+"<TOWNER;>;";
    			}
    		}
    		rpSignature = rpType + "("+rpArg+")"+rpRes;
            return cv.visitMethod(access, name, desc, rpSignature, exceptions);
        }
    
    String ArgumentTransform(String arg){
    	String returnArg="";
    	for(int i=0; i<arg.length(); i++){
    		returnArg = returnArg + arg.charAt(i);
    		if(arg.charAt(i)=='L'){
    			boolean marked = false;
    			while(!marked){
    				i++;
    				if(arg.charAt(i)=='<'){
        				returnArg = returnArg + arg.charAt(i);
    					String sub = arg.substring(i+1, arg.indexOf(">", i)+1);
    					returnArg = returnArg + "TOWNER;" + sub;
    					i += sub.length();
    					marked = true;
    				}
    				else if (arg.charAt(i)==';'){
    					returnArg = returnArg + "<TOWNER;>;";
    					marked = true;
    				}
    				else
    					returnArg = returnArg + arg.charAt(i);
    			}
    		}
    	}
    	return returnArg;
    }
}

