package hu.laci.cms.backend.config.media;

import hu.laci.cms.backend.config.app.ServletContextParameters;
import hu.laci.cms.backend.model.media.MediaStorageType;

import javax.servlet.ServletContext;
import java.nio.file.Path;
import java.util.Map;

public final class MediaStorageConfig {

    public static final String ENV_STORAGE_TYPE = "MEDIA_STORAGE_TYPE";
    public static final String ENV_FILESYSTEM_PATH = "MEDIA_FILESYSTEM_PATH";

    public static final String PARAM_STORAGE_TYPE = "media.storage.type";
    public static final String PARAM_FILESYSTEM_PATH = "media.filesystem.path";

    private static final MediaStorageConfig DEFAULT = new MediaStorageConfig(
            MediaStorageType.FILESYSTEM, Path.of("media-storage"));
    private static volatile MediaStorageConfig current = DEFAULT;

    private final MediaStorageType storageType;
    private final Path filesystemPath;

    private MediaStorageConfig(MediaStorageType storageType, Path filesystemPath) {
        this.storageType = storageType;
        this.filesystemPath = filesystemPath;
    }

    public static void initialize(ServletContext servletContext) {
        current = from(servletContext);
    }

    public static void reset() {
        current = DEFAULT;
    }

    public static MediaStorageConfig getCurrent() {
        return current;
    }

    private static MediaStorageConfig from(ServletContext servletContext) {
        return from(servletContext, System.getenv());
    }

    static MediaStorageConfig from(ServletContext servletContext, Map<String, String> environment) {
        String configuredStorageType = getEnvOrContext(environment, ENV_STORAGE_TYPE, servletContext, PARAM_STORAGE_TYPE);
        String configuredFilesystemPath = getEnvOrContext(environment, ENV_FILESYSTEM_PATH, servletContext,
                PARAM_FILESYSTEM_PATH);

        return new MediaStorageConfig(
                parseStorageType(configuredStorageType, DEFAULT.storageType),
                isBlank(configuredFilesystemPath) ? DEFAULT.filesystemPath : Path.of(configuredFilesystemPath.trim()));
    }

    private static MediaStorageType parseStorageType(String value, MediaStorageType defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return MediaStorageType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static String getEnvOrContext(Map<String, String> environment, String envKey,
                                          ServletContext servletContext, String paramName) {
        String envValue = environment.get(envKey);
        if (!isBlank(envValue)) {
            return envValue;
        }
        return ServletContextParameters.getString(servletContext, paramName);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public MediaStorageType getStorageType() {
        return storageType;
    }

    public Path getFilesystemPath() {
        return filesystemPath;
    }
}
