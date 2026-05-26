package practice2.litvinchuk.codestats;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Mojo, що генерує Markdown-звіт про Java-код проєкту: кількість файлів,
 * класів, інтерфейсів, енумів, методів, рядків коду; розподіл по пакетах;
 * топ-N найбільших класів.
 *
 * <p>Goal: {@code code-stats:report}.</p>
 *
 * <p>Це навмисно простий "regex-based" аналізатор — щоб не тягнути сторонні
 * парсери Java і залишатись чистим прикладом написання Maven Mojo.</p>
 */
@Mojo(name = "report",
        defaultPhase = LifecyclePhase.VERIFY,
        threadSafe = true)
public class CodeStatsMojo extends AbstractMojo {

    private static final Pattern PKG = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern CLASS_DECL = Pattern.compile(
            "(?m)^\\s*(?:public\\s+|private\\s+|protected\\s+|abstract\\s+|final\\s+|static\\s+)*"
                    + "(class|interface|enum)\\s+([A-Za-z_][\\w]*)");
    private static final Pattern METHOD_DECL = Pattern.compile(
            "(?m)^\\s*(?:public|private|protected|static|final|synchronized|native|abstract|default)"
                    + "[\\w<>,?\\s\\[\\]]*\\s+([A-Za-z_][\\w]*)\\s*\\([^;]*\\)\\s*(?:throws[\\w,\\s.]+)?\\s*\\{");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.basedir}/src/main/java", property = "stats.sourceDir")
    private String sourceDir;

    @Parameter(defaultValue = "${project.build.directory}/code-stats.md", property = "stats.outputFile")
    private String outputFile;

    /** Скільки топових (за рядками коду) класів виводити в звіт. */
    @Parameter(defaultValue = "10", property = "stats.topN")
    private int topN;

    @Override
    public void execute() throws MojoExecutionException {
        Path root = Path.of(sourceDir);
        if (!Files.exists(root)) {
            getLog().warn("Source directory does not exist: " + root + ". Skipping.");
            return;
        }

        List<FileStat> stats = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                if (Files.isRegularFile(p) && p.getFileName().toString().endsWith(".java")) {
                    stats.add(analyze(p, root));
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to walk " + root, e);
        }

        String md = renderReport(stats);
        Path output = Path.of(outputFile);
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, md, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write report " + output, e);
        }

        getLog().info("Code stats report written to " + output);
        getLog().info(String.format("Files=%d, classes=%d, methods=%d, lines=%d",
                stats.size(), totalTypes(stats), totalMethods(stats), totalLines(stats)));
    }

    private FileStat analyze(Path file, Path root) throws MojoExecutionException {
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read " + file, e);
        }
        String stripped = stripCommentsAndStrings(text);

        FileStat s = new FileStat();
        s.relPath = root.relativize(file).toString();

        Matcher pkgMatcher = PKG.matcher(stripped);
        s.pkg = pkgMatcher.find() ? pkgMatcher.group(1) : "<default>";

        Matcher classMatcher = CLASS_DECL.matcher(stripped);
        while (classMatcher.find()) {
            String kind = classMatcher.group(1);
            switch (kind) {
                case "class": s.classes++; break;
                case "interface": s.interfaces++; break;
                case "enum": s.enums++; break;
                default:
            }
        }

        Matcher methodMatcher = METHOD_DECL.matcher(stripped);
        while (methodMatcher.find()) {
            s.methods++;
        }

        s.totalLines = text.split("\\r?\\n", -1).length;
        s.codeLines = (int) text.lines().filter(l -> !l.isBlank()).count();
        return s;
    }

    /**
     * Дуже спрощений stripper: вирізає однорядкові коментарі, блокові коментарі
     * та вміст String-літералів (зокрема '{', '}', ключові слова всередині),
     * щоб регекси-аналізатори не плутались.
     */
    private static String stripCommentsAndStrings(String src) {
        StringBuilder out = new StringBuilder(src.length());
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                while (i < n && src.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) i++;
                i += 2;
            } else if (c == '"') {
                out.append('"');
                i++;
                while (i < n && src.charAt(i) != '"') {
                    if (src.charAt(i) == '\\' && i + 1 < n) i++;
                    i++;
                }
                out.append('"');
                if (i < n) i++;
            } else if (c == '\'') {
                out.append('\'');
                i++;
                while (i < n && src.charAt(i) != '\'') {
                    if (src.charAt(i) == '\\' && i + 1 < n) i++;
                    i++;
                }
                out.append('\'');
                if (i < n) i++;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private String renderReport(List<FileStat> stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Code Stats Report\n\n");
        sb.append("- Project: **").append(project.getGroupId()).append(':')
                .append(project.getArtifactId()).append(':').append(project.getVersion())
                .append("**\n");
        sb.append("- Source directory: `").append(sourceDir).append("`\n");
        sb.append("- Generated: ").append(LocalDateTime.now()).append("\n\n");

        sb.append("## Totals\n\n");
        sb.append("| Metric | Value |\n|---|---|\n");
        sb.append("| Files | ").append(stats.size()).append(" |\n");
        sb.append("| Classes | ").append(stats.stream().mapToInt(s -> s.classes).sum()).append(" |\n");
        sb.append("| Interfaces | ").append(stats.stream().mapToInt(s -> s.interfaces).sum()).append(" |\n");
        sb.append("| Enums | ").append(stats.stream().mapToInt(s -> s.enums).sum()).append(" |\n");
        sb.append("| Methods | ").append(totalMethods(stats)).append(" |\n");
        sb.append("| Total lines | ").append(totalLines(stats)).append(" |\n");
        sb.append("| Non-blank lines | ").append(stats.stream().mapToInt(s -> s.codeLines).sum())
                .append(" |\n\n");

        sb.append("## Per-package breakdown\n\n");
        Map<String, PkgAgg> byPkg = new TreeMap<>();
        for (FileStat s : stats) {
            byPkg.computeIfAbsent(s.pkg, k -> new PkgAgg()).add(s);
        }
        sb.append("| Package | Files | Types | Methods | Code lines |\n");
        sb.append("|---|---:|---:|---:|---:|\n");
        for (Map.Entry<String, PkgAgg> e : byPkg.entrySet()) {
            PkgAgg a = e.getValue();
            sb.append("| `").append(e.getKey()).append("` | ")
                    .append(a.files).append(" | ").append(a.types)
                    .append(" | ").append(a.methods).append(" | ").append(a.codeLines)
                    .append(" |\n");
        }
        sb.append('\n');

        sb.append("## Top ").append(topN).append(" largest files (by non-blank lines)\n\n");
        sb.append("| File | Types | Methods | Code lines |\n|---|---:|---:|---:|\n");
        stats.stream()
                .sorted(Comparator.comparingInt((FileStat s) -> s.codeLines).reversed())
                .limit(topN)
                .forEach(s -> sb.append("| `").append(s.relPath).append("` | ")
                        .append(s.classes + s.interfaces + s.enums)
                        .append(" | ").append(s.methods).append(" | ").append(s.codeLines)
                        .append(" |\n"));
        sb.append('\n');

        return sb.toString();
    }

    private int totalMethods(List<FileStat> stats) {
        return stats.stream().mapToInt(s -> s.methods).sum();
    }

    private int totalTypes(List<FileStat> stats) {
        return stats.stream().mapToInt(s -> s.classes + s.interfaces + s.enums).sum();
    }

    private int totalLines(List<FileStat> stats) {
        return stats.stream().mapToInt(s -> s.totalLines).sum();
    }

    private static class FileStat {
        String relPath;
        String pkg;
        int classes;
        int interfaces;
        int enums;
        int methods;
        int totalLines;
        int codeLines;
    }

    private static class PkgAgg {
        int files;
        int types;
        int methods;
        int codeLines;

        void add(FileStat s) {
            files++;
            types += s.classes + s.interfaces + s.enums;
            methods += s.methods;
            codeLines += s.codeLines;
        }
    }
}
