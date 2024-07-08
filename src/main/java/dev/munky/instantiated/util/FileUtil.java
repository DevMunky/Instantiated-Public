package dev.munky.instantiated.util;

import dev.munky.instantiated.Instantiated;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

public class FileUtil {
    public static void deleteWorld(World world){
        try {
            Files.walkFileTree(world.getWorldFolder().toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Objects.requireNonNull(file);
                    Objects.requireNonNull(attrs);
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    Objects.requireNonNull(dir);
                    if (exc != null)
                        throw exc;
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            Instantiated.logger().info("Deleted the instancing world");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Helper method to copy the world-folder.
     * @param source Source-File
     * @param target Target-File
     *
     * @return true if it had success
     */
    public static boolean copyFolder(File source, File target) {
        return copyFolder(source, target, null);
    }
    /**
     * Helper method to copy the world-folder.
     * @param source Source-File
     * @param target Target-File
     * @param excludeFiles files to ignore and not copy over to Target-File
     *
     * @return true if it had success
     */
    public static boolean copyFolder(File source, File target, List<String> excludeFiles) {
        Path sourceDir = source.toPath();
        Path targetDir = target.toPath();

        try {
            Files.walkFileTree(sourceDir, new CopyDirFileVisitor(sourceDir, targetDir, excludeFiles));
            return true;
        } catch (IOException e) {
            Instantiated.instance().getLogger().warning("Unable to copy directory " + e.getMessage());
            return false;
        }
    }
    private static class CopyDirFileVisitor extends SimpleFileVisitor<Path> {

        private final Path sourceDir;
        private final Path targetDir;
        private final List<String> excludeFiles;

        private CopyDirFileVisitor(Path sourceDir, Path targetDir, List<String> excludeFiles) {
            this.sourceDir = sourceDir;
            this.targetDir = targetDir;
            this.excludeFiles = excludeFiles;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path newDir = targetDir.resolve(sourceDir.relativize(dir));
            if (!Files.isDirectory(newDir)) {
                Files.createDirectory(newDir);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            // Pass files that are set to ignore
            if (excludeFiles != null && excludeFiles.contains(file.getFileName().toString()))
                return FileVisitResult.CONTINUE;
            // Copy the files
            Path targetFile = targetDir.resolve(sourceDir.relativize(file));
            Files.copy(file, targetFile, COPY_ATTRIBUTES);
            return FileVisitResult.CONTINUE;
        }
    }
}
