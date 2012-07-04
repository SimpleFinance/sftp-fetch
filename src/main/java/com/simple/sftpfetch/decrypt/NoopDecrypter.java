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
 */

package com.simple.sftpfetch.decrypt;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchProviderException;

/**
 * Does not actually decrypt files, it merely returns the input
 */
public class NoopDecrypter implements FileDecrypter {
    /**
     * Pretend to decrypt the given file, but merely return the input
     *
     * @param input an encrypted file
     * @return the decrypted file
     * @throws java.io.IOException
     * @throws java.security.NoSuchProviderException
     *
     */
    @Override
    public File decryptFile(File input) throws IOException, NoSuchProviderException {
        return input;
    }
}
