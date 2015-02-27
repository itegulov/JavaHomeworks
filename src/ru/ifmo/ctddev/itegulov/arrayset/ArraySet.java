package ru.ifmo.ctddev.itegulov.arrayset;

import java.util.*;

/**
 * @author Daniyar Itegulov
 * @since 19.02.15
 */
public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private static class ReversedUnmodifiableList<T> extends AbstractList<T> implements RandomAccess {
        private final List<T> list;

        public static <T> List<T> getInstance(List<T> list) {
            return Collections.unmodifiableList(new ReversedUnmodifiableList<>(list));
        }

        private ReversedUnmodifiableList(List<T> list) {
            this.list = list;
        }

        @Override
        public T get(int index) {
            return list.get(list.size() - 1 - index);
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            return new ReversedUnmodifiableList<>(list.subList(list.size() - toIndex, list.size() - fromIndex));
        }
    }

    private final List<T> list;
    private Comparator<T> comparator;
    private Comparator<T> equalsComparator = (x, y) -> {
        if (comparator.compare(x, y) == 0) {
            return 1;
        }
        return comparator.compare(x, y);
    };
    private boolean isNatural = false;

    public ArraySet() {
        this(new ArrayList<>());
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, (x, y) -> {
            @SuppressWarnings("unchecked")
            Comparable<? super T> comparable = (Comparable<? super T>) x;
            return comparable.compareTo(y);
        });
        isNatural = true;
    }

    @SuppressWarnings("unchecked")
    public ArraySet(Collection<? extends T> collection, Comparator<T> comparator) {
        TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        list = Collections.unmodifiableList(Arrays.asList((T[]) treeSet.toArray()));
        this.comparator = comparator;
    }

    private ArraySet(List<T> list, Comparator<T> comparator, boolean isNatural) {
        this.list = list;
        this.comparator = comparator;
        this.isNatural = isNatural;
    }

    private int binSearch(T key, Comparator<? super T> comparator) {
        int index = Collections.binarySearch(list, key, comparator);
        if (index < 0) {
            return -index - 2;
        }
        return index;
    }

    private T get(int pos) {
        if (pos >= 0 && pos <= list.size() - 1) {
            return list.get(pos);
        } else {
            return null;
        }
    }

    @Override
    public T lower(T t) {
        return get(binSearch(t, equalsComparator));
    }

    @Override
    public T floor(T t) {
        return get(binSearch(t, comparator));
    }

    @Override
    public T ceiling(T t) {
        return get(binSearch(t, equalsComparator) + 1);
    }

    @Override
    public T higher(T t) {
        return get(binSearch(t, comparator) + 1);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("pollFirst");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("pollLast");
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(ReversedUnmodifiableList.getInstance(list), Collections.reverseOrder(comparator), isNatural);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return new Iterator<T>() {
            private ListIterator<T> iterator = list.listIterator(list.size());

            @Override
            public boolean hasNext() {
                return iterator.hasPrevious();
            }

            @Override
            public T next() {
                return iterator.previous();
            }
        };
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return headSet(toElement, toInclusive).tailSet(fromElement, fromInclusive);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        int toIndex = binSearch(toElement, comparator);
        if (!inclusive && toIndex != -1 && comparator.compare(toElement, list.get(toIndex)) == 0) {
            toIndex--;
        }
        return new ArraySet<>(list.subList(0, toIndex + 1), comparator, isNatural);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int fromIndex = binSearch(fromElement, equalsComparator) + 1;
        if (fromIndex == -1 || fromIndex != list.size() &&
                (!inclusive && comparator.compare(fromElement, list.get(fromIndex)) == 0)) {
            fromIndex++;
        }
        return new ArraySet<>(list.subList(fromIndex, list.size()), comparator, isNatural);
    }

    @Override
    public Comparator<? super T> comparator() {
        return isNatural ? null : comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (list.isEmpty()) {
            throw new NoSuchElementException();
        }
        return list.get(0);
    }

    @Override
    public T last() {
        if (list.isEmpty()) {
            throw new NoSuchElementException();
        }
        return list.get(list.size() - 1);
    }

    @Override
    public int size() {
        return list.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(list, (T) o, comparator) >= 0;
    }
}