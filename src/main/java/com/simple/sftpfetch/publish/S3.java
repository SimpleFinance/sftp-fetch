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

package com.simple.sftpfetch.publish;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A simple client for uploading files to S3
 */
public class S3 {
    private AmazonS3Client s3;
    private String bucket;
    private String location;

    /**
     * Initialize the client using the supplied {@link AmazonS3Client} and bucket name.
     * This will verify if the bucket exists and retrieve the location of the given bucket to be used later.
     *
     * @param s3 the AmazonS3Client to use
     * @param bucket the name of the bucket to use
     */
    public S3(AmazonS3Client s3, String bucket) {
        this.bucket = bucket;
        this.s3 = s3;
        if (!this.s3.doesBucketExist(bucket)) {
            throw new AmazonServiceException("Bucket does not exist: " + bucket);
        }
        this.location = s3.getBucketLocation(bucket);
    }

    /**
     * Check if the given key exists
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    public boolean keyExists(String key){
        try {
            s3.getObjectMetadata(bucket, key);
        } catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == 404) {
                return false;
            } else {
                throw ase;
            }
        }
        return true;
    }

    /**
     * Upload the given file using the given key
     *
     * @param key the key to use
     * @param toUpload to file to upload
     */
    public void upload(String key, File toUpload) {
        s3.putObject(bucket, key, toUpload);
    }

    /**
     * Get the hostname to use in creating HTTP URLs for S3 objects
     *
     * @return the hostname
     */
    public String getS3HostName() {
        String prefix;
        if (location.equalsIgnoreCase("US")) {
            prefix = "s3";
        } else {
            prefix = "s3-" + this.location;
        }
        return prefix + ".amazonaws.com";
    }

    /**
     * Create an absolute HTTP URL for the object at the given key
     *
     * @param key the key
     *
     * @return the absolute HTTP URL
     * @throws MalformedURLException
     */
    public URL getURLFor(String key) throws MalformedURLException {
        return new URL("https://" + this.getS3HostName() + "/" + this.bucket + "/"  + key);
    }
}
