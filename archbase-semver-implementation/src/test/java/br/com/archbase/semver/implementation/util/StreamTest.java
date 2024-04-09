package br.com.archbase.semver.implementation.util;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;


public class StreamTest {

    @Test
    public void shouldBeBackedByArray() {
        Character[] input = {'a', 'b', 'c'};
        Stream<Character> stream = new Stream<Character>(input);
        assertArrayEquals(input, stream.toArray());
    }

    @Test
    public void shouldImplementIterable() {
        Character[] input = {'a', 'b', 'c'};
        Stream<Character> stream = new Stream<Character>(input);
        Iterator<Character> it = stream.iterator();
        for (Character chr : input) {
            assertEquals(chr, it.next());
        }
        assertFalse(it.hasNext());
    }

    @Test
    public void shouldNotReturnRealElementsArray() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        Character[] charArray = stream.toArray();
        charArray[0] = Character.valueOf('z');
        assertEquals(Character.valueOf('z'), charArray[0]);
        assertEquals(Character.valueOf('a'), stream.toArray()[0]);
    }

    @Test
    public void shouldReturnArrayOfElementsThatAreLeftInStream() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        stream.consume();
        stream.consume();
        assertEquals(1, stream.toArray().length);
        assertEquals(Character.valueOf('c'), stream.toArray()[0]);
    }

    @Test
    public void shouldConsumeElementsOneByOne() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        assertEquals(Character.valueOf('a'), stream.consume());
        assertEquals(Character.valueOf('b'), stream.consume());
        assertEquals(Character.valueOf('c'), stream.consume());
    }

    @Test
    public void shouldRaiseErrorWhenUnexpectedElementConsumed() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        try {
            stream.consume(new Stream.ElementType<Character>() {
                @Override
                public boolean isMatchedBy(Character element) {
                    return false;
                }
            });
        } catch (UnexpectedElementException e) {
            return;
        }
        fail("Deve gerar erro quando um tipo de elemento inesperado é consumido");
    }

    @Test
    public void shouldLookaheadWithoutConsuming() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        assertEquals(Character.valueOf('a'), stream.lookahead());
        assertEquals(Character.valueOf('a'), stream.lookahead());
    }

    @Test
    public void shouldLookaheadArbitraryNumberOfElements() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        assertEquals(Character.valueOf('a'), stream.lookahead(1));
        assertEquals(Character.valueOf('b'), stream.lookahead(2));
        assertEquals(Character.valueOf('c'), stream.lookahead(3));
    }

    @Test
    public void shouldCheckIfLookaheadIsOfExpectedTypes() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        assertTrue(stream.positiveLookahead(
                new Stream.ElementType<Character>() {
                    @Override
                    public boolean isMatchedBy(Character element) {
                        return element == 'a';
                    }
                }
        ));
        assertFalse(stream.positiveLookahead(
                new Stream.ElementType<Character>() {
                    @Override
                    public boolean isMatchedBy(Character element) {
                        return element == 'c';
                    }
                }
        ));
    }

    @Test
    public void shouldCheckIfElementOfExpectedTypesExistBeforeGivenType() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'1', '.', '0', '.', '0'}
        );
        assertTrue(stream.positiveLookaheadBefore(
                new Stream.ElementType<Character>() {
                    @Override
                    public boolean isMatchedBy(Character element) {
                        return element == '.';
                    }
                },
                new Stream.ElementType<Character>() {
                    @Override
                    public boolean isMatchedBy(Character element) {
                        return element == '1';
                    }
                }
        ));
        assertFalse(stream.positiveLookaheadBefore(
                new Stream.ElementType<Character>() {
                    @Override
                    public boolean isMatchedBy(Character element) {
                        return element == '1';
                    }
                },
                new Stream.ElementType<Character>() {
                    @Override
                    public boolean isMatchedBy(Character element) {
                        return element == '.';
                    }
                }
        ));
    }

    @Test
    public void shouldCheckIfElementOfExpectedTypesExistUntilGivenPosition() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'1', '.', '0', '.', '0'}
        );
        assertTrue(stream.positiveLookaheadUntil(
                3,
                new Stream.ElementType<Character>() {
                    @Override
                    public boolean isMatchedBy(Character element) {
                        return element == '0';
                    }
                }
        ));
        assertFalse(stream.positiveLookaheadUntil(
                3,
                new Stream.ElementType<Character>() {
                    @Override
                    public boolean isMatchedBy(Character element) {
                        return element == 'a';
                    }
                }
        ));
    }

    @Test
    public void shouldPushBackOneElementAtATime() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        assertEquals(Character.valueOf('a'), stream.consume());
        stream.pushBack();
        assertEquals(Character.valueOf('a'), stream.consume());
    }

    @Test
    public void shouldStopPushingBackWhenThereAreNoElements() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        assertEquals(Character.valueOf('a'), stream.consume());
        assertEquals(Character.valueOf('b'), stream.consume());
        assertEquals(Character.valueOf('c'), stream.consume());
        stream.pushBack();
        stream.pushBack();
        stream.pushBack();
        stream.pushBack();
        assertEquals(Character.valueOf('a'), stream.consume());
    }

    @Test
    public void shouldKeepTrackOfCurrentOffset() {
        Stream<Character> stream = new Stream<Character>(
                new Character[]{'a', 'b', 'c'}
        );
        assertEquals(0, stream.currentOffset());
        stream.consume();
        assertEquals(1, stream.currentOffset());
        stream.consume();
        stream.consume();
        assertEquals(3, stream.currentOffset());
    }
}
