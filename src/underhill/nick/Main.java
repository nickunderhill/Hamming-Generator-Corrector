package underhill.nick;

import java.io.*;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static final Random RANDOM = new Random();

    public static void main(String[] args) throws IOException {
        File sendFile = new File("send.txt");
        File encodedFile = new File("encoded.txt");
        File decodedFile = new File("decoded.txt");
        File receivedFile = new File("received.txt");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Write a mode: ");
        String mode = scanner.next();
        switch (mode) {
            case "encode": {
                // Read file to binary string
                String binaryViewInput = readBytesFromFileToBinaryString(sendFile);
                // Encode with Hamming Code
                String hammingEncoded = hamming74Encoder(binaryViewInput);
                // Write bytes to file encoded.txt
                writeBinaryStringAsBytesToFile(hammingEncoded, encodedFile);
                break;
            }
            case "send": {
                // Read file to binary string
                String binaryViewInput = readBytesFromFileToBinaryString(encodedFile);
                // Generate error in 1 bit for every 8-bit byte
                String damagedInput = makeNoise(binaryViewInput, 8);
                // Write bytes to file received.txt
                writeBinaryStringAsBytesToFile(damagedInput, receivedFile);
                break;
            }
            case "decode":
                // Decode received.txt and write to decoded.txt
                try (InputStream inputStream = new FileInputStream(receivedFile)) {
                    byte[] inputBytes = inputStream.readAllBytes();
                    StringBuilder result = new StringBuilder();
                    String[] parts = new String[inputBytes.length];

                    //Convert to binary 7-bit length bytes
                    for (int i = 0; i < inputBytes.length; i++) {
                        parts[i] = Integer.toBinaryString((inputBytes[i] & 0xFF) + 0x100).substring(1);
                    }

                    //Find problem byte
                    for (String p : parts) {
                        //data bits int values
                        int d3 = Character.getNumericValue(p.charAt(2));
                        int d5 = Character.getNumericValue(p.charAt(4));
                        int d6 = Character.getNumericValue(p.charAt(5));
                        int d7 = Character.getNumericValue(p.charAt(6));
                        //parity bits with errors
                        boolean par1 = (d3 + d5 + d7) % 2 == Character.getNumericValue(p.charAt(0));
                        boolean par2 = (d3 + d6 + d7) % 2 == Character.getNumericValue(p.charAt(1));
                        boolean par4 = (d5 + d6 + d7) % 2 == Character.getNumericValue(p.charAt(3));
                        //array of restored data bits
                        char[] data = {p.charAt(2), p.charAt(4), p.charAt(5), p.charAt(6)};
                        //find and restore damaged bit
                        if (!par1 && !par2 && !par4) {
                            // If all parity bits are corrupted error is in d7
                            data[3] = data[3] == '0' ? '1' : '0';
                        } else if (!par2 && !par4) {
                            // If 2nd and 4th parity bits are corrupted error is in d6
                            data[2] = data[2] == '0' ? '1' : '0';
                        } else if (!par1 && !par4) {
                            // If 1st and 4th parity bits are corrupted error is in d5
                            data[1] = data[1] == '0' ? '1' : '0';
                        } else if (!par1 && !par2) {
                            // If 1st and 2nd parity bits are corrupted error is in d3
                            data[0] = data[0] == '0' ? '1' : '0';
                        }
                        result.append(data);
                    }
                    // Write bytes to file decoded.txt
                    writeBinaryStringAsBytesToFile(result.toString(), decodedFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    /**
     * Takes {@code binaryString} and inverts one random binary {@code char} every {@code errFreq} chars.
     *
     * @param binaryString - binary string i.g. "011010101"
     * @param errFreq      - frequency of inversions, i.e. value 3 means that 1 inversion will be performed for every 3 chars
     */
    public static String makeNoise(String binaryString, int errFreq) {
        StringBuilder result = new StringBuilder(binaryString);
        for (int i = 0; i < binaryString.length(); i += errFreq) {
            int indexToRandomize = RANDOM.nextInt(errFreq);
            if (binaryString.charAt(i + indexToRandomize) == 48) {
                result.setCharAt(i + indexToRandomize, '1');
            } else {
                result.setCharAt(i + indexToRandomize, '0');
            }
        }
        return result.toString();
    }

    /**
     * Encodes provided {@code binaryString} using Hamming code[7,4] algorithm
     * d[n] variables represent data bits.
     * p[n] - parity bits.
     *
     * @param binaryString - binary string i.g. "011010101"
     * @return encoded String in which every 4 bits are converted to 7-bits long byte
     */
    public static String hamming74Encoder(String binaryString) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < binaryString.length(); i += 4) {
            char d3 = binaryString.charAt(i);
            char d5 = binaryString.charAt(i + 1);
            char d6 = binaryString.charAt(i + 2);
            char d7 = binaryString.charAt(i + 3);
            char p1 = (d3 + d5 + d7) % 2 == 0 ? '0' : '1';
            char p2 = (d3 + d6 + d7) % 2 == 0 ? '0' : '1';
            char p4 = (d5 + d6 + d7) % 2 == 0 ? '0' : '1';
            result.append(new char[]{p1, p2, d3, p4, d5, d6, d7})
                    .append(0);
        }
        return result.toString();
    }

    public static String readBytesFromFileToBinaryString(File input) throws IOException {
        try (InputStream inputStream = new FileInputStream(input)) {
            byte[] inputBytes = inputStream.readAllBytes();
            StringBuilder binaryViewInput = new StringBuilder();
            for (byte b : inputBytes) {
                binaryViewInput.append(Integer.toBinaryString((b & 0xFF) + 0x100).substring(1));
            }
            return binaryViewInput.toString();
        }
    }

    public static void writeBinaryStringAsBytesToFile(String binaryString, File output) throws IOException {
        String[] binaryBytes = binaryString.split("(?<=\\G.{8})");
        try (OutputStream outputStream = new FileOutputStream(output)) {
            for (String s : binaryBytes) {
                outputStream.write((byte) Integer.parseInt(s, 2));
            }
        }
    }

}
