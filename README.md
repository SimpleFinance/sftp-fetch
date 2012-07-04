# sftp-fetch

Download new files from an SFTP server and publish them to a queue,
using S3 to hold the data.

## Configuration

First, create a properties file that looks like

```
sftp.hostname=<the.sftp.hostname>
sftp.username=<the.sftp.username>
sftp.password=<the.sftp.password>
sftp.folder=<the.sftp.folder.to.download.from>

rabbit.hostname=<the.rabbit.hostname>
rabbit.port=5672
rabbit.username=<the.rabbit.username>
rabbit.password=<the.rabbit.password>
rabbit.vhost=<the.rabbit.vhost>
rabbit.exchange=<the.rabbit.exchange>

s3.bucket=<s3.bucket>
fetch.days=<number.of.days.to.fetch>
```

## Usage

### The Basics

Once you have created the appropriate config files you can invoke

```
java -jar sftp-fetch.jar -c </path/to/properties/file> -n
```

This will show you what files it would process without actually doing
it. Run without `-n` to actually process. Files that have already been
processed will be skipped unless you run with `--overwrite`

### Files that match a pattern

Optionally you can restrict operations only to files that match a
certain pattern using `-p`

```
java -jar sftp-fetch.jar -c </path/to/properties/file> -r <routingkey> -p '.*SomeFileName.*'
```

### Specify the routing key

You can specify a routing key using `--routing-key` at the command
line or `rabbit.routingkey` in the property file.

### GPG decryption

If you are fetching GPG encrypted files from SFTP you can optionally
have `sft-fetch` decrypt them by supplying the path to the GPG private
key in the property file using

```
decryption.key.path=</path/to/pgp/private/key>
```

Currently encrypted private keys are not supported.
