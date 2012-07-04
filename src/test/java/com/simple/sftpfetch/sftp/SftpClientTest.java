package com.simple.sftpfetch.sftp;

import com.jcraft.jsch.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


public class SftpClientTest {

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String HOSTNAME = "host";
    public static final int PORT = 22;
    public static final int TIMEOUT = 500;

    private JSch jSch = mock(JSch.class);
    private Session session = mock(Session.class);
    private ChannelSftp sftp = mock(ChannelSftp.class);
    private SftpConnectionInfo connectionInfo = new SftpConnectionInfo(USERNAME, PASSWORD, DOWNLOAD_FROM, HOSTNAME, PORT, TIMEOUT);
    private static final String DOWNLOAD_FROM = "OUT";

    @Before
    public void setUp() throws JSchException {
        when(jSch.getSession(eq(USERNAME), eq(HOSTNAME), eq(PORT))).thenReturn(session);
        when(session.openChannel("sftp")).thenReturn(sftp);
    }

    @Test
    public void connectingShouldEstablishSessionAndChannelUsingSuppliedInformation() throws Exception {
        new SftpClient(jSch, connectionInfo);

        verify(session).setUserInfo(eq(new PasswordBasedAuthentication(PASSWORD)));
        verify(session).connect(TIMEOUT);
        verify(sftp).connect(TIMEOUT);
    }

    @Test
    public void shouldGetFilesNewerThanTheNumberOfSuppliedDays() throws Exception {
        SftpClient client = new SftpClient(jSch, connectionInfo);

        ChannelSftp.LsEntry older = lsEntryWithGivenFilenameAndMTime("old.file", unixTimestampForDaysAgo(30));
        ChannelSftp.LsEntry newer = lsEntryWithGivenFilenameAndMTime("new.file", unixTimestampForDaysAgo(2));

        when(sftp.ls(DOWNLOAD_FROM)).thenReturn(new Vector<Object>(asList(older, newer)));
        Set<String> files = client.getFilesNewerThan(7);

        assertContainsOnly(files, new HashSet<String>(asList("new.file")));
    }

    @Test
    public void closeShouldDisconnectChannelAndSession() throws Exception {
        SftpClient client = new SftpClient(jSch, connectionInfo);
        client.close();

        verify(sftp).disconnect();
        verify(session).disconnect();
    }

    private long unixTimestampForDaysAgo(int days) {
        return new DateTime().minusDays(days).getMillis() / 1000;
    }

    private ChannelSftp.LsEntry lsEntryWithGivenFilenameAndMTime(String filename, long mtime) {
        ChannelSftp.LsEntry lsEntry = mock(ChannelSftp.LsEntry.class);
        SftpATTRS attrs = mock(SftpATTRS.class);
        when(lsEntry.getAttrs()).thenReturn(attrs);
        when(lsEntry.getFilename()).thenReturn(filename);
        when(attrs.getMTime()).thenReturn((int) mtime);
        return lsEntry;
    }

    public void assertContainsOnly(Set<String> collection, Set<String> elementsToAssert) {
        assertTrue("Did not find all expected elements", collection.containsAll(elementsToAssert));
        assertEquals("Found more elements than expected", collection.size(), elementsToAssert.size());
    }
}
