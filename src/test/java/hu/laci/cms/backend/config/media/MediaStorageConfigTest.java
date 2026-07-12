package hu.laci.cms.backend.config.media;

import hu.laci.cms.backend.model.media.MediaStorageType;
import hu.laci.cms.backend.service.media.DatabaseMediaStorageService;
import hu.laci.cms.backend.service.media.FileSystemMediaStorageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;

/** Unit tests for media storage configuration resolution and factory selection. */
class MediaStorageConfigTest {

    @Test
    void environmentOverridesContextAndInvalidTypeUsesDefault() {
        ServletContext context = context("DATABASE", "context-path");
        MediaStorageConfig config = MediaStorageConfig.from(context, Map.of(
                MediaStorageConfig.ENV_STORAGE_TYPE, "filesystem",
                MediaStorageConfig.ENV_FILESYSTEM_PATH, " env-path "));
        Assertions.assertEquals(MediaStorageType.FILESYSTEM, config.getStorageType());
        Assertions.assertEquals(Path.of("env-path"), config.getFilesystemPath());

        MediaStorageConfig invalid = MediaStorageConfig.from(context,
                Map.of(MediaStorageConfig.ENV_STORAGE_TYPE, "unsupported"));
        Assertions.assertEquals(MediaStorageType.FILESYSTEM, invalid.getStorageType());
    }

    @Test
    void factoryCreatesStorageServiceForConfiguredType() {
        MediaStorageConfig filesystem = MediaStorageConfig.from(context(null, "target/media-factory-test"), Map.of());
        MediaStorageConfig database = MediaStorageConfig.from(context("DATABASE", null), Map.of());
        Assertions.assertInstanceOf(FileSystemMediaStorageService.class, MediaStorageServiceFactory.create(filesystem));
        Assertions.assertInstanceOf(DatabaseMediaStorageService.class, MediaStorageServiceFactory.create(database));
    }

    @Test
    void contextValuesAreUsedWhenEnvironmentIsAbsent() {
        MediaStorageConfig config = MediaStorageConfig.from(context("DATABASE", "context-storage"), Map.of());

        Assertions.assertEquals(MediaStorageType.DATABASE, config.getStorageType());
        Assertions.assertEquals(Path.of("context-storage"), config.getFilesystemPath());
    }

    private ServletContext context(String storageType, String filesystemPath) {
        return (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (proxy, method, arguments) -> {
                    if ("getInitParameter".equals(method.getName())) {
                        String name = (String) arguments[0];
                        if (MediaStorageConfig.PARAM_STORAGE_TYPE.equals(name)) {
                            return storageType;
                        }
                        if (MediaStorageConfig.PARAM_FILESYSTEM_PATH.equals(name)) {
                            return filesystemPath;
                        }
                    }
                    return null;
                });
    }
}
