/*
 * Copyright (c) 2012 Simple Finance Technology Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Based on the org.bouncycastle.openpgp.examples.KeyBasedFileProcessor
 * example code, distributed under the following license license:
 *
 * Copyright (c) 2000 - 2011 The Legion Of The Bouncy Castle (http://www.bouncycastle.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE
 *
 */

package com.simple.sftpfetch.decrypt;

import org.bouncycastle.openpgp.*;
import org.bouncycastle.util.io.Streams;

import java.io.*;
import java.security.NoSuchProviderException;
import java.util.Iterator;

/**
 * PGP file decrypter based on {@link org.bouncycastle.openpgp.examples.KeyBasedFileProcessor}
 */
public class PGPFileDecrypter implements FileDecrypter {

    private PGPSecretKeyRingCollection pgpSec;

    /**
     * Initialize the PGPFileDecrypter with the private key from the given file
     *
     * @param key file containing unencrypted private key
     *
     * @throws IOException
     * @throws PGPException
     */
    public PGPFileDecrypter(File key) throws IOException, PGPException {
        InputStream keyIn = new FileInputStream(key);
        try {
            this.pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyIn));
        } finally {
            keyIn.close();
        }
    }

    /**
     * Decrypt the given file and return the resulting File
     *
     * @param input an encrypted file
     *
     * @return the decrypted file
     * @throws IOException
     * @throws NoSuchProviderException
     */
    @Override
    public File decryptFile(File input) throws IOException, NoSuchProviderException {
        InputStream in = new BufferedInputStream(new FileInputStream(input));
        File out = File.createTempFile("message", ".txt");
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(out));
        try {
            decryptFile(in, outStream);
        } finally {
            outStream.close();
            in.close();
        }

        return out;
    }

    private void decryptFile(InputStream in, OutputStream outputStream) throws IOException, NoSuchProviderException {
        in = PGPUtil.getDecoderStream(in);

        try {
            PGPEncryptedDataList enc = getEncryptedDataList(in);

            Iterator it = enc.getEncryptedDataObjects();
            PGPPrivateKey sKey = null;
            PGPPublicKeyEncryptedData pbe = null;

            while (sKey == null && it.hasNext()) {
                pbe = (PGPPublicKeyEncryptedData) it.next();
                sKey = getPrivateKey(sKey, pbe);
            }

            if (sKey == null) {
                throw new IllegalArgumentException("secret key for message not found.");
            }

            InputStream clear = pbe.getDataStream(sKey, "BC");
            Object message = new PGPObjectFactory(clear).nextObject();

            if (message instanceof PGPCompressedData) {
                PGPCompressedData cData = (PGPCompressedData) message;
                PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream());
                message = pgpFact.nextObject();
            }

            if (message instanceof PGPLiteralData) {
                PGPLiteralData ld = (PGPLiteralData) message;
                Streams.pipeAll(ld.getInputStream(), outputStream);
            } else if (message instanceof PGPOnePassSignatureList) {
                throw new PGPException("encrypted message contains a signed message - not literal data.");
            } else {
                throw new PGPException("message is not a simple encrypted file - type unknown.");
            }

            if (pbe.isIntegrityProtected() && !pbe.verify()) {
                throw new PGPException("message failed integrity check");
            }
        } catch (PGPException e) {
            System.err.println(e);
            if (e.getUnderlyingException() != null) {
                e.getUnderlyingException().printStackTrace();
            }
        }
    }

    private PGPEncryptedDataList getEncryptedDataList(InputStream in) throws IOException {
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject(); // the first object might be a PGP marker packet.
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }
        return enc;
    }

    /**
     * Get the matching PGPPrivateKey for the given public key encrypted data
     *
     * @param sKey private key information
     * @param pbe public key encrypted data
     *
     * @return the matching PGPPrivateKey
     *
     * @throws PGPException
     * @throws NoSuchProviderException
     */
    private PGPPrivateKey getPrivateKey(PGPPrivateKey sKey, PGPPublicKeyEncryptedData pbe) throws PGPException, NoSuchProviderException {
        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(pbe.getKeyID());
        if (pgpSecKey != null) {
            sKey = pgpSecKey.extractPrivateKey(new char[]{}, "BC");
        }
        return sKey;
    }
}
