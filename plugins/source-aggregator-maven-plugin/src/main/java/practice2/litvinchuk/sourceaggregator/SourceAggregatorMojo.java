package practice2.litvinchuk.sourceaggregator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Mojo, що збирає всі {@code .java}-файли проєкту в один файл.
 *
 * <p>Goal: {@code source-aggregator:aggregate}.</p>
 *
 * <p>За замовчуванням обробляє {@code src/main/java}; за потреби можна
 * увімкнути додавання тестів параметром {@code includeTests=true}.</p>
 */
@Mojo(name = "aggregate",
        defaultPhase = LifecyclePhase.PACKAGE,
        threadSafe = true)
public class SourceAggregatorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** Корінь основних джерел; за замовчуванням — стандартна директорія проєкту. */
    @Parameter(defaultValue = "${project.basedir}/src/main/java", property = "aggregator.sourceDir")
    private String sourceDir;

    /** Корінь тестових джерел. */
    @Parameter(defaultValue = "${project.basedir}/src/test/java", property = "aggregator.testSourceDir")
    private String testSourceDir;

    /** Файл, у який буде записаний агрегований код. */
    @Parameter(defaultValue = "${project.build.directory}/aggregated-sources.java",
            property = "aggregator.outputFile")
    private String outputFile;

    /** Чи включати тести у вивід. */
    @Parameter(defaultValue = "false", property = "aggregator.includeTests")
    private boolean includeTests;

    /** Скільки '=' використовувати для розділювача між файлами. */
    @Parameter(defaultValue = "80")
    private int separatorWidth;

    @Override
    public void execute() throws MojoExecutionException {
        Path output = Path.of(outputFile);
        try {
            Files.createDirectories(output.getParent());
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot create output directory: " + output.getParent(), e);
        }

        List<Path> mainSources = collectJavaFiles(Path.of(sourceDir));
        List<Path> testSources = includeTests ? collectJavaFiles(Path.of(testSourceDir))
                : List.of();

        getLog().info("Aggregating " + mainSources.size() + " main sources"
                + (includeTests ? " and " + testSources.size() + " test sources" : "")
                + " into " + output);

        try {
            Files.writeString(output, buildHeader(mainSources.size(), testSources.size()),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            appendSection(output, "MAIN SOURCES (" + sourceDir + ")", Path.of(sourceDir), mainSources);
            if (includeTests) {
                appendSection(output, "TEST SOURCES (" + testSourceDir + ")",
                        Path.of(testSourceDir), testSources);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write aggregated file " + output, e);
        }

        getLog().info("Aggregated source written to " + output
                + " (" + safeSize(output) + " bytes)");
    }

    private List<Path> collectJavaFiles(Path root) throws MojoExecutionException {
        if (!Files.exists(root)) {
            getLog().warn("Source directory does not exist, skipping: " + root);
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> list = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(list::add);
            return list;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to walk " + root, e);
        }
    }

    private void appendSection(Path output, String title, Path root, List<Path> files) throws IOException {
        String sep = "=".repeat(separatorWidth);
        StringBuilder section = new StringBuilder();
        section.append('\n').append(sep).append('\n')
                .append("// ").append(title).append('\n')
                .append(sep).append("\n\n");
        Files.writeString(output, section, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        for (Path file : files) {
            String rel = root.relativize(file).toString();
            StringBuilder header = new StringBuilder();
            header.append("// ").append("-".repeat(separatorWidth - 3)).append('\n')
                    .append("// FILE: ").append(rel).append('\n')
                    .append("// ").append("-".repeat(separatorWidth - 3)).append('\n');
            Files.writeString(output, header, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            Files.writeString(output, Files.readString(file, StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            Files.writeString(output, "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
    }

    private String buildHeader(int mainCount, int testCount) {
        String sep = "=".repeat(separatorWidth);
        StringBuilder sb = new StringBuilder();
        sb.append(sep).append('\n');
        sb.append("// Aggregated source code\n");
        sb.append("// Project   : ").append(project.getGroupId())
                .append(':').append(project.getArtifactId()).append(':')
                .append(project.getVersion()).append('\n');
        sb.append("// Generated : ").append(LocalDateTime.now()).append('\n');
        sb.append("// Files     : ").append(mainCount).append(" main");
        if (includeTests) {
            sb.append(" + ").append(testCount).append(" test");
        }
        sb.append('\n').append(sep).append('\n');
        return sb.toString();
    }

    private long safeSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return -1;
        }
    }
}
