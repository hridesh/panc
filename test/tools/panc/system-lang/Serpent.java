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
 * Contributor(s):
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;


/**
 * StreamIt Serpent benchmark
 * Description:
 * @author gupadhyaya
 *
 */

class BooleanC {
    boolean v;
    public BooleanC(boolean v) { this.v = v; }
    public boolean value() { return v; }
}

interface Perm {
    static int IP = 0;
    static int FP = 1;
}

interface sBoxT {
    static int inner = 0;
    static int outer = 1;
}

class Params {
    final int vector = 2;
    static int BITS_PER_WORD = 32;
    static int NBITS = 128;
    static int PHI = -1640531527;
    static int MAXROUNDS = 8;
    static boolean PRINTINFO = false;
    static int PLAINTEXT = 0;
    static int USERKEY = 1;
    static int CIPHERTEXT = 2;
    static int[][] USERKEYS = {
        {0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0},
        {-1829788726,-1804109491,838131579,-836508150,1614336722,1896051696,1339230894,-827807165},
        {-738420253,755581455,10502647,-483847052,1999748685,1314610597,415411168,-1591500888},
        {-1122733020,1623633375,-954274029,685956534,-1168406632,-1150893116,-746541904,1439352169}
    };
    static int USERKEY_LENGTH = (8 * BITS_PER_WORD);;
    static int[] IP = {0,32,64,96,1,33,65,97,2,34,66,98,3,35,67,99,4,36,68,100,5,37,69,101,6,38,70,102,7,39,71,103,8,40,72,104,9,41,73,105,10,42,74,106,11,43,75,107,12,44,76,108,13,45,77,109,14,46,78,110,15,47,79,111,16,48,80,112,17,49,81,113,18,50,82,114,19,51,83,115,20,52,84,116,21,53,85,117,22,54,86,118,23,55,87,119,24,56,88,120,25,57,89,121,26,58,90,122,27,59,91,123,28,60,92,124,29,61,93,125,30,62,94,126,31,63,95,127};
    static int[] FP = {0,4,8,12,16,20,24,28,32,36,40,44,48,52,56,60,64,68,72,76,80,84,88,92,96,100,104,108,112,116,120,124,1,5,9,13,17,21,25,29,33,37,41,45,49,53,57,61,65,69,73,77,81,85,89,93,97,101,105,109,113,117,121,125,2,6,10,14,18,22,26,30,34,38,42,46,50,54,58,62,66,70,74,78,82,86,90,94,98,102,106,110,114,118,122,126,3,7,11,15,19,23,27,31,35,39,43,47,51,55,59,63,67,71,75,79,83,87,91,95,99,103,107,111,115,119,123,127};
    static int[][] SBOXES = {
        {3,8,15,1,10,6,5,11,14,13,4,2,7,0,9,12},
        {15,12,2,7,9,0,5,10,1,11,14,8,6,13,3,4},
        {8,6,7,9,3,12,10,15,13,1,14,4,0,11,5,2},
        {0,15,11,8,12,9,6,3,13,1,2,4,10,7,5,14},
        {1,15,8,3,12,0,11,6,2,5,4,10,9,14,7,13},
        {15,5,2,11,4,10,9,12,0,3,14,8,13,6,7,1},
        {7,2,12,5,8,4,6,11,14,9,1,15,13,3,10,0},
        {1,13,15,0,14,8,2,11,7,4,12,10,9,3,5,6}
    };
    String inputFile;
    String outputFile;

    public Params(String input, String output) {
        inputFile = input;
        outputFile = output;
    }
}

signature Stage {
    BooleanC consume (int[] input);
}

capsule Serpent_full (int iterations, Stage reader) {
    void run() {
        // Steady phase
        for (int n = 0; n < iterations; n++) {
            BooleanC status = reader.consume(null);
            status.value();
        }
    }
}

capsule FileReader(String inputfile, Stage encoder) implements Stage {
    DataInputStream inputStream;
    =>{
        try{
            File inputFile = new File(inputfile);
            FileInputStream localFileInputStream = new FileInputStream(inputFile);
            inputStream = new DataInputStream(new BufferedInputStream(localFileInputStream));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private int endianFlip(int paramInt)
    {
        int i = paramInt >> 24 & 0xFF;
        int j = paramInt >> 16 & 0xFF;
        int k = paramInt >> 8 & 0xFF;
        int m = paramInt >> 0 & 0xFF;

        return i | j << 8 | k << 16 | m << 24;
    }

    private short endianFlip(short paramShort)
    {
        int i = paramShort >> 8 & 0xFF;
        int j = paramShort >> 0 & 0xFF;

        return (short)(i | j << 8);
    }

    BooleanC consume(int[] input) {
        int n = 128;
        int[] output = new int[n];
        for (int index=0; index < n; index++) {
            try{
                output[index] = endianFlip(inputStream.readInt());
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return (encoder.consume(output));
    }
}

capsule SerpentEncoder(Stage ipPerm) implements Stage {
    BooleanC consume (int[] input) {
        return (ipPerm.consume(input));
    }
}

capsule Permute(int type, int N, Stage rS) implements Stage {
    int[] permutation;
    =>{
        int[] IP = {0,32,64,96,1,33,65,97,2,34,66,98,3,35,67,99,4,36,68,100,5,37,69,101,6,38,70,102,7,39,71,103,8,40,72,104,9,41,73,105,10,42,74,106,11,43,75,107,12,44,76,108,13,45,77,109,14,46,78,110,15,47,79,111,16,48,80,112,17,49,81,113,18,50,82,114,19,51,83,115,20,52,84,116,21,53,85,117,22,54,86,118,23,55,87,119,24,56,88,120,25,57,89,121,26,58,90,122,27,59,91,123,28,60,92,124,29,61,93,125,30,62,94,126,31,63,95,127};
        int[] FP = {0,4,8,12,16,20,24,28,32,36,40,44,48,52,56,60,64,68,72,76,80,84,88,92,96,100,104,108,112,116,120,124,1,5,9,13,17,21,25,29,33,37,41,45,49,53,57,61,65,69,73,77,81,85,89,93,97,101,105,109,113,117,121,125,2,6,10,14,18,22,26,30,34,38,42,46,50,54,58,62,66,70,74,78,82,86,90,94,98,102,106,110,114,118,122,126,3,7,11,15,19,23,27,31,35,39,43,47,51,55,59,63,67,71,75,79,83,87,91,95,99,103,107,111,115,119,123,127};
        permutation = (type == Perm.IP)?IP:FP;
    }
    BooleanC consume (int[] input) {
        //BooleanC[] status = new BooleanC[rounds];
        int[] output = new int[N];
        for (int i = 0; (i < N); i++) {
            output[i] = input[permutation[i]];
        }
        /*for (int i = 0; (i < N); i++) {
            // just pop the input
        }*/
        return rS.consume(output);
    }
}

capsule Perms(int N, Stage rS) implements Stage {
    int[] permutation = {0,32,64,96,1,33,65,97,2,34,66,98,3,35,67,99,4,36,68,100,5,37,69,101,6,38,70,102,7,39,71,103,8,40,72,104,9,41,73,105,10,42,74,106,11,43,75,107,12,44,76,108,13,45,77,109,14,46,78,110,15,47,79,111,16,48,80,112,17,49,81,113,18,50,82,114,19,51,83,115,20,52,84,116,21,53,85,117,22,54,86,118,23,55,87,119,24,56,88,120,25,57,89,121,26,58,90,122,27,59,91,123,28,60,92,124,29,61,93,125,30,62,94,126,31,63,95,127};
    int callers=0;
    int[] out = new int[128];
    int oi=0;

    BooleanC consume (int[] input) {
        for(int i=0; i<input.length; i++) {
            if (oi == 128)  oi=0;
            out[oi++] = input[i];
        }
        callers++;
        if (callers == 4) {
            callers = 0;
            int[] output = new int[N];
            for (int i = 0; (i < N); i++) {
                output[i] = out[permutation[i]];
            }
            /*for (int i = 0; (i < N); i++) {
                // just pop the input
            }*/
            return rS.consume(output);
        }
        return new BooleanC(true);
    }
}

capsule R(Stage anonFilter_a8) implements Stage {
    BooleanC consume (int[] input) {
        return anonFilter_a8.consume(input);
    }
}

capsule AnonFilter_a8(Stage identity, Stage slowKeySch) implements Stage {
    BooleanC consume (int[] input) {
        int[] in1 = (int[])input.clone();
        int[] in2 = (int[])input.clone();
        BooleanC s1 = identity.consume(in1);
        BooleanC s2 = slowKeySch.consume(in2);
        if (s1.value() && s2.value())
            return new BooleanC(true);
        return new BooleanC(false);
    }
}

capsule slowKeySchedule(Stage anonFilter_a4) implements Stage {
    BooleanC consume (int[] input) {
        return anonFilter_a4.consume(input);
    }
}

capsule AnonFilter_a4(int round, Stage intoBits,
        int BITS_PER_WORD, int PHI, int USERKEY_LENGTH, int vector) implements Stage {
    int[] w = new int[140];
    int[][] USERKEYS = {
        {0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0},
        {-1829788726,-1804109491,838131579,-836508150,1614336722,1896051696,1339230894,-827807165},
        {-738420253,755581455,10502647,-483847052,1999748685,1314610597,415411168,-1591500888},
        {-1122733020,1623633375,-954274029,685956534,-1168406632,-1150893116,-746541904,1439352169}
    };
    =>{
        int[] key = { 0, 0, 0, 0, 0, 0, 0, 0 };
        int words = (USERKEY_LENGTH / BITS_PER_WORD);
        for (int i = (words - 1); (i >= 0); i--) {
            key[((words - 1) - i)] = USERKEYS[vector][i];
        }
        if ((USERKEY_LENGTH < 256)) {
            int msb;
            msb = key[(USERKEY_LENGTH / BITS_PER_WORD)];
            key[(USERKEY_LENGTH / BITS_PER_WORD)] = (msb | (1 << (USERKEY_LENGTH % BITS_PER_WORD)));
        }
        for (int i = 0; (i < 8); i++) {
            w[i] = key[i];
        }
        for (int i = 8; (i < 140); i++) {
            w[i] = (((((w[(i - 8)] ^ w[(i - 5)]) ^ w[(i - 3)]) ^ w[(i - 1)]) ^ PHI) ^ (i - 8));
            w[i] = LRotate(w[i], 11);
        }
    }

    private int LRotate(int x, int n) {
        int[] v = new int[32];
        int m = 1;
        for (int i = 0; (i < 32); i++) {
            if ((((x & m) >> i) != 0)) {
                v[i] = 1;
            }
            m = (m << 1);
        }
        int[] w = new int[32];
        for (int i = 0; (i < 32); i++) {
            w[i] = v[(((i + 32) - 11) % 32)];
        }
        int r = 0;
        for (int i = 0; (i < 32); i++) {
            r = (r | (w[i] << i));
        }
        return r;
    }

    BooleanC consume (int[] input) {
        int[] output = new int[4];
        int oIndex=0;
        int i = ((4 * round) + 8);
        output[oIndex++] = w[(i + 0)];
        output[oIndex++] = w[(i + 1)];
        output[oIndex++] = w[(i + 2)];
        output[oIndex++] = w[(i + 3)];
        return intoBits.consume(output);
    }
}

capsule IntoBits(Stage bitSlice) implements Stage {
    BooleanC consume (int[] input) {
        int n = 4;
        int oi=0;
        int[] output = new int[n*32];
        for (int index=0; index < n; index++) {
            int v = input[index];
            int m = 1;
            for (int i = 0; (i < 32); i++) {
                if ((((v & m) >> i) != 0)) {
                        output[oi++] = 1;
                } else {
                        output[oi++] = 0;
                }
                m = (m << 1);
            }
        }
        return bitSlice.consume(output);
    }
}

capsule BitSlice(Stage[] identities) implements Stage {
    int size;
    =>{
        size = (128/identities.length);
    }

    BooleanC consume (int[] input) {
        BooleanC[] status = new BooleanC[identities.length];
        for (int i=0; i<identities.length; i++) {
            int[] in = new int[size];
            for (int j=0; j<size; j++) {
                in[j] = input[i*size+j];
            }
            status[i] = identities[i].consume(in);
        }
        for (int i=0; i<identities.length; i++) {
            if (!status[i].value())
                return new BooleanC(false);
        }
        return new BooleanC(true);
    }
}

capsule Xor(Stage sbox) implements Stage {
    int callers=0;
    int[] out = new int[256];
    int oi = 0;

    BooleanC consume (int[] input) {
        for (int i=0; i<input.length; i++) {
            if (oi == 256)  oi=0;
            out[oi++] = input[i];
        }
        callers++;
        if (callers == 2) {
            oi=0;
            int[] output = new int[128];
            for (int i=0; i<128; i++) {
                int x = out[i];
                int y = out[128+i];
                x = (x ^ y);
                output[oi++] = x;
            }
            return sbox.consume(output);
        }
        return new BooleanC(true);
    }
}

capsule Sbox(int type, int rnd, Stage rawl) implements Stage {
    int round;
    int total;
    int[][] SBOXES = {
            {3,8,15,1,10,6,5,11,14,13,4,2,7,0,9,12},
            {15,12,2,7,9,0,5,10,1,11,14,8,6,13,3,4},
            {8,6,7,9,3,12,10,15,13,1,14,4,0,11,5,2},
            {0,15,11,8,12,9,6,3,13,1,2,4,10,7,5,14},
            {1,15,8,3,12,0,11,6,2,5,4,10,9,14,7,13},
            {15,5,2,11,4,10,9,12,0,3,14,8,13,6,7,1},
            {7,2,12,5,8,4,6,11,14,9,1,15,13,3,10,0},
            {1,13,15,0,14,8,2,11,7,4,12,10,9,3,5,6}
    };
    int callers=0;
    int[] out = new int[128];
    int oi=0;
    =>{
        round = (type == sBoxT.inner)?(((32 + 3) - rnd) % 8):(rnd % 8);
        total = (type == sBoxT.inner)?32:1;
    }

    BooleanC consume (int[] input) {
        //System.out.println("Round = "+rnd+" input len = "+input.length);
        for (int i=0; i<input.length; i++) {
            if (oi == 128)  oi=0;
            //System.out.println(oi);
            out[oi++] = input[i];
        }
        callers++;
        if (callers == total) {
            callers = 0;
            oi = 0;
            int i=0;
            int[] output = new int[128];
            for (int index=0; index < 32; index++) {
                int val = out[oi++];
                val = ((out[oi++] << 1) | val);
                val = ((out[oi++] << 2) | val);
                val = ((out[oi++] << 3) | val);
                int ov = SBOXES[round][val];
                output[i++] = ((int)(((ov & 1) >> 0)));
                output[i++] = ((int)(((ov & 2) >> 1)));
                output[i++] = ((int)(((ov & 4) >> 2)));
                output[i++] = ((int)(((ov & 8) >> 3)));
            }
            //System.out.println("Output");
            return rawl.consume(output);
        }
        return new BooleanC(true);
    }
}

capsule rawL(Stage r) implements Stage {
    BooleanC consume (int[] input) {
        int i=0;
        int[] output = new int[128];
        output[i++] = ((((((input[16] ^ input[52]) ^ input[56]) ^ input[70]) ^ input[83]) ^ input[94]) ^ input[105]);
        output[i++] = ((input[72] ^ input[114]) ^ input[125]);
        output[i++] = ((((((input[2] ^ input[9]) ^ input[15]) ^ input[30]) ^ input[76]) ^ input[84]) ^ input[126]);
        output[i++] = ((input[36] ^ input[90]) ^ input[103]);
        output[i++] = ((((((input[20] ^ input[56]) ^ input[60]) ^ input[74]) ^ input[87]) ^ input[98]) ^ input[109]);
        output[i++] = ((input[1] ^ input[76]) ^ input[118]);
        output[i++] = ((((((input[2] ^ input[6]) ^ input[13]) ^ input[19]) ^ input[34]) ^ input[80]) ^ input[88]);
        output[i++] = ((input[40] ^ input[94]) ^ input[107]);
        output[i++] = ((((((input[24] ^ input[60]) ^ input[64]) ^ input[78]) ^ input[91]) ^ input[102]) ^ input[113]);
        output[i++] = ((input[5] ^ input[80]) ^ input[122]);
        output[i++] = ((((((input[6] ^ input[10]) ^ input[17]) ^ input[23]) ^ input[38]) ^ input[84]) ^ input[92]);
        output[i++] = ((input[44] ^ input[98]) ^ input[111]);
        output[i++] = ((((((input[28] ^ input[64]) ^ input[68]) ^ input[82]) ^ input[95]) ^ input[106]) ^ input[117]);
        output[i++] = ((input[9] ^ input[84]) ^ input[126]);
        output[i++] = ((((((input[10] ^ input[14]) ^ input[21]) ^ input[27]) ^ input[42]) ^ input[88]) ^ input[96]);
        output[i++] = ((input[48] ^ input[102]) ^ input[115]);
        output[i++] = ((((((input[32] ^ input[68]) ^ input[72]) ^ input[86]) ^ input[99]) ^ input[110]) ^ input[121]);
        output[i++] = ((input[2] ^ input[13]) ^ input[88]);
        output[i++] = ((((((input[14] ^ input[18]) ^ input[25]) ^ input[31]) ^ input[46]) ^ input[92]) ^ input[100]);
        output[i++] = ((input[52] ^ input[106]) ^ input[119]);
        output[i++] = ((((((input[36] ^ input[72]) ^ input[76]) ^ input[90]) ^ input[103]) ^ input[114]) ^ input[125]);
        output[i++] = ((input[6] ^ input[17]) ^ input[92]);
        output[i++] = ((((((input[18] ^ input[22]) ^ input[29]) ^ input[35]) ^ input[50]) ^ input[96]) ^ input[104]);
        output[i++] = ((input[56] ^ input[110]) ^ input[123]);
        output[i++] = ((((((input[1] ^ input[40]) ^ input[76]) ^ input[80]) ^ input[94]) ^ input[107]) ^ input[118]);
        output[i++] = ((input[10] ^ input[21]) ^ input[96]);
        output[i++] = ((((((input[22] ^ input[26]) ^ input[33]) ^ input[39]) ^ input[54]) ^ input[100]) ^ input[108]);
        output[i++] = ((input[60] ^ input[114]) ^ input[127]);
        output[i++] = ((((((input[5] ^ input[44]) ^ input[80]) ^ input[84]) ^ input[98]) ^ input[111]) ^ input[122]);
        output[i++] = ((input[14] ^ input[25]) ^ input[100]);
        output[i++] = ((((((input[26] ^ input[30]) ^ input[37]) ^ input[43]) ^ input[58]) ^ input[104]) ^ input[112]);
        output[i++] = (input[3] ^ input[118]);
        output[i++] = ((((((input[9] ^ input[48]) ^ input[84]) ^ input[88]) ^ input[102]) ^ input[115]) ^ input[126]);
        output[i++] = ((input[18] ^ input[29]) ^ input[104]);
        output[i++] = ((((((input[30] ^ input[34]) ^ input[41]) ^ input[47]) ^ input[62]) ^ input[108]) ^ input[116]);
        output[i++] = (input[7] ^ input[122]);
        output[i++] = ((((((input[2] ^ input[13]) ^ input[52]) ^ input[88]) ^ input[92]) ^ input[106]) ^ input[119]);
        output[i++] = ((input[22] ^ input[33]) ^ input[108]);
        output[i++] = ((((((input[34] ^ input[38]) ^ input[45]) ^ input[51]) ^ input[66]) ^ input[112]) ^ input[120]);
        output[i++] = (input[11] ^ input[126]);
        output[i++] = ((((((input[6] ^ input[17]) ^ input[56]) ^ input[92]) ^ input[96]) ^ input[110]) ^ input[123]);
        output[i++] = ((input[26] ^ input[37]) ^ input[112]);
        output[i++] = ((((((input[38] ^ input[42]) ^ input[49]) ^ input[55]) ^ input[70]) ^ input[116]) ^ input[124]);
        output[i++] = ((input[2] ^ input[15]) ^ input[76]);
        output[i++] = ((((((input[10] ^ input[21]) ^ input[60]) ^ input[96]) ^ input[100]) ^ input[114]) ^ input[127]);
        output[i++] = ((input[30] ^ input[41]) ^ input[116]);
        output[i++] = ((((((input[0] ^ input[42]) ^ input[46]) ^ input[53]) ^ input[59]) ^ input[74]) ^ input[120]);
        output[i++] = ((input[6] ^ input[19]) ^ input[80]);
        output[i++] = (((((input[3] ^ input[14]) ^ input[25]) ^ input[100]) ^ input[104]) ^ input[118]);
        output[i++] = ((input[34] ^ input[45]) ^ input[120]);
        output[i++] = ((((((input[4] ^ input[46]) ^ input[50]) ^ input[57]) ^ input[63]) ^ input[78]) ^ input[124]);
        output[i++] = ((input[10] ^ input[23]) ^ input[84]);
        output[i++] = (((((input[7] ^ input[18]) ^ input[29]) ^ input[104]) ^ input[108]) ^ input[122]);
        output[i++] = ((input[38] ^ input[49]) ^ input[124]);
        output[i++] = ((((((input[0] ^ input[8]) ^ input[50]) ^ input[54]) ^ input[61]) ^ input[67]) ^ input[82]);
        output[i++] = ((input[14] ^ input[27]) ^ input[88]);
        output[i++] = (((((input[11] ^ input[22]) ^ input[33]) ^ input[108]) ^ input[112]) ^ input[126]);
        output[i++] = ((input[0] ^ input[42]) ^ input[53]);
        output[i++] = ((((((input[4] ^ input[12]) ^ input[54]) ^ input[58]) ^ input[65]) ^ input[71]) ^ input[86]);
        output[i++] = ((input[18] ^ input[31]) ^ input[92]);
        output[i++] = ((((((input[2] ^ input[15]) ^ input[26]) ^ input[37]) ^ input[76]) ^ input[112]) ^ input[116]);
        output[i++] = ((input[4] ^ input[46]) ^ input[57]);
        output[i++] = ((((((input[8] ^ input[16]) ^ input[58]) ^ input[62]) ^ input[69]) ^ input[75]) ^ input[90]);
        output[i++] = ((input[22] ^ input[35]) ^ input[96]);
        output[i++] = ((((((input[6] ^ input[19]) ^ input[30]) ^ input[41]) ^ input[80]) ^ input[116]) ^ input[120]);
        output[i++] = ((input[8] ^ input[50]) ^ input[61]);
        output[i++] = ((((((input[12] ^ input[20]) ^ input[62]) ^ input[66]) ^ input[73]) ^ input[79]) ^ input[94]);
        output[i++] = ((input[26] ^ input[39]) ^ input[100]);
        output[i++] = ((((((input[10] ^ input[23]) ^ input[34]) ^ input[45]) ^ input[84]) ^ input[120]) ^ input[124]);
        output[i++] = ((input[12] ^ input[54]) ^ input[65]);
        output[i++] = ((((((input[16] ^ input[24]) ^ input[66]) ^ input[70]) ^ input[77]) ^ input[83]) ^ input[98]);
        output[i++] = ((input[30] ^ input[43]) ^ input[104]);
        output[i++] = ((((((input[0] ^ input[14]) ^ input[27]) ^ input[38]) ^ input[49]) ^ input[88]) ^ input[124]);
        output[i++] = ((input[16] ^ input[58]) ^ input[69]);
        output[i++] = ((((((input[20] ^ input[28]) ^ input[70]) ^ input[74]) ^ input[81]) ^ input[87]) ^ input[102]);
        output[i++] = ((input[34] ^ input[47]) ^ input[108]);
        output[i++] = ((((((input[0] ^ input[4]) ^ input[18]) ^ input[31]) ^ input[42]) ^ input[53]) ^ input[92]);
        output[i++] = ((input[20] ^ input[62]) ^ input[73]);
        output[i++] = ((((((input[24] ^ input[32]) ^ input[74]) ^ input[78]) ^ input[85]) ^ input[91]) ^ input[106]);
        output[i++] = ((input[38] ^ input[51]) ^ input[112]);
        output[i++] = ((((((input[4] ^ input[8]) ^ input[22]) ^ input[35]) ^ input[46]) ^ input[57]) ^ input[96]);
        output[i++] = ((input[24] ^ input[66]) ^ input[77]);
        output[i++] = ((((((input[28] ^ input[36]) ^ input[78]) ^ input[82]) ^ input[89]) ^ input[95]) ^ input[110]);
        output[i++] = ((input[42] ^ input[55]) ^ input[116]);
        output[i++] = ((((((input[8] ^ input[12]) ^ input[26]) ^ input[39]) ^ input[50]) ^ input[61]) ^ input[100]);
        output[i++] = ((input[28] ^ input[70]) ^ input[81]);
        output[i++] = ((((((input[32] ^ input[40]) ^ input[82]) ^ input[86]) ^ input[93]) ^ input[99]) ^ input[114]);
        output[i++] = ((input[46] ^ input[59]) ^ input[120]);
        output[i++] = ((((((input[12] ^ input[16]) ^ input[30]) ^ input[43]) ^ input[54]) ^ input[65]) ^ input[104]);
        output[i++] = ((input[32] ^ input[74]) ^ input[85]);
        output[i++] = (((input[36] ^ input[90]) ^ input[103]) ^ input[118]);
        output[i++] = ((input[50] ^ input[63]) ^ input[124]);
        output[i++] = ((((((input[16] ^ input[20]) ^ input[34]) ^ input[47]) ^ input[58]) ^ input[69]) ^ input[108]);
        output[i++] = ((input[36] ^ input[78]) ^ input[89]);
        output[i++] = (((input[40] ^ input[94]) ^ input[107]) ^ input[122]);
        output[i++] = ((input[0] ^ input[54]) ^ input[67]);
        output[i++] = ((((((input[20] ^ input[24]) ^ input[38]) ^ input[51]) ^ input[62]) ^ input[73]) ^ input[112]);
        output[i++] = ((input[40] ^ input[82]) ^ input[93]);
        output[i++] = (((input[44] ^ input[98]) ^ input[111]) ^ input[126]);
        output[i++] = ((input[4] ^ input[58]) ^ input[71]);
        output[i++] = ((((((input[24] ^ input[28]) ^ input[42]) ^ input[55]) ^ input[66]) ^ input[77]) ^ input[116]);
        output[i++] = ((input[44] ^ input[86]) ^ input[97]);
        output[i++] = (((input[2] ^ input[48]) ^ input[102]) ^ input[115]);
        output[i++] = ((input[8] ^ input[62]) ^ input[75]);
        output[i++] = ((((((input[28] ^ input[32]) ^ input[46]) ^ input[59]) ^ input[70]) ^ input[81]) ^ input[120]);
        output[i++] = ((input[48] ^ input[90]) ^ input[101]);
        output[i++] = (((input[6] ^ input[52]) ^ input[106]) ^ input[119]);
        output[i++] = ((input[12] ^ input[66]) ^ input[79]);
        output[i++] = ((((((input[32] ^ input[36]) ^ input[50]) ^ input[63]) ^ input[74]) ^ input[85]) ^ input[124]);
        output[i++] = ((input[52] ^ input[94]) ^ input[105]);
        output[i++] = (((input[10] ^ input[56]) ^ input[110]) ^ input[123]);
        output[i++] = ((input[16] ^ input[70]) ^ input[83]);
        output[i++] = ((((((input[0] ^ input[36]) ^ input[40]) ^ input[54]) ^ input[67]) ^ input[78]) ^ input[89]);
        output[i++] = ((input[56] ^ input[98]) ^ input[109]);
        output[i++] = (((input[14] ^ input[60]) ^ input[114]) ^ input[127]);
        output[i++] = ((input[20] ^ input[74]) ^ input[87]);
        output[i++] = ((((((input[4] ^ input[40]) ^ input[44]) ^ input[58]) ^ input[71]) ^ input[82]) ^ input[93]);
        output[i++] = ((input[60] ^ input[102]) ^ input[113]);
        output[i++] = (((((input[3] ^ input[18]) ^ input[72]) ^ input[114]) ^ input[118]) ^ input[125]);
        output[i++] = ((input[24] ^ input[78]) ^ input[91]);
        output[i++] = ((((((input[8] ^ input[44]) ^ input[48]) ^ input[62]) ^ input[75]) ^ input[86]) ^ input[97]);
        output[i++] = ((input[64] ^ input[106]) ^ input[117]);
        output[i++] = (((((input[1] ^ input[7]) ^ input[22]) ^ input[76]) ^ input[118]) ^ input[122]);
        output[i++] = ((input[28] ^ input[82]) ^ input[95]);
        output[i++] = ((((((input[12] ^ input[48]) ^ input[52]) ^ input[66]) ^ input[79]) ^ input[90]) ^ input[101]);
        output[i++] = ((input[68] ^ input[110]) ^ input[121]);
        output[i++] = (((((input[5] ^ input[11]) ^ input[26]) ^ input[80]) ^ input[122]) ^ input[126]);
        output[i++] = ((input[32] ^ input[86]) ^ input[99]);
        return r.consume(output);
    }
}

capsule AnonFilter_a9(Stage identity, Stage keySchedule) implements Stage {
    BooleanC consume (int[] input) {
        int[] in1 = (int[])input.clone();
        int[] in2 = (int[])input.clone();
        BooleanC s1 = identity.consume(in1);
        BooleanC s2 = keySchedule.consume(in2);
        if (s1.value() && s2.value())
            return new BooleanC(true);
        return new BooleanC(false);
    }
}

capsule Identity(Stage xor) implements Stage {
    BooleanC consume (int[] input) {
        return xor.consume(input);
    }
}

capsule KeySchedule(Stage anonFilter_a0) implements Stage {
    BooleanC consume (int[] input) {
        return (anonFilter_a0.consume(input));
    }
}

capsule AnonFilter_a0(Stage xor,
        int BITS_PER_WORD, int MAXROUNDS, int NBITS,
        int PHI, int USERKEY_LENGTH, int vector) implements Stage {
    int[][] keys;
    int[] IP = {0,32,64,96,1,33,65,97,2,34,66,98,3,35,67,99,4,36,68,100,5,37,69,101,6,38,70,102,7,39,71,103,8,40,72,104,9,41,73,105,10,42,74,106,11,43,75,107,12,44,76,108,13,45,77,109,14,46,78,110,15,47,79,111,16,48,80,112,17,49,81,113,18,50,82,114,19,51,83,115,20,52,84,116,21,53,85,117,22,54,86,118,23,55,87,119,24,56,88,120,25,57,89,121,26,58,90,122,27,59,91,123,28,60,92,124,29,61,93,125,30,62,94,126,31,63,95,127};
    int[][] SBOXES = {
            {3,8,15,1,10,6,5,11,14,13,4,2,7,0,9,12},
            {15,12,2,7,9,0,5,10,1,11,14,8,6,13,3,4},
            {8,6,7,9,3,12,10,15,13,1,14,4,0,11,5,2},
            {0,15,11,8,12,9,6,3,13,1,2,4,10,7,5,14},
            {1,15,8,3,12,0,11,6,2,5,4,10,9,14,7,13},
            {15,5,2,11,4,10,9,12,0,3,14,8,13,6,7,1},
            {7,2,12,5,8,4,6,11,14,9,1,15,13,3,10,0},
            {1,13,15,0,14,8,2,11,7,4,12,10,9,3,5,6}
    };
    int[][] USERKEYS = {
            {0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0},
            {-1829788726,-1804109491,838131579,-836508150,1614336722,1896051696,1339230894,-827807165},
            {-738420253,755581455,10502647,-483847052,1999748685,1314610597,415411168,-1591500888},
            {-1122733020,1623633375,-954274029,685956534,-1168406632,-1150893116,-746541904,1439352169}
    };
    =>{
        keys = new int[(MAXROUNDS + 1)][NBITS];
        int[] userkey = {0,0,0,0,0,0,0,0};
        int[] w = new int[140];
        int words = (USERKEY_LENGTH / BITS_PER_WORD);

        for (int i = (words - 1); (i >= 0); i--) {
            userkey[((words - 1) - i)] = USERKEYS[vector][i];
        }
        if ((USERKEY_LENGTH < 256)) {
            int msb;
            msb = userkey[(USERKEY_LENGTH / BITS_PER_WORD)];
            userkey[(USERKEY_LENGTH / BITS_PER_WORD)] = (msb | (1 << (USERKEY_LENGTH % BITS_PER_WORD)));
        }
        for (int i = 0; (i < 8); i++) {
            w[i] = userkey[i];
        }
        for (int i = 8; (i < 140); i++) {
            w[i] = (((((w[(i - 8)] ^ w[(i - 5)]) ^ w[(i - 3)]) ^ w[(i - 1)]) ^ PHI) ^ (i - 8));
            w[i] = LRotate(w[i], 11);
        }
        for (int i = 0; (i <= MAXROUNDS); i++) {
            int[] sbox = new int[BITS_PER_WORD];
            for (int b = 0; (b < BITS_PER_WORD); b++) {
                int r;
                r = ((4 * i) + 8);
                int b0;
                b0 = ((w[(r + 0)] & (1 << b)) >> b);
                int b1;
                b1 = ((w[(r + 1)] & (1 << b)) >> b);
                int b2;
                b2 = ((w[(r + 2)] & (1 << b)) >> b);
                int b3;
                b3 = ((w[(r + 3)] & (1 << b)) >> b);
                int val;
                val = 0;
                if ((b0 != 0)) {
                    val = 1;
                }
                if ((b1 != 0)) {
                    val = (val | (1 << 1));
                }
                if ((b2 != 0)) {
                    val = (val | (1 << 2));
                }
                if ((b3 != 0)) {
                    val = (val | (1 << 3));
                }
                sbox[b] = SBOXES[(((32 + 3) - i) % 8)][val];
            }
            int[] key = new int[NBITS];
            for (int k = 0; (k < (NBITS / BITS_PER_WORD)); k++) {
                for (int b = 0; (b < BITS_PER_WORD); b++) {
                    int x;
                    x = ((sbox[b] & (1 << k)) >> k);
                    if ((x != 0)) {
                        key[((k * BITS_PER_WORD) + b)] = 1;
                    } else {
                        key[((k * BITS_PER_WORD) + b)] = 0;
                    }
                }
            }
            for (int b = 0; (b < NBITS); b++) {
                keys[i][b] = key[IP[b]];
            }
        }
    }

    private int LRotate(int x, int n) {
        int[] v = new int[32];
        int m = 1;
        for (int i = 0; (i < 32); i++) {
            if ((((x & m) >> i) != 0)) {
                v[i] = 1;
            }
            m = (m << 1);
        }
        int[] w = new int[32];
        for (int i = 0; (i < 32); i++) {
            w[i] = v[(((i + 32) - 11) % 32)];
        }
        int r = 0;
        for (int i = 0; (i < 32); i++) {
            r = (r | (w[i] << i));
        }
        return r;
    }

    BooleanC consume (int[] input) {
        //System.out.println("anonFilter_a0");
        int[] output = new int[NBITS];
        for (int i = 0; (i < NBITS); i++) {
            output[i] = keys[MAXROUNDS][i];
        }
        return (xor.consume(output));
    }
}

capsule FileWriter(String outputfile) implements Stage {
    DataOutputStream outputStream;
    =>{
        try{
            File outputFile = new File(outputfile);
            FileOutputStream localFileOutputStream = new FileOutputStream(outputFile);
            outputStream = new DataOutputStream(new BufferedOutputStream(localFileOutputStream));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private int endianFlip(int paramInt)
    {
        int i = paramInt >> 24 & 0xFF;
        int j = paramInt >> 16 & 0xFF;
        int k = paramInt >> 8 & 0xFF;
        int m = paramInt >> 0 & 0xFF;

        return i | j << 8 | k << 16 | m << 24;
    }

    private short endianFlip(short paramShort)
    {
        int i = paramShort >> 8 & 0xFF;
        int j = paramShort >> 0 & 0xFF;

        return (short)(i | j << 8);
    }

    BooleanC consume(int[] input) {
        int n = 128;
        //System.out.println("Writer");
        for (int index=0; index < n; index++) {
            try{
                //System.out.println(endianFlip(input[index]));
                outputStream.writeInt(endianFlip(input[index]));//)));
                outputStream.flush();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return new BooleanC(true);
    }
}

system Serpent {
    int vector = 2;
    int BITS_PER_WORD = 32;
    int NBITS = 128;
    int N = NBITS;
    int PHI = -1640531527;
    int MAXROUNDS = 8;
    boolean PRINTINFO = false;
    int PLAINTEXT = 0;
    int USERKEY = 1;
    int CIPHERTEXT = 2;
    int USERKEY_LENGTH = (8 * BITS_PER_WORD);

    int iterations = Integer.parseInt(args[0]);
    String inputFileName = args[1];
    String outputFileName = args[2];
    Serpent_full serpent;
    sequential FileReader reader;
    sequential SerpentEncoder encoder;
    sequential Permute ip_permute;
    sequential R rs[8];
    sequential AnonFilter_a8 anonFilter_a8[8];
    sequential Identity identity[8];
    sequential slowKeySchedule slowKeySch[8];
    sequential AnonFilter_a4 anonFilter_a4[8];
    sequential IntoBits intoBits[8];

    sequential BitSlice largebitSlice[8];
    int largeBitSliceIdentity = 32;
    sequential Identity largebitsliceidentity0[8];
    sequential Identity largebitsliceidentity1[8];
    sequential Identity largebitsliceidentity2[8];
    sequential Identity largebitsliceidentity3[8];
    sequential Identity largebitsliceidentity4[8];
    sequential Identity largebitsliceidentity5[8];
    sequential Identity largebitsliceidentity6[8];
    sequential Identity largebitsliceidentity7[8];
    sequential Sbox innersbox[8];

    int smallbitSliceidentitySize = 4;
    sequential Identity smallbitSliceidentity0[4];
    sequential Identity smallbitSliceidentity1[4];
    sequential Identity smallbitSliceidentity2[4];
    sequential Identity smallbitSliceidentity3[4];
    sequential Identity smallbitSliceidentity4[4];
    sequential Identity smallbitSliceidentity5[4];
    sequential Identity smallbitSliceidentity6[4];
    sequential Identity smallbitSliceidentity7[4];
    sequential BitSlice smallbitSlice[8];

    sequential Perms slowKeySchpermute[8];

    sequential Xor xor[9];
    sequential Sbox sbox[8];
    sequential rawL rawl[7];
    sequential AnonFilter_a9 anonFilter_a9;
    sequential Identity identity1;
    sequential KeySchedule keySchedule;
    sequential AnonFilter_a0 anonFilter_a0;
    sequential Permute fp_permute;
    sequential FileWriter writer;

    serpent(iterations,reader);
    reader(inputFileName,encoder);
    encoder(ip_permute);

    for(int i = 0; i < 8; i = i + 1)
        anonFilter_a4[i](i, intoBits[i],BITS_PER_WORD,PHI,USERKEY_LENGTH,vector);

    for(int i = 0; i < 8; i = i + 1)
        anonFilter_a8[0](identity[i], slowKeySch[i]);

    //TODO: associatiation operator [8] -> [8]
    for(int i = 0; i < 8; i = i + 1)
        rs[i](anonFilter_a8[i]);

    for(int i = 0; i < 8; i = i + 1)
        slowKeySch[i](anonFilter_a4[i]);

    ip_permute(0, NBITS, rs[0]);


    //TODO: add array slicing operator;
    //TODO: for with multiple statements
    for (int i = 0; i < 8; i = i + 1)
        largebitsliceidentity0[i](innersbox[0]);

    m2one(largebitsliceidentity0, innersbox[0]);

    for (int i = 0; i < 8; i = i + 1)
        largebitsliceidentity1[i](innersbox[1]);

    for (int i = 0; i < 8; i = i + 1)
        largebitsliceidentity2[i](innersbox[2]);

    for (int i = 0; i < 8; i = i + 1)
        largebitsliceidentity3[i](innersbox[3]);

    for (int i = 0; i < 8; i = i + 1)
        largebitsliceidentity4[i](innersbox[4]);

    for (int i = 0; i < 8; i = i + 1)
        largebitsliceidentity5[i](innersbox[5]);

    for (int i = 0; i < 8; i = i + 1)
        largebitsliceidentity6[i](innersbox[6]);

    for (int i = 0; i < 7; i = i + 1)
        largebitsliceidentity7[i](innersbox[7]);

    largebitSlice[0](largebitsliceidentity0);
    largebitSlice[1](largebitsliceidentity1);
    largebitSlice[2](largebitsliceidentity2);
    largebitSlice[3](largebitsliceidentity3);
    largebitSlice[4](largebitsliceidentity4);
    largebitSlice[5](largebitsliceidentity5);
    largebitSlice[6](largebitsliceidentity6);
    largebitSlice[7](largebitsliceidentity7);

    for(int i = 0; i < smallbitSliceidentitySize; i = i + 1)
        smallbitSliceidentity0[i](slowKeySchpermute[0]);

    for(int i = 0; i < smallbitSliceidentitySize; i = i + 1)
        smallbitSliceidentity1[i](slowKeySchpermute[1]);

    for(int i = 0; i < smallbitSliceidentitySize; i = i + 1)
        smallbitSliceidentity2[i](slowKeySchpermute[2]);

    for(int i = 0; i < smallbitSliceidentitySize; i = i + 1)
        smallbitSliceidentity3[i](slowKeySchpermute[3]);

    for(int i = 0; i < smallbitSliceidentitySize; i = i + 1)
        smallbitSliceidentity4[i](slowKeySchpermute[4]);

    for(int i = 0; i < smallbitSliceidentitySize; i = i + 1)
        smallbitSliceidentity5[i](slowKeySchpermute[5]);

    for(int i = 0; i < smallbitSliceidentitySize; i = i + 1)
        smallbitSliceidentity6[i](slowKeySchpermute[6]);

    for(int i = 0; i < smallbitSliceidentitySize; i = i + 1)
        smallbitSliceidentity7[i](slowKeySchpermute[7]);

    smallbitSlice[0](smallbitSliceidentity0);
    smallbitSlice[1](smallbitSliceidentity1);
    smallbitSlice[2](smallbitSliceidentity2);
    smallbitSlice[3](smallbitSliceidentity3);
    smallbitSlice[4](smallbitSliceidentity4);
    smallbitSlice[5](smallbitSliceidentity5);
    smallbitSlice[6](smallbitSliceidentity6);
    smallbitSlice[7](smallbitSliceidentity7);

    for(int i =0; i< 8; i = i+ 1)
        innersbox[i](0, i, smallbitSlice[i]);

    for(int i =0; i < 8 ; i = i + 1)
        slowKeySchpermute[0](N,xor[0]);

    for(int i =0; i < 8 ; i = i + 1)
        identity[i](xor[i]);

    for(int i =0; i < 7 ; i = i + 1)
        xor[i](sbox[i]);

    for(int i =0; i < 7 ; i = i + 1)
        sbox[i](1, i, rawl[i]);

    anonFilter_a9(identity1, keySchedule);
    keySchedule(anonFilter_a0);
    xor[8](fp_permute);
    identity1(xor[8]);
    anonFilter_a0(xor[8],BITS_PER_WORD,MAXROUNDS,NBITS,PHI,USERKEY_LENGTH,vector);
    writer(outputFileName);
    fp_permute(1, N, writer);


//    serpent(iterations,reader);
//    reader(inputFileName,encoder);

//    rs[0](anonFilter_a8[0]);

//    slowKeySch[0](anonFilter_a4[0]);
//    anonFilter_a4[0](0, intoBits[0],BITS_PER_WORD,PHI,USERKEY_LENGTH,vector);
//    intoBits[0](largebitSlice[0]);
//    largebitSlice[0](largebitsliceidentity0);
//    largebitsliceidentity0[0](innersbox[0]);largebitsliceidentity0[1](innersbox[0]);largebitsliceidentity0[2](innersbox[0]);largebitsliceidentity0[3](innersbox[0]);largebitsliceidentity0[4](innersbox[0]);largebitsliceidentity0[5](innersbox[0]);largebitsliceidentity0[6](innersbox[0]);largebitsliceidentity0[7](innersbox[0]);largebitsliceidentity0[8](innersbox[0]);largebitsliceidentity0[9](innersbox[0]);largebitsliceidentity0[10](innersbox[0]);largebitsliceidentity0[11](innersbox[0]);largebitsliceidentity0[12](innersbox[0]);largebitsliceidentity0[13](innersbox[0]);largebitsliceidentity0[14](innersbox[0]);largebitsliceidentity0[15](innersbox[0]);largebitsliceidentity0[16](innersbox[0]);largebitsliceidentity0[17](innersbox[0]);largebitsliceidentity0[18](innersbox[0]);largebitsliceidentity0[19](innersbox[0]);largebitsliceidentity0[20](innersbox[0]);largebitsliceidentity0[21](innersbox[0]);largebitsliceidentity0[22](innersbox[0]);largebitsliceidentity0[23](innersbox[0]);largebitsliceidentity0[24](innersbox[0]);largebitsliceidentity0[25](innersbox[0]);largebitsliceidentity0[26](innersbox[0]);largebitsliceidentity0[27](innersbox[0]);largebitsliceidentity0[28](innersbox[0]);largebitsliceidentity0[29](innersbox[0]);largebitsliceidentity0[30](innersbox[0]);largebitsliceidentity0[31](innersbox[0]);
//    innersbox[0](0, 0, smallbitSlice[0]);
//    smallbitSlice[0](smallbitSliceidentity0);
//    smallbitSliceidentity0[0](slowKeySchpermute[0]);
//    smallbitSliceidentity0[1](slowKeySchpermute[0]);
//    smallbitSliceidentity0[2](slowKeySchpermute[0]);
//    smallbitSliceidentity0[3](slowKeySchpermute[0]);
//    slowKeySchpermute[0](N,xor[0]);
//    identity[0](xor[0]);
//    xor[0](sbox[0]);
//    sbox[0](1, 0, rawl[0]);
//    rawl[0](rs[1]);
//    rs[1](anonFilter_a8[1]);
//    anonFilter_a8[1](identity[1], slowKeySch[1]);
//    slowKeySch[1](anonFilter_a4[1]);
//    anonFilter_a4[1](1, intoBits[1],BITS_PER_WORD,PHI,USERKEY_LENGTH,vector);
//    intoBits[1](largebitSlice[1]);
//    largebitSlice[1](largebitsliceidentity1);
//    largebitsliceidentity1[0](innersbox[1]);largebitsliceidentity1[1](innersbox[1]);largebitsliceidentity1[2](innersbox[1]);largebitsliceidentity1[3](innersbox[1]);largebitsliceidentity1[4](innersbox[1]);largebitsliceidentity1[5](innersbox[1]);largebitsliceidentity1[6](innersbox[1]);largebitsliceidentity1[7](innersbox[1]);largebitsliceidentity1[8](innersbox[1]);largebitsliceidentity1[9](innersbox[1]);largebitsliceidentity1[10](innersbox[1]);largebitsliceidentity1[11](innersbox[1]);largebitsliceidentity1[12](innersbox[1]);largebitsliceidentity1[13](innersbox[1]);largebitsliceidentity1[14](innersbox[1]);largebitsliceidentity1[15](innersbox[1]);largebitsliceidentity1[16](innersbox[1]);largebitsliceidentity1[17](innersbox[1]);largebitsliceidentity1[18](innersbox[1]);largebitsliceidentity1[19](innersbox[1]);largebitsliceidentity1[20](innersbox[1]);largebitsliceidentity1[21](innersbox[1]);largebitsliceidentity1[22](innersbox[1]);largebitsliceidentity1[23](innersbox[1]);largebitsliceidentity1[24](innersbox[1]);largebitsliceidentity1[25](innersbox[1]);largebitsliceidentity1[26](innersbox[1]);largebitsliceidentity1[27](innersbox[1]);largebitsliceidentity1[28](innersbox[1]);largebitsliceidentity1[29](innersbox[1]);largebitsliceidentity1[30](innersbox[1]);largebitsliceidentity1[31](innersbox[1]);
//    innersbox[1](0, 1, smallbitSlice[1]);
//    smallbitSlice[1](smallbitSliceidentity1);
//    smallbitSliceidentity1[0](slowKeySchpermute[1]);
//    smallbitSliceidentity1[1](slowKeySchpermute[1]);
//    smallbitSliceidentity1[2](slowKeySchpermute[1]);
//    smallbitSliceidentity1[3](slowKeySchpermute[1]);
//    slowKeySchpermute[1](N,xor[1]);
//    identity[1](xor[1]);
//    xor[1](sbox[1]);
//    sbox[1](1, 1, rawl[1]);
//    rawl[1](rs[2]);
//    rs[2](anonFilter_a8[2]);
//    anonFilter_a8[2](identity[2], slowKeySch[2]);
//    slowKeySch[2](anonFilter_a4[2]);
//    anonFilter_a4[2](2, intoBits[2],BITS_PER_WORD,PHI,USERKEY_LENGTH,vector);
//    intoBits[2](largebitSlice[2]);
//    largebitSlice[2](largebitsliceidentity2);
//    largebitsliceidentity2[0](innersbox[2]);largebitsliceidentity2[1](innersbox[2]);largebitsliceidentity2[2](innersbox[2]);largebitsliceidentity2[3](innersbox[2]);largebitsliceidentity2[4](innersbox[2]);largebitsliceidentity2[5](innersbox[2]);largebitsliceidentity2[6](innersbox[2]);largebitsliceidentity2[7](innersbox[2]);largebitsliceidentity2[8](innersbox[2]);largebitsliceidentity2[9](innersbox[2]);largebitsliceidentity2[10](innersbox[2]);largebitsliceidentity2[11](innersbox[2]);largebitsliceidentity2[12](innersbox[2]);largebitsliceidentity2[13](innersbox[2]);largebitsliceidentity2[14](innersbox[2]);largebitsliceidentity2[15](innersbox[2]);largebitsliceidentity2[16](innersbox[2]);largebitsliceidentity2[17](innersbox[2]);largebitsliceidentity2[18](innersbox[2]);largebitsliceidentity2[19](innersbox[2]);largebitsliceidentity2[20](innersbox[2]);largebitsliceidentity2[21](innersbox[2]);largebitsliceidentity2[22](innersbox[2]);largebitsliceidentity2[23](innersbox[2]);largebitsliceidentity2[24](innersbox[2]);largebitsliceidentity2[25](innersbox[2]);largebitsliceidentity2[26](innersbox[2]);largebitsliceidentity2[27](innersbox[2]);largebitsliceidentity2[28](innersbox[2]);largebitsliceidentity2[29](innersbox[2]);largebitsliceidentity2[30](innersbox[2]);largebitsliceidentity2[31](innersbox[2]);
//    innersbox[2](0, 2, smallbitSlice[2]);
//    smallbitSlice[2](smallbitSliceidentity2);
//    smallbitSliceidentity2[0](slowKeySchpermute[2]);
//    smallbitSliceidentity2[1](slowKeySchpermute[2]);
//    smallbitSliceidentity2[2](slowKeySchpermute[2]);
//    smallbitSliceidentity2[3](slowKeySchpermute[2]);
//    slowKeySchpermute[2](N,xor[2]);
//    identity[2](xor[2]);
//    xor[2](sbox[2]);
//    sbox[2](1, 2, rawl[2]);
//    rawl[2](rs[3]);
//    rs[3](anonFilter_a8[3]);
//    anonFilter_a8[3](identity[3], slowKeySch[3]);
//    slowKeySch[3](anonFilter_a4[3]);
//    anonFilter_a4[3](3, intoBits[3],BITS_PER_WORD,PHI,USERKEY_LENGTH,vector);
//    intoBits[3](largebitSlice[3]);
//    largebitSlice[3](largebitsliceidentity3);
//    largebitsliceidentity3[0](innersbox[3]);largebitsliceidentity3[1](innersbox[3]);largebitsliceidentity3[2](innersbox[3]);largebitsliceidentity3[3](innersbox[3]);largebitsliceidentity3[4](innersbox[3]);largebitsliceidentity3[5](innersbox[3]);largebitsliceidentity3[6](innersbox[3]);largebitsliceidentity3[7](innersbox[3]);largebitsliceidentity3[8](innersbox[3]);largebitsliceidentity3[9](innersbox[3]);largebitsliceidentity3[10](innersbox[3]);largebitsliceidentity3[11](innersbox[3]);largebitsliceidentity3[12](innersbox[3]);largebitsliceidentity3[13](innersbox[3]);largebitsliceidentity3[14](innersbox[3]);largebitsliceidentity3[15](innersbox[3]);largebitsliceidentity3[16](innersbox[3]);largebitsliceidentity3[17](innersbox[3]);largebitsliceidentity3[18](innersbox[3]);largebitsliceidentity3[19](innersbox[3]);largebitsliceidentity3[20](innersbox[3]);largebitsliceidentity3[21](innersbox[3]);largebitsliceidentity3[22](innersbox[3]);largebitsliceidentity3[23](innersbox[3]);largebitsliceidentity3[24](innersbox[3]);largebitsliceidentity3[25](innersbox[3]);largebitsliceidentity3[26](innersbox[3]);largebitsliceidentity3[27](innersbox[3]);largebitsliceidentity3[28](innersbox[3]);largebitsliceidentity3[29](innersbox[3]);largebitsliceidentity3[30](innersbox[3]);largebitsliceidentity3[31](innersbox[3]);
//    innersbox[3](0, 3, smallbitSlice[3]);
//    smallbitSlice[3](smallbitSliceidentity3);
//    smallbitSliceidentity3[0](slowKeySchpermute[3]);
//    smallbitSliceidentity3[1](slowKeySchpermute[3]);
//    smallbitSliceidentity3[2](slowKeySchpermute[3]);
//    smallbitSliceidentity3[3](slowKeySchpermute[3]);
//    slowKeySchpermute[3](N,xor[3]);
//    identity[3](xor[3]);
//    xor[3](sbox[3]);
//    sbox[3](1, 3, rawl[3]);
//    rawl[3](rs[4]);
//    rs[4](anonFilter_a8[4]);
//    anonFilter_a8[4](identity[4], slowKeySch[4]);
//    slowKeySch[4](anonFilter_a4[4]);
//    anonFilter_a4[4](4, intoBits[4],BITS_PER_WORD,PHI,USERKEY_LENGTH,vector);
//    intoBits[4](largebitSlice[4]);
//    largebitSlice[4](largebitsliceidentity4);
//    largebitsliceidentity4[0](innersbox[4]);
//    largebitsliceidentity4[1](innersbox[4]);
//    largebitsliceidentity4[2](innersbox[4]);largebitsliceidentity4[3](innersbox[4]);largebitsliceidentity4[4](innersbox[4]);largebitsliceidentity4[5](innersbox[4]);largebitsliceidentity4[6](innersbox[4]);largebitsliceidentity4[7](innersbox[4]);largebitsliceidentity4[8](innersbox[4]);largebitsliceidentity4[9](innersbox[4]);largebitsliceidentity4[10](innersbox[4]);largebitsliceidentity4[11](innersbox[4]);largebitsliceidentity4[12](innersbox[4]);largebitsliceidentity4[13](innersbox[4]);largebitsliceidentity4[14](innersbox[4]);largebitsliceidentity4[15](innersbox[4]);largebitsliceidentity4[16](innersbox[4]);largebitsliceidentity4[17](innersbox[4]);largebitsliceidentity4[18](innersbox[4]);largebitsliceidentity4[19](innersbox[4]);largebitsliceidentity4[20](innersbox[4]);largebitsliceidentity4[21](innersbox[4]);largebitsliceidentity4[22](innersbox[4]);largebitsliceidentity4[23](innersbox[4]);largebitsliceidentity4[24](innersbox[4]);largebitsliceidentity4[25](innersbox[4]);largebitsliceidentity4[26](innersbox[4]);largebitsliceidentity4[27](innersbox[4]);largebitsliceidentity4[28](innersbox[4]);largebitsliceidentity4[29](innersbox[4]);largebitsliceidentity4[30](innersbox[4]);largebitsliceidentity4[31](innersbox[4]);
//    innersbox[4](0, 4, smallbitSlice[4]);
//    smallbitSlice[4](smallbitSliceidentity4);
//    smallbitSliceidentity4[0](slowKeySchpermute[4]);
//    smallbitSliceidentity4[1](slowKeySchpermute[4]);
//    smallbitSliceidentity4[2](slowKeySchpermute[4]);
//    smallbitSliceidentity4[3](slowKeySchpermute[4]);
//    slowKeySchpermute[4](N,xor[4]);
//    identity[4](xor[4]);
//    xor[4](sbox[4]);
//    sbox[4](1, 4, rawl[4]);
//    rawl[4](rs[5]);
//    rs[5](anonFilter_a8[5]);
//    anonFilter_a8[5](identity[5], slowKeySch[5]);
//    slowKeySch[5](anonFilter_a4[5]);
//    anonFilter_a4[5](5, intoBits[5],BITS_PER_WORD,PHI,USERKEY_LENGTH,vector);
//    intoBits[5](largebitSlice[5]);
//    largebitSlice[5](largebitsliceidentity5);
//    largebitsliceidentity5[0](innersbox[5]);largebitsliceidentity5[1](innersbox[5]);largebitsliceidentity5[2](innersbox[5]);largebitsliceidentity5[3](innersbox[5]);largebitsliceidentity5[4](innersbox[5]);largebitsliceidentity5[5](innersbox[5]);largebitsliceidentity5[6](innersbox[5]);largebitsliceidentity5[7](innersbox[5]);largebitsliceidentity5[8](innersbox[5]);largebitsliceidentity5[9](innersbox[5]);largebitsliceidentity5[10](innersbox[5]);largebitsliceidentity5[11](innersbox[5]);largebitsliceidentity5[12](innersbox[5]);largebitsliceidentity5[13](innersbox[5]);largebitsliceidentity5[14](innersbox[5]);largebitsliceidentity5[15](innersbox[5]);largebitsliceidentity5[16](innersbox[5]);largebitsliceidentity5[17](innersbox[5]);largebitsliceidentity5[18](innersbox[5]);largebitsliceidentity5[19](innersbox[5]);largebitsliceidentity5[20](innersbox[5]);largebitsliceidentity5[21](innersbox[5]);largebitsliceidentity5[22](innersbox[5]);largebitsliceidentity5[23](innersbox[5]);largebitsliceidentity5[24](innersbox[5]);largebitsliceidentity5[25](innersbox[5]);largebitsliceidentity5[26](innersbox[5]);largebitsliceidentity5[27](innersbox[5]);largebitsliceidentity5[28](innersbox[5]);largebitsliceidentity5[29](innersbox[5]);largebitsliceidentity5[30](innersbox[5]);largebitsliceidentity5[31](innersbox[5]);
//    innersbox[5](0, 5, smallbitSlice[5]);
//    smallbitSlice[5](smallbitSliceidentity5);
//    smallbitSliceidentity5[0](slowKeySchpermute[5]);
//    smallbitSliceidentity5[1](slowKeySchpermute[5]);
//    smallbitSliceidentity5[2](slowKeySchpermute[5]);
//    smallbitSliceidentity5[3](slowKeySchpermute[5]);
//    slowKeySchpermute[5](N,xor[5]);
//    identity[5](xor[5]);
//    xor[5](sbox[5]);
//    sbox[5](1, 5, rawl[5]);
//    rawl[5](rs[6]);
//    rs[6](anonFilter_a8[6]);
//    anonFilter_a8[6](identity[6], slowKeySch[6]);
//    slowKeySch[6](anonFilter_a4[6]);
//    anonFilter_a4[6](6, intoBits[6],BITS_PER_WORD,PHI,USERKEY_LENGTH,vector);
//    intoBits[6](largebitSlice[6]);
//    largebitSlice[6](largebitsliceidentity6);
//    largebitsliceidentity6[0](innersbox[6]);largebitsliceidentity6[1](innersbox[6]);largebitsliceidentity6[2](innersbox[6]);largebitsliceidentity6[3](innersbox[6]);largebitsliceidentity6[4](innersbox[6]);largebitsliceidentity6[5](innersbox[6]);largebitsliceidentity6[6](innersbox[6]);largebitsliceidentity6[7](innersbox[6]);largebitsliceidentity6[8](innersbox[6]);largebitsliceidentity6[9](innersbox[6]);largebitsliceidentity6[10](innersbox[6]);largebitsliceidentity6[11](innersbox[6]);largebitsliceidentity6[12](innersbox[6]);largebitsliceidentity6[13](innersbox[6]);largebitsliceidentity6[14](innersbox[6]);largebitsliceidentity6[15](innersbox[6]);largebitsliceidentity6[16](innersbox[6]);largebitsliceidentity6[17](innersbox[6]);largebitsliceidentity6[18](innersbox[6]);largebitsliceidentity6[19](innersbox[6]);largebitsliceidentity6[20](innersbox[6]);largebitsliceidentity6[21](innersbox[6]);largebitsliceidentity6[22](innersbox[6]);largebitsliceidentity6[23](innersbox[6]);largebitsliceidentity6[24](innersbox[6]);largebitsliceidentity6[25](innersbox[6]);largebitsliceidentity6[26](innersbox[6]);largebitsliceidentity6[27](innersbox[6]);largebitsliceidentity6[28](innersbox[6]);largebitsliceidentity6[29](innersbox[6]);largebitsliceidentity6[30](innersbox[6]);largebitsliceidentity6[31](innersbox[6]);
//    innersbox[6](0, 6, smallbitSlice[6]);
//    smallbitSlice[6](smallbitSliceidentity6);
//    smallbitSliceidentity6[0](slowKeySchpermute[6]);
//    smallbitSliceidentity6[1](slowKeySchpermute[6]);
//    smallbitSliceidentity6[2](slowKeySchpermute[6]);
//    smallbitSliceidentity6[3](slowKeySchpermute[6]);
//    slowKeySchpermute[6](N,xor[6]);
//    identity[6](xor[6]);
//    xor[6](sbox[6]);
//    sbox[6](1, 6, rawl[6]);
//    rawl[6](rs[7]);
//    rs[7](anonFilter_a8[7]);
//    anonFilter_a8[7](identity[7], slowKeySch[7]);
//    slowKeySch[7](anonFilter_a4[7]);
//    anonFilter_a4[7](7, intoBits[7],BITS_PER_WORD,PHI,USERKEY_LENGTH,vector);
//    intoBits[7](largebitSlice[7]);
//    largebitSlice[7](largebitsliceidentity7);
//    largebitsliceidentity7[0](innersbox[7]);largebitsliceidentity7[1](innersbox[7]);largebitsliceidentity7[2](innersbox[7]);largebitsliceidentity7[3](innersbox[7]);largebitsliceidentity7[4](innersbox[7]);largebitsliceidentity7[5](innersbox[7]);largebitsliceidentity7[6](innersbox[7]);largebitsliceidentity7[7](innersbox[7]);largebitsliceidentity7[8](innersbox[7]);largebitsliceidentity7[9](innersbox[7]);largebitsliceidentity7[10](innersbox[7]);largebitsliceidentity7[11](innersbox[7]);largebitsliceidentity7[12](innersbox[7]);largebitsliceidentity7[13](innersbox[7]);largebitsliceidentity7[14](innersbox[7]);largebitsliceidentity7[15](innersbox[7]);largebitsliceidentity7[16](innersbox[7]);largebitsliceidentity7[17](innersbox[7]);largebitsliceidentity7[18](innersbox[7]);largebitsliceidentity7[19](innersbox[7]);largebitsliceidentity7[20](innersbox[7]);largebitsliceidentity7[21](innersbox[7]);largebitsliceidentity7[22](innersbox[7]);largebitsliceidentity7[23](innersbox[7]);largebitsliceidentity7[24](innersbox[7]);largebitsliceidentity7[25](innersbox[7]);largebitsliceidentity7[26](innersbox[7]);largebitsliceidentity7[27](innersbox[7]);largebitsliceidentity7[28](innersbox[7]);largebitsliceidentity7[29](innersbox[7]);largebitsliceidentity7[30](innersbox[7]);largebitsliceidentity7[31](innersbox[7]);
//    innersbox[7](0, 7, smallbitSlice[7]);
//    smallbitSlice[7](smallbitSliceidentity7);
//    smallbitSliceidentity7[0](slowKeySchpermute[7]);
//    smallbitSliceidentity7[1](slowKeySchpermute[7]);
//    smallbitSliceidentity7[2](slowKeySchpermute[7]);
//    smallbitSliceidentity7[3](slowKeySchpermute[7]);
//    slowKeySchpermute[7](N,xor[7]);
//    identity[7](xor[7]);
//    xor[7](sbox[7]);
//    sbox[7](1, 7, anonFilter_a9);

}



//