package ru.ifmo.ctddev.itegulov.arrayset;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ArraySetTest {
    private static final Comparator<Integer> NATURAL_ORDER = (o1, o2) -> o1 - o2;

    private static final List<Integer> valuesWithDuplication = generateList(1, 1, 2, 3, 4, 5, 6, 2, 1, 3, 7, 8, 9);
    private static final List<Integer> values = generateList(1, 2, 3, 4, 5, 6, 7, 8, 9);
    
    private static List<Integer> generateList(Integer... values) {
        return Arrays.asList(values);
    }
    
    private static ArraySet<Integer> generateArraySet(Integer... values) {
        return new ArraySet<>(generateList(values));
    }

    private static void assertEqualsSet(SortedSet<Integer> set, List<Integer> expectedData) {
        Assert.assertEquals(expectedData.size(), set.size());
        Object[] toArray = set.toArray();
        assertEquals(expectedData.size(), toArray.length);
        for (int i = 0; i < expectedData.size(); i++) {
            assertEquals(expectedData.get(i), toArray[i]);
        }
    }

    @Test
    public void testEmpty() throws Exception {
        ArraySet<Integer> arraySet = new ArraySet<>();
        assertEquals(0, arraySet.size());
    }

    @Test
    public void testLower() throws Exception {
        ArraySet<Integer> arraySet = generateArraySet(1, 1, 2, 2, 2, 2, 3, 3, 4, 4, 5, 8, 17, 17, 17);
        assertEquals(17, (int) arraySet.lower(2015));
        assertEquals(8, (int) arraySet.lower(17));
        assertEquals(4, (int) arraySet.lower(5));
        assertEquals(3, (int) arraySet.lower(4));
        assertEquals(2, (int) arraySet.lower(3));
        assertEquals(1, (int) arraySet.lower(2));
        assertEquals(null, arraySet.lower(1));
        assertEquals(5, (int) arraySet.lower(6));
        assertEquals(5, (int) arraySet.lower(7));
        assertEquals(5, (int) arraySet.lower(8));
        assertEquals(8, (int) arraySet.lower(9));

        arraySet = generateArraySet();
        assertEquals(null, arraySet.lower(1));
        assertEquals(null, arraySet.lower(-12938));

        arraySet = generateArraySet(1, 15, 30);
        assertEquals(null, arraySet.lower(-3483));
        assertEquals(null, arraySet.lower(1));
        assertEquals(1, (int) arraySet.lower(7));
        assertEquals(1, (int) arraySet.lower(15));
        assertEquals(15, (int) arraySet.lower(20));
        assertEquals(15, (int) arraySet.lower(30));
        assertEquals(30, (int) arraySet.lower(454));

        assertEquals(Integer.valueOf(6), generateArraySet(1, 2, 6).lower(10));
        assertEquals(null, generateArraySet(1, 2, 6).lower(1));
        assertEquals(null, generateArraySet(1, 2, 6).lower(-10));
    }

    @Test
    public void testFloor() throws Exception {
        ArraySet<Integer> arraySet = generateArraySet(1, 1, 2, 2, 2, 2, 3, 3, 4, 4, 5, 8, 17, 17, 17);
        assertEquals(null, arraySet.floor(0));
        assertEquals(1, (int) arraySet.floor(1));
        assertEquals(2, (int) arraySet.floor(2));
        assertEquals(3, (int) arraySet.floor(3));
        assertEquals(4, (int) arraySet.floor(4));
        assertEquals(5, (int) arraySet.floor(5));
        assertEquals(5, (int) arraySet.floor(6));
        assertEquals(5, (int) arraySet.floor(7));
        assertEquals(8, (int) arraySet.floor(8));
        assertEquals(8, (int) arraySet.floor(15));
        assertEquals(17, (int) arraySet.floor(17));
        assertEquals(17, (int) arraySet.floor(231));

        arraySet = generateArraySet();
        assertEquals(null, arraySet.floor(1));
        assertEquals(null, arraySet.floor(-12938));
        
        arraySet = generateArraySet(1, 15, 30);
        assertEquals(null, arraySet.floor(-1239));
        assertEquals(1, (int) arraySet.floor(1));
        assertEquals(1, (int) arraySet.floor(10));
        assertEquals(15, (int) arraySet.floor(15));
        assertEquals(15, (int) arraySet.floor(20));
        assertEquals(30, (int) arraySet.floor(30));
        assertEquals(30, (int) arraySet.floor(100));
    }

    @Test
    public void testCeiling() throws Exception {
        ArraySet<Integer> arraySet = generateArraySet(1, 1, 2, 2, 2, 2, 3, 3, 4, 4, 5, 8, 17, 17, 17);
        assertEquals(1, (int) arraySet.ceiling(0));
        assertEquals(1, (int) arraySet.ceiling(1));
        assertEquals(2, (int) arraySet.ceiling(2));
        assertEquals(3, (int) arraySet.ceiling(3));
        assertEquals(4, (int) arraySet.ceiling(4));
        assertEquals(5, (int) arraySet.ceiling(5));
        assertEquals(8, (int) arraySet.ceiling(6));
        assertEquals(8, (int) arraySet.ceiling(7));
        assertEquals(8, (int) arraySet.ceiling(8));
        assertEquals(17, (int) arraySet.ceiling(15));
        assertEquals(17, (int) arraySet.ceiling(17));
        assertEquals(null, arraySet.ceiling(231));

        arraySet = generateArraySet();
        assertEquals(null, arraySet.ceiling(1));
        assertEquals(null, arraySet.ceiling(-12938));

        arraySet = generateArraySet(1, 15, 30);
        assertEquals(1, (int) arraySet.ceiling(-1239));
        assertEquals(1, (int) arraySet.ceiling(1));
        assertEquals(15, (int) arraySet.ceiling(10));
        assertEquals(15, (int) arraySet.ceiling(15));
        assertEquals(30, (int) arraySet.ceiling(20));
        assertEquals(30, (int) arraySet.ceiling(30));
        assertEquals(null, arraySet.ceiling(100));
    }

    @Test
    public void testHigher() throws Exception {
        ArraySet<Integer> arraySet = generateArraySet(1, 1, 2, 2, 2, 2, 3, 3, 4, 4, 5, 8, 17, 17, 17);
        assertEquals(null, arraySet.higher(2015));
        assertEquals(null, arraySet.higher(17));
        assertEquals(8, (int) arraySet.higher(5));
        assertEquals(5, (int) arraySet.higher(4));
        assertEquals(4, (int) arraySet.higher(3));
        assertEquals(3, (int) arraySet.higher(2));
        assertEquals(2, (int) arraySet.higher(1));
        assertEquals(8, (int) arraySet.higher(6));
        assertEquals(8, (int) arraySet.higher(7));
        assertEquals(17, (int) arraySet.higher(8));
        assertEquals(17, (int) arraySet.higher(9));

        arraySet = generateArraySet();
        assertEquals(null, arraySet.higher(1));
        assertEquals(null, arraySet.higher(-12938));

        arraySet = generateArraySet(1, 15, 30);
        assertEquals(1, (int) arraySet.higher(-3483));
        assertEquals(15, (int) arraySet.higher(1));
        assertEquals(15, (int) arraySet.higher(7));
        assertEquals(30, (int) arraySet.higher(15));
        assertEquals(30, (int) arraySet.higher(20));
        assertEquals(null, arraySet.higher(30));
        assertEquals(null, arraySet.higher(454));

        assertEquals(null, generateArraySet(1, 2, 6).higher(6));
        assertEquals(Integer.valueOf(6), generateArraySet(1, 2, 6).higher(3));
    }

    @Test
    public void testCreationAndToArray() throws Exception {
        ArraySet<Integer> set = new ArraySet<>(values, NATURAL_ORDER);
        assertEqualsSet(set, values);
    }

    @Test
    public void testCreationWithDublication() throws Exception {
        ArraySet<Integer> set = new ArraySet<>(valuesWithDuplication, NATURAL_ORDER);
        assertEqualsSet(set, values);
    }

    @Test
    public void testCreationFromComparable() throws Exception {
        ArraySet<Integer> set = new ArraySet<>(valuesWithDuplication);
        assertEqualsSet(set, values);
    }

    @Test
    public void testSubSet() throws Exception {
        NavigableSet<Integer> set = generateArraySet(1, 3, 6, 7, 9);
        SortedSet<Integer> subSet = set.subSet(3, 7);
        assertEqualsSet(subSet, generateList(3, 6));

        subSet = set.subSet(3, true, 7, false);
        assertEqualsSet(subSet, generateList(3, 6));

        subSet = set.subSet(3, false, 7, false);
        assertEqualsSet(subSet, generateList(6));

        subSet = set.subSet(3, false, 7, true);
        assertEqualsSet(subSet, generateList(6, 7));

        subSet = set.subSet(3, true, 7, true);
        assertEqualsSet(subSet, generateList(3, 6, 7));
    }

    @Test
    public void testHeadSet() throws Exception {
        NavigableSet<Integer> set = generateArraySet(1, 3, 6, 7, 9);
        SortedSet<Integer> subSet = set.headSet(8);
        assertEqualsSet(subSet, generateList(1, 3, 6, 7));

        subSet = set.headSet(7, true);
        assertEqualsSet(subSet, generateList(1, 3, 6, 7));

        subSet = set.headSet(7, false);
        assertEqualsSet(subSet, generateList(1, 3, 6));
    }

    @Test
    public void testTailSet() throws Exception {
        NavigableSet<Integer> set = generateArraySet(1, 3, 6, 7, 9);
        SortedSet<Integer> subSet = set.tailSet(8);
        assertEqualsSet(subSet, generateList(9));
        
        subSet = set.tailSet(7, true);
        assertEqualsSet(subSet, generateList(7, 9));

        subSet = set.tailSet(7, false);
        assertEqualsSet(subSet, generateList(9));
    }

    @Test
    public void testDescending() throws Exception {
        NavigableSet<Integer> set = generateArraySet(1, 3, 6, 7, 9);
        NavigableSet<Integer> descendingSet = set.descendingSet();
        
        assertEqualsSet(descendingSet, generateList(9, 7, 6, 3, 1));
        
        List<Integer> expected = generateList(9, 7, 6, 3, 1);
        int cnt = 0;
        for (Iterator<Integer> iterator = set.descendingIterator(); iterator.hasNext();) {
            Integer i = iterator.next();
            assertEquals(expected.get(cnt++), i);
        }
    }

    @Test
    public void testDescendingSet() throws Exception {
        NavigableSet<Integer> set = generateArraySet(1, 3, 6, 7, 9);
        NavigableSet<Integer> descendingSet = set.descendingSet();

        List<Integer> expected = generateList(9, 7, 6, 3, 1);
        int cnt = 0;
        for (Iterator<Integer> iterator = descendingSet.iterator(); iterator.hasNext();) {
            Integer i = iterator.next();
            assertEquals(expected.get(cnt++), i);
        }

        for (Iterator<Integer> iterator = descendingSet.descendingIterator(); iterator.hasNext();) {
            Integer i = iterator.next();
            assertEquals(expected.get(--cnt), i);
        }
        
        SortedSet<Integer> subSet = descendingSet.subSet(7, 3);
        assertEqualsSet(subSet, generateList(7, 6));
        
        subSet = descendingSet.headSet(3);
        assertEqualsSet(subSet, generateList(9, 7, 6));
        
        subSet = descendingSet.tailSet(7);
        assertEqualsSet(subSet, generateList(7, 6, 3, 1));
        
        subSet = descendingSet.tailSet(0);
        assertEqualsSet(subSet, generateList());
        
        assertArrayEquals(generateList(9, 7, 6, 3, 1).toArray(), descendingSet.toArray(new Integer[descendingSet.size()]));
        assertArrayEquals(generateList(9, 7, 6, 3, 1).toArray(), descendingSet.toArray());

        assertEquals(null, descendingSet.lower(9));
        assertEquals(9, (int) descendingSet.lower(7));
        assertEquals(7, (int) descendingSet.lower(6));
        assertEquals(6, (int) descendingSet.lower(3));
        assertEquals(3, (int) descendingSet.lower(1));
        assertEquals(1, (int) descendingSet.lower(0));

        assertEquals(9, (int) descendingSet.floor(9));
        assertEquals(7, (int) descendingSet.floor(7));
        assertEquals(6, (int) descendingSet.floor(6));
        assertEquals(6, (int) descendingSet.floor(5));
        assertEquals(3, (int) descendingSet.floor(3));
        assertEquals(1, (int) descendingSet.floor(1));
        assertEquals(1, (int) descendingSet.floor(0));
        
        assertNull(descendingSet.comparator());

        assertEquals(9, (int) descendingSet.first());
        assertEquals(1, (int) descendingSet.last());
        
        assertTrue(descendingSet.contains(9));
        assertTrue(descendingSet.contains(7));
        assertTrue(descendingSet.contains(3));
        assertFalse(descendingSet.contains(5));
        
        assertTrue(descendingSet.containsAll(generateList(9, 7, 6, 3, 1)));
    }

    @Test
    public void testB() throws Exception {
        Integer a = null;
        Number b = (Number) a;
    }
}