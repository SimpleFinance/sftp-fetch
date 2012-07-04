package com.simple.sftpfetch.publish;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class S3Test {
    public static final String BUCKET = "bucket";
    private AmazonS3Client client;

    @Before
    public void setUp() throws Exception {
        client = mock(AmazonS3Client.class);
        when(client.doesBucketExist(BUCKET)).thenReturn(true);
    }

    @Test(expected=AmazonServiceException.class)
    public void shouldVerifyThatTheBucketExists() {
        when(client.doesBucketExist(BUCKET)).thenReturn(false);
        new S3(client, BUCKET);
    }

    @Test
    public void keyExistsShouldReturnFalseOn404() {
        S3 s3 = new S3(client, BUCKET);
        String key = "key.that.does.not.exist";

        AmazonServiceException exception = new AmazonServiceException("Not found yo!");
        exception.setStatusCode(404);
        when(client.getObjectMetadata(BUCKET, key)).thenThrow(exception);
        assertFalse("The key should not exist", s3.keyExists(key));
    }

    @Test
    public void shouldUploadToTheCorrectBucket() {
        S3 s3 = new S3(client, BUCKET);
        String key = "the.key";
        File toUpload = new File("/foo/bar/baz");
        s3.upload(key, toUpload);
        verify(client).putObject(BUCKET, key, toUpload);
    }

    @Test
    public void shouldConstructCorrectUrlForUSStandard() throws Exception {
        when(client.getBucketLocation(BUCKET)).thenReturn("US");
        S3 s3 = new S3(client, BUCKET);

        URL url = s3.getURLFor("formal.jpg");
        assertEquals("https://s3.amazonaws.com/bucket/formal.jpg", url.toString());
    }

    @Test
    public void shouldConstructCorrectUrlOutsideUSStandard() throws Exception {
        when(client.getBucketLocation(BUCKET)).thenReturn("us-west-2");
        S3 s3 = new S3(client, BUCKET);

        URL url = s3.getURLFor("formal.jpg");
        assertEquals("https://s3-us-west-2.amazonaws.com/bucket/formal.jpg", url.toString());
    }
}
