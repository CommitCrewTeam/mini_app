package com.miniecommerce.common.storage;

import com.miniecommerce.common.hexagonal.adapter.Pojo.StagedFile;
import java.nio.file.Path;
import java.util.function.Function;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Stages a multipart upload into a StagedFile and hands it to the caller via
 * factory. FilePart (transport type) stays in the web adapter.
 */
@Component
public class UploadTempFileStagerReactor {

    private final FileUtil fileUtil;

    public UploadTempFileStagerReactor(FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    public <C> Mono<C> stage(FilePart file, String prefix, String suffix,
                             Function<StagedFile, C> factory) {
        return Mono.usingWhen(
                allocate(prefix, suffix),
                tmp -> toStaged(file, tmp).map(factory),
                tmp -> Mono.empty(),          // success: ownership chuyển sang service/job
                (tmp, err) -> discard(tmp),   // error
                this::discard);               // cancel
    }

    private Mono<Path> allocate(String prefix, String suffix) {
        return Mono.fromCallable(() -> fileUtil.allocate(prefix, suffix))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<StagedFile> toStaged(FilePart file, Path tmp) {
        return file.transferTo(tmp)
                .then(Mono.fromCallable(() -> fileUtil.size(tmp))
                          .subscribeOn(Schedulers.boundedElastic()))
                .map(size -> new StagedFile(file.filename(), size, tmp));
    }

    private Mono<Void> discard(Path tmp) {
        return Mono.<Void>fromRunnable(() -> fileUtil.deleteQuietly(tmp))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
