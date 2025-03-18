package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joiningNonEmpty;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import dev.ebullient.convert.tools.Tags;

/** A collection of traits stored as {@link QuteDataRef}s to the trait note. */
public class QuteDataTraits implements Collection<QuteDataRef> {
    private final Set<QuteDataRef> refs = new TreeSet<>();

    public QuteDataTraits() {}

    /** The first trait which has this category, as a {@link QuteDataRef}. Case-sensitive. */
    public QuteDataRef getFirst(String category) {
        return refs.stream().filter(ref -> ref.title() != null && ref.title().contains(category)).findFirst().orElse(null);
    }

    /** Return these traits as a comma-delimited string without any extra formatting (eg no title attributes). */
    public String formattedWithoutTitles() {
        return refs.stream().map(QuteDataRef::withoutTitle).collect(joiningNonEmpty(", "));
    }

    /** Return traits without any size, alignment, or rarity traits. */
    public QuteDataTraits getGenericTraits() {
        return refs.stream()
            .filter(ref -> !isPresent(ref.title()) ||
                !(ref.title().contains("Alignment") || ref.title().contains("Size") || ref.title().contains("Rarity")))
            .collect(Collectors.toCollection(QuteDataTraits::new));
    }

    public QuteDataTraits addToTags(Tags tags) {
        for (QuteDataRef ref : this) {
            tags.add("trait", ref.displayText());
        }
        return this;
    }

    public QuteDataTraits addTrait(String trait) {
        if (isPresent(trait)) {
            refs.add(new QuteDataRef(trait));
        }
        return this;
    }

    public QuteDataTraits addTraits(Collection<String> c) {
        c.forEach(this::addTrait);
        return this;
    }

    @Override
    public String toString() {
        return join(", ", refs);
    }

    @Override
    public int size() {
        return refs.size();
    }

    @Override
    public boolean isEmpty() {
        return refs.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof QuteDataRef) {
            return refs.contains(o);
        } else if (o instanceof String) {
            return refs.stream().anyMatch(ref -> ref.displayText().equals(o));
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof QuteDataRef) {
            return refs.remove(o);
        } else if (o instanceof String) {
            Optional<QuteDataRef> ref = refs.stream().filter(r -> r.displayText().equals(o)).findAny();
            if (ref.isEmpty()) {
                return false;
            }
            return refs.remove(ref);
        }
        return false;
    }

    @Override
    public boolean add(QuteDataRef quteDataRef) {
        return refs.add(quteDataRef);
    }

    @Override
    public Iterator<QuteDataRef> iterator() {
        return refs.iterator();
    }

    @Override
    public Object[] toArray() {
        return refs.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return refs.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(Collection<? extends QuteDataRef> c) {
        return c.stream().allMatch(this::add);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return c.stream().allMatch(this::remove);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return refs.removeIf(ref -> !c.contains(ref) && !c.contains(ref.displayText()));
    }

    @Override
    public void clear() {
        refs.clear();
    }
}
