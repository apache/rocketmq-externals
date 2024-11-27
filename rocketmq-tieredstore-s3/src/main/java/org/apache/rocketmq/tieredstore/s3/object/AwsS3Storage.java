/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.tieredstore.s3.object;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.ssl.OpenSsl;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.tieredstore.s3.object.bytebuf.ByteBufAlloc;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

@SuppressWarnings({"this-escape", "NPathComplexity"})
public class AwsS3Storage extends AbstractS3Storage {
    public static final String PATH_STYLE_KEY = "pathStyle";
    public static final String AUTH_TYPE_KEY = "authType";
    public static final String STATIC_AUTH_TYPE = "static";
    public static final String INSTANCE_AUTH_TYPE = "instance";
    public static final String CHECKSUM_ALGORITHM_KEY = "checksumAlgorithm";

    private final String bucket;
    private final S3AsyncClient readS3Client;
    private final S3AsyncClient writeS3Client;

    private final ChecksumAlgorithm checksumAlgorithm;

    private volatile static InstanceProfileCredentialsProvider instanceProfileCredentialsProvider;

    public AwsS3Storage(S3URI objectURI, int maxObjectStorageConcurrency,
        boolean readWriteIsolate) {
        super(objectURI, maxObjectStorageConcurrency, readWriteIsolate);
        this.bucket = objectURI.bucket();
        List<AwsCredentialsProvider> credentialsProviders = credentialsProviders();

        ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.fromValue(objectURI.extensionString(CHECKSUM_ALGORITHM_KEY));
        if (checksumAlgorithm == null) {
            checksumAlgorithm = ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION;
        }
        this.checksumAlgorithm = checksumAlgorithm;

        Supplier<S3AsyncClient> clientSupplier =
            () -> newS3Client(objectURI.endpoint(), objectURI.region(), objectURI.extensionBool(PATH_STYLE_KEY, false), credentialsProviders,
                maxObjectStorageConcurrency);
        this.writeS3Client = clientSupplier.get();
        this.readS3Client = readWriteIsolate ? clientSupplier.get() : writeS3Client;
    }

    public S3AsyncClient getReadS3Client() {
        return readS3Client;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    CompletableFuture<ByteBuf> doRangeRead(String path, long start, long end) {
        GetObjectRequest.Builder builder = GetObjectRequest.builder().bucket(bucket).key(path).range(range(start, end));

        if (checksumAlgorithm != ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION) {
            builder.checksumMode(ChecksumMode.ENABLED);
        }

        CompletableFuture<ByteBuf> cf = new CompletableFuture<>();
        readS3Client.getObject(builder.build(), AsyncResponseTransformer.toPublisher())
            .thenAccept(responsePublisher -> {
                CompositeByteBuf buf = ByteBufAlloc.compositeByteBuffer();
                responsePublisher.subscribe(bytes -> {
                    // the aws client will copy DefaultHttpContent to heap ByteBuffer
                    buf.addComponent(true, Unpooled.wrappedBuffer(bytes));
                })
                    .whenComplete((rst, ex) -> {
                        if (ex != null) {
                            buf.release();
                            cf.completeExceptionally(ex);
                        }
                        else {
                            cf.complete(buf);
                        }
                    });
            })
            .exceptionally(ex -> {
                cf.completeExceptionally(ex);
                return null;
            });
        return cf;
    }

    @Override
    CompletableFuture<Void> doWrite(String path, ByteBuf data) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder().bucket(bucket).key(path);
        if (checksumAlgorithm != ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION) {
            builder.checksumAlgorithm(checksumAlgorithm);
        }

        PutObjectRequest request = builder.build();
        AsyncRequestBody body = AsyncRequestBody.fromByteBuffersUnsafe(data.nioBuffers());
        return writeS3Client.putObject(request, body).thenApply(rst -> null);
    }

    @Override
    CompletableFuture<String> doCreateMultipartUpload(String path) {
        CreateMultipartUploadRequest.Builder builder = CreateMultipartUploadRequest.builder().bucket(bucket).key(path);

        if (checksumAlgorithm != ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION) {
            builder.checksumAlgorithm(checksumAlgorithm);
        }

        CreateMultipartUploadRequest request = builder.build();
        return writeS3Client.createMultipartUpload(request).thenApply(CreateMultipartUploadResponse::uploadId);
    }

    @Override
    CompletableFuture<ObjectStorageCompletedPart> doUploadPart(String path, String uploadId,
        int partNumber, ByteBuf part) {
        AsyncRequestBody body = AsyncRequestBody.fromByteBuffersUnsafe(part.nioBuffers());
        UploadPartRequest.Builder builder = UploadPartRequest.builder()
            .bucket(bucket)
            .key(path)
            .uploadId(uploadId)
            .partNumber(partNumber);

        if (checksumAlgorithm != ChecksumAlgorithm.UNKNOWN_TO_SDK_VERSION) {
            builder.checksumAlgorithm(checksumAlgorithm);
        }

        return writeS3Client.uploadPart(builder.build(), body)
            .thenApply(resp -> {
                String checksum;
                switch (checksumAlgorithm) {
                    case CRC32_C:
                        checksum = resp.checksumCRC32C();
                        break;
                    case CRC32:
                        checksum = resp.checksumCRC32();
                        break;
                    case SHA1:
                        checksum = resp.checksumSHA1();
                        break;
                    case SHA256:
                        checksum = resp.checksumSHA256();
                        break;
                    default:
                        checksum = null;
                }
                return new ObjectStorageCompletedPart(partNumber, resp.eTag(), checksum);
            });
    }

    @Override
    CompletableFuture<ObjectStorageCompletedPart> doUploadPartCopy(String sourcePath, String path,
        long start, long end, String uploadId, int partNumber) {
        UploadPartCopyRequest request = UploadPartCopyRequest.builder().sourceBucket(bucket).sourceKey(sourcePath)
            .destinationBucket(bucket).destinationKey(path).copySourceRange(range(start, end)).uploadId(uploadId).partNumber(partNumber)
            .build();
        return writeS3Client.uploadPartCopy(request)
            .thenApply(resp -> new ObjectStorageCompletedPart(partNumber, resp.copyPartResult().eTag(), resp.copyPartResult().checksumCRC32C()));
    }

    @Override
    public CompletableFuture<Void> doCompleteMultipartUpload(String path, String uploadId,
        List<ObjectStorageCompletedPart> parts) {
        List<CompletedPart> completedParts = parts.stream()
            .map(part -> CompletedPart.builder().partNumber(part.getPartNumber()).eTag(part.getPartId()).checksumCRC32C(part.getCheckSum()).build())
            .collect(Collectors.toList());
        CompletedMultipartUpload multipartUpload = CompletedMultipartUpload.builder().parts(completedParts).build();
        CompleteMultipartUploadRequest request =
            CompleteMultipartUploadRequest.builder().bucket(bucket).key(path).uploadId(uploadId).multipartUpload(multipartUpload).build();
        return writeS3Client.completeMultipartUpload(request).thenApply(resp -> null);
    }

    @Override
    CompletableFuture<Boolean> doAbortMultipartUpload(String key, String uploadId) {
        AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
            .bucket(bucket)
            .key(key)
            .uploadId(uploadId)
            .build();
        return writeS3Client.abortMultipartUpload(request).thenApply(v -> true).exceptionally(e -> false);
    }

    public CompletableFuture<Void> doDeleteObjects(List<String> objectKeys) {
        ObjectIdentifier[] toDeleteKeys = objectKeys.stream().map(key ->
            ObjectIdentifier.builder()
                .key(key)
                .build()
        ).toArray(ObjectIdentifier[]::new);

        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
            .bucket(bucket)
            .delete(Delete.builder().objects(toDeleteKeys).build())
            .build();
        CompletableFuture<Void> cf = new CompletableFuture<>();
        this.writeS3Client.deleteObjects(request)
            .thenAccept(resp -> {
                try {
                    cf.complete(null);
                }
                catch (Throwable ex) {
                    cf.completeExceptionally(ex);
                }
            })
            .exceptionally(ex -> {
                cf.completeExceptionally(ex);
                return null;
            });
        return cf;
    }

    @Override
    void doClose() {
        writeS3Client.close();
        if (readS3Client != writeS3Client) {
            readS3Client.close();
        }
    }

    @Override
    CompletableFuture<List<ObjectInfo>> doList(String prefix) {
        return readS3Client.listObjectsV2(builder -> builder.bucket(bucket).prefix(prefix).maxKeys(Integer.MAX_VALUE))
            .thenApply(resp ->
                resp.contents()
                    .stream()
                    .map(object -> new ObjectInfo(object.key(), object.lastModified().toEpochMilli(), object.size()))
                    .collect(Collectors.toList()));
    }

    @Override
    CompletableFuture<ObjectInfo> doReadHeader(String objectPath) {
        return readS3Client.headObject(builder -> builder.bucket(bucket).key(objectPath))
            .thenApply(resp ->
                new ObjectInfo(objectPath, resp.lastModified() == null ? -1 : resp.lastModified().toEpochMilli(), resp.contentLength()));
    }

    protected List<AwsCredentialsProvider> credentialsProviders() {
        String authType = objectURI.extensionString(AUTH_TYPE_KEY, STATIC_AUTH_TYPE);
        switch (authType) {
            case STATIC_AUTH_TYPE: {
                String accessKey = objectURI.extensionString(S3URI.ACCESS_KEY_KEY, System.getenv("TIERED_STORE_S3_ACCESS_KEY"));
                String secretKey = objectURI.extensionString(S3URI.SECRET_KEY_KEY, System.getenv("TIERED_STORE_S3_SECRET_KEY"));
                if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
                    return Collections.emptyList();
                }
                return Lists.newArrayList(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
            }
            case INSTANCE_AUTH_TYPE: {
                return Lists.newArrayList(instanceProfileCredentialsProvider());
            }
            default:
                throw new UnsupportedOperationException("Unsupported auth type: " + authType);
        }
    }

    protected AwsCredentialsProvider instanceProfileCredentialsProvider() {
        if (instanceProfileCredentialsProvider == null) {
            synchronized (AwsS3Storage.class) {
                if (instanceProfileCredentialsProvider == null) {
                    instanceProfileCredentialsProvider = InstanceProfileCredentialsProvider.builder().build();
                }
            }
        }
        return instanceProfileCredentialsProvider;
    }

    private String range(long start, long end) {
        if (end == -1L) {
            return "bytes=" + start + "-";
        }
        // the range end is inclusive end
        return "bytes=" + start + "-" + (end - 1);
    }

    protected S3AsyncClient newS3Client(String endpoint, String region, boolean forcePathStyle,
        List<AwsCredentialsProvider> credentialsProviders, int maxConcurrency) {
        S3AsyncClientBuilder builder = S3AsyncClient.builder().region(Region.of(region));
        if (StringUtils.isNotBlank(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        if (!OpenSsl.isAvailable()) {
            log.warn("OpenSSL is not available, using JDK SSL provider, which may have performance issue.", OpenSsl.unavailabilityCause());
        }
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
            .maxConcurrency(maxConcurrency)
            .build();
        builder.httpClient(httpClient);
        builder.serviceConfiguration(c -> c.pathStyleAccessEnabled(forcePathStyle));
        builder.credentialsProvider(newCredentialsProviderChain(credentialsProviders));
        builder.overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(2))
            .apiCallAttemptTimeout(Duration.ofSeconds(60)));
        return builder.build();
    }

    private AwsCredentialsProvider newCredentialsProviderChain(List<AwsCredentialsProvider> credentialsProviders) {
        List<AwsCredentialsProvider> providers = new ArrayList<>(credentialsProviders);
        // Add default providers to the end of the chain
        providers.add(InstanceProfileCredentialsProvider.create());
        providers.add(AnonymousCredentialsProvider.create());
        return AwsCredentialsProviderChain.builder()
            .reuseLastProviderEnabled(true)
            .credentialsProviders(providers)
            .build();
    }

    public boolean readinessCheck() {
        return new ReadinessCheck().readinessCheck();
    }

    class ReadinessCheck {
        public boolean readinessCheck() {
            log.info("Start readiness check for {}", objectURI);
            String normalPath = String.format("__rocketmq/readiness_check/normal_obj/%d", System.nanoTime());
            try {
                writeS3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(normalPath).build()).get();
            }
            catch (Throwable e) {
                Throwable cause = futureCause(e);
                if (cause instanceof SdkClientException) {
                    log.error("Cannot connect to s3, please check the s3 endpoint config", cause);
                }
                else if (cause instanceof S3Exception) {
                    int code = ((S3Exception) cause).statusCode();
                    switch (code) {
                        case HttpStatusCode.NOT_FOUND:
                            break;
                        case HttpStatusCode.FORBIDDEN:
                            log.error("Please check whether config is correct", cause);
                            return false;
                        default:
                            log.error("Please check config is correct", cause);
                    }
                }
            }

            try {
                byte[] content = new Date().toString().getBytes(StandardCharsets.UTF_8);
                doWrite(normalPath, Unpooled.wrappedBuffer(content)).get();
            }
            catch (Throwable e) {
                Throwable cause = futureCause(e);
                if (cause instanceof S3Exception && ((S3Exception) cause).statusCode() == HttpStatusCode.NOT_FOUND) {
                    log.error("Cannot find the bucket={}", bucket, cause);
                }
                else {
                    log.error("Please check the identity have the permission to do Write Object operation", cause);
                }
                return false;
            }

            try {
                doDeleteObjects(Lists.newArrayList(normalPath)).get();
            }
            catch (Throwable e) {
                log.error("Please check the identity have the permission to do Delete Object operation", futureCause(e));
                return false;
            }

            String multiPartPath = String.format("__rocketmq/readiness_check/multi_obj/%d", System.nanoTime());
            try {
                String uploadId = doCreateMultipartUpload(multiPartPath).get();
                byte[] content = new Date().toString().getBytes(StandardCharsets.UTF_8);
                ObjectStorageCompletedPart part = doUploadPart(multiPartPath, uploadId, 1, Unpooled.wrappedBuffer(content)).get();
                doCompleteMultipartUpload(multiPartPath, uploadId, Lists.newArrayList(part)).get();

                ByteBuf buf = doRangeRead(multiPartPath, 0, -1L).get();
                byte[] readContent = new byte[buf.readableBytes()];
                buf.readBytes(readContent);
                buf.release();
                if (!Arrays.equals(content, readContent)) {
                    log.error("Read get mismatch content from multi-part upload object, expect {}, but {}", content, readContent);
                }
                doDeleteObjects(Lists.newArrayList(multiPartPath)).get();
            }
            catch (Throwable e) {
                log.error("Please check the identity have the permission to do MultiPart Object operation", futureCause(e));
                return false;
            }

            log.info("Readiness check pass!");
            return true;
        }

    }

    public static class Builder {
        private S3URI bucketURI;
        private boolean readWriteIsolate;
        private int maxObjectStorageConcurrency;

        public Builder bucket(S3URI bucketURI) {
            this.bucketURI = bucketURI;
            return this;
        }

        public Builder readWriteIsolate(boolean readWriteIsolate) {
            this.readWriteIsolate = readWriteIsolate;
            return this;
        }

        public Builder maxObjectStorageConcurrency(int maxObjectStorageConcurrency) {
            this.maxObjectStorageConcurrency = maxObjectStorageConcurrency;
            return this;
        }

        public AwsS3Storage build() {
            return new AwsS3Storage(bucketURI, maxObjectStorageConcurrency, readWriteIsolate);
        }
    }
}
