package dev.ebullient.convert.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public class MarkdownDoclet implements Doclet {
    Pattern preformattedText = Pattern.compile("```|<pre>");

    Reporter reporter;
    DocletEnvironment environment;
    Path outputDirectory;
    Path currentResource;
    Set<PackageElement> packages;
    Map<String, String> classNameMapping = new HashMap<>();
    /** A map of unqualified class names to their qualified class name. */
    Map<String, String> qualifiedClassNameMapping = new HashMap<>();

    MarkdownOption targetDir = new MarkdownOption() {
        String value;

        @Override
        public int getArgumentCount() {
            return 2;
        }

        @Override
        public String getDescription() {
            return "The target output directory.";
        }

        @Override
        public Kind getKind() {
            return Kind.OTHER;
        }

        @Override
        public List<String> getNames() {
            return List.of("-d", "--destination");
        }

        @Override
        public String getParameters() {
            return "directory";
        }

        public String getValue() {
            return (value != null) ? value : "docs/templates/";
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            value = arguments.get(0);
            return true;
        }
    };

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of(targetDir);
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        try {
            System.out.println("TTRPG Convert Cli Markdown Doclet: run begin");
            System.out.println("target: " + targetDir.getValue());
            processFiles(environment);
            System.out.println("TTRPG Convert Cli Markdown Doclet: run end");
        } catch (final Exception e) {
            reporter.print(Diagnostic.Kind.ERROR, e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Process classes and create markdown files.
     *
     * @throws IOException
     */
    protected void processFiles(DocletEnvironment environment) throws IOException {
        this.environment = environment;
        DocTrees docTrees = environment.getDocTrees();

        outputDirectory = Paths.get(targetDir.getValue());
        if (!Files.exists(outputDirectory)) {
            Files.createDirectories(outputDirectory);
        }
        reporter.print(Diagnostic.Kind.NOTE, "Writing to " + outputDirectory.toAbsolutePath());

        Set<? extends Element> elements = environment.getIncludedElements();

        // Find TOP_LEVEL elements that enclose (interesting) others
        ElementFilter.typesIn(elements).stream()
                .filter(t -> isQute(t)) // only include template-related classes
                .filter(t -> !isIgnored(t)) // skip @JavadocIgnore and Builder classes
                .filter(t -> t.getNestingKind() != NestingKind.TOP_LEVEL) // find nested elements
                .filter(t -> t.getKind() != ElementKind.INTERFACE) // skip inner interfaces
                .map(TypeElement::getEnclosingElement) // map to enclosing element
                .distinct() // remove duplicates
                .forEach(te -> {
                    // Append "README" to the class name to generate a README file
                    // inside the directory for GH-based documentation
                    String reference = te.toString();
                    classNameMapping.put(reference, reference + ".README");
                });

        // Print package indexes (README.md)
        packages = ElementFilter.packagesIn(elements);
        for (PackageElement p : packages) {
            if (isQute(p) && !isIgnored(p)) {
                writeReadmeFile(docTrees, p);
            }
        }

        for (TypeElement t : ElementFilter.typesIn(elements)) {
            if (!isQute(t) || isExcluded(t)) {
                continue;
            }
            String mapping = classNameMapping.get(t.getQualifiedName().toString());
            System.out.println(
                    t.getKind().toString().substring(0, 4)
                            + "\t" + t.getQualifiedName()
                            + (mapping == null ? "" : "\n\t-- " + mapping));
            writeReferenceFile(docTrees, t);
        }
    }

    private void debugFile(String type, Name name, Path outFile) {
        // String out = outFile.toString().replace(targetDir.getValue(), "");
        // System.out.println(type + ", " + name.toString() + " --> " + out);
    }

    protected void writeReferenceFile(DocTrees docTrees, TypeElement t) throws IOException {
        String name = t.getSimpleName().toString();
        if (name.contains("Builder")) {
            return;
        }
        Path outFile = getOutputFile(t);
        debugFile("reference", t.getQualifiedName(), outFile);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outFile))) {
            Aggregator aggregator = new Aggregator();
            aggregator.add("# " + name + "\n\n");

            // Add class description
            aggregator.addFullBody(docTrees.getDocCommentTree(t));

            // Add class attributes and methods
            Map<String, Element> members = new TreeMap<>();
            for (Element e : t.getEnclosedElements()) {
                processElement(docTrees, members, e);
            }

            // Include attributes and methods from superclass
            TypeElement superType = getSuperclassElement(t);
            while (superType != null) {
                for (Element e : superType.getEnclosedElements()) {
                    processElement(docTrees, members, e);
                }
                superType = getSuperclassElement(superType);
            }

            aggregator.add("\n\n## Attributes\n\n");
            aggregator.add(members.keySet().stream()
                    .map(s -> String.format("[%s](#%s)", s, s.toLowerCase()))
                    .collect(Collectors.joining(", ")));
            aggregator.add("\n\n");

            Map<String, List<? extends DocTree>> recordContent = new HashMap<>();
            if (t.getKind() == ElementKind.RECORD) {
                // If it's a record, then we can't retrieve the attributes as Elements, so we have to parse them from
                // the comment tree instead.
                docTrees.getDocCommentTree(t)
                        .getBlockTags().stream()
                        .filter(e -> e.getKind() == DocTree.Kind.PARAM)
                        .map(param -> (ParamTree) param)
                        .filter(p -> !p.getName().toString().startsWith("_")) // fields with "_" prefix are internal
                        .forEach(param -> {
                            recordContent.put(param.getName().toString(), param.getDescription());
                        });
            }

            for (Map.Entry<String, Element> entry : members.entrySet()) {
                aggregator.add("\n\n### " + entry.getKey() + "\n\n");
                var content = recordContent.get(entry.getKey());
                if (content != null) {
                    aggregator.addAll(content);
                } else {
                    aggregator.addFullBody(docTrees.getDocCommentTree(entry.getValue()));
                }
            }

            out.println(aggregator);
            out.flush();
        }
    }

    protected void processElement(DocTrees docTrees, Map<String, Element> members, Element e) {
        String name = e.getSimpleName().toString();
        ElementKind kind = e.getKind();

        if (isIgnored(e)) {
            // Return early if the element is annotated with @JavadocIgnore
            return;
        }

        if (!isIncludedVerbatim(e)) {
            // If the element is not annotated with @JavadocVerbatim,
            // filter and format the element name

            if (!e.getModifiers().stream().anyMatch(m -> m == Modifier.PUBLIC)
                    || e.getModifiers().stream().anyMatch(m -> m == Modifier.STATIC)) {
                // Skip non-public and static elements
                return;
            }
            if (kind == ElementKind.METHOD) {
                if (!name.startsWith("get") && !name.startsWith("is")) {
                    // Skip methods that don't start with "get" or "is"
                    return;
                }
                if (e.getAnnotation(Deprecated.class) != null) {
                    // Skip deprecated methods
                    return;
                }

                name = name.replaceFirst("(get|is)", "");
                name = name.substring(0, 1).toLowerCase() + name.substring(1);
            } else if (!kind.isField() && kind != ElementKind.RECORD_COMPONENT) {
                // Skip any other non-field elements
                return;
            }
        }

        members.put(name, e);
    }

    void writeReadmeFile(DocTrees docTrees, PackageElement p) throws IOException {
        Path outFile = getOutputFile(p);
        debugFile("readme", p.getQualifiedName(), outFile);

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outFile))) {
            Aggregator aggregator = new Aggregator();

            // Add package description
            aggregator.addFullBody(docTrees.getDocCommentTree(p));

            // Make list linking to package members
            Map<String, TypeElement> members = new TreeMap<>();
            for (Element e : p.getEnclosedElements()) {
                TypeElement te = (TypeElement) e;
                if (isExcluded(e)) {
                    continue;
                }
                if (te.getKind() == ElementKind.INTERFACE) {
                    continue;
                }
                members.put(te.getSimpleName().toString(), te);
            }

            Aggregator elements = new Aggregator();
            members.values().forEach(te -> {
                var ref = classNameMapping.get(te.getQualifiedName().toString());
                if (ref == null) {
                    elements.add("- [" + te.getSimpleName() + "](" + te.getSimpleName() + ".md)"
                            + getDescription(docTrees, te) + "\n");
                } else {
                    elements.add("- [" + te.getSimpleName() + "](" + te.getSimpleName() + "/README.md)"
                            + getDescription(docTrees, te) + "\n");
                }
            });

            String result = elements.toString();
            if (!result.isEmpty()) {
                aggregator.add("\n\n## References\n\n");
                aggregator.add(result);
            }
            out.println(aggregator.toString());
            out.flush();
        }
    }

    private boolean isQute(QualifiedNameable e) {
        return e.getQualifiedName().toString().contains("qute");
    }

    boolean isIgnored(Element element) {
        return element.getAnnotation(JavadocIgnore.class) != null
                || element.getSimpleName().toString().contains("Builder");
    }

    boolean isIncludedVerbatim(Element element) {
        return element.getAnnotation(JavadocVerbatim.class) != null;
    }

    boolean isExcluded(Element element) {
        if (isIncludedVerbatim(element)) {
            return false;
        }

        boolean excludeKind = switch (element.getKind()) {
            case CLASS, INTERFACE, RECORD, ENUM -> false;
            default -> true;
        };

        return !environment.isIncluded(element)
                || isIgnored(element)
                || excludeKind;
    }

    String getDescription(DocTrees docTrees, TypeElement te) {
        DocCommentTree docCommentTree = docTrees.getDocCommentTree(te);
        if (docCommentTree != null) {
            Aggregator aggregator = new Aggregator();
            aggregator.addAll(docCommentTree.getFirstSentence());
            return ": " + aggregator.toString().replaceAll("#+ ", "").trim();
        }
        return "";
    }

    Path getOutputFile(QualifiedNameable element) throws IOException {
        Path outFile = currentResource = outputDirectory.resolve(qualifiedNameToPath(element));
        if (!Files.exists(outFile.getParent())) {
            Files.createDirectories(outFile.getParent());
        }
        reporter.print(Diagnostic.Kind.NOTE, "Writing " + outFile.toAbsolutePath());
        return outFile;
    }

    static TypeElement getSuperclassElement(TypeElement typeElement) {
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind().equals(TypeKind.NONE)) {
            return null;
        }
        if (superclass.toString().equals("java.lang.Object")) {
            return null;
        }
        if (superclass.toString().equals("java.lang.Record")) {
            return null;
        }
        return (TypeElement) ((DeclaredType) superclass).asElement();
    }

    String qualifiedNameToPath(QualifiedNameable element) {
        return qualifiedNameToPath(element.getQualifiedName().toString());
    }

    String qualifiedNameToPath(String reference) {
        reference = classNameMapping.getOrDefault(reference, reference);
        if (reference.endsWith("qute")) {
            reference += ".README";
        } else if (!isValidClass(reference.replace(".README", ""))) {
            // Check if the reference is a valid class
            throw new RuntimeException("Invalid class reference: " + reference);
        }
        return reference
                .replace("dev.ebullient.convert.", "")
                .replace("tools.", "")
                .replace("qute.", "")
                .replace(".", "/")
                + ".md";
    }

    static interface MarkdownOption extends Option {
        String getValue();
    }

    class Aggregator {
        List<String> content = new ArrayList<>();
        Deque<HtmlElement> htmlEntity = new ArrayDeque<>();

        void addFullBody(DocCommentTree docCommentTree) {
            if (docCommentTree == null) {
                return;
            }
            addAll(docCommentTree.getFullBody());
        }

        void addAll(List<? extends DocTree> docTrees) {
            if (docTrees == null) {
                return;
            }
            for (DocTree docTree : docTrees) {
                add(docTree);
            }
        }

        void add(DocTree docTree) {
            switch (docTree.getKind()) {
                case TEXT:
                    // Always remove single leading javadoc space
                    String text = ((TextTree) docTree).getBody().toString()
                            .replaceAll("\n ", "\n")
                            .replaceAll("\n\n\n", "\n\n"); // consolidate extra lines

                    Matcher m = preformattedText.matcher(text);
                    if (!m.find()) {
                        // if there isn't any pre-formatted text, remove any other leading whitespace
                        text = text.replaceAll("\n +", "\n");
                    }
                    add(text);
                    break;
                case CODE:
                case LITERAL:
                    add("`" + ((LiteralTree) docTree).getBody().toString() + "`");
                    break;
                case START_ELEMENT:
                    startEntity(docTree.toString());
                    break;
                case END_ELEMENT:
                    endEntity(docTree.toString());
                    break;
                case LINK:
                    LinkTree linkTree = (LinkTree) docTree;
                    String reference = linkTree.getReference().toString();

                    // look at label before anchor has been removed from reference
                    String label = linkTree.getLabel().toString();
                    if (label == null || label.isEmpty()) {
                        int classBegin = reference.lastIndexOf(".");
                        label = classBegin > -1 ? reference.substring(classBegin + 1) : reference;
                    }

                    // remove anchor from the class reference
                    String anchor = "";
                    int hash = reference.indexOf("#");
                    if (hash > -1) {
                        anchor = reference.substring(hash);
                        reference = reference.substring(0, hash);
                    }

                    reference = maybeGetQualifiedName(reference);
                    // resolve the class reference to a path (and then make that link relative)
                    reference = qualifiedNameToPath(reference);
                    if (!reference.startsWith("http")) {
                        Path target = outputDirectory.resolve(reference);
                        if (target.equals(currentResource)) {
                            reference = "";
                        } else {
                            Path relative = currentResource.getParent().relativize(target);
                            reference = relative.toString();
                        }
                        anchor = anchor
                                .replaceFirst("^#(get|is)", "#")
                                .replace("()", "").toLowerCase();
                        label = label
                                .replaceFirst("#(get|is)", "#")
                                .replace("()", "");
                    }
                    add(String.format("[%s](%s%s)", label, reference, anchor));
                    break;
                case ENTITY:
                    add(replacementFor(docTree.toString()));
                    break;
                default:
                    System.out.println("tree kind: " + docTree.getKind() + ", content: " + docTree);
                    break;
            }
        }

        /** Try to get a qualified name for the reference. If we can't, then return the reference unchanged. */
        private String maybeGetQualifiedName(String reference) {
            Elements elemUtil = environment.getElementUtils();
            if (reference.startsWith("dev.ebullient.convert")) {
                return reference;
            }
            // If we've already seen this before, then retrieve that name
            if (qualifiedClassNameMapping.containsKey(reference)) {
                return qualifiedClassNameMapping.get(reference);
            }
            // Try to get a unique qualified name from the reference
            List<String> qualifiedNames = packages.stream()
                    .map(pkg -> elemUtil.getTypeElement(pkg.getQualifiedName().toString() + "." + reference))
                    .filter(Objects::nonNull)
                    .map(e -> e.getQualifiedName().toString())
                    .toList();
            // If we can't get a single unique name, then end it here and just return the original reference
            if (qualifiedNames.size() != 1) {
                return reference;
            }
            qualifiedClassNameMapping.put(reference, qualifiedNames.get(0));
            return qualifiedNames.get(0);
        }

        void add(String text) {
            if (htmlEntity.isEmpty()) {
                content.add(text.replaceAll(" +", " "));
            } else {
                htmlEntity.peek().add(text);
            }
        }

        void startEntity(String text) {
            HtmlElement element = new HtmlElement(text);
            if (!htmlEntity.isEmpty()) {
                element.testOuter(htmlEntity.peek());
            }
            htmlEntity.push(element);
        }

        void endEntity(String text) {
            HtmlElement element = htmlEntity.pop();
            element.end(text);
            add(element.sanitize());
        }

        @Override
        public String toString() {
            while (!htmlEntity.isEmpty()) {
                endEntity("");
            }
            return String.join("", content)
                    .replaceAll("(?m) +$", "")
                    .replaceAll("</br/>", "")
                    .replace("\n\n\n", "\n\n")
                    .replaceAll("<br ?/?>", "  \n") // do this late.
                    .trim();
        }
    }

    static class HtmlElement {
        List<String> html = new ArrayList<>();
        String li = "";
        String indent = "";
        String tag;

        public HtmlElement(String text) {
            html.add(text);
            tag = text.replaceAll("<([^ >]+).*", "$1");
            if (tag.equalsIgnoreCase("ol")) {
                li = "1. ";
            } else if (tag.equalsIgnoreCase("ul")) {
                li = "- ";
            }
        }

        public void testOuter(HtmlElement peek) {
            boolean listBegin = tag.equalsIgnoreCase("ol") || tag.equalsIgnoreCase("ul");
            if (listBegin && peek != null && !peek.indent.isEmpty()) {
                indent = peek.indent + "  ";
            }
            if (tag.equals("li")) {
                li = peek == null ? "- " : peek.li;
            }
        }

        public void add(String text) {
            html.add(text);
        }

        public void end(String text) {
            if (text.isEmpty()) {
                html.add("</" + tag + ">");
            }
            html.add(text);
        }

        public String sanitize() {
            return String.join("", html)
                    .replaceAll("</?tt>", "`")
                    .replaceAll("</?b>", "**")
                    .replaceAll("</?i>", "*")
                    .replaceAll("<h1>", "# ")
                    .replaceAll("<h2>", "## ")
                    .replaceAll("<h3>", "### ")
                    .replaceAll("<(p|ol|ul)>\\s*", "\n\n")
                    .replaceAll("</(ol|ul|h.)>", "\n")
                    .replaceAll("</(li|p)>\\s*", "")
                    .replaceAll("<li>", "\n" + indent + li)
                    .replaceAll("<a href=\"(.*)\">(.*)</a>", "[$2]($1)");
        }
    }

    public static void main(String[] args) {
        String docletName = MarkdownDoclet.class.getName();

        String[] docletArgs = new String[] {
                "-doclet", docletName,
                "-docletpath", "target/classes/",
                "-sourcepath", "src/main/java/",
                "dev.ebullient.convert.qute",
                "dev.ebullient.convert.tools.dnd5e.qute",
                "dev.ebullient.convert.tools.pf2e.qute"
        };
        DocumentationTool docTool = ToolProvider.getSystemDocumentationTool();
        docTool.run(System.in, System.out, System.err, docletArgs);
    }

    private String replacementFor(String str) {
        switch (str) {
            case "&quot;":
                return "\"";
            case "&amp;":
                return "&";
            case "&#39;":
                return "'";
            case "&lt;":
                return "<";
            case "&gt;":
                return ">";
            default:
                return str;
        }
    }

    private boolean isValidClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            int pos = className.lastIndexOf(".");
            if (pos > -1) {
                String innerClass = className.substring(0, pos) + "$" + className.substring(pos + 1);
                return isValidClass(innerClass);
            }
            return false;
        }
    }
}
