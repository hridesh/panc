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
 * Contributor(s): Sean L. Mooney
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/*
 * @test
 * @summary Test compile the AddressBook example.
 */
public class CompileAddressBook extends  CompileKnownExample {
    public static void main(String[] args) throws Exception {
        new CompileAddressBook().run();
    }

    void run() throws Exception {
        run(  new File[]{  new File(examples, "AddressBook.java")
                         , new File(examples, "Address.java")
                         , new File(examples, "AddressRequest.java")
                         , new File(examples, "Book.java")
                         , new File(examples, "CSVBook.java")
                         , new File(examples, "DexBook.java")
                         , new File(examples, "ISUBook.java")
                         , new File(examples, "UI.java")
                         , new File(examples, "XMLBook.java")
                         , new File(examples, "YellowBook.java")
        
                        }
            , new File[]{new File(examples, "htmlparser.jar")} );
    }

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!NO COMMIT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    @Override
    File getDestDir() {
        return new File("/tmp");
    }
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!NO COMMIT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    
    
    @Override
    File getSrcDir() {
        // Assume the 'test dir' is test/tools/panc
        return new File(super.getSrcDir(), "AddressBook");
    }

}

