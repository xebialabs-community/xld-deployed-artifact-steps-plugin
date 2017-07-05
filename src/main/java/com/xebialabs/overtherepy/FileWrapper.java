/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.xebialabs.overthere.OverthereFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * OverthereFile and its HashCode
 */
class FileWrapper {

    private HashFunction hashFunction = Hashing.goodFastHash(32);

    private OverthereFile file;

    private HashCode hashCode;

    FileWrapper(OverthereFile file) {
        this.file = file;
    }

    public OverthereFile getFile() {
        return file;
    }


    @Override
    public int hashCode() {
        return file.getName().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof FileWrapper) {
            return file.getName().equals(((FileWrapper) obj).file.getName());
        }
        return false;
    }

    @Override
    public String toString() {
        return file.toString();
    }

    public HashCode getHashCode() {
        if (hashCode == null) {
            try {
                hashCode = asByteSource(file).hash(hashFunction);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return hashCode;
    }

    private static ByteSource asByteSource(final OverthereFile file) {
        return new OverthereFileByteSource(file);
    }

    private static final class OverthereFileByteSource extends ByteSource {

        final OverthereFile file;

        private OverthereFileByteSource(final OverthereFile file) {
            this.file = file;
        }

        @Override
        public InputStream openStream() throws IOException {
            return file.getInputStream();
        }

        @Override
        public long size() throws IOException {
            if (!file.isFile()) {
                throw new FileNotFoundException(file.toString());
            }
            return file.length();
        }

        @Override
        public String toString() {
            return "OverthereFileByteSource.asByteSource(" + file + ")";
        }
    }


}
