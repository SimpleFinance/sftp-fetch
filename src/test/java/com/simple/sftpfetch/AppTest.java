package com.simple.sftpfetch;

import com.jcraft.jsch.SftpException;
import com.simple.sftpfetch.decrypt.PGPFileDecrypter;
import com.simple.sftpfetch.publish.RabbitClient;
import com.simple.sftpfetch.publish.S3;
import com.simple.sftpfetch.sftp.SftpClient;
import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class AppTest {

    private String routing_key = "routingkey";
    private SftpClient sftpClient = mock(SftpClient.class);
    private S3 s3 = mock(S3.class);
    private RabbitClient rabbitClient = mock(RabbitClient.class);
    private PGPFileDecrypter decrypter = mock(PGPFileDecrypter.class);
    private App app = new App(sftpClient, s3, rabbitClient, decrypter, mock(PrintStream.class));
    private String filename = "foo";
    private URL url;

    @Before
    public void setUp() throws Exception {
        createOneRemoteFile();
        url = new URL("http://google.com");
        when(s3.getURLFor(filename)).thenReturn(url);
    }

    @Test
    public void shouldContainShortOptions() {
        Options options = App.getOptions();
        for (String option : asList("n", "o", "c", "d", "h")) {
            assertNotNull("Cannot find option: " + option, options.getOption(option));
        }
    }

    @Test
    public void shouldContainLongOptions() {
        Options options = App.getOptions();
        for (String option : asList("noop", "overwrite", "config", "days", "help")) {
            assertNotNull("Cannot find option: " + option, options.getOption(option));
        }
    }

    @Test
    public void shouldAskForLastDaysWorthOfOptiosn() throws Exception {
        int days = 4;
        createOneRemoteFile();

        app.run(routing_key, days);

        verify(sftpClient).getFilesNewerThan(days, App.MATCH_EVERYTHING);
    }

    @Test
    public void shouldCheckIfFilesExistInS3() throws Exception {
        createOneRemoteFile();

        invokeTheDefault();

        verify(s3).keyExists(filename);
    }

    @Test
    public void shouldSkipOverFilesThatExistInS3() throws Exception {
        createOneRemoteFile();
        theFileExistsInS3();

        invokeTheDefault();

        verifyFileNotUploaded();
    }

    @Test
    public void shouldUploadFilesThatDoNotExistInS3() throws Exception {
        createOneRemoteFile();
        theFileDoesNotExistInS3();

        invokeTheDefault();

        verifyFileUploaded();
    }

    @Test
    public void shouldUploadFilesThatExistIfOverwrite() throws Exception {
        createOneRemoteFile();
        theFileExistsInS3();

        invokeWithOverwrite();

        verifyFileUploaded();
    }

    @Test
    public void shouldNotUploadFilesThatDoNotExistIfNoop() throws Exception {
        createOneRemoteFile();
        theFileDoesNotExistInS3();

        invokeWithNoop();

        verifyFileNotUploaded();
    }

    @Test
    public void shouldPublishUrlsToRabbitForNewFiles() throws Exception {
        createOneRemoteFile();
        theFileDoesNotExistInS3();

        invokeTheDefault();

        verifyRabbitDidPublish();
    }

    @Test
    public void shouldNotPublishUrlsToRabbitIfNoop() throws Exception {
        createOneRemoteFile();
        theFileDoesNotExistInS3();

        invokeWithNoop();

        verifyRabbitDidNotPublish();
    }

    @Test
    public void shouldPublishUrlsToRabbitForExistingFilesIfOverwrite() throws Exception {
        createOneRemoteFile();
        theFileExistsInS3();

        invokeWithOverwrite();

        verifyRabbitDidPublish();
    }

    private void verifyRabbitDidPublish() throws IOException {
        verify(rabbitClient).publishURL(routing_key, url);
    }

    private void verifyRabbitDidNotPublish() {
        verifyZeroInteractions(rabbitClient);
    }

    private void invokeWithOverwrite() throws Exception {
        app.run(routing_key, 1, App.MATCH_EVERYTHING, false, true);
    }

    private void invokeWithNoop() throws Exception {
        app.run(routing_key, 1, App.MATCH_EVERYTHING, true, false);
    }

    private void invokeTheDefault() throws Exception {
        app.run(routing_key, 1);
    }

    private void verifyFileNotUploaded() {
        verify(s3, never()).upload(eq(filename), any(File.class));
    }

    private void verifyFileUploaded() {
        verify(s3).upload(eq(filename), any(File.class));
    }

    private void theFileDoesNotExistInS3() {
        when(s3.keyExists(filename)).thenReturn(false);
    }

    private void theFileExistsInS3() {
        when(s3.keyExists(filename)).thenReturn(true);
    }

    private void createOneRemoteFile() throws SftpException {
        when(sftpClient.getFilesNewerThan(anyInt(), eq(App.MATCH_EVERYTHING))).thenReturn(new HashSet<String>(asList(filename)));
    }
}
