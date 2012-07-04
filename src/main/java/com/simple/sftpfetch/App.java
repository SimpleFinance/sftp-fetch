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

package com.simple.sftpfetch;

import com.amazonaws.services.s3.AmazonS3Client;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.SftpException;
import com.rabbitmq.client.ConnectionFactory;
import com.simple.sftpfetch.decrypt.FileDecrypter;
import com.simple.sftpfetch.decrypt.NoopDecrypter;
import com.simple.sftpfetch.decrypt.PGPFileDecrypter;
import com.simple.sftpfetch.publish.RabbitClient;
import com.simple.sftpfetch.publish.RabbitConnectionInfo;
import com.simple.sftpfetch.publish.S3;
import com.simple.sftpfetch.publish.SuppliedAWSCredentials;
import com.simple.sftpfetch.sftp.SftpClient;
import com.simple.sftpfetch.sftp.SftpConnectionInfo;
import org.apache.commons.cli.*;

import java.io.*;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 * Main application entry point
 */
public class App {
    public static final String FETCH_DAYS = "fetch.days";
    public static final Pattern MATCH_EVERYTHING = Pattern.compile(".*");
    private SftpClient sftp;
    private S3 s3;
    private RabbitClient rabbit;
    private FileDecrypter decrypter;
    private PrintStream out;

    public App(SftpClient sftpClient, S3 s3, RabbitClient rabbitClient, FileDecrypter decrypter, PrintStream out) {
        this.sftp = sftpClient;
        this.s3 = s3;
        this.rabbit = rabbitClient;
        this.decrypter = decrypter;
        this.out = out;
    }

    private static S3 s3FromProperties(Properties properties) {
        String s3Bucket = properties.getProperty("s3.bucket");
        final String awsAccessKey = properties.getProperty("s3.access.key", "");
        final String awsSecretKey = properties.getProperty("s3.secret.key", "");

        AmazonS3Client client;
        if (awsAccessKey.isEmpty() || awsSecretKey.isEmpty()) {
            client = new AmazonS3Client();
        } else {
            client = new AmazonS3Client(new SuppliedAWSCredentials(awsAccessKey, awsSecretKey));
        }

        return new S3(client, s3Bucket);
    }


    /**
     * A simpler entry point with sane defaults, used by the tests
     *
     * @param routingKey the routing key to use for publishing rabbit messages
     * @param daysToFetch the number of days back to fetch from SFTP
     *
     * @throws IOException
     * @throws NoSuchProviderException
     * @throws SftpException
     */
    void run(String routingKey, int daysToFetch) throws IOException, NoSuchProviderException, SftpException {
        run(routingKey, daysToFetch, App.MATCH_EVERYTHING, false, false);
    }

    /**
     * Run the application
     *
     * @param routingKey the routing key to use for publishing rabbit messages
     * @param daysToFetch the number of days back to fetch from SFTP
     * @param pattern a regular expression pattern that files must match to be processed
     * @param noop if true do not actually modify anything
     * @param overwrite re-publish previously seen files
     *
     * @throws SftpException
     * @throws IOException
     * @throws NoSuchProviderException
     */
    public void run(String routingKey, int daysToFetch, Pattern pattern, boolean noop, boolean overwrite) throws SftpException, IOException, NoSuchProviderException {
        for (String filename : sftp.getFilesNewerThan(daysToFetch, pattern)) {
            if (s3.keyExists(filename)) {
                out.println("Previously seen: " + filename);
                if (!overwrite) {
                    continue;
                }
            }
            if (noop) {
                out.println("Would process: " + filename);
            } else {
                File downloaded = sftp.downloadFile(filename);
                File toUpload = decrypter.decryptFile(downloaded);
                    s3.upload(filename, toUpload);
                rabbit.publishURL(routingKey, s3.getURLFor(filename));
                out.println("Processed: " + filename);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        Options options = getOptions();

        List<String> requiredProperties = asList("c");


        CommandLineParser parser = new PosixParser();
        try {
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.hasOption("h")) {
                printUsage(options);
                System.exit(0);
            }

            for (String opt : requiredProperties) {
                if (!commandLine.hasOption(opt)) {
                    System.err.println("The option: " + opt + " is required.");
                    printUsage(options);
                    System.exit(1);
                }
            }

            Pattern pattern;
            if (commandLine.hasOption("p")) {
                pattern = Pattern.compile(commandLine.getOptionValue("p"));
            } else {
                pattern = MATCH_EVERYTHING;
            }

            String filename = commandLine.getOptionValue("c");
            Properties properties = new Properties();
            try {
                InputStream stream = new FileInputStream(new File(filename));
                properties.load(stream);
            } catch (IOException ioe) {
                System.err.println("Unable to read properties from: " + filename);
                System.exit(2);
            }

            String routingKey = "";
            if (commandLine.hasOption("r")) {
                routingKey = commandLine.getOptionValue("r");
            } else if (properties.containsKey("rabbit.routingkey")) {
                routingKey = properties.getProperty("rabbit.routingkey");
            }

            int daysToFetch;
            if (commandLine.hasOption("d")) {
                daysToFetch = Integer.valueOf(commandLine.getOptionValue("d"));
            } else {
                daysToFetch = Integer.valueOf(properties.getProperty(FETCH_DAYS));
            }

            FileDecrypter decrypter = null;
            if (properties.containsKey("decryption.key.path")) {
                decrypter = new PGPFileDecrypter(new File(properties.getProperty("decryption.key.path")));
            } else {
                decrypter = new NoopDecrypter();
            }

            SftpClient sftpClient = new SftpClient(new JSch(), new SftpConnectionInfo(properties));
            try {
                App app = new App(sftpClient,
                        s3FromProperties(properties),
                        new RabbitClient(new ConnectionFactory(), new RabbitConnectionInfo(properties)),
                        decrypter,
                        System.out);
                app.run(routingKey, daysToFetch, pattern, commandLine.hasOption("n"), commandLine.hasOption("o"));
            } finally {
                sftpClient.close();
            }
            System.exit(0);
        } catch (UnrecognizedOptionException uoe) {
            System.err.println(uoe.getMessage());
            printUsage(options);
            System.exit(10);
        }
    }

    public static Options getOptions() {
        Options options = new Options();
        options.addOption("n", "noop", false, "Don't download or publish anything, simply mention what would be done");
        options.addOption("o", "overwrite", false, "Re-publish previously seen files");
        options.addOption("p", "pattern", true, "Only download files that match this pattern.");
        options.addOption("r", "routing-key", true, "Routing key for posting messages");
        options.addOption("c", "config", true, "Properties file containing configuration options");
        options.addOption("d", "days", true, "Download files newer than this many days ago");
        options.addOption("h", "help", false, "Show this screen");
        return options;
    }

    public static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar sftp-fetch.jar [options]", options);
    }
}
